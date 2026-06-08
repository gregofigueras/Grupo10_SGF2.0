package ServidorCentral.persistencia;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ServidorCentral.seguridad.Encriptador;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Type;

public class RenotificacionJSON implements IRenotificacionDAO {
    private final String RUTA_ARCHIVO = "renotificaciones.json";
    private final Gson gson = new Gson();
    private final Encriptador encriptador = new Encriptador(123456);

    private Map<String, Integer> leerArchivo() {
        try {
            File file = new File(RUTA_ARCHIVO);
            if (!file.exists())
                return new HashMap<>();
            String encriptado = new String(Files.readAllBytes(Paths.get(RUTA_ARCHIVO)));
            if (encriptado.isEmpty())
                return new HashMap<>();

            String json = encriptador.desencriptar(encriptado);
            Type tipoMapa = new TypeToken<HashMap<String, Integer>>() {
            }.getType();
            Map<String, Integer> mapa = gson.fromJson(json, tipoMapa);
            return mapa == null ? new HashMap<>() : mapa;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private void escribirArchivo(Map<String, Integer> mapa) {
        try {
            String json = gson.toJson(mapa);
            String encriptado = encriptador.encriptar(json);
            Files.write(Paths.get(RUTA_ARCHIVO), encriptado.getBytes());
        } catch (Exception e) {
            System.out.println("Error guardando renotificaciones: " + e.getMessage());
        }
    }

    @Override
    public void registrarIntentoReintentado(String dni) {
        Map<String, Integer> intentos = leerArchivo();
        // Si el DNI existe le suma 1, si no, lo crea con valor 1
        intentos.put(dni, intentos.getOrDefault(dni, 0) + 1);
        escribirArchivo(intentos);
    }

    @Override
    public int obtenerCantidadIntentos(String dni) {
        return leerArchivo().getOrDefault(dni, 0);
    }

    @Override
    public void limpiarHistorialIntentos(String dni) {
        Map<String, Integer> intentos = leerArchivo();
        intentos.remove(dni);
        escribirArchivo(intentos);
    }
}