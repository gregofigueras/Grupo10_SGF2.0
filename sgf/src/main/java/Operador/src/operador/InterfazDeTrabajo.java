package Operador.src.operador;

import java.awt.event.ActionListener;

/**
 * Interfaz que define los métodos necesarios para la interacción entre el
 * modelo (Puesto) y la vista (PanelOperador) en el patrón MVC. Esta interfaz
 * permite que el controlador del puesto de atención pueda manejar los eventos
 * de la vista y actualizarla en consecuencia, sin acoplar directamente el
 * modelo con la vista. Además, define métodos para controlar el estado de los
 * botones y mostrar el turno actual en la interfaz gráfica, facilitando la
 * comunicación entre el modelo y la vista a través del controlador.
 */
public interface InterfazDeTrabajo {
    public static final String LLAMAR = "LLAMAR";
    public static final String RELLAMAR = "RELLAMAR";

    /**
     * Establece el ActionListener para los botones de la interfaz. Este método esta
     * pensado para ser llamado desde el controlador del puesto de atención,
     * permitiendo que el controlador maneje los eventos de los botones y actualice
     * la vista en consecuencia.
     * 
     * @param al El ActionListener que se asignará a los botones de la interfaz.
     */
    public void setActionListener(ActionListener al);

    /**
     * Resetea el botón de rellamar.
     */
    public void resetearBotonRellamar();

    /**
     * Actualiza el turno actual que se muestra en la interfaz gráfica. Este método
     * es utilizado por el controlador para mostrar el turno que está siendo
     * atendido actualmente.
     * 
     * @param turno El turno actual que se mostrará en la interfaz gráfica.
     */
    public void setTurnoActual(String turno);

    /**
     * Inhabilita el botón de rellamar.
     */
    public void inhabilitarBotonRellamar();
}
