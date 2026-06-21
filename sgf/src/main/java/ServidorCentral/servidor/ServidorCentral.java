package ServidorCentral.servidor;

import Globales.Turno;
import ServidorCentral.persistencia.*;
import ServidorCentral.seguridad.Encriptador;

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

    private static int PUERTO_KIOSCO;
    private static int PUERTO_OPERADOR;
    private static String IP_PANTALLA;
    private static int PUERTO_PANTALLA;
    private static boolean esPrimario;
    private static String IP_RESPALDO;
    private static int PUERTO_HEARTBEAT = 7000;

    private static Encriptador encriptador;

    private static PersistenciaFactory fabricaPersistencia;
    private static IColaEsperaDAO colaEsperaDAO;
    private static IHistorialLlamadosDAO historialDAO;
    private static IRenotificacionDAO renotificacionDAO;

    // ESTE MÉTODO REEMPLAZA AL ANTIGUO MAIN
    public void iniciar(boolean primario, int pKiosco, int pOperador, String ipPan, int pPan, String ipRes, String formato, int clave, String idNodo) {
        esPrimario = primario;
        ConfigPersistencia.setSufijo("_" + idNodo);
        PUERTO_KIOSCO = pKiosco;
        PUERTO_OPERADOR = pOperador;
        IP_PANTALLA = ipPan;
        PUERTO_PANTALLA = pPan;
        IP_RESPALDO = ipRes;

        encriptador = new Encriptador(clave);

        if ("XML".equals(formato)) {
            fabricaPersistencia = new XMLFactory();
        } else if ("TXT".equals(formato)) {
            fabricaPersistencia = new TextoPlanoFactory();
        } else {
            fabricaPersistencia = new JSONFactory();
        }

        colaEsperaDAO = fabricaPersistencia.crearColaEsperaDAO();
        historialDAO = fabricaPersistencia.crearHistorialLlamadosDAO();
        renotificacionDAO = fabricaPersistencia.crearRenotificacionDAO();

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
                InetAddress ipResp = InetAddress.getByName(IP_RESPALDO);
                while (true) {
                    byte[] buffer = "PING".getBytes();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, ipResp, PUERTO_HEARTBEAT);
                    socket.send(packet);
                    Thread.sleep(2000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void enviarMensajeAlRespaldo(String mensaje) {
        if (!esPrimario)
            return;
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress ipResp = InetAddress.getByName(IP_RESPALDO);
            byte[] buffer = mensaje.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, ipResp, PUERTO_HEARTBEAT);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void notificarCambioAlRespaldo() {
        if (!esPrimario)
            return;
        StringBuilder estadoFila = new StringBuilder("SYNC:");
        for (Turno t : filaEspera)
            estadoFila.append(t.getDniCliente()).append(",");
        estadoFila.append("|");
        for (Integer p : puestosActivos)
            estadoFila.append(p).append(",");

        enviarMensajeAlRespaldo(estadoFila.toString());
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void actualizarEstadoDesdePrimario(String mensaje) {
        if ("PING".equals(mensaje)) {
            return;
        }

        if (mensaje.startsWith("SYNC:")) {
            filaEspera.clear();
            puestosActivos.clear();

            // Ojo: esto no vacía toda la persistencia, solo consume 1 elemento.
            if (colaEsperaDAO != null) {
                colaEsperaDAO.getSiguiente();
            }

            String[] partes = mensaje.substring(5).split("\\|");

            if (partes.length > 0 && !partes[0].isEmpty()) {
                String[] dnis = partes[0].split(",");
                for (String dni : dnis) {
                    if (!dni.trim().isEmpty()) {
                        Turno t = new Turno(dni);
                        filaEspera.add(t);

                        // El respaldo también persiste
                        if (colaEsperaDAO != null) {
                            colaEsperaDAO.guardarTurno(t);
                        }
                    }
                }
            }

            if (partes.length > 1 && !partes[1].isEmpty()) {
                String[] puestos = partes[1].split(",");
                for (String p : puestos) {
                    if (!p.trim().isEmpty()) {
                        puestosActivos.add(Integer.parseInt(p));
                    }
                }
            }

        } else if (mensaje.startsWith("HISTORIAL_ADD:")) {
            Turno t = parsearTurnoMensaje(mensaje.substring(14));
            if (t != null && historialDAO != null) historialDAO.registrarLlamado(t);
        } else if (mensaje.startsWith("HISTORIAL_UPD:")) {
            Turno t = parsearTurnoMensaje(mensaje.substring(14));
            if (t != null && historialDAO != null) historialDAO.actualizarLlamado(t);
        } else if (mensaje.startsWith("RENOTIF_ADD:")) {
            String dni = mensaje.substring(12);
            if (renotificacionDAO != null) renotificacionDAO.registrarIntentoReintentado(dni);
        } else if (mensaje.startsWith("RENOTIF_DEL:")) {
            String dni = mensaje.substring(12);
            if (renotificacionDAO != null) renotificacionDAO.limpiarHistorialIntentos(dni);
        }
    }

    private static Turno parsearTurnoMensaje(String datos) {
        String[] campos = datos.split(";");
        if (campos.length >= 3) {
            Turno t = new Turno(campos[0]);
            t.setPuestoAtencion(Integer.parseInt(campos[1]));
            int intentos = Integer.parseInt(campos[2]);
            for (int i = 0; i < intentos; i++) {
                t.incrementarIntentos();
            }
            if (campos.length > 3) {
                t.setExpirado(Boolean.parseBoolean(campos[3]));
            }
            return t;
        }
        return null;
    }

    private static void asumirRolPrimario() {
        System.out.println("Asumiendo rol de SERVIDOR PRIMARIO...");
        esPrimario = true;

        if (colaEsperaDAO != null) {
            Queue<Turno> pendientes = colaEsperaDAO.getTodosPendientes();
            if (pendientes != null && !pendientes.isEmpty()) {
                filaEspera.clear();
                filaEspera.addAll(pendientes);
                System.out.println("[Failover] Estado sincronizado desde el disco. Turnos en espera: " + filaEspera.size());
            }
        }

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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void manejarRegistroKiosco(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String msjCifrado = in.readLine();
            if (msjCifrado != null) {
                // DESENCRIPTA EL DNI QUE LLEGA
                String nuevoDni = encriptador.desencriptar(msjCifrado);

                boolean existe = filaEspera.stream().anyMatch(t -> t.getDniCliente().equals(nuevoDni));

                if (existe) {
                    // ENCRIPTA LA RESPUESTA
                    out.println(encriptador.encriptar("DUPLICADO"));
                } else {
                    Turno nuevoTurno = new Turno(nuevoDni);
                    filaEspera.add(nuevoTurno);

                    // Tanto primario como respaldo persisten
                    if (colaEsperaDAO != null) {
                        colaEsperaDAO.guardarTurno(nuevoTurno);
                    }

                    notificarCambioAlRespaldo();

                    // ENCRIPTA LA RESPUESTA
                    out.println(encriptador.encriptar("OK"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void iniciarEscuchaOperadores() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PUERTO_OPERADOR)) {
                System.out.println("Escuchando Operadores en puerto " + PUERTO_OPERADOR);
                while (true) {
                    Socket socketOperador = serverSocket.accept();
                    new Thread(() -> manejarLlamadoOperador(socketOperador)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void manejarLlamadoOperador(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String cifrado = in.readLine();
            if (cifrado == null)
                return;

            // DESENCRIPTA EL COMANDO DEL OPERADOR
            String comando = encriptador.desencriptar(cifrado);
            String[] partes = comando.split("_");
            String accion = partes[0];
            int idPuesto = Integer.parseInt(partes[1]);

            if ("REGISTRAR".equals(accion)) {
                if (puestosActivos.contains(idPuesto)) {
                    out.println(encriptador.encriptar("DUPLICADO"));
                } else {
                    puestosActivos.add(idPuesto);
                    notificarCambioAlRespaldo();
                    out.println(encriptador.encriptar("OK"));
                }
            } else if ("DESCONECTAR".equals(accion)) {
                puestosActivos.remove(idPuesto);
                notificarCambioAlRespaldo();
                out.println(encriptador.encriptar("OK"));
            } else if ("LLAMAR".equals(accion)) {
                if (filaEspera.isEmpty()) {
                    out.println(encriptador.encriptar("VACIA"));
                } else {
                    Turno turno = filaEspera.poll();
                    turno.setPuestoAtencion(idPuesto);
                    turno.incrementarIntentos();

                    // Solo primario persiste y avisa al respaldo
                    if (colaEsperaDAO != null) {
                        colaEsperaDAO.getSiguiente();
                    }
                    if (historialDAO != null) {
                        historialDAO.registrarLlamado(turno);
                        enviarMensajeAlRespaldo("HISTORIAL_ADD:" + turno.getDniCliente() + ";" + turno.getPuestoAtencion() + ";" + turno.getIntentosLlamado() + ";" + turno.isExpirado());
                    }

                    notificarCambioAlRespaldo();
                    puestosAtencion.put(idPuesto, turno);

                    out.println(encriptador.encriptar("OK_" + turno.getDniCliente()));
                    notificarPantalla("NUEVO_" + turno.getDniCliente() + "_" + idPuesto);
                }
            } else if ("RELLAMAR".equals(accion)) {
                Turno turno = puestosAtencion.get(idPuesto);
                if (turno != null) {
                    turno.incrementarIntentos();

                    // Solo primario persiste y avisa al respaldo
                    if (renotificacionDAO != null) {
                        renotificacionDAO.registrarIntentoReintentado(turno.getDniCliente());
                        enviarMensajeAlRespaldo("RENOTIF_ADD:" + turno.getDniCliente());
                    }
                    if (historialDAO != null) {
                        historialDAO.actualizarLlamado(turno);
                        enviarMensajeAlRespaldo("HISTORIAL_UPD:" + turno.getDniCliente() + ";" + turno.getPuestoAtencion() + ";" + turno.getIntentosLlamado() + ";" + turno.isExpirado());
                    }

                    if (turno.getIntentosLlamado() <= 3) {
                        out.println(encriptador.encriptar("OK_" + turno.getDniCliente()));
                        notificarPantalla("URGENTE_" + turno.getDniCliente() + "_" + idPuesto);
                    } else {
                        puestosAtencion.remove(idPuesto);
                        turno.setExpirado(true);

                        // Solo primario persiste y avisa al respaldo
                        if (renotificacionDAO != null) {
                            renotificacionDAO.limpiarHistorialIntentos(turno.getDniCliente());
                            enviarMensajeAlRespaldo("RENOTIF_DEL:" + turno.getDniCliente());
                        }
                        if (historialDAO != null) {
                            historialDAO.actualizarLlamado(turno);
                            enviarMensajeAlRespaldo("HISTORIAL_UPD:" + turno.getDniCliente() + ";" + turno.getPuestoAtencion() + ";" + turno.getIntentosLlamado() + ";" + turno.isExpirado());
                        }

                        out.println(encriptador.encriptar("DESCARTADO"));
                        notificarPantalla("DESCARTADO_" + turno.getDniCliente() + "_" + idPuesto);
                    }
                } else {
                    out.println(encriptador.encriptar("VACIA"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void notificarPantalla(String mensaje) {
        try (Socket socket = new Socket(IP_PANTALLA, PUERTO_PANTALLA);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // ENCRIPTA EL MENSAJE HACIA EL MONITOR PÚBLICO
            out.println(encriptador.encriptar(mensaje));
        } catch (IOException e) {
        }
    }
}
