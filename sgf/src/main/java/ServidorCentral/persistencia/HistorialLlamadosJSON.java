package ServidorCentral.persistencia;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import Globales.Turno;
import ServidorCentral.seguridad.Encriptador;

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
            if (!file.exists()) {
                return new ArrayList<>();
            }

            String json = new String(Files.readAllBytes(Paths.get(RUTA_ARCHIVO)));
            if (json.trim().isEmpty()) {
                return new ArrayList<>();
            }

            Type tipoLista = new TypeToken<ArrayList<Turno>>() {}.getType();
            List<Turno> listaGuardada = gson.fromJson(json, tipoLista);

            List<Turno> lista = new ArrayList<>();
            if (listaGuardada != null) {
                for (Turno t : listaGuardada) {
                    String dniGuardado = t.getDniCliente();
                    String dniReal = dniGuardado;

                    try {
                        dniReal = encriptador.desencriptar(dniGuardado);
                    } catch (Exception ignored) {
                        // Si ya venía en claro, lo dejamos igual
                    }

                    Turno copia = new Turno(dniReal);
                    copia.setPuestoAtencion(t.getPuestoAtencion());
                    for (int i = 0; i < t.getIntentosLlamado(); i++) {
                        copia.incrementarIntentos();
                    }
                    lista.add(copia);
                }
            }

            return lista;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void escribirArchivo(List<Turno> lista) {
        try {
            List<Turno> listaParaGuardar = new ArrayList<>();

            for (Turno t : lista) {
                String dniEncriptado = t.getDniCliente();
                if (dniEncriptado != null && !dniEncriptado.isEmpty()) {
                    dniEncriptado = encriptador.encriptar(dniEncriptado);
                }

                Turno copia = new Turno(dniEncriptado);
                copia.setPuestoAtencion(t.getPuestoAtencion());
                for (int i = 0; i < t.getIntentosLlamado(); i++) {
                    copia.incrementarIntentos();
                }
                listaParaGuardar.add(copia);
            }

            String json = gson.toJson(listaParaGuardar);
            Files.write(Paths.get(RUTA_ARCHIVO), json.getBytes());
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
        if (historial.size() <= cantidad) {
            return historial;
        }
        return historial.subList(historial.size() - cantidad, historial.size());
    }
}
