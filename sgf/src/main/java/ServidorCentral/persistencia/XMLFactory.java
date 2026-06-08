package ServidorCentral.persistencia;

public class XMLFactory implements PersistenciaFactory {

    @Override
    public IColaEsperaDAO crearColaEsperaDAO() {
        return new ColaEsperaXML();
    }

    @Override
    public IHistorialLlamadosDAO crearHistorialLlamadosDAO() {
        return new HistorialLlamadosXML();
    }

    @Override
    public IRenotificacionDAO crearRenotificacionDAO() {
        return new RenotificacionXML();
    }
}