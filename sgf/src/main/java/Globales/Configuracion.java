package Globales;

/**
 * Contiene la configuración IP de un componente<br>
 * 
 * Invariantes de clase:
 * <ul>
 * <li>ipPrimario != null</li>
 * <li>ipRespaldo != null</li>
 * <li>puertoServidor > 0</li>
 * </ul>
 */
public class Configuracion {

    private String ipPrimario;
    private String ipRespaldo;
    private int puertoServidor;

    /**
     * Crea una nueva configuración con los datos dados<br>
     * 
     * @param ipPrimario     - IP del servidor primario
     * @param ipRespaldo     - IP del servidor de respaldo
     * @param puertoServidor - Puerto del servidor
     */
    public Configuracion(String ipPrimario, String ipRespaldo, int puertoServidor) {
        assert ipPrimario != null : "La IP primaria no puede ser null";
        assert ipRespaldo != null : "La IP de respaldo no puede ser null";
        assert puertoServidor > 0 : "El puerto del servidor debe ser mayor a 0";
        this.ipPrimario = ipPrimario;
        this.ipRespaldo = ipRespaldo;
        this.puertoServidor = puertoServidor;
    }

    /**
     * Devuelve la IP del servidor primario<br>
     * 
     * @return IP del servidor primario
     */
    public String getIpPrimario() {
        return ipPrimario;
    }

    /**
     * Devuelve la IP del servidor de respaldo<br>
     * 
     * @return IP del servidor de respaldo
     */
    public String getIpRespaldo() {
        return ipRespaldo;
    }

    /**
     * Devuelve el puerto del servidor<br>
     * 
     * @return Puerto del servidor
     */
    public int getPuertoServidor() {
        return puertoServidor;
    }
}
