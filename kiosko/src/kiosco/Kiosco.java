package kiosco; // Fijate que coincida con el nombre de tu carpeta (con c o con k)

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Kiosco extends JFrame {

    private JTextField txtDni;
    private JButton btnSolicitar;

    // Configuración de red (Apuntamos al Operador)
    private static final String IP_OPERADOR = "127.0.0.1"; // Localhost
    private static final int PUERTO_OPERADOR = 5000;

    public Kiosco() {
        // 1. Configuramos la ventana principal
        setTitle("Terminal de Registro - Solicitud de Turno");
        setSize(350, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));
        setLocationRelativeTo(null); // Centra la ventana

        // 2. Agregamos los componentes visuales
        add(new JLabel("Ingrese su número de documento:"));

        txtDni = new JTextField(15);
        add(txtDni);

        btnSolicitar = new JButton("Solicitar Turno");
        add(btnSolicitar);

        // 3. Le damos acción al botón
        btnSolicitar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                procesarTurno();
            }
        });
    }

    private void procesarTurno() {
        String dni = txtDni.getText().trim();

        // VALIDACIÓN: No se deben permitir ingresos de documentos vacíos o con caracteres no numéricos.
        if (dni.isEmpty() || !dni.matches("\\d+")) {
            JOptionPane.showMessageDialog(this,
                    "Error: El DNI no puede estar vacío y debe contener solo números.",
                    "DNI Inválido",
                    JOptionPane.WARNING_MESSAGE);
            return; // Cortamos la ejecución acá si no es válido
        }

        // COMUNICACIÓN POR RED (Cliente TCP)
        try {
            Socket socket = new Socket(IP_OPERADOR, PUERTO_OPERADOR);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Canal de entrada para escuchar la respuesta del Operador
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Enviamos el DNI
            out.println(dni);

            // El Kiosco frena y lee lo que le contesta el Operador
            String respuesta = in.readLine();

            // Evaluamos la respuesta para saber qué cartel mostrar
            if ("OK".equals(respuesta)) {
                JOptionPane.showMessageDialog(this,
                        "¡Turno Confirmado!\nTe registraste con el DNI: " + dni,
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);
                txtDni.setText(""); // Limpiamos el campo para el siguiente cliente

            } else if ("DUPLICADO".equals(respuesta)) {
                JOptionPane.showMessageDialog(this,
                        "Atención: El DNI " + dni + " ya se encuentra esperando en la fila.",
                        "Turno Duplicado",
                        JOptionPane.WARNING_MESSAGE);
            }

            // Cerramos las conexiones
            in.close();
            out.close();
            socket.close();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo conectar con el Operador. Verifique que el sistema de atención esté iniciado.",
                    "Error de Conexión",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Kiosco().setVisible(true);
            }
        });
    }
}