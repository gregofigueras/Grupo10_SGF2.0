package ServidorCentral.persistencia;

public class ConfigPersistencia {
    private static String sufijo = "_Nodo1";

    public static void setSufijo(String nuevoSufijo) {
        sufijo = nuevoSufijo;
    }

    public static String getSufijo() {
        return sufijo;
    }
}