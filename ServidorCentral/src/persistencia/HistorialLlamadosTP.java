package persistencia;

import modelo.Turno;
import seguridad.Encriptador;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class HistorialLlamadosTP implements IHistorialLlamadosDAO {
    private final String RUTA_ARCHIVO = "historial_llamados.txt";
    private final Encriptador encriptador = new Encriptador(123456);

    private List<Turno> leerArchivo() {
        List<Turno> historial = new ArrayList<>();
        try {
            File file = new File(RUTA_ARCHIVO);
            if (!file.exists()) return historial;
            String encriptado = new String(Files.readAllBytes(Paths.get(RUTA_ARCHIVO)));
            if (encriptado.trim().isEmpty()) return historial;

            String datosPuros = encriptador.desencriptar(encriptado);
            String[] lineas = datosPuros.split("\n");
            for (String linea : lineas) {
                if (linea.trim().isEmpty()) continue;
                String[] campos = linea.split(";");
                Turno t = new Turno(campos[0]);
                t.setPuestoAtencion(Integer.parseInt(campos[1]));
                int intentos = Integer.parseInt(campos[2]);
                for (int i = 0; i < intentos; i++) t.incrementarIntentos();
                historial.add(t);
            }
        } catch (Exception e) { }
        return historial;
    }

    private void escribirArchivo(List<Turno> lista) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Turno t : lista) {
                sb.append(t.getDniCliente()).append(";")
                        .append(t.getPuestoAtencion()).append(";")
                        .append(t.getIntentosLlamado()).append("\n");
            }
            String encriptado = encriptador.encriptar(sb.toString());
            Files.write(Paths.get(RUTA_ARCHIVO), encriptado.getBytes());
        } catch (Exception e) { }
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