package kiosco;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Kiosco extends JFrame {

    private JTextField txtDni;
    private JButton btnSolicitar;
    private JPanel panelMensaje;
    private JLabel lblMensajeTitulo;
    private JLabel lblMensajeDetalle;

    private static final String IP_OPERADOR = "127.0.0.1";
    private static final int PUERTO_OPERADOR = 5000;

    public Kiosco() {
        setTitle("Terminal de Registro");
        setSize(360, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.WHITE);

        // --- ENCABEZADO AZUL ---
        JPanel panelHeader = new JPanel(new GridLayout(2, 1));
        panelHeader.setBackground(new Color(37, 99, 235));
        panelHeader.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JLabel lblTitulo = new JLabel("Solicitar Turno", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitulo.setForeground(Color.WHITE);

        JLabel lblSubtitulo = new JLabel("Ingrese su DNI para obtener un turno", SwingConstants.CENTER);
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSubtitulo.setForeground(new Color(200, 220, 255));

        panelHeader.add(lblTitulo);
        panelHeader.add(lblSubtitulo);
        add(panelHeader, BorderLayout.NORTH);

        // --- CUERPO CENTRAL ---
        JPanel panelCentro = new JPanel();
        panelCentro.setLayout(new BoxLayout(panelCentro, BoxLayout.Y_AXIS));
        panelCentro.setBackground(Color.WHITE);
        panelCentro.setBorder(BorderFactory.createEmptyBorder(30, 20, 20, 20));

        JLabel lblInstruccion = new JLabel("Ingrese su DNI");
        lblInstruccion.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblInstruccion.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblAclaracion = new JLabel("Número de documento sin puntos");
        lblAclaracion.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblAclaracion.setForeground(Color.GRAY);
        lblAclaracion.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtDni = new JTextField();
        txtDni.setMaximumSize(new Dimension(300, 40));
        txtDni.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        txtDni.setHorizontalAlignment(JTextField.CENTER);
        txtDni.setAlignmentX(Component.CENTER_ALIGNMENT); // <--- ALINEACIÓN CENTRADA
        txtDni.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        btnSolicitar = new JButton("Solicitar Turno");
        btnSolicitar.setMaximumSize(new Dimension(300, 45));
        btnSolicitar.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnSolicitar.setBackground(new Color(37, 99, 235));
        btnSolicitar.setForeground(Color.WHITE);
        btnSolicitar.setFocusPainted(false);
        btnSolicitar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        btnSolicitar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSolicitar.setAlignmentX(Component.CENTER_ALIGNMENT); // <--- ALINEACIÓN CENTRADA

        // Armado del panel central
        panelCentro.add(Box.createRigidArea(new Dimension(0, 20)));
        panelCentro.add(lblInstruccion);
        panelCentro.add(lblAclaracion);
        panelCentro.add(Box.createRigidArea(new Dimension(0, 15)));
        panelCentro.add(txtDni);
        panelCentro.add(Box.createRigidArea(new Dimension(0, 20))); // Espacio entre el input y el botón
        panelCentro.add(btnSolicitar);

        add(panelCentro, BorderLayout.CENTER);

        // --- PANEL DE MENSAJE (Oculto por defecto) ---
        panelMensaje = new JPanel(new GridLayout(2, 1));
        panelMensaje.setPreferredSize(new Dimension(360, 80));
        panelMensaje.setVisible(false);

        lblMensajeTitulo = new JLabel("", SwingConstants.CENTER);
        lblMensajeTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblMensajeDetalle = new JLabel("", SwingConstants.CENTER);
        lblMensajeDetalle.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        panelMensaje.add(lblMensajeTitulo);
        panelMensaje.add(lblMensajeDetalle);
        add(panelMensaje, BorderLayout.SOUTH);

        btnSolicitar.addActionListener(e -> procesarTurno());
    }

    private void procesarTurno() {
        String dni = txtDni.getText().trim();

        if (dni.isEmpty() || !dni.matches("\\d+")) {
            mostrarMensajeError("DNI Inválido", "Debe contener solo números.");
            return;
        }

        long numeroDni = Long.parseLong(dni);
        if (numeroDni < 500000 || numeroDni > 100000000) {
            mostrarMensajeError("DNI Inválido", "Debe estar entre 500.000 y 100.000.000.");
            return;
        }

        try {
            Socket socket = new Socket(IP_OPERADOR, PUERTO_OPERADOR);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(dni);
            String respuesta = in.readLine();

            if ("OK".equals(respuesta)) {
                mostrarMensajeExito("¡Turno Confirmado!", "DNI registrado exitosamente.");
                txtDni.setText("");
            } else if ("DUPLICADO".equals(respuesta)) {
                mostrarMensajeError("Turno Duplicado", "El DNI ya está esperando en la fila.");
            }

            in.close();
            out.close();
            socket.close();
        } catch (Exception e) {
            mostrarMensajeError("Error de Conexión", "No se pudo conectar con el Servidor.");
        }
    }

    private void mostrarMensajeExito(String titulo, String detalle) {
        panelMensaje.setBackground(new Color(220, 252, 231));
        panelMensaje.setBorder(BorderFactory.createLineBorder(new Color(34, 197, 94), 2));
        lblMensajeTitulo.setForeground(new Color(21, 128, 61));
        lblMensajeDetalle.setForeground(new Color(21, 128, 61));

        lblMensajeTitulo.setText(titulo);
        lblMensajeDetalle.setText(detalle);
        panelMensaje.setVisible(true);
        Timer timer = new Timer(5000, e -> panelMensaje.setVisible(false));
        timer.setRepeats(false);
        timer.start();
    }

    private void mostrarMensajeError(String titulo, String detalle) {
        panelMensaje.setBackground(new Color(254, 226, 226));
        panelMensaje.setBorder(BorderFactory.createLineBorder(new Color(239, 68, 68), 2));
        lblMensajeTitulo.setForeground(new Color(185, 28, 28));
        lblMensajeDetalle.setForeground(new Color(185, 28, 28));

        lblMensajeTitulo.setText(titulo);
        lblMensajeDetalle.setText(detalle);
        panelMensaje.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Kiosco().setVisible(true));
    }
}