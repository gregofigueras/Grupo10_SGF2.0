package Globales;

import ServidorCentral.seguridad.Encriptador;

public class UtilsEncriptacion {
    private static Encriptador encriptador;

    public static void setEncriptador(Encriptador enc) {
        encriptador = enc;
    }

    public static String encriptarDNI(String dni) {
        if (encriptador == null) return dni;
        return encriptador.encriptar(dni);
    }

    public static String desencriptarDNI(String dniEncriptado) {
        if (encriptador == null) return dniEncriptado;
        return encriptador.desencriptar(dniEncriptado);
    }
}
