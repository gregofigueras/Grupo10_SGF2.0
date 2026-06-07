package seguridad;

public interface Strategy {
    String encriptar(String x, int clave);
    String desencriptar(String x, int clave);
}