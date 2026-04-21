package servidor;

import modelo.Turno;
import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServidorCentral {
    // RAM 100% preparada para concurrencia (múltiples kioscos y puestos a la vez)
    private static Queue<Turno> filaEspera = new ConcurrentLinkedQueue<>();
    private static Map<Integer, Turno> puestosAtencion = new ConcurrentHashMap<>();

    private static final int PUERTO_KIOSCO = 5000;
    private static final int PUERTO_OPERADOR = 5001;
    private static final String IP_PANTALLA = "127.0.0.1";
    private static final int PUERTO_PANTALLA = 6000;

    public static void main(String[] args) {
        System.out.println("Servidor Central Iniciado (Modo Multi-hilo)...");
        iniciarEscuchaKioscos();
        iniciarEscuchaOperadores();
    }

    private static void iniciarEscuchaKioscos() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PUERTO_KIOSCO)) {
                System.out.println("Escuchando Kioscos en puerto " + PUERTO_KIOSCO);
                while (true) {
                    Socket socketKiosco = serverSocket.accept();
                    // Ejecutamos cada petición en un hilo separado para que no bloquee a otros kioscos
                    new Thread(() -> manejarRegistroKiosco(socketKiosco)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void manejarRegistroKiosco(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void iniciarEscuchaOperadores() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PUERTO_OPERADOR)) {
                System.out.println("Escuchando Operadores en puerto " + PUERTO_OPERADOR);
                while (true) {
                    Socket socketOperador = serverSocket.accept();
                    // Ejecutamos en hilo separado. El Puesto 1 NUNCA bloqueará al Puesto 2.
                    new Thread(() -> manejarLlamadoOperador(socketOperador)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void manejarLlamadoOperador(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String comando = in.readLine();
            if (comando == null) return;

            String[] partes = comando.split("_");
            String accion = partes[0];
            int idPuesto = Integer.parseInt(partes[1]);

            if ("LLAMAR".equals(accion)) {
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
                    out.println("VACIA"); // Si toca rellamar pero el turno ya se eliminó
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void notificarPantalla(String mensaje) {
        try (Socket socket = new Socket(IP_PANTALLA, PUERTO_PANTALLA);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(mensaje);
        } catch (IOException e) {
            // Ignoramos el error en consola para no ensuciar si la pantalla no está abierta
        }
    }
}