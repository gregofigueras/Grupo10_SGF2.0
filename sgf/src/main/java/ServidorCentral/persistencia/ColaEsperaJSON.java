package ServidorCentral.persistencia;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import Globales.Turno;
import ServidorCentral.seguridad.Encriptador;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Queue;
import java.lang.reflect.Type;

public class ColaEsperaJSON implements IColaEsperaDAO {
    private final String RUTA_ARCHIVO = "cola_espera" + ConfigPersistencia.getSufijo() + ".json";
    private final Gson gson = new Gson();
    private final Encriptador encriptador = new Encriptador(123456); // Clave secreta numérica

    // Métodos privados para no repetir código de lectura/escritura
    private Queue<Turno> leerArchivo() {
        try {
            File file = new File(RUTA_ARCHIVO);
            if (!file.exists())
                return new LinkedList<>();

            String json = new String(Files.readAllBytes(Paths.get(RUTA_ARCHIVO)));
            if (json.isEmpty())
                return new LinkedList<>();

            Type tipoCola = new TypeToken<LinkedList<Turno>>() {}.getType();
            Queue<Turno> colaGuardada = gson.fromJson(json, tipoCola);

            Queue<Turno> cola = new LinkedList<>();
            if (colaGuardada != null) {
                for (Turno t : colaGuardada) {
                    String dniReal = t.getDniCliente();
                    try {
                        dniReal = encriptador.desencriptar(dniReal);
                    } catch (Exception ignored) {
                    }

                    Turno copia = new Turno(dniReal, t.isExpirado());
                    copia.setPuestoAtencion(t.getPuestoAtencion());
                    for (int i = 0; i < t.getIntentosLlamado(); i++) {
                        copia.incrementarIntentos();
                    }
                    cola.add(copia);
                }
            }

            return cola;
        } catch (Exception e) {
            return new LinkedList<>();
        }
    }


    private void escribirArchivo(Queue<Turno> cola) {
        try {
            Queue<Turno> colaParaGuardar = new LinkedList<>();

            for (Turno t : cola) {
                Turno copia = new Turno(encriptador.encriptar(t.getDniCliente()), t.isExpirado());
                copia.setPuestoAtencion(t.getPuestoAtencion());

                for (int i = 0; i < t.getIntentosLlamado(); i++) {
                    copia.incrementarIntentos();
                }

                colaParaGuardar.add(copia);
            }

            String json = gson.toJson(colaParaGuardar);
            Files.write(Paths.get(RUTA_ARCHIVO), json.getBytes());
        } catch (Exception e) {
            System.out.println("Error guardando cola de espera: " + e.getMessage());
        }
    }


    // --- MÉTODOS DE LA INTERFAZ ---

    @Override
    public void guardarTurno(Turno turno) {
        Queue<Turno> cola = leerArchivo();
        cola.add(turno);
        escribirArchivo(cola);
    }

    @Override
    public Turno getSiguiente() {
        Queue<Turno> cola = leerArchivo();
        Turno t = cola.poll(); // Saca el primero
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