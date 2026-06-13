package Operador.src.operador;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Observable;

import javax.swing.Timer;

/**
 * Clase que representa el modelo del puesto de atención. Esta clase se encarga
 * de manejar la lógica de negocio relacionada con el puesto, como conectar con
 * el servidor, llamar al siguiente turno, re-notificar al turno actual y
 * manejar el cooldown para el botón de re-notificar. Utiliza el patrón Observer
 * para notificar a la vista (PanelOperador) sobre los cambios en el estado del
 * puesto, como el turno actual o el estado del botón de re-notificar,
 * permitiendo una actualización fluida de la interfaz gráfica en respuesta a
 * las acciones del usuario y las respuestas del servidor. Además, maneja la
 * lógica de reconexión en caso de fallos en la conexión, intentando conectarse
 * primero al servidor primario y luego al servidor de respaldo si el primario
 * no está disponible.
 */
@SuppressWarnings("deprecation") // Para evitar warnings de Observable, que es una clase obsoleta pero sigue
                                 // siendo útil para este caso
public class Puesto extends Observable {
    public static final int TIEMPO_DE_RELLAMADO = 30; // Tiempo en segundos para el cooldown
    public static final int MAX_REINTENTOS = 3;
    public static final String HABILITAR_RELLAMADO = "HABILITAR_RELLAMADO";
    public static final String LLAMAR_ = "LLAMAR_";
    public static final String RELLAMAR_ = "RELLAMAR_";
    public static final String REGISTRAR_ = "REGISTRAR_";
    public static final String ERROR_DE_CONEXION = "ERROR_DE_CONEXION";
    private ConfiguracionPuesto configuracion;
    private Timer cooldownTimer;
    private int segundosRestantes;

    /**
     * Constructor del modelo del puesto de atención. Inicializa el objeto sin
     * configuración, que debe ser establecida posteriormente antes de intentar
     * conectar con el servidor.
     * 
     * @param configuracion La configuración específica para este puesto de
     *                      atención, que incluye las direcciones IP del servidor
     *                      primario y de respaldo, el puerto de conexión y el ID
     *                      del puesto.
     */
    public void setConfiguracion(ConfiguracionPuesto configuracion) {
        this.configuracion = configuracion;
    }

    /**
     * Método privado que maneja la lógica de comunicación con el servidor para
     * enviar comandos como llamar al siguiente turno o re-notificar al turno
     * actual. Este método se encarga de establecer la conexión con el servidor
     * (intentando primero el primario y luego el de respaldo), enviar el comando
     * correspondiente y leer la respuesta del servidor, que luego es devuelta al
     * llamador para su procesamiento. En caso de errores en la conexión, se lanza
     * una excepción que puede ser manejada por los métodos públicos que llaman a
     * este método para mostrar mensajes de error adecuados al usuario.
     * 
     * @param comando El comando que se enviará al servidor, como "LLAMAR_" seguido
     *                del ID del puesto o "RELLAMAR_" seguido del ID del puesto.
     * @return La respuesta del servidor a la acción realizada, que puede ser "OK",
     *         "DUPLICADO" o "VACIO", entre otras posibles respuestas dependiendo de
     *         la lógica del servidor.
     * @throws Exception Si ocurre un error en la conexión con el servidor o en la
     *                   comunicación, se lanza una excepción para que el llamador
     *                   pueda manejarla adecuadamente.
     */
    private String getRespuesta(String comando) throws Exception {
        // Cambiamos 'new Socket' por 'conectarConReintento' y usamos 'puertoServidor'
        Socket socket = conectarConReintento(configuracion.getPuertoServidor());
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println(comando);
        String respuesta = in.readLine();
        return respuesta;
    }

