package Pantalla.src.pantalla;

import Globales.Turno;
import ServidorCentral.persistencia.IHistorialLlamadosDAO;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Observable;
import java.util.LinkedList;

/**
 * Clase que representa el monitor de la sala de espera. Se encarga de escuchar
 * en un puerto específico para recibir actualizaciones del servidor central
 * sobre el turno actual y el historial de llamados. Utiliza el patrón Observer
 * para notificar a la interfaz gráfica cada vez que hay un cambio en el turno
 * actual o en el historial. Además, mantiene una lista de los últimos 4 turnos
 * atendidos para mostrar en la pantalla pública.
 */
@SuppressWarnings("deprecation") // Para Observable, que es lo que nos pidieron usar
public class MonitorSala extends Observable {
    private Turno turnoActual;
    private LinkedList<String> historial;
    private int puertoEscucha;

    /**
     * Constructor del MonitorSala. Inicializa el puerto de escucha y el historial,
     * y comienza a escuchar por conexiones del servidor central para recibir
     * actualizaciones sobre el turno actual y el historial de llamados.
     * 
     * @param puertoEscucha Puerto en el que el monitor escuchará las
     *                      actualizaciones del servidor central.
     */
    public MonitorSala(int puertoEscucha) {
        this.puertoEscucha = puertoEscucha;
        historial = new LinkedList<>();
        iniciarServidorOperador();
    }

    /**
     * Obtiene el historial de los últimos turnos atendidos. El historial se
     * mantiene actualizado con las últimas 4 entradas, mostrando el turno actual en
     * la parte superior y los anteriores debajo. Cada entrada del historial incluye
     * el DNI del cliente y el puesto de atención o el estado de expirado si el
     * turno fue descartado por expirar. El método devuelve una lista de cadenas que
     * representan el historial formateado para su visualización en la pantalla
     * pública.
     * 
     * @return Lista de cadenas representando el historial de turnos atendidos, con
     *         el más reciente en la parte superior. Cada entrada incluye el DNI del
     *         cliente y el puesto de atención o el estado de expirado si el turno
     *         fue descartado por expirar.
     */
    public LinkedList<String> getHistorial() {
        return historial;
    }

    /**
     * Carga el historial inicial de los últimos turnos atendidos desde el servidor
     * central. Este método se llama al iniciar el monitor para mostrar los últimos
     * turnos atendidos antes de recibir nuevas actualizaciones. El historial se
     * mantiene actualizado con las últimas 4 entradas, mostrando el turno actual en
     * la parte superior y los anteriores debajo. Cada entrada del historial incluye
     * el DNI del cliente y el puesto de atención o el estado de expirado si el
     * turno fue descartado por expirar. Si ocurre un error al obtener el historial,
     * se limpia la lista de historial para evitar mostrar información
     * desactualizada o incorrecta.
     */
    public void cargarHistorialInicial() {
        try {
            IHistorialLlamadosDAO historialDAO = new ServidorCentral.persistencia.JSONFactory()
                    .crearHistorialLlamadosDAO();
            List<Globales.Turno> llamados = historialDAO.obtenerUltimosLlamados(4);

            historial.clear();
            // Queremos mostrar el historial con el más reciente arriba
            for (Turno turno : llamados) {
                historial.addFirst(turno.toString());
            }

            setChanged();
            notifyObservers(historial);
        } catch (Exception e) {
            historial.clear();
        }
    }

