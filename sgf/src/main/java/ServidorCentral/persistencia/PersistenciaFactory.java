package ServidorCentral.persistencia;

public interface PersistenciaFactory {
    IColaEsperaDAO crearColaEsperaDAO();

    IHistorialLlamadosDAO crearHistorialLlamadosDAO();

    IRenotificacionDAO crearRenotificacionDAO();
}