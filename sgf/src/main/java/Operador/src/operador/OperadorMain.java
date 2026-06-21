package Operador.src.operador;

import javax.swing.SwingUtilities;

/**
 * Punto de entrada del módulo Operador.
 * La configuración y el armado MVC los maneja ControladorPuesto.
 */
public class OperadorMain {
    public static void main(String[] args) {
        // Evita warning de parámetro no usado.
        if (args != null && args.length > 0) {
            System.out.println("OperadorMain: argumentos ignorados.");
        }

        // Inicia la UI/controlador en el hilo de eventos de Swing.
        SwingUtilities.invokeLater(ControladorPuesto::new);
    }
}
