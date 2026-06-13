package Operador.src.operador;

import Globales.Configuracion;

public class ConfiguracionPuesto extends Configuracion {
    private int idPuesto;

    public ConfiguracionPuesto(String ipPrimario, String ipRespaldo, int puertoServidor, int idPuesto) {
        super(ipPrimario, ipRespaldo, puertoServidor);
        this.idPuesto = idPuesto;
    }

    public int getIdPuesto() {
        return idPuesto;
    }
}
