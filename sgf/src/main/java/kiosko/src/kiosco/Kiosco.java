package kiosko.src.kiosco;

import javax.swing.*;

import Globales.Configuracion;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Kiosco extends JFrame {

    private Configuracion config;

    public Kiosco(Configuracion config) {
        this.config = config;
    }

    public String procesarTurno(String dni) {
        try (Socket socket = conectarConReintento(config.getPuertoServidor());
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(dni);
            String respuesta = in.readLine();
            return respuesta;
            // if ("OK".equals(respuesta)) {
            // mostrarMensajeExito("¡Turno Confirmado!", "DNI registrado exitosamente.");
            // txtDni.setText("");
            // } else if ("DUPLICADO".equals(respuesta)) {
            // mostrarMensajeError("Turno Duplicado", "El DNI ya está esperando en la
            // fila.");
            // }
        } catch (Exception e) {
            // mostrarMensajeError("Error de Conexión", "No se pudo conectar con el
            // Servidor.");
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