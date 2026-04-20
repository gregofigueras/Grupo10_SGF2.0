package modelo;

public class Turno {
    private String dniCliente;
    private int puestoAtencion; // Para saber a qué puesto debe ir
    private int intentosLlamado; // Para la re-notificación (máximo 3)

    public Turno(String dniCliente) {
        this.dniCliente = dniCliente;
        this.intentosLlamado = 0; // Inicia en 0 intentos
        this.puestoAtencion = -1; // -1 significa que aún no fue asignado a un puesto
    }

    public String getDniCliente() { return dniCliente; }
    public void setDniCliente(String dniCliente) { this.dniCliente = dniCliente; }

    public int getPuestoAtencion() { return puestoAtencion; }
    public void setPuestoAtencion(int puestoAtencion) { this.puestoAtencion = puestoAtencion; }

    public int getIntentosLlamado() { return intentosLlamado; }
    public void incrementarIntentos() { this.intentosLlamado++; }
}