package Operador.src.operador;

import Globales.Configuracion;

/**
 * Clase que representa la configuración específica para un puesto de atención.
 */
public class ConfiguracionPuesto extends Configuracion {
    private int idPuesto;

    /**
     * Constructor para la configuración del puesto de atención. Hereda los
     * parámetros de conexión del servidor y agrega el ID del puesto.
     * 
     * @param ipPrimario
     * @param ipRespaldo
     * @param puertoServidor
     * @param idPuesto
     */
    public ConfiguracionPuesto(String ipPrimario, String ipRespaldo, int puertoServidor, int idPuesto) {
        super(ipPrimario, ipRespaldo, puertoServidor);
        this.idPuesto = idPuesto;
    }

    /**
     * Obtiene el ID del puesto de atención.
     * 
     * @return El ID del puesto de atención.
     */
    public int getIdPuesto() {
        return idPuesto;
    }
}
