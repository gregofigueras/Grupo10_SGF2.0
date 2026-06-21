package ServidorCentral.persistencia;

import Globales.Turno;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class HistorialLlamadosXML implements IHistorialLlamadosDAO {
    private final String RUTA_ARCHIVO = "historial_llamados.xml";

    private List<Turno> leerArchivo() {
        List<Turno> historial = new ArrayList<>();
        try {
            File file = new File(RUTA_ARCHIVO);
            if (!file.exists()) {
                return historial;
            }

            String xml = new String(Files.readAllBytes(Paths.get(RUTA_ARCHIVO)));
            if (xml.trim().isEmpty()) {
                return historial;
            }

            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));

            NodeList nList = doc.getElementsByTagName("turno");
            for (int i = 0; i < nList.getLength(); i++) {
                Element e = (Element) nList.item(i);

                String dni = e.getElementsByTagName("dni").item(0).getTextContent();
                Turno t = new Turno(dni);
                t.setPuestoAtencion(Integer.parseInt(e.getElementsByTagName("puesto").item(0).getTextContent()));

                int intentos = Integer.parseInt(e.getElementsByTagName("intentos").item(0).getTextContent());
                for (int j = 0; j < intentos; j++) {
                    t.incrementarIntentos();
                }

                historial.add(t);
            }
        } catch (Exception e) {
        }
        return historial;
    }

    private void escribirArchivo(List<Turno> lista) {
        try {
            StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<historial>\n");

            for (Turno t : lista) {
                sb.append("  <turno>\n")
                        .append("    <dni>").append(t.getDniCliente()).append("</dni>\n")
                        .append("    <puesto>").append(t.getPuestoAtencion()).append("</puesto>\n")
                        .append("    <intentos>").append(t.getIntentosLlamado()).append("</intentos>\n")
                        .append("  </turno>\n");
            }

            sb.append("</historial>");
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
        if (historial.size() <= cantidad) {
            return historial;
        }
        return historial.subList(historial.size() - cantidad, historial.size());
    }
}
