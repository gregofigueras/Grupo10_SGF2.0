package persistencia;

import seguridad.Encriptador;
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
    private final String RUTA_ARCHIVO = "renotificaciones.xml";
    private final Encriptador encriptador = new Encriptador(123456);

    private Map<String, Integer> leerArchivo() {
        Map<String, Integer> mapa = new HashMap<>();
        try {
            File file = new File(RUTA_ARCHIVO);
            if (!file.exists()) return mapa;
            String encriptado = new String(Files.readAllBytes(Paths.get(RUTA_ARCHIVO)));
            if (encriptado.trim().isEmpty()) return mapa;

            String xml = encriptador.desencriptar(encriptado);
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));

            NodeList nList = doc.getElementsByTagName("registro");
            for (int i = 0; i < nList.getLength(); i++) {
                Element e = (Element) nList.item(i);
                String dni = e.getElementsByTagName("dni").item(0).getTextContent();
                int intentos = Integer.parseInt(e.getElementsByTagName("intentos").item(0).getTextContent());
                mapa.put(dni, intentos);
            }
        } catch (Exception e) { }
        return mapa;
    }

    private void escribirArchivo(Map<String, Integer> mapa) {
        try {
            StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<renotificaciones>\n");
            for (Map.Entry<String, Integer> entry : mapa.entrySet()) {
                sb.append("  <registro>\n")
                        .append("    <dni>").append(entry.getKey()).append("</dni>\n")
                        .append("    <intentos>").append(entry.getValue()).append("</intentos>\n")
                        .append("  </registro>\n");
            }
            sb.append("</renotificaciones>");

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