    /**
     * Llama al siguiente turno en la cola de atención. Este método utiliza el
     * método privado getRespuesta para enviar el comando de llamar al servidor y
     * obtener la respuesta, que luego es devuelta al llamador para su
     * procesamiento. En caso de errores en la conexión, se lanza una excepción que
     * puede ser manejada por el llamador para mostrar mensajes de error adecuados
     * al usuario. Este método es llamado desde el controlador del puesto de
     * atención cuando el operador hace clic en el botón de llamar al siguiente
     * 
     * @return La respuesta del servidor a la acción de llamar al siguiente turno,
     *         que puede ser "OK_<DNI>" o "VACIO", entre otras posibles respuestas
     *         dependiendo de la lógica del servidor.
     */
    public String llamar() {
        try {
            return getRespuesta(LLAMAR_ + configuracion.getIdPuesto());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Re-notifica al turno actual que está siendo atendido. Este método utiliza el
     * método privado getRespuesta para enviar el comando de re-notificar al
     * servidor y obtener la respuesta, que luego es devuelta al llamador para su
     * procesamiento. En caso de errores en la conexión, se lanza una excepción que
     * puede ser manejada por el llamador para mostrar mensajes de error adecuados
     * al usuario. Este método es llamado desde el controlador del puesto de
     * atención cuando el operador hace clic en el botón de re-notificar al turno
     * actual, y también inicia el cooldown para el botón de re-notificar,
     * notificando a la vista para que actualice el estado del botón y muestre el
     * tiempo restante para el próximo re-notificado.
     * 
     * @return La respuesta del servidor a la acción de re-notificar al turno
     *         actual, que puede ser "OK" o "DESCARTADO", entre otras posibles
     *         respuestas dependiendo de la lógica del servidor. En caso de errores
     *         en la conexión, se devuelve null para que el llamador pueda manejarlo
     *         adecuadamente.
     */
    public String rellamar() {
        try {
            return getRespuesta(RELLAMAR_ + configuracion.getIdPuesto());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Inicia el cooldown para el botón de re-notificar, estableciendo un Timer que
     * cuenta los segundos restantes y notifica a la vista para actualizar el estado
     * del botón y mostrar el tiempo restante. Si el Timer ya está corriendo, se
     * detiene antes de iniciar uno nuevo para evitar múltiples Timers corriendo
     * simultáneamente. Este método es llamado desde el controlador del puesto de
     * atención después de que el operador hace clic en el botón de re-notificar al
     * turno actual, y se encarga de manejar la lógica de cooldown para evitar que
     * el operador pueda re-notificar al mismo turno repetidamente sin esperar un
     * tiempo razonable entre cada re-notificación.
     */
    public void iniciarCooldownRellamado() {
        if (cooldownTimer != null && cooldownTimer.isRunning())
            cooldownTimer.stop();

        segundosRestantes = TIEMPO_DE_RELLAMADO;
        cooldownTimer = new Timer(1000, e -> {
            segundosRestantes--;
            if (segundosRestantes <= 0) {
                cooldownTimer.stop();
                setChanged();
                notifyObservers(HABILITAR_RELLAMADO);
            } else {
                setChanged();
                notifyObservers(new Integer(segundosRestantes));
            }
        });
        cooldownTimer.start();
    }

    /**
     * Detiene el Timer de cooldown para el botón de re-notificar, si es que está
     * corriendo. Este método puede ser llamado desde el controlador del puesto de
     * atención cuando se necesite detener el cooldown por alguna razón, como al
     * cerrar la ventana del puesto de atención para asegurar que no queden Timers
     * corriendo en segundo plano después de que el puesto se haya cerrado. Si el
     * Timer no está corriendo, este método no hace nada, evitando errores o
     * excepciones por intentar detener un Timer que no existe o que ya se ha
     * detenido.
     */
    public void detenerCooldownTimer() {
        if (cooldownTimer != null && cooldownTimer.isRunning())
            cooldownTimer.stop();
    }

    /**
     * Método privado que maneja la lógica de conexión con el servidor, intentando
     * primero conectarse al servidor primario y, en caso de fallo, intentando
     * conectarse al servidor de respaldo. Este método es utilizado por los métodos
     * que necesitan establecer una conexión con el servidor para enviar comandos,
     * como llamar al siguiente turno o re-notificar al turno actual, asegurando que
     * el puesto de atención pueda seguir funcionando incluso si el servidor
     * primario no está disponible, siempre y cuando el servidor de respaldo esté
     * operativo. Si ambos intentos de conexión fallan, se lanza una excepción que
     * puede ser manejada por el llamador para mostrar mensajes de error adecuados
     * al usuario.
     * 
     * @param puerto El puerto de conexión que se utilizará para intentar conectarse
     *               con el servidor, que es el mismo para el servidor primario y el
     *               de respaldo según la configuración establecida para el puesto
     *               de atención.
     * @return Un Socket conectado al servidor, ya sea el primario o el de respaldo,
     *         dependiendo de cuál esté disponible. Si ambos intentos de conexión
     *         fallan, se lanza una excepción que puede ser manejada por el llamador
     *         para mostrar mensajes de error adecuados al usuario. Este método es
     *         utilizado internamente por los métodos que necesitan establecer una
     *         conexión con el servidor para enviar comandos, como llamar al
     *         siguiente turno o re-notificar al turno actual, asegurando que el
     *         puesto de atención pueda seguir funcionando incluso si el servidor
     *         primario no está disponible, siempre y cuando el servidor de respaldo
     *         esté operativo.
     * @throws Exception Si ambos intentos de conexión fallan, se lanza una
     *                   excepción que puede ser manejada por el llamador para
     *                   mostrar mensajes de error adecuados al usuario.
     */
    private Socket conectarConReintento(int puerto) throws Exception {
        try {
            return new Socket(configuracion.getIpPrimario(), puerto);
        } catch (Exception e) {
            System.out.println("Fallo el Servidor Primario. Reintentando con Servidor de Respaldo...");
            return new Socket(configuracion.getIpRespaldo(), puerto);
        }
    }

    /**
     * Obtiene los segundos restantes para el cooldown del botón de re-notificar.
     * Este método puede ser utilizado por la vista (PanelOperador) para mostrar el
     * tiempo restante para el próximo re-notificado al operador, permitiendo que el
     * operador sepa cuánto tiempo debe esperar antes de poder re-notificar al mismo
     * turno nuevamente. El valor de segundos restantes es actualizado por el Timer
     * de cooldown que se inicia cuando el operador hace clic en el botón de
     * re-notificar al turno actual, y se notifica a la vista para que actualice la
     * interfaz gráfica en consecuencia.
     * 
     * @return Los segundos restantes para el cooldown del botón de re-notificar.
     */
    public int getSegundosRestantes() {
        return segundosRestantes;
    }

    /**
     * Conecta el puesto de atención con el servidor, enviando el comando de
     * registro con el ID del puesto. Este método es llamado desde el constructor
     * del controlador del puesto de atención para establecer la conexión inicial
     * con el servidor y registrar el puesto de atención, permitiendo que el
     * servidor lo reconozca y pueda enviarle los turnos correspondientes para
     * atender. Si la conexión es exitosa, se devuelve la respuesta del servidor,
     * que puede ser "OK" si el registro fue exitoso, "DUPLICADO" si el ID del
     * puesto ya está registrado, o "ERROR_DE_CONEXION" si no se pudo establecer la
     * conexión con el servidor. En caso de errores en la conexión, se lanza una
     * excepción que puede ser manejada por el llamador para mostrar mensajes de
     * error adecuados al usuario.
     * 
     * @return La respuesta del servidor después de intentar registrar el puesto.
     */
    public String conectar() {
        String respuesta;
        try {
            respuesta = getRespuesta(REGISTRAR_ + this.configuracion.getIdPuesto());
        } catch (Exception e) {
            respuesta = ERROR_DE_CONEXION;
        }
        return respuesta;
    }

    /**
     * Desconecta el puesto de atención del servidor, enviando un comando de
     * desconexión con el ID del puesto. Este método es llamado desde el controlador
     * del puesto de atención cuando se cierra la ventana del puesto, asegurando que
     * el servidor reciba la notificación de que el puesto se está desconectando y
     * pueda liberar los recursos asociados a ese puesto, como el turno que estaba
     * siendo atendido, permitiendo una desconexión limpia y evitando que el
     * servidor siga enviando turnos a un puesto que ya no está operativo. En caso
     * de errores en la conexión, se maneja la excepción internamente sin lanzar una
     * nueva, ya que el puesto ya se está cerrando de todas formas y no es necesario
     * mostrar un mensaje de error adicional al usuario en este caso.
     */
    public void desconectar() {
        try (Socket socket = conectarConReintento(configuracion.getPuertoServidor());
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println("DESCONECTAR_" + configuracion.getIdPuesto());
        } catch (Exception e) {
            // No hacemos nada, el puesto ya se está cerrando de todas formas
        }
    }

}