package Globales;

/**
 * Representa un turno asignado a un cliente identificado por su DNI.
 *
 * <p>
 * Esta clase lleva el control del DNI del cliente, el puesto de atención
 * asignado (si corresponde) y la cantidad de intentos de llamado realizados,
 * utilizada para lógicas de re-notificación.
 * </p>
 *
 * Uso típico:
 * 
 * <pre>
 * Turno t = new Turno("12345678");
 * t.setPuestoAtencion(2);
 * t.incrementarIntentos();
 * </pre>
 */
public class Turno {
    /** DNI del cliente asociado al turno. */
    private String dniCliente;

    /**
     * Número del puesto de atención al que debe dirigirse el cliente.
     * Un valor de {@code -1} indica que aún no se asignó ningún puesto.
     */
    private int puestoAtencion; // Para saber a qué puesto debe ir

    /**
     * Contador de intentos de llamado para este turno. Se utiliza en la lógica
     * de re-notificaciones; por convención el sistema puede limitarlo (p.ej. 3).
     */
    private int intentosLlamado; // Para la re-notificación (máximo 3)

    /**
     * Crea un nuevo turno para el cliente con el DNI indicado.
     *
     * @param dniCliente DNI del cliente asociado al turno
     */
    public Turno(String dniCliente) {
        this.dniCliente = dniCliente;
        this.intentosLlamado = 0; // Inicia en 0 intentos
        this.puestoAtencion = -1; // -1 significa que aún no fue asignado a un puesto
    }

    /**
     * Devuelve el DNI del cliente asociado a este turno.
     *
     * @return DNI del cliente
     */
    public String getDniCliente() {
        return dniCliente;
    }

    /**
     * Establece el DNI del cliente asociado a este turno.
     *
     * @param dniCliente nuevo DNI del cliente
     */
    public void setDniCliente(String dniCliente) {
        this.dniCliente = dniCliente;
    }

    /**
     * Devuelve el número del puesto de atención asignado.
     *
     * @return número de puesto, o {@code -1} si no está asignado
     */
    public int getPuestoAtencion() {
        return puestoAtencion;
    }

    /**
     * Asigna un puesto de atención al turno.
     *
     * @param puestoAtencion número del puesto a asignar (usar {@code -1} para
     *                       indicar "no asignado")
     */
    public void setPuestoAtencion(int puestoAtencion) {
        this.puestoAtencion = puestoAtencion;
    }

    /**
     * Devuelve la cantidad de intentos de llamado realizados para este turno.
     *
     * @return número de intentos de llamado
     */
    public int getIntentosLlamado() {
        return intentosLlamado;
    }

    /**
     * Incrementa en uno el contador de intentos de llamado.
     *
     * <p>
     * No realiza validaciones sobre el máximo de intentos; la política de
     * límite (p.ej. 3) debe aplicarla la lógica de negocio que use esta clase.
     * </p>
     */
    public void incrementarIntentos() {
        this.intentosLlamado++;
    }
}