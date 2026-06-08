package Globales;

/**
 * Fábrica para crear objetos de tipo Configuración<br>
 * Consiste de un único método estático que recibe los parámetros necesarios
 * para crear una configuración y devuelve un objeto de tipo Configuración<br>
 */
public class ConfiguracionFactory {
    /**
     * Crea una nueva configuración con los datos dados<br>
     * Precondiciones:
     * <ul>
     * <li>ipPrimario != null</li>
     * <li>ipRespaldo != null</li>
     * <li>puertoServidor > 0</li>
     * </ul>
     * 
     * @param ipPrimario     - IP del servidor primario
     * @param ipRespaldo     - IP del servidor de respaldo
     * @param puertoServidor - Puerto del servidor
     * @return - Objeto de tipo Configuración
     */
    public static Configuracion crearConfiguracion(String ipPrimario, String ipRespaldo, int puertoServidor) {
        return new Configuracion(ipPrimario, ipRespaldo, puertoServidor);
    }
}
