package modelo;

public class Turno {

    private String dniCliente;

    // Podrías agregar la fecha/hora acá si quisieras llevar estadísticas en el futuro,
    // pero para cumplir con la lógica FIFO estricta del MVP, el DNI alcanza.

    public Turno(String dniCliente) {
        this.dniCliente = dniCliente;
    }

    public String getDniCliente() {
        return dniCliente;
    }

    public void setDniCliente(String dniCliente) {
        this.dniCliente = dniCliente;
    }
}