package Operador.src.operador;

import java.awt.event.ActionListener;

public interface InterfazDeTrabajo {
    public static final String LLAMAR = "LLAMAR";
    public static final String RELLAMAR = "RELLAMAR";

    public void setActionListener(ActionListener al);

    public void resetearBotonRellamar();

    public void setTurnoActual(String turno);

    public void inhabilitarBotonRellamar();
}
