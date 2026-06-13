package Operador.src.operador;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Observable;

import javax.swing.Timer;

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

    public void setConfiguracion(ConfiguracionPuesto configuracion) {
        this.configuracion = configuracion;
    }

    private String getRespuesta(String comando) throws Exception {
        // Cambiamos 'new Socket' por 'conectarConReintento' y usamos 'puertoServidor'
        Socket socket = conectarConReintento(configuracion.getPuertoServidor());
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println(comando);
        String respuesta = in.readLine();
        return respuesta;
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

    public void detenerCooldownTimer() {
        if (cooldownTimer != null && cooldownTimer.isRunning())
            cooldownTimer.stop();
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
        String respuesta;
        try {
            respuesta = getRespuesta(REGISTRAR_ + this.configuracion.getIdPuesto());
        } catch (Exception e) {
            respuesta = ERROR_DE_CONEXION;
        }
        return respuesta;
    }

    public void desconectar() {
        try (Socket socket = conectarConReintento(configuracion.getPuertoServidor());
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println("DESCONECTAR_" + configuracion.getIdPuesto());
        } catch (Exception e) {
            // No hacemos nada, el puesto ya se está cerrando de todas formas
        }
    }

}