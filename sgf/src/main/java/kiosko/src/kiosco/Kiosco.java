package kiosko.src.kiosco;

import javax.swing.*;

import Globales.Configuracion;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Cliente de red responsable de comunicarse con el servidor central para
 * procesar solicitudes de turno desde el kiosco.
 *
 * <p>
 * Extiende {@link JFrame} únicamente por conveniencia de empaquetado con la
 * vista; contiene la lógica de envío/recepción de mensajes al servidor.
 * </p>
 */
public class Kiosco {

    /** Configuración con IPs y puerto para conectarse al servidor. */
    private Configuracion config;

    /**
     * Construye un cliente kiosco con la configuración especificada.
     *
     * @param config objeto {@link Configuracion} con los datos de conexión
     */
    public Kiosco(Configuracion config) {
        this.config = config;
    }

    /**
     * Envía el DNI al servidor y devuelve la respuesta recibida.
     *
     * <p>
     * El protocolo simple: se envía la línea con el DNI y se espera una
     * respuesta de una línea. En caso de error de conexión devuelve
     * {@code "ERROR"}.
     * </p>
     *
     * @param dni DNI del cliente a procesar
     * @return respuesta del servidor: {@code "OK"}, {@code "DUPLICADO"} u
     *         {@code "ERROR"} en caso de fallo
     */
    public String procesarTurno(String dni) {
        try (Socket socket = conectarConReintento(config.getPuertoServidor());
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(dni);
            String respuesta = in.readLine();
            return respuesta;
        } catch (Exception e) {
            return "ERROR";
        }
    }

    /**
     * Intenta conectar primero con el servidor primario y en caso de fallo
     * reintenta con el servidor de respaldo.
     *
     * @param puerto puerto al que conectar
     * @return socket conectado
     * @throws Exception si ninguna conexión pudo establecerse
     */
    private Socket conectarConReintento(int puerto) throws Exception {
        try {
            return new Socket(this.config.getIpPrimario(), puerto);
        } catch (Exception e) {
            System.out.println("Fallo el Primario. Reintentando con Servidor de Respaldo...");
            return new Socket(this.config.getIpRespaldo(), puerto);
        }
    }

}