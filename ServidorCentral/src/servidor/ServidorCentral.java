package servidor;

import modelo.Turno;
import persistencia.*;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServidorCentral {
    private static Queue<Turno> filaEspera = new ConcurrentLinkedQueue<>();
    private static Map<Integer, Turno> puestosAtencion = new ConcurrentHashMap<>();
    private static java.util.Set<Integer> puestosActivos = ConcurrentHashMap.newKeySet();

    private static int PUERTO_KIOSCO = 5000;
    private static int PUERTO_OPERADOR = 5001;
    private static String IP_PANTALLA = "127.0.0.1";
    private static int PUERTO_PANTALLA = 6000;

    private static boolean esPrimario = true;
    private static String IP_RESPALDO = "127.0.0.2";
    private static int PUERTO_HEARTBEAT = 7000;

    // --- VARIABLES DE PERSISTENCIA (NUEVO PATRÓN DAO) ---
    private static PersistenciaFactory fabricaPersistencia;
    private static IColaEsperaDAO colaEsperaDAO;
    private static IHistorialLlamadosDAO historialDAO;
    private static IRenotificacionDAO renotificacionDAO;

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("RESPALDO")) {
            esPrimario = false;
        }

        // --- VENTANITA DE CONFIGURACIÓN ---
        javax.swing.JTextField txtKiosco = new javax.swing.JTextField("5000");
        javax.swing.JTextField txtOperador = new javax.swing.JTextField("5001");
        javax.swing.JTextField txtIpPantalla = new javax.swing.JTextField("127.0.0.1");
        javax.swing.JTextField txtPtoPantalla = new javax.swing.JTextField("6000");
        javax.swing.JTextField txtIpRespaldo = new javax.swing.JTextField("127.0.0.2");

        String[] opcionesFormato = {"JSON", "XML", "TXT"};
        javax.swing.JComboBox<String> cmbFormato = new javax.swing.JComboBox<>(opcionesFormato);

        Object[] message = {
                "Puerto de Escucha para Kioscos:", txtKiosco,
                "Puerto de Escucha para Operadores:", txtOperador,
                "IP de la Pantalla (Monitor):", txtIpPantalla,
                "Puerto de la Pantalla:", txtPtoPantalla,
                "IP del Servidor de Respaldo:", txtIpRespaldo,
                "Formato de Backup (RF-05):", cmbFormato
        };

        String titulo = esPrimario ? "Configuración Servidor PRIMARIO" : "Configuración Servidor RESPALDO";
        int option = javax.swing.JOptionPane.showConfirmDialog(null, message, titulo, javax.swing.JOptionPane.OK_CANCEL_OPTION);

        if (option != javax.swing.JOptionPane.OK_OPTION) System.exit(0);

        PUERTO_KIOSCO = Integer.parseInt(txtKiosco.getText().trim());
        PUERTO_OPERADOR = Integer.parseInt(txtOperador.getText().trim());
        IP_PANTALLA = txtIpPantalla.getText().trim();
        PUERTO_PANTALLA = Integer.parseInt(txtPtoPantalla.getText().trim());
        IP_RESPALDO = txtIpRespaldo.getText().trim();

        // --- CONFIGURACIÓN DE PERSISTENCIA (ABSTRACT FACTORY) ---
        String formatoElegido = (String) cmbFormato.getSelectedItem();
        if ("XML".equals(formatoElegido)) {
            fabricaPersistencia = new XMLFactory();
        } else if ("TXT".equals(formatoElegido)) {
            fabricaPersistencia = new TextoPlanoFactory();
        } else {
            fabricaPersistencia = new JSONFactory();
        }

        // Inicializamos los DAOs para cualquier rol (para poder recuperar al asumir como primario)
        colaEsperaDAO = fabricaPersistencia.crearColaEsperaDAO();
        historialDAO = fabricaPersistencia.crearHistorialLlamadosDAO();
        renotificacionDAO = fabricaPersistencia.crearRenotificacionDAO();

        // --- ARRANQUE ---
        if (esPrimario) {
            System.out.println("Servidor Central Iniciado (Modo PRIMARIO)...");

            // Carga inicial del estado
            Queue<Turno> pendientes = colaEsperaDAO.getTodosPendientes();
            if (pendientes != null && !pendientes.isEmpty()) {
                filaEspera.addAll(pendientes);
                System.out.println("[Recuperación] Backup cargado. Turnos en espera: " + filaEspera.size());
            }

            iniciarServicios();
            iniciarEmisorHeartbeat();
        } else {
            System.out.println("Servidor Central Iniciado (Modo RESPALDO - Warm Standby)...");
            iniciarMonitorHeartbeat();
        }
    }

    private static void iniciarServicios() {
        iniciarEscuchaKioscos();
        iniciarEscuchaOperadores();
    }

    private static void iniciarEmisorHeartbeat() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress ipRespaldo = InetAddress.getByName(IP_RESPALDO);
                while (true) {
                    byte[] buffer = "PING".getBytes();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, ipRespaldo, PUERTO_HEARTBEAT);
                    socket.send(packet);
                    Thread.sleep(2000);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private static void notificarCambioAlRespaldo() {
        if (!esPrimario) return;

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress ipRespaldo = InetAddress.getByName(IP_RESPALDO);

            StringBuilder estadoFila = new StringBuilder("SYNC:");
            for (Turno t : filaEspera) estadoFila.append(t.getDniCliente()).append(",");
            estadoFila.append("|");
            for (Integer p : puestosActivos) estadoFila.append(p).append(",");

            byte[] buffer = estadoFila.toString().getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, ipRespaldo, PUERTO_HEARTBEAT);
            socket.send(packet);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void iniciarMonitorHeartbeat() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(PUERTO_HEARTBEAT)) {
                socket.setSoTimeout(5000);
                byte[] buffer = new byte[1024];

                while (!esPrimario) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        String mensaje = new String(packet.getData(), 0, packet.getLength());
                        actualizarEstadoDesdePrimario(mensaje);
                    } catch (SocketTimeoutException timeout) {
                        System.out.println("¡ALERTA! Se perdió conexión con el Servidor Primario.");
                        asumirRolPrimario();
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private static void actualizarEstadoDesdePrimario(String mensaje) {
        if ("PING".equals(mensaje)) return;

        if (mensaje.startsWith("SYNC:")) {
            filaEspera.clear();
            puestosActivos.clear();
            String[] partes = mensaje.substring(5).split("\\|");

            if (partes.length > 0) {
                String[] dnis = partes[0].split(",");
                for (String dni : dnis) {
                    if (!dni.trim().isEmpty()) filaEspera.add(new Turno(dni));
                }
            }
            if (partes.length > 1) {
                String[] puestos = partes[1].split(",");
                for (String p : puestos) {
                    if (!p.trim().isEmpty()) puestosActivos.add(Integer.parseInt(p));
                }
            }
        }
    }

    private static void asumirRolPrimario() {
        System.out.println("Asumiendo rol de SERVIDOR PRIMARIO...");
        esPrimario = true;

        // --- EL FIX: Recuperamos la verdad absoluta desde el DAO (Disco) ---
        if (colaEsperaDAO != null) {
            Queue<Turno> pendientes = colaEsperaDAO.getTodosPendientes();
            if (pendientes != null && !pendientes.isEmpty()) {
                filaEspera.clear(); // Limpiamos la RAM por las dudas
                filaEspera.addAll(pendientes); // Cargamos lo que dice el disco
                System.out.println("[Failover] Estado sincronizado desde el disco. Turnos en espera: " + filaEspera.size());
            }
        }
        // -------------------------------------------------------------------

        iniciarServicios();
        iniciarEmisorHeartbeat();
    }

    private static void iniciarEscuchaKioscos() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PUERTO_KIOSCO)) {
                System.out.println("Escuchando Kioscos en puerto " + PUERTO_KIOSCO);
                while (true) {
                    Socket socketKiosco = serverSocket.accept();
                    new Thread(() -> manejarRegistroKiosco(socketKiosco)).start();
                }
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    private static void manejarRegistroKiosco(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            String nuevoDni = in.readLine();
            if (nuevoDni != null) {
                boolean existe = filaEspera.stream().anyMatch(t -> t.getDniCliente().equals(nuevoDni));
                if (existe) {
                    out.println("DUPLICADO");
                } else {
                    Turno nuevoTurno = new Turno(nuevoDni);
                    filaEspera.add(nuevoTurno);

                    // --- PATRÓN DAO: Guardar en cola_espera.json ---
                    if (esPrimario && colaEsperaDAO != null) {
                        colaEsperaDAO.guardarTurno(nuevoTurno);
                    }

                    notificarCambioAlRespaldo();
                    out.println("OK");
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static void iniciarEscuchaOperadores() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PUERTO_OPERADOR)) {
                System.out.println("Escuchando Operadores en puerto " + PUERTO_OPERADOR);
                while (true) {
                    Socket socketOperador = serverSocket.accept();
                    new Thread(() -> manejarLlamadoOperador(socketOperador)).start();
                }
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    private static void manejarLlamadoOperador(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            String comando = in.readLine();
            if (comando == null) return;

            String[] partes = comando.split("_");
            String accion = partes[0];
            int idPuesto = Integer.parseInt(partes[1]);

            if ("REGISTRAR".equals(accion)) {
                if (puestosActivos.contains(idPuesto)) {
                    out.println("DUPLICADO");
                } else {
                    puestosActivos.add(idPuesto);
                    notificarCambioAlRespaldo();
                    out.println("OK");
                }
            } else if ("DESCONECTAR".equals(accion)) {
                puestosActivos.remove(idPuesto);
                notificarCambioAlRespaldo();
                out.println("OK");
            } else if ("LLAMAR".equals(accion)) {
                if (filaEspera.isEmpty()) {
                    out.println("VACIA");
                } else {
                    Turno turno = filaEspera.poll();
                    turno.setPuestoAtencion(idPuesto);
                    turno.incrementarIntentos();

                    // --- PATRÓN DAO: Actualizar cola y guardar historial ---
                    if (esPrimario && colaEsperaDAO != null) {
                        colaEsperaDAO.getSiguiente(); // Actualiza el archivo de la cola
                        historialDAO.registrarLlamado(turno); // Guarda en historial_llamados.json
                    }

                    notificarCambioAlRespaldo();
                    puestosAtencion.put(idPuesto, turno);
                    out.println("OK_" + turno.getDniCliente());
                    notificarPantalla("NUEVO_" + turno.getDniCliente() + "_" + idPuesto);
                }
            } else if ("RELLAMAR".equals(accion)) {
                Turno turno = puestosAtencion.get(idPuesto);
                if (turno != null) {
                    turno.incrementarIntentos();

                    // --- PATRÓN DAO: Registrar reintento ---
                    if (esPrimario && renotificacionDAO != null) {
                        renotificacionDAO.registrarIntentoReintentado(turno.getDniCliente());
                    }

                    if (turno.getIntentosLlamado() <= 3) {
                        out.println("OK_" + turno.getDniCliente());
                        notificarPantalla("URGENTE_" + turno.getDniCliente() + "_" + idPuesto);
                    } else {
                        puestosAtencion.remove(idPuesto);

                        // --- PATRÓN DAO: Limpiar reintentos si se descarta ---
                        if (esPrimario && renotificacionDAO != null) {
                            renotificacionDAO.limpiarHistorialIntentos(turno.getDniCliente());
                        }

                        out.println("DESCARTADO");
                        notificarPantalla("DESCARTADO_" + turno.getDniCliente() + "_" + idPuesto);
                    }
                } else {
                    out.println("VACIA");
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void notificarPantalla(String mensaje) {
        try (Socket socket = new Socket(IP_PANTALLA, PUERTO_PANTALLA);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(mensaje);
        } catch (IOException e) { }
    }
}