package ServidorCentral.persistencia;

public class TextoPlanoFactory implements PersistenciaFactory {

    @Override
    public IColaEsperaDAO crearColaEsperaDAO() {
        return new ColaEsperaTP();
    }

    @Override
    public IHistorialLlamadosDAO crearHistorialLlamadosDAO() {
        return new HistorialLlamadosTP();
    }

    @Override
    public IRenotificacionDAO crearRenotificacionDAO() {
        return new RenotificacionTP();
    }
}