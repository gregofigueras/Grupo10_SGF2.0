package Operador.src.operador;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Observable;

import javax.swing.Timer;

import ServidorCentral.seguridad.Encriptador;

/**
 * Clase que representa el modelo del puesto de atención. Esta clase se encarga
 * de manejar la lógica de negocio relacionada con el puesto, como conectar con
 * el servidor, llamar al siguiente turno, re-notificar al turno actual y
 * manejar el cooldown para el botón de re-notificar.
 */
@SuppressWarnings("deprecation")
public class Puesto extends Observable {
    public static final int TIEMPO_DE_RELLAMADO = 30;
    public static final int MAX_REINTENTOS = 3;
    public static final String HABILITAR_RELLAMADO = "HABILITAR_RELLAMADO";
    public static final String LLAMAR_ = "LLAMAR_";
    public static final String RELLAMAR_ = "RELLAMAR_";
    public static final String REGISTRAR_ = "REGISTRAR_";
    public static final String DESCONECTAR_ = "DESCONECTAR_";
    public static final String ERROR_DE_CONEXION = "ERROR_DE_CONEXION";

    /**
     * IMPORTANTE:
     * Esta clave debe coincidir con la que ingresás en ServidorMain.
     * Si cambiás la clave en el servidor, cambiála acá también.
     */
    private static final int CLAVE_ENCRIPTACION = 12345;

    private final Encriptador encriptador = new Encriptador(CLAVE_ENCRIPTACION);

    private ConfiguracionPuesto configuracion;
    private Timer cooldownTimer;
    private int segundosRestantes;

    public void setConfiguracion(ConfiguracionPuesto configuracion) {
        this.configuracion = configuracion;
    }

    private String getRespuesta(String comando) throws Exception {
        try (Socket socket = conectarConReintento(configuracion.getPuertoServidor());
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Enviamos comando cifrado
            out.println(encriptador.encriptar(comando));

            // Recibimos respuesta cifrada y la desencriptamos
            String respuestaCifrada = in.readLine();
            if (respuestaCifrada == null) {
                return null;
            }
            return encriptador.desencriptar(respuestaCifrada);
        }
    }

    public String llamar() {
        try {
            return getRespuesta(LLAMAR_ + configuracion.getIdPuesto());
        } catch (Exception e) {
            return null;
        }
    }

    public String rellamar() {
        try {
            return getRespuesta(RELLAMAR_ + configuracion.getIdPuesto());
        } catch (Exception e) {
            return null;
        }
    }

    public void iniciarCooldownRellamado() {
        if (cooldownTimer != null && cooldownTimer.isRunning()) {
            cooldownTimer.stop();
        }

        segundosRestantes = TIEMPO_DE_RELLAMADO;
        cooldownTimer = new Timer(1000, e -> {
            segundosRestantes--;
            if (segundosRestantes <= 0) {
                cooldownTimer.stop();
                setChanged();
                notifyObservers(HABILITAR_RELLAMADO);
            } else {
                setChanged();
                notifyObservers(Integer.valueOf(segundosRestantes));
            }
        });
        cooldownTimer.start();
    }

    public void detenerCooldownTimer() {
        if (cooldownTimer != null && cooldownTimer.isRunning()) {
            cooldownTimer.stop();
        }
    }

    private Socket conectarConReintento(int puerto) throws Exception {
        try {
            return new Socket(configuracion.getIpPrimario(), puerto);
        } catch (Exception e) {
            System.out.println("Fallo el Servidor Primario. Reintentando con Servidor de Respaldo...");
            return new Socket(configuracion.getIpRespaldo(), puerto);
        }
    }

    public int getSegundosRestantes() {
        return segundosRestantes;
    }

    public String conectar() {
        try {
            return getRespuesta(REGISTRAR_ + this.configuracion.getIdPuesto());
        } catch (Exception e) {
            return ERROR_DE_CONEXION;
        }
    }

    public void desconectar() {
        try (Socket socket = conectarConReintento(configuracion.getPuertoServidor());
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(encriptador.encriptar(DESCONECTAR_ + configuracion.getIdPuesto()));
            in.readLine(); // leemos respuesta si el servidor la envía
        } catch (Exception e) {
            // El puesto se está cerrando; ignoramos errores de desconexión
        }
    }
}
