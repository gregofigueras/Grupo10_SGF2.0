package Pantalla.src.pantalla;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import Globales.Turno;

/**
 * VistaMonitor es la clase encargada de mostrar la pantalla de turnos al
 * público. Implementa la interfaz InterfazPublico para recibir la configuración
 * inicial y la interfaz Observer para actualizar la pantalla cada vez que el
 * MonitorSala notifique un cambio en el turno actual o en el historial de
 * llamados. La pantalla muestra el turno actual, el puesto al que debe
 * dirigirse el cliente, y un historial de los últimos turnos llamados o
 * expirados. Además, tiene un efecto de parpadeo para resaltar los turnos
 * urgentes.
 */
@SuppressWarnings("deprecation")
public class VistaMonitor extends JFrame implements InterfazPublico, Observer {
    private JLabel lblTurnoActual;
    private JLabel lblPuesto;
    private JPanel panelActual;
    private DefaultListModel<String> historialModel;
    private JList<String> listHistorial;
    private Thread hiloParpadeo; // Controlamos el parpadeo para que no se pisen

    // Paleta de colores oscuros
    private final Color COLOR_FONDO = new Color(15, 23, 42);
    private final Color COLOR_PANEL = new Color(30, 41, 59);
    private final Color COLOR_TEXTO_CLARO = new Color(241, 245, 249);
    private final Color COLOR_ROJO_TURNO = new Color(239, 68, 68);

    // Configuraciones inválidas
    public static final int CONFIG_INVALIDA = -1;
    public static final int CONFIG_CANCELADA = -2;

    /**
     * Constructor de la VistaMonitor. Configura la ventana principal, los paneles y
     * los componentes para mostrar el turno actual, el puesto y el historial de
     * turnos. También establece el diseño y los colores de la interfaz para que sea
     * atractiva y fácil de leer para el público. Al finalizar la configuración,
     * hace visible la ventana.
     */
    public VistaMonitor() {
        setTitle("Pantalla de Turnos");
        setSize(600, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(15, 15));
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_FONDO);

        // --- CONTENEDOR SUPERIOR ---
        JPanel panelNorte = new JPanel(new BorderLayout(10, 10));
        panelNorte.setBackground(COLOR_FONDO);

