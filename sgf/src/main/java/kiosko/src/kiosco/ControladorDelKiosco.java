package kiosko.src.kiosco;

import java.awt.event.ActionListener;

import Globales.Configuracion;

import java.awt.event.ActionEvent;

/**
 * Controlador del kiosco que medía la interacción entre la vista de cliente
 * y el modelo `Kiosco`.
 *
 * <p>
 * Implementa `ActionListener` para recibir eventos generados por la
 * interfaz de usuario (por ejemplo, solicitudes de turno). La lógica de
 * procesamiento de turnos se delega al modelo `Kiosco`.
 * </p>
 */
public class ControladorDelKiosco implements ActionListener {
    /**
     * Referencia a la vista que interactúa con el cliente (entrada/salida).
     */
    private InterfazDelCliente vista;

    /**
     * Modelo que contiene la lógica de negocio del kiosco (gestión de turnos).
     */
    private Kiosco modelo;

    /**
     * Crea e inicializa el controlador.
     *
     * <p>
     * Construye la vista `VistaKiosco`, obtiene la configuración de la misma
     * y crea el modelo `Kiosco` con dicha configuración. Finalmente registra
     * este controlador como `ActionListener` de la vista.
     * </p>
     */
    public ControladorDelKiosco() {
        Configuracion config;
        this.vista = new VistaKiosco();
        config = vista.getConfiguracion();
        if (config == null) {
            System.exit(0);
        }
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
