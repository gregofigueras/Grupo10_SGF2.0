package ServidorCentral.persistencia;

import Globales.Turno;
import ServidorCentral.seguridad.Encriptador;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Queue;

public class ColaEsperaTP implements IColaEsperaDAO {
    private final String RUTA_ARCHIVO = "cola_espera.txt";
    private final Encriptador encriptador = new Encriptador(123456);

    private Queue<Turno> leerArchivo() {
        Queue<Turno> cola = new LinkedList<>();
        try {
            File file = new File(RUTA_ARCHIVO);
            if (!file.exists()) {
                return cola;
            }

            String encriptado = new String(Files.readAllBytes(Paths.get(RUTA_ARCHIVO)));
            if (encriptado.trim().isEmpty()) {
                return cola;
            }

            String datosPuros = encriptador.desencriptar(encriptado);
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

                Turno t = new Turno(dniReal);
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
                        .append(t.getIntentosLlamado()).append("\n");
            }

            String encriptado = encriptador.encriptar(sb.toString());
            Files.write(Paths.get(RUTA_ARCHIVO), encriptado.getBytes());
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
