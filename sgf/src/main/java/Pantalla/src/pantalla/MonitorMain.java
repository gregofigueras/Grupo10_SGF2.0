package Pantalla.src.pantalla;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import java.util.Observer;

@SuppressWarnings("deprecation")
public class MonitorMain {

    static void main(String[] args) {
        // Evita warning de argumento no usado
        if (args != null && args.length > 0) {
            System.out.println("MonitorMain: argumentos ignorados.");
        }

        InterfazPublico vista = new VistaMonitor();

        JTextField txtPuerto = new JTextField("6000");
        String[] opcionesFormato = { "JSON", "XML", "TXT" };
        JComboBox<String> cmbFormato = new JComboBox<>(opcionesFormato);
        JTextField txtClave = new JTextField("12345");

        Object[] message = {
                "Puerto de Escucha:", txtPuerto,
                "Formato de Persistencia (del servidor):", cmbFormato,
                "Clave de Encriptación (numérica):", txtClave
        };

        int option = JOptionPane.showConfirmDialog(
                null,
                message,
                "Configuración del Monitor",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (option != JOptionPane.OK_OPTION) {
            System.exit(0);
            return;
        }

        try {
            int puerto = Integer.parseInt(txtPuerto.getText().trim());
            String formato = (String) cmbFormato.getSelectedItem();
            int clave = Integer.parseInt(txtClave.getText().trim());

            if (puerto < 1024 || puerto > 65535) {
                JOptionPane.showMessageDialog(
                        null,
                        "Error: El puerto debe estar entre 1024 y 65535.",
                        "Error de validación",
                        JOptionPane.ERROR_MESSAGE
                );
                System.exit(0);
                return;
            }

            MonitorSala monitor = new MonitorSala(puerto, formato, clave);
            monitor.addObserver((Observer) vista);
            monitor.cargarHistorialInicial();

            System.out.println("[Sistema] Monitor iniciado correctamente.");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Error: El puerto y la clave deben ser números enteros.",
                    "Error de formato",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(0);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Error al iniciar el monitor: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.err.println("Error al iniciar monitor: " + e.getMessage());
            System.exit(0);
        }
    }
}
