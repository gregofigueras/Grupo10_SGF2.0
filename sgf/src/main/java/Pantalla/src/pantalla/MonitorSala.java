package Pantalla.src.pantalla;

import Globales.Turno;
import ServidorCentral.persistencia.IHistorialLlamadosDAO;
import ServidorCentral.persistencia.JSONFactory;
import ServidorCentral.persistencia.PersistenciaFactory;
import ServidorCentral.persistencia.TextoPlanoFactory;
import ServidorCentral.persistencia.XMLFactory;
import ServidorCentral.seguridad.Encriptador;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;

@SuppressWarnings("deprecation")
public class MonitorSala extends Observable {

    private static final int MAX_HISTORIAL = 5;

    private final int puerto;
    private final String formato;
    private final Encriptador encriptador;
    private final IHistorialLlamadosDAO historialDAO;

    private final LinkedList<Turno> historial = new LinkedList<>();

    private volatile Turno turnoActual;
    private volatile String dniActual = "---";
    private volatile int puestoActual = -1;

    public MonitorSala(int puerto, String formato, int clave) {
        this.puerto = puerto;
        this.formato = formato;
        this.encriptador = new Encriptador(clave);

        PersistenciaFactory fabrica = crearFabricaPersistencia(formato);
        this.historialDAO = fabrica.crearHistorialLlamadosDAO();

        iniciarEscuchaServidorCentral();
    }

    private PersistenciaFactory crearFabricaPersistencia(String formatoPersistencia) {
        if ("XML".equalsIgnoreCase(formatoPersistencia)) {
            return new XMLFactory();
        }
        if ("TXT".equalsIgnoreCase(formatoPersistencia)) {
            return new TextoPlanoFactory();
        }
        return new JSONFactory();
    }

    public void cargarHistorialInicial() {
        try {
            List<Turno> lista = historialDAO.obtenerUltimosLlamados(MAX_HISTORIAL);
            synchronized (historial) {
                historial.clear();
                if (lista != null) {
                    historial.addAll(lista);
                    Collections.reverse(historial); // Para mostrar el más reciente primero
                }
            }

            setChanged();
            notifyObservers(getHistorial());
        } catch (Exception e) {
            setChanged();
            notifyObservers(new IOException("No se pudo cargar historial inicial", e));
        }
    }

    private void iniciarEscuchaServidorCentral() {
        Thread hilo = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(puerto)) {
                while (true) {
                    Socket socket = serverSocket.accept();
                    Thread cliente = new Thread(() -> procesarNotificacion(socket));
                    cliente.setDaemon(true);
                    cliente.start();
                }
            } catch (IOException e) {
                setChanged();
                notifyObservers(e);
            }
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    private void procesarNotificacion(Socket socket) {
        try (Socket s = socket;
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

            String cifrado = in.readLine();
            if (cifrado == null || cifrado.trim().isEmpty()) {
                return;
            }

            String mensaje = encriptador.desencriptar(cifrado);
            String[] partes = mensaje.split("_");
            if (partes.length < 3) {
                return;
            }

            String tipo = partes[0];
            String dni = partes[1];
            int puesto = Integer.parseInt(partes[2]);

            switch (tipo) {
                case "NUEVO":
                    procesarNuevo(dni, puesto);
                    setChanged();
                    notifyObservers("NUEVO");
                    break;

                case "URGENTE":
                    procesarUrgente(dni, puesto);
                    setChanged();
                    notifyObservers("URGENTE");
                    break;

                case "DESCARTADO":
                    procesarDescartado(dni, puesto);
                    setChanged();
                    notifyObservers("DESCARTADO");
                    break;

                default:
                    // Mensaje desconocido: ignorar
                    break;
            }

        } catch (Exception e) {
            setChanged();
            notifyObservers(new IOException("Error procesando notificacion del servidor", e));
        }
    }

    private void procesarNuevo(String dni, int puesto) {
        // El turno que estaba en pantalla pasa al historial.
        if (turnoActual != null) {
            agregarAlHistorial(turnoActual);
        }

        Turno nuevo = new Turno(dni);
        nuevo.setPuestoAtencion(puesto);

        turnoActual = nuevo;
        dniActual = dni;
        puestoActual = puesto;
    }

    private void procesarUrgente(String dni, int puesto) {
        // En urgente mantenemos el turno actual sincronizado con el servidor.
        Turno urgente = new Turno(dni);
        urgente.setPuestoAtencion(puesto);
        urgente.incrementarIntentos();

        if (turnoActual != null && !turnoActual.getDniCliente().equals(dni)) {
            agregarAlHistorial(turnoActual);
            historial.removeIf(s -> s.equals(urgente));
        }
        turnoActual = urgente;
        dniActual = dni;
        puestoActual = puesto;
    }

    private void procesarDescartado(String dni, int puesto) {
        // Guardamos un turno marcado como expirado para que se refleje en historial.
        Turno expirado = new Turno(dni, true);
        expirado.setPuestoAtencion(puesto);
        agregarAlHistorial(expirado);
    }

    private void agregarAlHistorial(Turno turno) {
        synchronized (historial) {
            historial.addFirst(turno);
            while (historial.size() > MAX_HISTORIAL) {
                historial.removeLast();
            }
        }
    }

    public Turno getTurnoActual() {
        return turnoActual;
    }

    public String getDniActual() {
        return dniActual;
    }

    public int getPuestoActual() {
        return puestoActual;
    }

    public LinkedList<Turno> getHistorial() {
        synchronized (historial) {
            return new LinkedList<>(historial);
        }
    }

    public String getFormato() {
        return formato;
    }
}
