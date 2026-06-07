package persistencia;

public interface IRenotificacionDAO {
    void registrarIntentoReintentado(String dni);
    int obtenerCantidadIntentos(String dni);
    void limpiarHistorialIntentos(String dni);
}