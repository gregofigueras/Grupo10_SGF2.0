package ServidorCentral.persistencia;

import Globales.Turno;
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
import java.util.LinkedList;
import java.util.Queue;

public class ColaEsperaXML implements IColaEsperaDAO {
    private final String RUTA_ARCHIVO = "cola_espera" + ConfigPersistencia.getSufijo() + ".xml";
    private final Encriptador encriptador = new Encriptador(123456);

    private Queue<Turno> leerArchivo() {
        Queue<Turno> cola = new LinkedList<>();
        try {
            File file = new File(RUTA_ARCHIVO);
            if (!file.exists()) {
                return cola;
            }

            String xml = new String(Files.readAllBytes(Paths.get(RUTA_ARCHIVO)));
            if (xml.trim().isEmpty()) {
                return cola;
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

                cola.add(t);
            }
        } catch (Exception e) {
        }
        return cola;
    }

    private void escribirArchivo(Queue<Turno> cola) {
        try {
            StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<turnos>\n");

            for (Turno t : cola) {
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

            sb.append("</turnos>");

            // Guardamos XML normal, pero con el DNI encriptado
            Files.write(Paths.get(RUTA_ARCHIVO), sb.toString().getBytes());
        } catch (Exception e) {
        }
    }

    @Override
    public void guardarTurno(Turno turno) {
        Queue<Turno> cola = leerArchivo();
        cola.add(turno);
        escribirArchivo(cola);
    }

    @Override
    public Turno getSiguiente() {
        Queue<Turno> cola = leerArchivo();
        Turno t = cola.poll();
        escribirArchivo(cola);
        return t;
    }

    @Override
    public boolean existeCliente(String dni) {
        return leerArchivo().stream().anyMatch(t -> t.getDniCliente().equals(dni));
    }

    @Override
    public Queue<Turno> getTodosPendientes() {
        return leerArchivo();
    }
}
