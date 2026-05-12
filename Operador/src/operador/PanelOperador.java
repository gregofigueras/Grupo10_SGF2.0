package operador;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class PanelOperador extends JFrame {

    private JLabel lblTurnoActual;
    private JButton btnLlamar;
    private JButton btnRellamar;

    private Timer cooldownTimer;
    private int segundosRestantes;

    private String ipPrimario;
    private String ipRespaldo;
    private int puertoServidor;
    private int idPuesto;

    public PanelOperador() {
        // 1. Pedimos la configuración dinámica y validamos el puesto con el servidor
        if (!configurarPuesto()) {
            System.exit(0);
        }

        setTitle("Puesto de Atención #" + idPuesto);
        setSize(450, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(243, 244, 246));

        // --- HEADER OSCURO ---
        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBackground(new Color(30, 41, 59));
        panelHeader.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel lblHeaderTitulo = new JLabel("Puesto #" + idPuesto + " · Estado: Disponible", SwingConstants.CENTER);
        lblHeaderTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblHeaderTitulo.setForeground(Color.WHITE);
        panelHeader.add(lblHeaderTitulo, BorderLayout.CENTER);
        add(panelHeader, BorderLayout.NORTH);

        // --- CONTENEDOR CENTRAL ---
        JPanel panelCentro = new JPanel();
        panelCentro.setLayout(new BoxLayout(panelCentro, BoxLayout.Y_AXIS));
        panelCentro.setBackground(new Color(243, 244, 246));
        panelCentro.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel cardTurno = crearTarjeta();
        JLabel lblAtencion = new JLabel("Turno en atención:");
        lblAtencion.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblAtencion.setForeground(Color.GRAY);

        lblTurnoActual = new JLabel("Nadie", SwingConstants.LEFT);
        lblTurnoActual.setFont(new Font("Segoe UI", Font.BOLD, 28));

        cardTurno.add(lblAtencion);
        cardTurno.add(lblTurnoActual);

        btnLlamar = new JButton("Llamar Siguiente");
        btnLlamar.setMaximumSize(new Dimension(400, 60));
        btnLlamar.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btnLlamar.setBackground(new Color(139, 92, 246));
        btnLlamar.setForeground(Color.WHITE);
        btnLlamar.setFocusPainted(false);
        btnLlamar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel cardRellamar = crearTarjeta();
        cardRellamar.setBackground(new Color(254, 243, 199));
        cardRellamar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(252, 211, 77), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel lblReTitle = new JLabel("Re-notificación");
        lblReTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));

        btnRellamar = new JButton("Re-notificar (Máx 3)");
        btnRellamar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRellamar.setBackground(new Color(220, 38, 38));
        btnRellamar.setForeground(Color.WHITE);
        btnRellamar.setFocusPainted(false);
        btnRellamar.setEnabled(false);

        cardRellamar.add(lblReTitle);
        cardRellamar.add(Box.createRigidArea(new Dimension(0, 10)));
        cardRellamar.add(btnRellamar);

        panelCentro.add(cardTurno);
        panelCentro.add(Box.createRigidArea(new Dimension(0, 20)));
        panelCentro.add(btnLlamar);
        panelCentro.add(Box.createRigidArea(new Dimension(0, 20)));
        panelCentro.add(cardRellamar);

        add(panelCentro, BorderLayout.CENTER);

        // Eventos
        btnLlamar.addActionListener(e -> enviarComando("LLAMAR_" + idPuesto));
        btnRellamar.addActionListener(e -> enviarComando("RELLAMAR_" + idPuesto));

        // 2. TÁCTICA DE RECUPERACIÓN: Notificar al servidor al cerrar para liberar el puesto
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                try (Socket socket = conectarConReintento(puertoServidor);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                    out.println("DESCONECTAR_" + idPuesto);
                } catch (Exception ignored) {}
            }
        });
    }

    private JPanel crearTarjeta() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 231, 235), 1), // Borde gris claro
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        // Alinear todo a la izquierda dentro de la tarjeta
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        return panel;
    }

    private void enviarComando(String comando) {
        // Cambiamos 'new Socket' por 'conectarConReintento' y usamos 'puertoServidor'
        try (Socket socket = conectarConReintento(puertoServidor);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(comando);
            String respuesta = in.readLine();

            if ("VACIA".equals(respuesta)) {
                JOptionPane.showMessageDialog(this, "No hay clientes pendientes en la fila.");
                lblTurnoActual.setText("Nadie");
                resetearBotonRellamar();
            } else if ("DESCARTADO".equals(respuesta)) {
                JOptionPane.showMessageDialog(this, "El cliente no se presentó tras 3 intentos. Turno cancelado.");
                lblTurnoActual.setText("Nadie");
                resetearBotonRellamar();
            } else if (respuesta != null && respuesta.startsWith("OK")) {
                String dni = respuesta.split("_")[1];
                lblTurnoActual.setText("DNI: " + dni);
                iniciarCooldownRellamado();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error de red: No se pudo comunicar con el servidor.");
        }
    }

    private void iniciarCooldownRellamado() {
        if (cooldownTimer != null && cooldownTimer.isRunning()) cooldownTimer.stop();

        btnRellamar.setEnabled(false);
        segundosRestantes = 30;
        btnRellamar.setText("Re-notificar en " + segundosRestantes + "s");

        cooldownTimer = new Timer(1000, e -> {
            segundosRestantes--;
            if (segundosRestantes <= 0) {
                cooldownTimer.stop();
                btnRellamar.setText("Re-notificar");
                btnRellamar.setEnabled(true);
            } else {
                btnRellamar.setText("Re-notificar en " + segundosRestantes + "s");
            }
        });
        cooldownTimer.start();
    }

    private void resetearBotonRellamar() {
        if (cooldownTimer != null && cooldownTimer.isRunning()) cooldownTimer.stop();
        btnRellamar.setText("Re-notificar");
        btnRellamar.setEnabled(false);
    }
    private Socket conectarConReintento(int puerto) throws Exception {
        try {
            return new Socket(ipPrimario, puerto);
        } catch (Exception e) {
            System.out.println("Fallo el Servidor Primario. Reintentando con Servidor de Respaldo...");
            return new Socket(ipRespaldo, puerto);
        }
    }
    private boolean configurarPuesto() {
        while (true) {
            JTextField txtIpPrimario = new JTextField("127.0.0.1");
            JTextField txtIpRespaldo = new JTextField("127.0.0.2");
            JTextField txtPuerto = new JTextField("5001");
            JTextField txtPuesto = new JTextField("1");

            Object[] message = {
                    "IP Servidor Primario:", txtIpPrimario,
                    "IP Servidor Respaldo:", txtIpRespaldo,
                    "Puerto del Servidor:", txtPuerto,
                    "Número de Puesto:", txtPuesto
            };

            int option = JOptionPane.showConfirmDialog(null, message, "Configuración del Puesto", JOptionPane.OK_CANCEL_OPTION);
            if (option != JOptionPane.OK_OPTION) {
                return false; // El usuario canceló o cerró la ventana
            }

            try {
                this.ipPrimario = txtIpPrimario.getText().trim();
                this.ipRespaldo = txtIpRespaldo.getText().trim();
                this.puertoServidor = Integer.parseInt(txtPuerto.getText().trim());
                this.idPuesto = Integer.parseInt(txtPuesto.getText().trim());

                // Intentamos registrar el puesto en el servidor
                try (Socket socket = conectarConReintento(this.puertoServidor);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    out.println("REGISTRAR_" + this.idPuesto);
                    String respuesta = in.readLine();

                    if ("DUPLICADO".equals(respuesta)) {
                        JOptionPane.showMessageDialog(null, "Error: El Puesto #" + this.idPuesto + " ya está conectado al Servidor. Elija otro número.", "Puesto Ocupado", JOptionPane.ERROR_MESSAGE);
                    } else if ("OK".equals(respuesta)) {
                        return true; // Registro exitoso
                    }
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Error de red: No se pudo conectar con el servidor. Verifique IP y Puerto.", "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PanelOperador().setVisible(true));
    }
}