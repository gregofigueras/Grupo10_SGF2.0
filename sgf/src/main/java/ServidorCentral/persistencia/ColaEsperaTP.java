package ServidorCentral.persistencia;

import Globales.Turno;
import ServidorCentral.seguridad.Encriptador;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Queue;

public class ColaEsperaTP implements IColaEsperaDAO {
    private final String RUTA_ARCHIVO = "cola_espera" + ConfigPersistencia.getSufijo() + ".txt";
    private final Encriptador encriptador = new Encriptador(123456);

    private Queue<Turno> leerArchivo() {
        Queue<Turno> cola = new LinkedList<>();
        try {
            File file = new File(RUTA_ARCHIVO);
            if (!file.exists()) {
                return cola;
            }

            String datosPuros = new String(Files.readAllBytes(Paths.get(RUTA_ARCHIVO)));
            if (datosPuros.trim().isEmpty()) {
                return cola;
            }

            String[] lineas = datosPuros.split("\n");
            for (String linea : lineas) {
                if (linea.trim().isEmpty()) {
                    continue;
                }

                String[] campos = linea.split(";");
                if (campos.length < 3) {
                    continue;
                }

                String dniGuardado = campos[0];
                String dniReal = dniGuardado;
                try {
                    dniReal = encriptador.desencriptar(dniGuardado);
                } catch (Exception ignored) {
                }

                boolean expirado = false;
                if (campos.length > 3) {
                    expirado = Boolean.parseBoolean(campos[3].trim());
                }

                Turno t = new Turno(dniReal, expirado);
                t.setPuestoAtencion(Integer.parseInt(campos[1]));

                int intentos = Integer.parseInt(campos[2]);
                for (int i = 0; i < intentos; i++) {
                    t.incrementarIntentos();
                }

                cola.add(t);
            }
        } catch (Exception e) {
        }
        return cola;
    }

    private void escribirArchivo(Queue<Turno> cola) {
        try {
            StringBuilder sb = new StringBuilder();

            for (Turno t : cola) {
                String dniEncriptado = t.getDniCliente();
                if (dniEncriptado != null && !dniEncriptado.isEmpty()) {
                    dniEncriptado = encriptador.encriptar(dniEncriptado);
                }

                sb.append(dniEncriptado).append(";")
                        .append(t.getPuestoAtencion()).append(";")
                        .append(t.getIntentosLlamado()).append(";")
                        .append(t.isExpirado()).append("\n");
            }

            Files.write(Paths.get(RUTA_ARCHIVO), sb.toString().getBytes());
        } catch (Exception e) {
        }
    }

    @Override
    public void guardarTurno(Turno turno) {
        Queue<Turno> cola = leerArchivo();
        cola.add(turno);
        escribirArchivo(cola);
    }

    @Override
    public Turno getSiguiente() {
        Queue<Turno> cola = leerArchivo();
        Turno t = cola.poll();
        escribirArchivo(cola);
        return t;
    }

    @Override
    public boolean existeCliente(String dni) {
        return leerArchivo().stream().anyMatch(t -> t.getDniCliente().equals(dni));
    }

    @Override
    public Queue<Turno> getTodosPendientes() {
        return leerArchivo();
    }
}
