package persistencia;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import modelo.Turno;
import seguridad.Encriptador;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Type;

public class HistorialLlamadosJSON implements IHistorialLlamadosDAO {
    private final String RUTA_ARCHIVO = "historial_llamados.json";
    private final Gson gson = new Gson();
    private final Encriptador encriptador = new Encriptador(123456);

    private List<Turno> leerArchivo() {
        try {
            File file = new File(RUTA_ARCHIVO);
            if (!file.exists()) return new ArrayList<>();
            String encriptado = new String(Files.readAllBytes(Paths.get(RUTA_ARCHIVO)));
            if (encriptado.isEmpty()) return new ArrayList<>();

            String json = encriptador.desencriptar(encriptado);
            Type tipoLista = new TypeToken<ArrayList<Turno>>(){}.getType();
            List<Turno> lista = gson.fromJson(json, tipoLista);
            return lista == null ? new ArrayList<>() : lista;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void escribirArchivo(List<Turno> lista) {
        try {
            String json = gson.toJson(lista);
            String encriptado = encriptador.encriptar(json);
            Files.write(Paths.get(RUTA_ARCHIVO), encriptado.getBytes());
        } catch (Exception e) {
            System.out.println("Error guardando historial: " + e.getMessage());
        }
    }

    @Override
    public void registrarLlamado(Turno turno) {
        List<Turno> historial = leerArchivo();
        historial.add(turno);
        escribirArchivo(historial);
    }

    @Override
    public List<Turno> obtenerUltimosLlamados(int cantidad) {
        List<Turno> historial = leerArchivo();
        if (historial.size() <= cantidad) return historial;
        return historial.subList(historial.size() - cantidad, historial.size());
    }
}