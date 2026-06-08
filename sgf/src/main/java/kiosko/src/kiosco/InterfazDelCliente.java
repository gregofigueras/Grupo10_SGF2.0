package kiosko.src.kiosco;

import java.awt.event.ActionListener;
import Globales.Configuracion;

public interface InterfazDelCliente {
    public static final String SOLICITAR = "SOLICITAR";

    public int getDNI();

    public Configuracion getConfiguracion();

    public void mostrarMensajeExito(String titulo, String detalle);

    public void mostrarMensajeError(String titulo, String detalle);

    public void setActionListener(ActionListener al);

    public void limpiarCampoDNI();
}
