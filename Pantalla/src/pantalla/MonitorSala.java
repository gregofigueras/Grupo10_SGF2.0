package pantalla;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class MonitorSala extends JFrame {

    private JLabel lblTurnoActual;
    private DefaultListModel<String> historialModel;
    private JList<String> listHistorial;

    // Lista para manejar la lógica interna de los 4 últimos turnos
    private LinkedList<String> historial;

    private static final int PUERTO_ESCUCHA_OPERADOR = 6000;

    public MonitorSala() {
        historial = new LinkedList<>();

        // 1. Configuración de la ventana principal
        setTitle("Monitor de Sala - Pantalla Pública");
        setSize(500, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null);

        // 2. Panel Superior: Turno Actual (El número más grande)
        JPanel panelActual = new JPanel(new BorderLayout());
        panelActual.setBackground(new Color(220, 240, 255));
        panelActual.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel lblTitulo = new JLabel("TURNO ACTUAL", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 28));
        panelActual.add(lblTitulo, BorderLayout.NORTH);

        lblTurnoActual = new JLabel("---", SwingConstants.CENTER);
        lblTurnoActual.setFont(new Font("Arial", Font.BOLD, 56));
        lblTurnoActual.setForeground(new Color(0, 102, 204));
        panelActual.add(lblTurnoActual, BorderLayout.CENTER);

        add(panelActual, BorderLayout.NORTH);

        // 3. Panel Inferior: Historial de los 4 anteriores
        JPanel panelHistorial = new JPanel(new BorderLayout());
        panelHistorial.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "Últimos Turnos Llamados",
                // Configuración de la fuente del título del borde
                0, 0, new Font("Arial", Font.BOLD, 14)));

        historialModel = new DefaultListModel<>();
        listHistorial = new JList<>(historialModel);
        listHistorial.setFont(new Font("Arial", Font.PLAIN, 20));
        listHistorial.setBackground(new Color(245, 245, 245));

        // El usuario no interactúa con la lista, es solo visual
        listHistorial.setEnabled(false);

        panelHistorial.add(new JScrollPane(listHistorial), BorderLayout.CENTER);
        add(panelHistorial, BorderLayout.CENTER);

        // 4. Iniciar el servidor en un hilo secundario para no congelar la pantalla
        iniciarServidorOperador();
    }

    private void iniciarServidorOperador() {
        Thread hiloServidor = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(PUERTO_ESCUCHA_OPERADOR);
                    System.out.println("Monitor de sala encendido. Escuchando en puerto " + PUERTO_ESCUCHA_OPERADOR);

                    while (true) {
                        // Esperamos a que el Operador presione "Llamar Siguiente"
                        Socket socketOperador = serverSocket.accept();

                        BufferedReader in = new BufferedReader(new InputStreamReader(socketOperador.getInputStream()));
                        String dniLlamado = in.readLine();

                        if (dniLlamado != null) {
                            // Actualizamos la interfaz gráfica de forma segura
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    actualizarPantalla(dniLlamado);
                                }
                            });
                        }

                        in.close();
                        socketOperador.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        hiloServidor.start();
    }

    private void actualizarPantalla(String nuevoDni) {
        String turnoAnterior = lblTurnoActual.getText();

        // Si ya había alguien siendo atendido, lo empujamos al historial
        if (!turnoAnterior.equals("---")) {
            historial.addFirst(turnoAnterior); // Lo agregamos arriba de todo

            // Si superamos los 4 registros históricos, borramos el más viejo para mantener 5 lugares en total
            if (historial.size() > 4) {
                historial.removeLast();
            }

            // Actualizamos la lista visual
            historialModel.clear();
            for (int i = 0; i < historial.size(); i++) {
                historialModel.addElement("      " + (i + 1) + ". DNI: " + historial.get(i));
            }
        }

        // Finalmente, actualizamos el número grande del turno actual
        lblTurnoActual.setText(nuevoDni);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MonitorSala().setVisible(true);
            }
        });
    }
}