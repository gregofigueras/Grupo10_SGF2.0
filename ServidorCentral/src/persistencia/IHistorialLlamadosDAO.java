package persistencia;

import modelo.Turno;
import java.util.List;

public interface IHistorialLlamadosDAO {
    void registrarLlamado(Turno turno);
    List<Turno> obtenerUltimosLlamados(int cantidad);
}