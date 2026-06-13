package Operador.src.operador;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JOptionPane;
import javax.swing.JTextField;

import Globales.ConfiguracionFactory;

/**
 * Controlador del puesto de atención. Se encarga de manejar la lógica de
 * negocio del puesto, interactuar con el modelo (Puesto) y actualizar la vista
 * (PanelOperador). Implementa ActionListener para responder a los eventos de
 * los botones y WindowListener para manejar el cierre de la ventana y asegurar
 * una desconexión limpia del servidor. En el constructor, solicita la
 * configuración al usuario, intenta conectar con el servidor y maneja los casos
 * de error. Si la conexión es exitosa, inicializa la interfaz gráfica y
 * establece las relaciones necesarias entre el modelo (Puesto) y la vista
 * (PanelOperador).
 */
public class ControladorPuesto implements ActionListener, WindowListener {
    private PanelOperador panel;
    private Puesto puesto;

    /**
     * Constructor del controlador del puesto de atención. Se encarga de solicitar
     * la configuración al usuario, intentar conectar con el servidor y manejar los
     * casos de error. Si la conexión es exitosa, inicializa la interfaz gráfica y
     * establece las relaciones necesarias entre el modelo (Puesto)
     */
    public ControladorPuesto() {
        String respuesta;
        this.puesto = new Puesto();
        ConfiguracionPuesto config;
        do {
            config = getConfiguracionPuesto();
            if (config == null) {
                System.exit(0);
            }
            this.puesto.setConfiguracion(config);
            respuesta = this.puesto.conectar();
            switch (respuesta) {
                case "DUPLICADO":
                    JOptionPane.showMessageDialog(null,
                            "El número de puesto ya está registrado. Por favor, elija otro.");
                    break;
                case Puesto.ERROR_DE_CONEXION:
                    JOptionPane.showMessageDialog(null,
                            "No se pudo conectar con el servidor. Verifique la configuración e intente nuevamente.");
                    break;
            }
        } while (!respuesta.equals("OK"));
        if (config != null) {
            this.panel = new PanelOperador(config.getIdPuesto());
            this.puesto.setConfiguracion(config);
            this.panel.setActionListener(this);
            this.panel.addWindowListener(this);
            this.puesto.addObserver(this.panel);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String respuesta = null;
        switch (e.getActionCommand()) {
            case InterfazDeTrabajo.LLAMAR:
                respuesta = this.puesto.llamar();
                break;
            case InterfazDeTrabajo.RELLAMAR:
                respuesta = this.puesto.rellamar();
                break;
            default:
                break;
        }
        if (respuesta == null) {
            JOptionPane.showMessageDialog(this.panel, "Error de comunicación con el servidor. Intente nuevamente.");
        } else if ("VACIA".equals(respuesta)) {
            JOptionPane.showMessageDialog(this.panel, "No hay clientes pendientes en la fila.");
            this.panel.setTurnoActual("Nadie");
            this.panel.resetearBotonRellamar();
        } else if ("DESCARTADO".equals(respuesta)) {
            JOptionPane.showMessageDialog(this.panel, "El cliente no se presentó tras 3 intentos. Turno cancelado.");
            this.panel.setTurnoActual("Nadie");
            this.panel.resetearBotonRellamar();
        } else if (respuesta.startsWith("OK")) {
            String dni = respuesta.split("_")[1];
            this.panel.setTurnoActual("DNI: " + dni);
            this.panel.inhabilitarBotonRellamar();
            this.puesto.iniciarCooldownRellamado();
        }

    }

    /**
     * Solicita al usuario la configuración del puesto de atención a través de un
     * diálogo de entrada.
     * 
     * @return Objeto ConfiguracionPuesto con los datos ingresados por el usuario, o
     *         null si el usuario cancela la operación.
     */
    private ConfiguracionPuesto getConfiguracionPuesto() {
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

        int option = JOptionPane.showConfirmDialog(null, message, "Configuración del Puesto",
                JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION) {
            return null; // El usuario canceló o cerró la ventana
        }

        String ipPrimario = txtIpPrimario.getText().trim();
        String ipRespaldo = txtIpRespaldo.getText().trim();
        int puertoServidor = Integer.parseInt(txtPuerto.getText().trim());
        int idPuesto = Integer.parseInt(txtPuesto.getText().trim());
        return ConfiguracionFactory.crearConfiguracionPuesto(ipPrimario, ipRespaldo, puertoServidor, idPuesto);
    }

    @Override
    public void windowOpened(WindowEvent e) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'windowOpened'");
    }

    @Override
    public void windowClosing(WindowEvent e) {
        this.puesto.desconectar();
    }

    @Override
    public void windowClosed(WindowEvent e) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'windowClosed'");
    }

    @Override
    public void windowIconified(WindowEvent e) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'windowIconified'");
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'windowDeiconified'");
    }

    @Override
    public void windowActivated(WindowEvent e) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'windowActivated'");
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'windowDeactivated'");
    }
}
