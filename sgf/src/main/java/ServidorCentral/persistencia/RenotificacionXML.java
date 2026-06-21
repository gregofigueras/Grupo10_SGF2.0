package ServidorCentral.persistencia;

import ServidorCentral.seguridad.Encriptador;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class RenotificacionXML implements IRenotificacionDAO {
    private final String RUTA_ARCHIVO = "renotificaciones" + ConfigPersistencia.getSufijo() + ".xml";
    private final Encriptador encriptador = new Encriptador(123456);

    private Map<String, Integer> leerArchivo() {
        Map<String, Integer> mapa = new HashMap<>();
        try {
            File file = new File(RUTA_ARCHIVO);
            if (!file.exists())
                return mapa;
            String xml = new String(Files.readAllBytes(Paths.get(RUTA_ARCHIVO)));
            if (xml.trim().isEmpty())
                return mapa;

            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));

            NodeList nList = doc.getElementsByTagName("registro");
            for (int i = 0; i < nList.getLength(); i++) {
                Element e = (Element) nList.item(i);
                String dniGuardado = e.getElementsByTagName("dni").item(0).getTextContent();
                String dniReal = dniGuardado;
                try {
                    dniReal = encriptador.desencriptar(dniGuardado);
                } catch (Exception ignored) {
                }
                int intentos = Integer.parseInt(e.getElementsByTagName("intentos").item(0).getTextContent());
                mapa.put(dniReal, intentos);
            }
        } catch (Exception e) {
        }
        return mapa;
    }

    private void escribirArchivo(Map<String, Integer> mapa) {
        try {
            StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<renotificaciones>\n");
            for (Map.Entry<String, Integer> entry : mapa.entrySet()) {
                String dniEncriptado = entry.getKey();
                if (dniEncriptado != null && !dniEncriptado.isEmpty()) {
                    dniEncriptado = encriptador.encriptar(dniEncriptado);
                }

                sb.append("  <registro>\n")
                        .append("    <dni>").append(dniEncriptado).append("</dni>\n")
                        .append("    <intentos>").append(entry.getValue()).append("</intentos>\n")
                        .append("  </registro>\n");
            }
            sb.append("</renotificaciones>");

            Files.write(Paths.get(RUTA_ARCHIVO), sb.toString().getBytes());
        } catch (Exception e) {
        }
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