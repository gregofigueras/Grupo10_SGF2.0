package kiosko.src.kiosco;

import java.awt.event.ActionListener;
import Globales.Configuracion;

/**
 * Interfaz que define la API de la vista del cliente en el kiosco.
 *
 * <p>
 * Las implementaciones deben proveer métodos para obtener el DNI ingresado,
 * solicitar la configuración del kiosco, mostrar mensajes de éxito/error,
 * registrar un {@link ActionListener} y limpiar el campo de entrada.
 * </p>
 */
public interface InterfazDelCliente {
    /** Comando de acción usado cuando el usuario solicita un turno. */
    public static final String SOLICITAR = "SOLICITAR";

    /**
     * Devuelve el DNI ingresado por el usuario en la interfaz.
     *
     * @return DNI como entero
     */
    public int getDNI();

    /**
     * Solicita a la vista que entregue la configuración para conectarse al
     * servidor (IPs, puerto, etc.).
     *
     * @return instancia de {@link Configuracion} con los datos ingresados
     */
    public Configuracion getConfiguracion();

    /**
     * Muestra un mensaje de éxito al usuario.
     *
     * @param titulo  título del mensaje
     * @param detalle texto descriptivo del mensaje
     */
    public void mostrarMensajeExito(String titulo, String detalle);

    /**
     * Muestra un mensaje de error al usuario.
     *
     * @param titulo  título del mensaje de error
     * @param detalle texto descriptivo del error
     */
    public void mostrarMensajeError(String titulo, String detalle);

    /**
     * Registra un {@link ActionListener} para eventos provenientes de la vista
     * (por ejemplo, presionar el botón de solicitar turno).
     *
     * @param al listener a registrar
     */
    public void setActionListener(ActionListener al);

    /**
     * Limpia el campo de ingreso de DNI en la vista.
     */
    public void limpiarCampoDNI();
}
