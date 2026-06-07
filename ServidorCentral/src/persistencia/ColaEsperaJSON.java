package persistencia;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import modelo.Turno;
import seguridad.Encriptador;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Queue;
import java.lang.reflect.Type;

public class ColaEsperaJSON implements IColaEsperaDAO {
    private final String RUTA_ARCHIVO = "cola_espera.json";
    private final Gson gson = new Gson();
    private final Encriptador encriptador = new Encriptador(123456); // Clave secreta numérica

    // Métodos privados para no repetir código de lectura/escritura
    private Queue<Turno> leerArchivo() {
        try {
            File file = new File(RUTA_ARCHIVO);
            if (!file.exists()) return new LinkedList<>();

            String encriptado = new String(Files.readAllBytes(Paths.get(RUTA_ARCHIVO)));
            if (encriptado.isEmpty()) return new LinkedList<>();

            String json = encriptador.desencriptar(encriptado);
            // TypeToken le enseña a Gson cómo leer una Colección genérica
            Type tipoCola = new TypeToken<LinkedList<Turno>>(){}.getType();
            Queue<Turno> cola = gson.fromJson(json, tipoCola);

            return cola == null ? new LinkedList<>() : cola;
        } catch (Exception e) {
            return new LinkedList<>();
        }
    }

    private void escribirArchivo(Queue<Turno> cola) {
        try {
            String json = gson.toJson(cola);
            String encriptado = encriptador.encriptar(json);
            Files.write(Paths.get(RUTA_ARCHIVO), encriptado.getBytes());
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