    /**
     * Inicia un servidor en un hilo separado que escucha por conexiones del
     * servidor central para recibir actualizaciones sobre el turno actual y el
     * historial de llamados. El servidor acepta conexiones entrantes, lee los
     * mensajes enviados por el servidor central, y procesa cada mensaje para
     * actualizar el turno actual y el historial de llamados. Cada mensaje se espera
     * que tenga el formato "TIPO_DNI_PUESTO", donde TIPO puede ser "NUEVO",
     * "URGENTE" o "DESCARTADO". Si el mensaje indica que un turno fue descartado
     * por expirar, se actualiza el historial para reflejar ese estado. Si el
     * mensaje indica un nuevo turno o un turno urgente, se actualiza el turno
     * actual y se mantiene el historial actualizado con los últimos turnos
     * atendidos. Cada vez que se procesa un mensaje, se notifica a los observadores
     * para que la interfaz gráfica pueda actualizarse con la nueva información.
     */
    private void iniciarServidorOperador() {
        Thread hiloServidor = new Thread(() -> {
            // Usamos la variable 'puertoEscucha' que el usuario ingresó
            try (ServerSocket serverSocket = new ServerSocket(puertoEscucha)) {
                System.out.println("Monitor escuchando en el puerto " + puertoEscucha);
                while (true) {
                    Socket socketOperador = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socketOperador.getInputStream()));
                    String mensaje = in.readLine();
                    if (mensaje != null)
                        SwingUtilities.invokeLater(() -> procesarMensaje(mensaje));
                    in.close();
                    socketOperador.close();
                }
            } catch (IOException e) {
                setChanged();
                notifyObservers(e);
            }
        });
        hiloServidor.start();
    }

    /**
     * Procesa un mensaje recibido del servidor central para actualizar el turno
     * actual y el historial de llamados.
     * 
     * <pre>
     * Precondición:
     * El mensaje debe ser distinto de null y se espera que tenga el formato
     * "TIPO_DNI_PUESTO", donde TIPO puede ser "NUEVO", "URGENTE" o "DESCARTADO".
     * </pre>
     * 
     * @param mensaje Mensaje recibido del servidor central con la información del
     *                turno actual o el historial de llamados. El mensaje se espera
     *                que tenga el formato "TIPO_DNI_PUESTO", donde TIPO puede ser
     *                "NUEVO", "URGENTE" o "DESCARTADO".
     */
    private void procesarMensaje(String mensaje) {
        try {
            String[] partes = mensaje.split("_");
            String tipo = partes[0];
            String dni = partes[1];
            String puesto = partes[2];

            // NUEVA LÓGICA: Si el servidor avisa que el turno fue descartado (Expiró)
            if ("DESCARTADO".equals(tipo)) {
                // Lo borramos si ya estaba en el historial (por algún motivo)
                historial.removeIf(s -> s.contains(dni));
                // Lo mandamos al historial con la etiqueta de EXPIRADO
                historial.addFirst("   " + dni + "                    EXPIRADO");
                if (historial.size() > 4)
                    historial.removeLast();

            } else {
                // --- LÓGICA NORMAL (NUEVO O URGENTE) ---
                String turnoAnterior = turnoActual != null ? turnoActual.toString() : "---";
                String puestoAnteriorStr = turnoActual != null ? String.valueOf(turnoActual.getPuestoAtencion())
                        : "---";

                if (!turnoAnterior.equals("---") && !turnoAnterior.equals(dni)) {
                    historial.removeIf(s -> s.contains(turnoAnterior));
                    // Insertamos el turno anterior en la cima del historial (más reciente arriba)
                    historial.addFirst("   " + turnoAnterior + "                    Puesto " + puestoAnteriorStr);
                    if (historial.size() > 4)
                        historial.removeLast();
                }

                historial.removeIf(s -> s.contains(dni));

                if (turnoActual == null) {
                    turnoActual = new Turno(dni);
                } else {
                    turnoActual.setDniCliente(dni);
                }
                turnoActual.setPuestoAtencion(Integer.parseInt(puesto));
            }
            setChanged();
            notifyObservers(tipo);
        } catch (Exception e) {
            System.out.println("Formato desconocido: " + mensaje);
        }

    }

    /**
     * Obtiene el turno actual que se está atendiendo. El turno actual se actualiza
     * cada vez que se recibe un mensaje del servidor central indicando un nuevo
     * turno o un turno urgente. Si el mensaje indica que un turno fue descartado
     * por expirar, el turno actual no se actualiza, pero el historial se mantiene
     * actualizado para reflejar ese estado. El método devuelve el turno actual con
     * su DNI y puesto de atención, o null si no hay un turno actual definido. El
     * turno actual se utiliza para mostrar la información del cliente que se está
     * atendiendo en la pantalla pública, incluyendo su DNI y el puesto de atención
     * asignado. Si no hay un turno actual definido, se muestra "---" en la pantalla
     * pública para indicar que no hay un cliente siendo atendido en ese momento.
     * 
     * @return El turno actual que se está atendiendo, con su DNI y puesto de
     *         atención, o null si no hay un turno actual definido. El turno actual
     *         se utiliza para mostrar la información del cliente que se está
     *         atendiendo en la pantalla pública, incluyendo su DNI y el puesto de
     *         atención asignado. Si no hay un turno actual definido, se debe
     *         mostrar "---" en la pantalla pública para indicar que no hay un
     *         cliente siendo atendido en ese momento.
     */
    public Turno getTurnoActual() {
        return turnoActual;
    }

    /**
     * Obtiene el puesto de atención del turno actual que se está atendiendo. El
     * puesto de atención se actualiza cada vez que se recibe un mensaje del
     * servidor central indicando un nuevo turno o un turno urgente. Si el mensaje
     * indica que un turno fue descartado por expirar, el puesto de atención del
     * turno actual no se actualiza, pero el historial se mantiene actualizado para
     * reflejar ese estado. El método devuelve el puesto de atención del turno
     * actual, o "---" si no hay un turno actual definido. El puesto de atención se
     * utiliza para mostrar la información del cliente que se está atendiendo en la
     * pantalla pública, junto con su DNI. Si no hay un turno actual definido, se
     * muestra "---" en la pantalla pública para indicar que no hay un cliente
     * siendo atendido en ese momento. Si el turno actual tiene un puesto de
     * atención asignado, se muestra ese número en la pantalla pública para indicar
     * el puesto donde se está atendiendo al cliente.
     * 
     * @return El puesto de atención del turno actual que se está atendiendo, o
     *         "---" si no hay un turno actual definido. El puesto de atención se
     *         utiliza para mostrar la información del cliente que se está
     *         atendiendo en la pantalla pública, junto con su DNI. Si no hay un
     *         turno actual definido, se debe mostrar "---" en la pantalla pública
     *         para indicar que no hay un cliente siendo atendido en ese momento. Si
     *         el turno actual tiene un puesto de atención asignado, se debe mostrar
     *         ese número en la pantalla pública para indicar el puesto donde se
     *         está atendiendo al cliente.
     */
    public String getPuestoActual() {
        return turnoActual != null ? String.valueOf(turnoActual.getPuestoAtencion()) : "---";
    }

    /**
     * Obtiene el DNI del cliente del turno actual que se está atendiendo. El DNI
     * del cliente se actualiza cada vez que se recibe un mensaje del servidor
     * central indicando un nuevo turno o un turno urgente. Si el mensaje indica que
     * un turno fue descartado por expirar, el DNI del cliente del turno actual no
     * se actualiza, pero el historial se mantiene actualizado para reflejar ese
     * estado. El método devuelve el DNI del cliente del turno actual, o "---" si no
     * hay un turno actual definido. El DNI del cliente se utiliza para mostrar la
     * información del cliente que se está atendiendo en la pantalla pública, junto
     * con el puesto de atención. Si no hay un turno actual definido, se muestra
     * "---" en la pantalla pública para indicar que no hay un cliente siendo
     * atendido en ese momento. Si el turno actual tiene un DNI de cliente asignado,
     * se muestra ese número en la pantalla pública para indicar el cliente que se
     * está atendiendo.
     * 
     * @return El DNI del cliente del turno actual que se está atendiendo, o "---"
     *         si no hay un turno actual definido. El DNI del cliente se utiliza
     *         para mostrar la información del cliente que se está atendiendo en la
     *         pantalla pública, junto con el puesto de atención. Si no hay un turno
     *         actual definido, se debe mostrar "---" en la pantalla pública para
     *         indicar que no hay un cliente siendo atendido en ese momento. Si el
     *         turno actual tiene un DNI de cliente asignado, se debe mostrar ese
     *         número en la pantalla pública para indicar el cliente que se está
     *         atendiendo.
     */
    public String getDniActual() {
        return turnoActual != null ? turnoActual.getDniCliente() : "---";
    }
}