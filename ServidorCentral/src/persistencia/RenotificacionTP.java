package persistencia;

import seguridad.Encriptador;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class RenotificacionTP implements IRenotificacionDAO {
    private final String RUTA_ARCHIVO = "renotificaciones.txt";
    private final Encriptador encriptador = new Encriptador(123456);

    private Map<String, Integer> leerArchivo() {
        Map<String, Integer> mapa = new HashMap<>();
        try {
            File file = new File(RUTA_ARCHIVO);
            if (!file.exists()) return mapa;
            String encriptado = new String(Files.readAllBytes(Paths.get(RUTA_ARCHIVO)));
            if (encriptado.trim().isEmpty()) return mapa;

            String datosPuros = encriptador.desencriptar(encriptado);
            String[] lineas = datosPuros.split("\n");
            for (String linea : lineas) {
                if (linea.trim().isEmpty()) continue;
                String[] campos = linea.split(";");
                mapa.put(campos[0], Integer.parseInt(campos[1]));
            }
        } catch (Exception e) { }
        return mapa;
    }

    private void escribirArchivo(Map<String, Integer> mapa) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> entry : mapa.entrySet()) {
                sb.append(entry.getKey()).append(";").append(entry.getValue()).append("\n");
            }
            String encriptado = encriptador.encriptar(sb.toString());
            Files.write(Paths.get(RUTA_ARCHIVO), encriptado.getBytes());
        } catch (Exception e) { }
    }

    @Override
    public void registrarIntentoReintentado(String dni) {
        Map<String, Integer> intentos = leerArchivo();
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