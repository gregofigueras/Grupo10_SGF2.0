package ServidorCentral.persistencia;

import Globales.Turno;
import ServidorCentral.seguridad.Encriptador;

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
            if (!file.exists())
                return historial;

            // LEER TXT NORMAL (NO ENCRIPTADO)
            String datosPuros = new String(Files.readAllBytes(Paths.get(RUTA_ARCHIVO)));
            if (datosPuros.trim().isEmpty())
                return historial;

            String[] lineas = datosPuros.split("\n");
            for (String linea : lineas) {
                if (linea.trim().isEmpty())
                    continue;
                String[] campos = linea.split(";");

                // DESENCRIPTAR SOLO EL DNI (primer campo)
                String dniEncriptado = campos[0];
                String dniReal = dniEncriptado;
                try {
                    dniReal = encriptador.desencriptar(dniEncriptado);
                } catch (Exception e) {
                    // El DNI no estaba encriptado
                }

                Turno t = new Turno(dniReal);
                t.setPuestoAtencion(Integer.parseInt(campos[1]));
                int intentos = Integer.parseInt(campos[2]);
                for (int i = 0; i < intentos; i++)
                    t.incrementarIntentos();
                historial.add(t);
            }
        } catch (Exception e) {
        }
        return historial;
    }

    private void escribirArchivo(List<Turno> lista) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Turno t : lista) {
                // ENCRIPTAR SOLO EL DNI
                String dniEncriptado = encriptador.encriptar(t.getDniCliente());

                sb.append(dniEncriptado).append(";")
                        .append(t.getPuestoAtencion()).append(";")
                        .append(t.getIntentosLlamado()).append("\n");
            }
            // GUARDAR TXT SIN ENCRIPTAR (SOLO DNIs ENCRIPTADOS)
            Files.write(Paths.get(RUTA_ARCHIVO), sb.toString().getBytes());
        } catch (Exception e) {
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
        if (historial.size() <= cantidad)
            return historial;
        return historial.subList(historial.size() - cantidad, historial.size());
    }
}
