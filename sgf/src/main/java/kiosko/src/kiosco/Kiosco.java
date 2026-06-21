package kiosko.src.kiosco;

import Globales.Configuracion;
import ServidorCentral.seguridad.Encriptador;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Cliente de red responsable de comunicarse con el servidor central para
 * procesar solicitudes de turno desde el kiosco.
 */
public class Kiosco {

    private static final int CLAVE_ENCRIPTACION = 12345;

    private final Encriptador encriptador = new Encriptador(CLAVE_ENCRIPTACION);

    /** Configuración con IPs y puerto para conectarse al servidor. */
    private Configuracion config;

    public Kiosco(Configuracion config) {
        this.config = config;
    }

    public String procesarTurno(String dni) {
        try (Socket socket = conectarConReintento(config.getPuertoServidor());
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Enviamos DNI cifrado
            out.println(encriptador.encriptar(dni));

            // Leemos respuesta cifrada y la desencriptamos
            String respuestaCifrada = in.readLine();
            if (respuestaCifrada == null) {
                return "ERROR";
            }
            return encriptador.desencriptar(respuestaCifrada);

        } catch (Exception e) {
            return "ERROR";
        }
    }

    private Socket conectarConReintento(int puerto) throws Exception {
        try {
            return new Socket(this.config.getIpPrimario(), puerto);
        } catch (Exception e) {
            System.out.println("Fallo el Primario. Reintentando con Servidor de Respaldo...");
            return new Socket(this.config.getIpRespaldo(), puerto);
        }
    }
}
