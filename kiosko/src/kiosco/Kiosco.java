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

    private String ipPrimario;
    private String ipRespaldo;
    private int puertoServidor;

    public Kiosco() {
        // LLAMADA A CONFIGURACIÓN DINÁMICA
        if (!configurarKiosco()) {
            System.exit(0);
        }

        setTitle("Terminal de Registro");
        setSize(380, 750); // Agrandamos un poco la ventana para que entre el teclado
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

        JLabel lblSubtitulo = new JLabel("Ingrese su DNI con el teclado en pantalla", SwingConstants.CENTER);
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSubtitulo.setForeground(new Color(200, 220, 255));

        panelHeader.add(lblTitulo);
        panelHeader.add(lblSubtitulo);
        add(panelHeader, BorderLayout.NORTH);

        // --- CUERPO CENTRAL ---
        JPanel panelCentro = new JPanel();
        panelCentro.setLayout(new BoxLayout(panelCentro, BoxLayout.Y_AXIS));
        panelCentro.setBackground(Color.WHITE);
        panelCentro.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel lblInstruccion = new JLabel("Ingrese su DNI");
        lblInstruccion.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblInstruccion.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtDni = new JTextField();
        txtDni.setMaximumSize(new Dimension(300, 50));
        txtDni.setFont(new Font("Segoe UI", Font.BOLD, 24));
        txtDni.setHorizontalAlignment(JTextField.CENTER);
        txtDni.setAlignmentX(Component.CENTER_ALIGNMENT);
        txtDni.setEditable(false); // Lo hacemos no-editable con el teclado físico para forzar el uso del touch
        txtDni.setBackground(new Color(248, 250, 252));
        txtDni.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        // --- NUEVO: TECLADO NUMÉRICO ---
        JPanel panelTeclado = new JPanel(new GridLayout(4, 3, 10, 10));
        panelTeclado.setBackground(Color.WHITE);
        panelTeclado.setMaximumSize(new Dimension(300, 280));

        String[] botonesTeclado = {
                "1", "2", "3",
                "4", "5", "6",
                "7", "8", "9",
                "C", "0", "<-"
        };

        for (String texto : botonesTeclado) {
            JButton btn = new JButton(texto);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 22));
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            // Colores especiales para Borrar y Limpiar
            if ("C".equals(texto) || "<-".equals(texto)) {
                btn.setBackground(new Color(254, 226, 226));
                btn.setForeground(new Color(220, 38, 38));
            } else {
                btn.setBackground(new Color(241, 245, 249));
                btn.setForeground(new Color(30, 41, 59));
            }

            // Lógica al presionar un botón del teclado
            btn.addActionListener(e -> {
                String actual = txtDni.getText();
                if ("C".equals(texto)) {
                    txtDni.setText(""); // Limpiar todo
                } else if ("<-".equals(texto)) {
                    if (actual.length() > 0) {
                        txtDni.setText(actual.substring(0, actual.length() - 1)); // Borrar el último
                    }
                } else {
                    if (actual.length() < 9) { // Límite de largo de DNI
                        txtDni.setText(actual + texto); // Agregar número
                    }
                }
            });
            panelTeclado.add(btn);
        }

        // --- BOTON SOLICITAR ---
        btnSolicitar = new JButton("Solicitar Turno");
        btnSolicitar.setMaximumSize(new Dimension(300, 50));
        btnSolicitar.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btnSolicitar.setBackground(new Color(37, 99, 235));
        btnSolicitar.setForeground(Color.WHITE);
        btnSolicitar.setFocusPainted(false);
        btnSolicitar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSolicitar.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Armado del panel central
        panelCentro.add(lblInstruccion);
        panelCentro.add(Box.createRigidArea(new Dimension(0, 10)));
        panelCentro.add(txtDni);
        panelCentro.add(Box.createRigidArea(new Dimension(0, 20)));
        panelCentro.add(panelTeclado); // Agregamos el teclado a la pantalla
        panelCentro.add(Box.createRigidArea(new Dimension(0, 20)));
        panelCentro.add(btnSolicitar);

        add(panelCentro, BorderLayout.CENTER);

        // --- PANEL DE MENSAJE ---
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

        try (Socket socket = conectarConReintento(puertoServidor);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(dni);
            String respuesta = in.readLine();

            if ("OK".equals(respuesta)) {
                mostrarMensajeExito("¡Turno Confirmado!", "DNI registrado exitosamente.");
                txtDni.setText("");
            } else if ("DUPLICADO".equals(respuesta)) {
                mostrarMensajeError("Turno Duplicado", "El DNI ya está esperando en la fila.");
            }
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

    private boolean configurarKiosco() {
        JTextField txtIpPrimario = new JTextField("127.0.0.1");
        JTextField txtIpRespaldo = new JTextField("127.0.0.2");
        JTextField txtPuerto = new JTextField("5000");

        Object[] message = {
                "IP Servidor Primario:", txtIpPrimario,
                "IP Servidor Respaldo:", txtIpRespaldo,
                "Puerto del Servidor para Kioscos:", txtPuerto
        };

        int option = JOptionPane.showConfirmDialog(null, message, "Configuración del Kiosco", JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION) {
            return false;
        }

        this.ipPrimario = txtIpPrimario.getText().trim();
        this.ipRespaldo = txtIpRespaldo.getText().trim();
        this.puertoServidor = Integer.parseInt(txtPuerto.getText().trim());
        return true;
    }

    private Socket conectarConReintento(int puerto) throws Exception {
        try {
            return new Socket(ipPrimario, puerto);
        } catch (Exception e) {
            System.out.println("Fallo el Primario. Reintentando con Servidor de Respaldo...");
            return new Socket(ipRespaldo, puerto);
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Kiosco().setVisible(true));
    }
}