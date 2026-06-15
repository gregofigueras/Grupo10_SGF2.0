package Pantalla.src.pantalla;

/**
 * Interfaz que define los métodos que la pantalla pública debe implementar para
 * interactuar con el main. Permite obtener la configuración actual
 * de la pantalla, como el puerto en el que está escuchando.
 */
public interface InterfazPublico {
    /**
     * Obtiene la configuración actual de la pantalla.
     * 
     * @return Puerto de la pantalla o -2 si se ha cancelado, -1 si la configuración
     *         es inválida.
     */
    public int getConfiguracion();
}
