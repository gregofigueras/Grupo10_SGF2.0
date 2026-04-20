package servidor;

import modelo.Turno;
import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.Queue;

public class ServidorCentral {
    // La memoria RAM centralizada que pide el TP
    private static Queue<Turno> filaEspera = new LinkedList<>();

    private static final int PUERTO_KIOSCO = 5000;
    // Luego agregaremos los puertos para Operador y Pantalla

    public static void main(String[] args) {
        System.out.println("Servidor Central Iniciado...");
        iniciarEscuchaKioscos();
        iniciarEscuchaOperadores();
    }

    private static void iniciarEscuchaKioscos() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PUERTO_KIOSCO)) {
                System.out.println("Escuchando Kioscos en puerto " + PUERTO_KIOSCO);
                while (true) {
                    Socket socketKiosco = serverSocket.accept();
                    manejarRegistroKiosco(socketKiosco);
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
                // Validación de Duplicados (movida desde el Operador al Servidor Central)
                boolean existe = filaEspera.stream().anyMatch(t -> t.getDniCliente().equals(nuevoDni));

                if (existe) {
                    out.println("DUPLICADO");
                    System.out.println("Rechazado: DNI " + nuevoDni + " ya está en fila.");
                } else {
                    filaEspera.add(new Turno(nuevoDni));
                    out.println("OK");
                    System.out.println("Registrado: DNI " + nuevoDni + ". Total en fila: " + filaEspera.size());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static final int PUERTO_OPERADOR = 5001;
    private static final String IP_PANTALLA = "127.0.0.1";
    private static final int PUERTO_PANTALLA = 6000;

    // (Tu main y el iniciarEscuchaKioscos() quedan igual, pero agrega esta llamada al main:)
    // iniciarEscuchaOperadores();

    private static void iniciarEscuchaOperadores() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PUERTO_OPERADOR)) {
                System.out.println("Escuchando Operadores en puerto " + PUERTO_OPERADOR);
                while (true) {
                    Socket socketOperador = serverSocket.accept();
                    manejarLlamadoOperador(socketOperador);
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

            if ("LLAMAR".equals(comando)) {
                if (filaEspera.isEmpty()) {
                    out.println("VACIA"); // Le avisa al Operador que no hay nadie
                } else {
                    Turno turno = filaEspera.poll(); // Saca de la RAM
                    out.println(turno.getDniCliente()); // Le pasa el DNI al Operador

                    // EL SERVIDOR LE MANDA EL MENSAJE A LA PANTALLA (Cumple con el TP)
                    notificarPantalla(turno.getDniCliente());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void notificarPantalla(String dni) {
        try (Socket socket = new Socket(IP_PANTALLA, PUERTO_PANTALLA);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(dni);
        } catch (IOException e) {
            System.out.println("Error al notificar a la Pantalla. ¿Está encendida el MonitorSala?");
        }
    }
}