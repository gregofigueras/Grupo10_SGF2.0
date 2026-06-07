package persistencia;

import modelo.Turno;
import java.util.Queue;

public interface IColaEsperaDAO {
    void guardarTurno(Turno turno);
    Turno getSiguiente();
    boolean existeCliente(String dni);
    Queue<Turno> getTodosPendientes();
}