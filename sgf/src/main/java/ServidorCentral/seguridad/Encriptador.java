package ServidorCentral.seguridad;

public class Encriptador {
    private int clave;
    private Strategy estrategia;

    public Encriptador(int clave) {
        this.clave = clave;
        // Según tu diagrama, el Encriptador conoce a la estrategia concreta
        this.estrategia = new CifradoSimetrico();
    }

    public String encriptar(String x) {
        return estrategia.encriptar(x, this.clave);
    }

    public String desencriptar(String x) {
        return estrategia.desencriptar(x, this.clave);
    }
}