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
    private final String RUTA_ARCHIVO = "historial_llamados" + ConfigPersistencia.getSufijo() + ".xml";
    private final ServidorCentral.seguridad.Encriptador encriptador = new ServidorCentral.seguridad.Encriptador(123456);

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

                String dniGuardado = e.getElementsByTagName("dni").item(0).getTextContent();
                String dniReal = dniGuardado;
                try {
                    dniReal = encriptador.desencriptar(dniGuardado);
                } catch (Exception ignored) {
                }

                Turno t = new Turno(dniReal);
                
                NodeList expiradoList = e.getElementsByTagName("expirado");
                if (expiradoList != null && expiradoList.getLength() > 0) {
                    t.setExpirado(Boolean.parseBoolean(expiradoList.item(0).getTextContent()));
                }

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
                String dniEncriptado = t.getDniCliente();
                if (dniEncriptado != null && !dniEncriptado.isEmpty()) {
                    dniEncriptado = encriptador.encriptar(dniEncriptado);
                }

                sb.append("  <turno>\n")
                        .append("    <dni>").append(dniEncriptado).append("</dni>\n")
                        .append("    <puesto>").append(t.getPuestoAtencion()).append("</puesto>\n")
                        .append("    <intentos>").append(t.getIntentosLlamado()).append("</intentos>\n")
                        .append("    <expirado>").append(t.isExpirado()).append("</expirado>\n")
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
    public void actualizarLlamado(Turno turno) {
        List<Turno> historial = leerArchivo();
        boolean modificado = false;
        for (int i = 0; i < historial.size(); i++) {
            if (historial.get(i).getDniCliente().equals(turno.getDniCliente())) {
                historial.set(i, turno);
                modificado = true;
                break;
            }
        }
        if (modificado) {
            escribirArchivo(historial);
        }
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