        JLabel lblHeader = new JLabel("Pantalla de Turnos", SwingConstants.CENTER);
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 30));
        lblHeader.setForeground(COLOR_TEXTO_CLARO);
        lblHeader.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        panelNorte.add(lblHeader, BorderLayout.NORTH);

        // --- PANEL CENTRAL (Turno Actual) ---
        panelActual = new JPanel(new GridLayout(3, 1));
        panelActual.setBackground(COLOR_PANEL);
        panelActual.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_ROJO_TURNO, 2),
                BorderFactory.createEmptyBorder(30, 20, 30, 20)));

        JLabel lblSubtitulo = new JLabel("Turno Llamado", SwingConstants.LEFT);
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lblSubtitulo.setForeground(new Color(148, 163, 184));
        panelActual.add(lblSubtitulo);

        lblTurnoActual = new JLabel("---", SwingConstants.LEFT);
        lblTurnoActual.setFont(new Font("Segoe UI", Font.BOLD, 80));
        lblTurnoActual.setForeground(COLOR_ROJO_TURNO);
        panelActual.add(lblTurnoActual);

        lblPuesto = new JLabel("Diríjase al Puesto: ---", SwingConstants.RIGHT);
        lblPuesto.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblPuesto.setForeground(COLOR_TEXTO_CLARO);
        panelActual.add(lblPuesto);

        JPanel wrapperCentro = new JPanel(new BorderLayout());
        wrapperCentro.setBackground(COLOR_FONDO);
        wrapperCentro.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        wrapperCentro.add(panelActual, BorderLayout.CENTER);

        panelNorte.add(wrapperCentro, BorderLayout.CENTER);
        add(panelNorte, BorderLayout.NORTH);

        // --- PANEL INFERIOR (Historial) ---
        JPanel panelHistorial = new JPanel(new BorderLayout());
        panelHistorial.setBackground(COLOR_FONDO);
        panelHistorial.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JLabel lblHistorialTitulo = new JLabel("Anteriores");
        lblHistorialTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblHistorialTitulo.setForeground(new Color(148, 163, 184));
        lblHistorialTitulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panelHistorial.add(lblHistorialTitulo, BorderLayout.NORTH);

        historialModel = new DefaultListModel<>();
        listHistorial = new JList<>(historialModel);
        listHistorial.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        listHistorial.setBackground(COLOR_PANEL);
        listHistorial.setForeground(new Color(203, 213, 225));
        listHistorial.setFixedCellHeight(50);
        listHistorial.setEnabled(false);

        JScrollPane scrollPane = new JScrollPane(listHistorial);
        scrollPane.setBorder(BorderFactory.createLineBorder(COLOR_PANEL));
        panelHistorial.add(scrollPane, BorderLayout.CENTER);

        add(panelHistorial, BorderLayout.CENTER);
        this.setVisible(true);
    }

    /**
     * Aplica un efecto de parpadeo al panel del turno actual para resaltar los
     * turnos urgentes.
     */
    private void efectoParpadeo() {
        // Interrumpimos parpadeo anterior si es que alguien tocó re-llamar dos veces
        // rápido
        if (hiloParpadeo != null && hiloParpadeo.isAlive()) {
            hiloParpadeo.interrupt();
        }
        hiloParpadeo = new Thread(() -> {
            try {
                for (int i = 0; i < 4; i++) {
                    panelActual.setBackground(new Color(153, 27, 27));
                    lblTurnoActual.setForeground(Color.WHITE);
                    Thread.sleep(250);
                    panelActual.setBackground(COLOR_PANEL);
                    lblTurnoActual.setForeground(COLOR_ROJO_TURNO);
                    Thread.sleep(250);
                }
            } catch (InterruptedException ignored) {
                // Si lo interrumpen, vuelve al color normal
                panelActual.setBackground(COLOR_PANEL);
                lblTurnoActual.setForeground(COLOR_ROJO_TURNO);
            }
        });
        hiloParpadeo.start();
    }

    /**
     * Agrega el turno actual al historial visible. Si el turno expiró, se marca
     * como "EXPIRADO". Si el turno fue llamado normalmente, se muestra el número de
     * turno y el puesto al que debía dirigirse. El historial solo muestra los
     * últimos 5 turnos.
     * 
     * @param expirado Indica si el turno expiró (true) o fue llamado normalmente
     *                 (false).
     */
    public void agregarTurnoAHistorial(boolean expirado) {
        if (expirado) {
            historialModel.add(0,
                    "   " + lblTurnoActual.getText() + "                    EXPIRADO");
        } else if (!lblTurnoActual.getText().equals("---")) {
            historialModel.add(0,
                    "   " + lblTurnoActual.getText() + "                    Puesto "
                            + lblPuesto.getText().replace("Diríjase al Puesto: ", ""));
        }
        if (historialModel.size() > 4)
            historialModel.removeElementAt(historialModel.size() - 1);
    }

    /**
     * Actualiza el historial visible con la lista de turnos proporcionada. Este
     * método se llama cuando el MonitorSala notifica un cambio en el historial de
     * llamados, y se encarga de reflejar ese cambio en la interfaz del público. El
     * historial se muestra en orden cronológico inverso, con el turno más reciente
     * en la parte superior. Cada entrada del historial indica el número de turno,
     * el puesto al que debía dirigirse, y si el turno expiró o fue llamado
     * normalmente.
     * 
     * @param historial Lista de turnos que representa el historial de llamados.
     *                  Cada turno debe tener su estado (expirado o no) para
     *                  mostrarlo correctamente.
     */
    private void actualizarHistorialVisible(LinkedList<Turno> historial) {
        historialModel.clear();
        for (Turno turno : historial) {
            historialModel.addElement(turno.toString());
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        MonitorSala monitor = (MonitorSala) o;
        if (arg instanceof IOException) {
            JOptionPane.showMessageDialog(this,
                    "Error de conexión con el servidor: " + ((IOException) arg).getMessage(),
                    "Error de Conexión", JOptionPane.ERROR_MESSAGE);
        } else if (arg instanceof String) {
            String mensaje = (String) arg;
            switch (mensaje) {
                case "DESCARTADO":
                    // Si el turno que expiró es el actual, lo limpiamos
                    Turno turnoActual = monitor.getTurnoActual();
                    String dni = turnoActual != null ? turnoActual.getDniCliente() : "";
                    agregarTurnoAHistorial(true);
                    if (lblTurnoActual.getText().equals(dni)) {
                        if (hiloParpadeo != null && hiloParpadeo.isAlive())
                            hiloParpadeo.interrupt();
                        lblTurnoActual.setText("---");
                        lblPuesto.setText("Diríjase al Puesto: ---");
                        panelActual.setBackground(COLOR_PANEL);
                        lblTurnoActual.setForeground(COLOR_ROJO_TURNO);
                    }
                    break;
                case "URGENTE":
                    if (!lblTurnoActual.getText().equals(monitor.getDniActual())) {
                        actualizarHistorialVisible(monitor.getHistorial());
                        Turno turnoUrgente = monitor.getTurnoActual();
                        lblTurnoActual.setText(turnoUrgente.getDniCliente());
                        lblPuesto.setText("Diríjase al Puesto: " + turnoUrgente.getPuestoAtencion());
                    }
                    efectoParpadeo();
                    break;
                default: // "NUEVO"
                    agregarTurnoAHistorial(false);
                    if (hiloParpadeo != null && hiloParpadeo.isAlive())
                        hiloParpadeo.interrupt();
                    panelActual.setBackground(COLOR_PANEL);
                    lblTurnoActual.setForeground(COLOR_ROJO_TURNO);
                    lblTurnoActual.setText(monitor.getDniActual());
                    lblPuesto.setText("Diríjase al Puesto: " + monitor.getPuestoActual());
                    break;
            }
        } else {
            actualizarHistorialVisible(monitor.getHistorial());
        }
    }

    @Override
    public int getConfiguracion() {
        JTextField txtPuerto = new JTextField("6000");
        Object[] message = { "Puerto de Escucha para Notificaciones:", txtPuerto };
        int respuesta;

        int option = JOptionPane.showConfirmDialog(null, message, "Configuración del Monitor",
                JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION) {
            respuesta = CONFIG_CANCELADA;
        } else {

            try {
                respuesta = Integer.parseInt(txtPuerto.getText().trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Puerto inválido.");
                return CONFIG_INVALIDA;
            }
        }
        return respuesta;
    }
}
