package Pantalla.src.pantalla;

import java.util.Observer;

@SuppressWarnings("deprecation") // Para Observable
public class MonitorMain {
    public static void main(String[] args) {
        InterfazPublico vista = new VistaMonitor();
        int puerto;
        do {
            puerto = vista.getConfiguracion();
        } while (puerto == VistaMonitor.CONFIG_INVALIDA);
        if (puerto != VistaMonitor.CONFIG_CANCELADA) {
            MonitorSala monitor = new MonitorSala(puerto);
            monitor.addObserver((Observer) vista);
            monitor.cargarHistorialInicial();
        } else {
            System.exit(0);
        }
    }
}
