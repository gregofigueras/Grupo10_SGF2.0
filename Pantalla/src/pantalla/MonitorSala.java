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
    private JLabel lblPuesto;
    private JPanel panelActual;
    private DefaultListModel<String> historialModel;
    private JList<String> listHistorial;

    private LinkedList<String> historial;
    private Thread hiloParpadeo; // Controlamos el parpadeo para que no se pisen

    private int puertoEscucha;

    // Paleta de colores oscuros
    private final Color COLOR_FONDO = new Color(15, 23, 42);
    private final Color COLOR_PANEL = new Color(30, 41, 59);
    private final Color COLOR_TEXTO_CLARO = new Color(241, 245, 249);
    private final Color COLOR_ROJO_TURNO = new Color(239, 68, 68);

    public MonitorSala() {
        if (!configurarPantalla()) {
            System.exit(0);
        }

        historial = new LinkedList<>();

        setTitle("Pantalla de Turnos");
        setSize(600, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(15, 15));
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_FONDO);

        // --- CONTENEDOR SUPERIOR ---
        JPanel panelNorte = new JPanel(new BorderLayout(10, 10));
        panelNorte.setBackground(COLOR_FONDO);

        JLabel lblHeader = new JLabel("Pantalla de Turnos", SwingConstants.CENTER);
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 30));
        lblHeader.setForeground(COLOR_TEXTO_CLARO);
        lblHeader.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        panelNorte.add(lblHeader, BorderLayout.NORTH);

        // --- PANEL CENTRAL (Turno Actual) ---
        panelActual = new JPanel(new GridLayout(3, 1));
        panelActual.setBackground(COLOR_PANEL);
        panelActual.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_ROJO_TURNO, 2),
                BorderFactory.createEmptyBorder(30, 20, 30, 20)
        ));

        JLabel lblSubtitulo = new JLabel("Turno Llamado", SwingConstants.LEFT);
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lblSubtitulo.setForeground(new Color(148, 163, 184));
        panelActual.add(lblSubtitulo);

        lblTurnoActual = new JLabel("---", SwingConstants.LEFT);
        lblTurnoActual.setFont(new Font("Segoe UI", Font.BOLD, 80));
        lblTurnoActual.setForeground(COLOR_ROJO_TURNO);
        panelActual.add(lblTurnoActual);

        lblPuesto = new JLabel("Diríjase al Puesto: ---", SwingConstants.RIGHT);
        lblPuesto.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblPuesto.setForeground(COLOR_TEXTO_CLARO);
        panelActual.add(lblPuesto);

        JPanel wrapperCentro = new JPanel(new BorderLayout());
        wrapperCentro.setBackground(COLOR_FONDO);
        wrapperCentro.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        wrapperCentro.add(panelActual, BorderLayout.CENTER);

        panelNorte.add(wrapperCentro, BorderLayout.CENTER);
        add(panelNorte, BorderLayout.NORTH);

        // --- PANEL INFERIOR (Historial) ---
        JPanel panelHistorial = new JPanel(new BorderLayout());
        panelHistorial.setBackground(COLOR_FONDO);
        panelHistorial.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JLabel lblHistorialTitulo = new JLabel("Anteriores");
        lblHistorialTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblHistorialTitulo.setForeground(new Color(148, 163, 184));
        lblHistorialTitulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panelHistorial.add(lblHistorialTitulo, BorderLayout.NORTH);

        historialModel = new DefaultListModel<>();
        listHistorial = new JList<>(historialModel);
        listHistorial.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        listHistorial.setBackground(COLOR_PANEL);
        listHistorial.setForeground(new Color(203, 213, 225));
        listHistorial.setFixedCellHeight(50);
        listHistorial.setEnabled(false);

        JScrollPane scrollPane = new JScrollPane(listHistorial);
        scrollPane.setBorder(BorderFactory.createLineBorder(COLOR_PANEL));
        panelHistorial.add(scrollPane, BorderLayout.CENTER);

        add(panelHistorial, BorderLayout.CENTER);

        iniciarServidorOperador();
    }

    private void iniciarServidorOperador() {
        Thread hiloServidor = new Thread(() -> {
            // Usamos la variable 'puertoEscucha' que el usuario ingresó
            try (ServerSocket serverSocket = new ServerSocket(puertoEscucha)) {
                System.out.println("Monitor escuchando en el puerto " + puertoEscucha);
                while (true) {
                    Socket socketOperador = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socketOperador.getInputStream()));
                    String mensaje = in.readLine();
                    if (mensaje != null) SwingUtilities.invokeLater(() -> procesarMensaje(mensaje));
                    in.close();
                    socketOperador.close();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: No se pudo abrir el puerto " + puertoEscucha);
            }
        });
        hiloServidor.start();
    }

    private void procesarMensaje(String mensaje) {
        try {
            String[] partes = mensaje.split("_");
            String tipo = partes[0];
            String dni = partes[1];
            String puesto = partes[2];

            // NUEVA LÓGICA: Si el servidor avisa que el turno fue descartado (Expiró)
            if ("DESCARTADO".equals(tipo)) {
                // Lo borramos si ya estaba en el historial (por algún motivo)
                historial.removeIf(s -> s.contains(dni));
                // Lo mandamos al historial con la etiqueta de EXPIRADO
                historial.addFirst("   " + dni + "                    EXPIRADO");
                if (historial.size() > 4) historial.removeLast();

                historialModel.clear();
                for (String s : historial) historialModel.addElement(s);

                // Si el turno que expiró es el que está en el recuadro grande, lo limpiamos
                if (lblTurnoActual.getText().equals(dni)) {
                    if (hiloParpadeo != null && hiloParpadeo.isAlive()) hiloParpadeo.interrupt();
                    lblTurnoActual.setText("---");
                    lblPuesto.setText("Diríjase al Puesto: ---");
                    panelActual.setBackground(COLOR_PANEL);
                    lblTurnoActual.setForeground(COLOR_ROJO_TURNO);
                }
                return; // Cortamos la ejecución acá para este mensaje
            }

            // --- LÓGICA NORMAL (NUEVO O URGENTE) ---
            String turnoAnterior = lblTurnoActual.getText();
            String puestoAnteriorStr = lblPuesto.getText().replaceAll("\\D+","");

            if (!turnoAnterior.equals("---") && !turnoAnterior.equals(dni)) {
                historial.removeIf(s -> s.contains(turnoAnterior));
                historial.addFirst("   " + turnoAnterior + "                    Puesto " + puestoAnteriorStr);
                if (historial.size() > 4) historial.removeLast();
            }

            historial.removeIf(s -> s.contains(dni));

            historialModel.clear();
            for (String s : historial) historialModel.addElement(s);

            lblTurnoActual.setText(dni);
            lblPuesto.setText("Diríjase al Puesto " + puesto);

            if ("URGENTE".equals(tipo)) {
                efectoParpadeo();
            } else {
                if (hiloParpadeo != null && hiloParpadeo.isAlive()) hiloParpadeo.interrupt();
                panelActual.setBackground(COLOR_PANEL);
                lblTurnoActual.setForeground(COLOR_ROJO_TURNO);
            }
        } catch (Exception e) {
            System.out.println("Formato desconocido: " + mensaje);
        }
    }

    private void efectoParpadeo() {
        // Interrumpimos parpadeo anterior si es que alguien tocó re-llamar dos veces rápido
        if (hiloParpadeo != null && hiloParpadeo.isAlive()) {
            hiloParpadeo.interrupt();
        }
        hiloParpadeo = new Thread(() -> {
            try {
                for (int i = 0; i < 4; i++) {
                    panelActual.setBackground(new Color(153, 27, 27));
                    lblTurnoActual.setForeground(Color.WHITE);
                    Thread.sleep(250);
                    panelActual.setBackground(COLOR_PANEL);
                    lblTurnoActual.setForeground(COLOR_ROJO_TURNO);
                    Thread.sleep(250);
                }
            } catch (InterruptedException ignored) {
                // Si lo interrumpen, vuelve al color normal
                panelActual.setBackground(COLOR_PANEL);
                lblTurnoActual.setForeground(COLOR_ROJO_TURNO);
            }
        });
        hiloParpadeo.start();
    }
    private boolean configurarPantalla() {
        JTextField txtPuerto = new JTextField("6000");
        Object[] message = {"Puerto de Escucha para Notificaciones:", txtPuerto};

        int option = JOptionPane.showConfirmDialog(null, message, "Configuración del Monitor", JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION) {
            return false;
        }

        try {
            this.puertoEscucha = Integer.parseInt(txtPuerto.getText().trim());
            return true;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Puerto inválido.");
            return false;
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MonitorSala().setVisible(true));
    }
}