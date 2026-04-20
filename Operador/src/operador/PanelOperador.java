package operador;

import modelo.Turno; // Importamos la entidad de dominio

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

public class PanelOperador extends JFrame {

    // Lógica de la fila (Memoria RAM, FIFO) usando la clase Turno
    private Queue<Turno> filaEspera;

    // Interfaz gráfica
    private JLabel lblEstado;
    private JLabel lblTurnoActual;
    private JButton btnLlamar;

    // Configuración de reddale
    private static final int PUERTO_ESCUCHA_KIOSCO = 5000;
    private static final String IP_PANTALLA = "127.0.0.1";
    private static final int PUERTO_PANTALLA = 6000;

    public PanelOperador() {
        filaEspera = new LinkedList<>(); // Estructura de datos FIFO

        // 1. Configuramos la ventana principal [cite: 243]
        setTitle("Panel de Operador - Puesto Único");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(4, 1, 10, 10));
        setLocationRelativeTo(null); // Centra la ventana

        // 2. Componentes visuales
        lblEstado = new JLabel("Personas en fila: 0", SwingConstants.CENTER);
        lblEstado.setFont(new Font("Arial", Font.BOLD, 16));
        add(lblEstado);

        lblTurnoActual = new JLabel("Atendiendo a: Nadie", SwingConstants.CENTER);
        lblTurnoActual.setFont(new Font("Arial", Font.PLAIN, 18));
        add(lblTurnoActual);

        btnLlamar = new JButton("Llamar Siguiente");
        btnLlamar.setFont(new Font("Arial", Font.BOLD, 14));
        add(btnLlamar);

        // 3. Acción del botón "Llamar Siguiente" (Actúa como Cliente hacia la Pantalla)
        btnLlamar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                llamarSiguiente();
            }
        });

        // 4. Iniciamos el hilo que escucha al Kiosco en segundo plano
        iniciarServidorKiosco();
    }

    private void llamarSiguiente() {
        if (filaEspera.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay nadie en la fila de espera.");
            return;
        }

        // Lógica FIFO: Extrae al cliente más antiguo de la fila como un objeto Turno
        Turno turnoLlamado = filaEspera.poll();

        lblTurnoActual.setText("Atendiendo a: " + turnoLlamado.getDniCliente());
        actualizarContador();

        // COMUNICACIÓN POR RED (Cliente TCP hacia el Monitor de Sala)
        try {
            Socket socket = new Socket(IP_PANTALLA, PUERTO_PANTALLA);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(turnoLlamado.getDniCliente()); // Enviamos el DNI a la pantalla
            out.close();
            socket.close();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al comunicar con el Monitor de Sala. ¿Está encendido?");
        }
    }

    // Este método corre en un Hilo separado para no congelar la ventana gráfica
    private void iniciarServidorKiosco() {
        Thread hiloServidor = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Abrimos el puerto para escuchar a los kioscos
                    ServerSocket serverSocket = new ServerSocket(PUERTO_ESCUCHA_KIOSCO);
                    System.out.println("Operador escuchando en puerto " + PUERTO_ESCUCHA_KIOSCO);

                    while (true) {
                        // El método accept() bloquea la ejecución hasta que un cliente socket se conecte[cite: 360].
                        Socket socketCliente = serverSocket.accept();

                        // Leemos el DNI que mandó el Kiosco
                        BufferedReader in = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));

                        // NUEVO: Canal de salida para responderle al Kiosco
                        PrintWriter out = new PrintWriter(socketCliente.getOutputStream(), true);

                        String nuevoDni = in.readLine();

                        if (nuevoDni != null) {
                            // Validamos si ya existe en la cola
                            boolean existe = false;
                            for (Turno t : filaEspera) {
                                if (t.getDniCliente().equals(nuevoDni)) {
                                    existe = true;
                                    break;
                                }
                            }

                            if (existe) {
                                // Le avisamos al Kiosco que rebotó
                                out.println("DUPLICADO");
                            } else {
                                // Lo agregamos y le damos el OK
                                filaEspera.add(new Turno(nuevoDni));
                                out.println("OK");

                                // Actualizamos la interfaz gráfica de forma segura
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        actualizarContador();
                                    }
                                });
                            }
                        }

                        // Cerramos los canales y el socket
                        out.close();
                        in.close();
                        socketCliente.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        hiloServidor.start(); // Iniciamos el hilo paralelo
    }

    private void actualizarContador() {
        lblEstado.setText("Personas en fila: " + filaEspera.size());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new PanelOperador().setVisible(true);
            }
        });
    }
}