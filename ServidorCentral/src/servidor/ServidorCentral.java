package servidor;

import modelo.Turno;
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

    // --- CONFIGURACIÓN DE REDUNDANCIA ---
    private static boolean esPrimario = true;
    private static String IP_RESPALDO = "127.0.0.2"; // Cambiar si usan otra IP
    private static int PUERTO_HEARTBEAT = 7000;

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("RESPALDO")) {
            esPrimario = false;
        }

        // --- VENTANITA DE CONFIGURACIÓN DEL SERVIDOR ---
        javax.swing.JTextField txtKiosco = new javax.swing.JTextField("5000");
        javax.swing.JTextField txtOperador = new javax.swing.JTextField("5001");
        javax.swing.JTextField txtIpPantalla = new javax.swing.JTextField("127.0.0.1");
        javax.swing.JTextField txtPtoPantalla = new javax.swing.JTextField("6000");
        javax.swing.JTextField txtIpRespaldo = new javax.swing.JTextField("127.0.0.2");

        Object[] message = {
                "Puerto de Escucha para Kioscos:", txtKiosco,
                "Puerto de Escucha para Operadores:", txtOperador,
                "IP de la Pantalla (Monitor):", txtIpPantalla,
                "Puerto de la Pantalla:", txtPtoPantalla,
                "IP del Servidor de Respaldo:", txtIpRespaldo
        };

        String titulo = esPrimario ? "Configuración Servidor PRIMARIO" : "Configuración Servidor RESPALDO";
        int option = javax.swing.JOptionPane.showConfirmDialog(null, message, titulo, javax.swing.JOptionPane.OK_CANCEL_OPTION);

        // Si el usuario cancela, matamos el servidor
        if (option != javax.swing.JOptionPane.OK_OPTION) {
            System.exit(0);
        }

        // Asignamos los puertos dinámicamente según lo que escribió el usuario
        PUERTO_KIOSCO = Integer.parseInt(txtKiosco.getText().trim());
        PUERTO_OPERADOR = Integer.parseInt(txtOperador.getText().trim());
        IP_PANTALLA = txtIpPantalla.getText().trim();
        PUERTO_PANTALLA = Integer.parseInt(txtPtoPantalla.getText().trim());
        IP_RESPALDO = txtIpRespaldo.getText().trim();

        if (esPrimario) {
            System.out.println("Servidor Central Iniciado (Modo PRIMARIO)...");
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

    // --- TÁCTICA: HEARTBEAT Y RESINCRONIZACIÓN (EMISOR EN EL PRIMARIO) ---
    private static void iniciarEmisorHeartbeat() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress ipRespaldo = InetAddress.getByName(IP_RESPALDO);
                while (true) {
                    StringBuilder estadoFila = new StringBuilder("SYNC:");
                    for (Turno t : filaEspera) estadoFila.append(t.getDniCliente()).append(",");
                    estadoFila.append("|");
                    for (Integer p : puestosActivos) estadoFila.append(p).append(",");

                    byte[] buffer = estadoFila.toString().getBytes();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, ipRespaldo, PUERTO_HEARTBEAT);
                    socket.send(packet);

                    Thread.sleep(2000); // Latido cada 2 segundos
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    // --- TÁCTICA: HEARTBEAT Y MONITOREO (RECEPTOR EN EL RESPALDO) ---
    private static void iniciarMonitorHeartbeat() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(PUERTO_HEARTBEAT)) {
                socket.setSoTimeout(5000); // Límite de 5 segundos sin latido
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
        iniciarServicios();
        iniciarEmisorHeartbeat(); // Ahora este manda los latidos
    }

    // --- TUS MÉTODOS ORIGINALES DE ATENCIÓN DE CLIENTES ---
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
                    filaEspera.add(new Turno(nuevoDni));
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
                    out.println("OK");
                }
            } else if ("DESCONECTAR".equals(accion)) {
                puestosActivos.remove(idPuesto);
                out.println("OK");
            } else if ("LLAMAR".equals(accion)) {
                if (filaEspera.isEmpty()) {
                    out.println("VACIA");
                } else {
                    Turno turno = filaEspera.poll();
                    turno.setPuestoAtencion(idPuesto);
                    turno.incrementarIntentos();
                    puestosAtencion.put(idPuesto, turno);
                    out.println("OK_" + turno.getDniCliente());
                    notificarPantalla("NUEVO_" + turno.getDniCliente() + "_" + idPuesto);
                }
            } else if ("RELLAMAR".equals(accion)) {
                Turno turno = puestosAtencion.get(idPuesto);
                if (turno != null) {
                    turno.incrementarIntentos();
                    if (turno.getIntentosLlamado() <= 3) {
                        out.println("OK_" + turno.getDniCliente());
                        notificarPantalla("URGENTE_" + turno.getDniCliente() + "_" + idPuesto);
                    } else {
                        puestosAtencion.remove(idPuesto);
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