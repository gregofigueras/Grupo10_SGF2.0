package Operador.src.operador;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

@SuppressWarnings("deprecation")
public class PanelOperador extends JFrame implements InterfazDeTrabajo, Observer {

    private JLabel lblTurnoActual;
    private JButton btnLlamar;
    private JButton btnRellamar;

    public PanelOperador(int idPuesto) {

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
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));

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
        btnLlamar.setActionCommand(LLAMAR);
        btnRellamar.setActionCommand(RELLAMAR);
        this.setVisible(true);
    }

    private JPanel crearTarjeta() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 231, 235), 1), // Borde gris claro
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        // Alinear todo a la izquierda dentro de la tarjeta
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        return panel;
    }

    @Override
    public void resetearBotonRellamar() {
        btnRellamar.setText("Re-notificar");
        btnRellamar.setEnabled(false);
    }

    @Override
    public void setActionListener(ActionListener al) {
        this.btnLlamar.addActionListener(al);
        this.btnRellamar.addActionListener(al);
    }

    @Override
    public void setTurnoActual(String turno) {
        lblTurnoActual.setText(turno);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof String && arg.equals(Puesto.HABILITAR_RELLAMADO)) {
            btnRellamar.setEnabled(true);
            btnRellamar.setText("Re-notificar");
        } else if (arg instanceof Integer) {
            int segundos = (Integer) arg;
            btnRellamar.setText("Re-notificar en " + segundos + " s");
        }
    }

    @Override
    public void inhabilitarBotonRellamar() {
        btnRellamar.setEnabled(false);
    }

}