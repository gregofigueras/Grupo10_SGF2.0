package ServidorCentral.persistencia;

public class JSONFactory implements PersistenciaFactory {

    @Override
    public IColaEsperaDAO crearColaEsperaDAO() {
        return new ColaEsperaJSON();
    }

    @Override
    public IHistorialLlamadosDAO crearHistorialLlamadosDAO() {
        return new HistorialLlamadosJSON();
    }

    @Override
    public IRenotificacionDAO crearRenotificacionDAO() {
        return new RenotificacionJSON();
    }
}