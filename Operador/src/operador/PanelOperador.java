package operador;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class PanelOperador extends JFrame {

    private JLabel lblEstado;
    private JLabel lblTurnoActual;
    private JButton btnLlamar;

    // Ahora apuntamos al Servidor Central (usaremos el puerto 5001 para operadores)
    private static final String IP_SERVIDOR = "127.0.0.1";
    private static final int PUERTO_SERVIDOR = 5001;

    public PanelOperador() {
        setTitle("Panel de Operador - Puesto Único");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(4, 1, 10, 10));
        setLocationRelativeTo(null);

        lblEstado = new JLabel("Conectado al Servidor Central", SwingConstants.CENTER);
        lblEstado.setFont(new Font("Arial", Font.BOLD, 16));
        add(lblEstado);

        lblTurnoActual = new JLabel("Atendiendo a: Nadie", SwingConstants.CENTER);
        lblTurnoActual.setFont(new Font("Arial", Font.PLAIN, 18));
        add(lblTurnoActual);

        btnLlamar = new JButton("Llamar Siguiente");
        btnLlamar.setFont(new Font("Arial", Font.BOLD, 14));
        add(btnLlamar);

        // Acción del botón: Le pide al servidor que saque un turno de la fila
        btnLlamar.addActionListener(e -> llamarSiguiente());
    }

    private void llamarSiguiente() {
        try (Socket socket = new Socket(IP_SERVIDOR, PUERTO_SERVIDOR);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("LLAMAR"); // Le enviamos el comando al Servidor

            String respuesta = in.readLine(); // Esperamos el DNI o el aviso de vacía

            if ("VACIA".equals(respuesta)) {
                JOptionPane.showMessageDialog(this, "No hay clientes pendientes.");
                lblTurnoActual.setText("Atendiendo a: Nadie");
            } else {
                lblTurnoActual.setText("Atendiendo a: " + respuesta);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al comunicar con el Servidor Central.");
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PanelOperador().setVisible(true));
    }
}