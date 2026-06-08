package kiosko.src.kiosco;

import java.awt.event.ActionListener;

import Globales.Configuracion;

import java.awt.event.ActionEvent;

public class ControladorDelKiosco implements ActionListener {
    private InterfazDelCliente vista;
    private Kiosco modelo;

    public ControladorDelKiosco() {
        Configuracion config;
        this.vista = new VistaKiosco();
        config = vista.getConfiguracion();
        this.modelo = new Kiosco(config);
        this.vista.setActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (InterfazDelCliente.SOLICITAR.equals(e.getActionCommand())) {
            int dni = vista.getDNI();
            String resultado = modelo.procesarTurno(String.valueOf(dni));
            if ("OK".equals(resultado)) {
                vista.mostrarMensajeExito("¡Turno Confirmado!", "DNI registrado exitosamente.");
                vista.limpiarCampoDNI();
            } else if ("DUPLICADO".equals(resultado)) {
                vista.mostrarMensajeError("Turno Duplicado", "El DNI ya está esperando en la fila.");
            } else {
                vista.mostrarMensajeError("Error de Conexión", "No se pudo conectar con el Servidor.");
            }
        }
    }

}
