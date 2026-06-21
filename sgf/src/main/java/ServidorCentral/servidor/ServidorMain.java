package ServidorCentral.servidor;

import javax.swing.*;

public class ServidorMain {
    public static void main(String[] args) {
        boolean esPrimario = true;
        if (args.length > 0 && args[0].equalsIgnoreCase("RESPALDO")) {
            esPrimario = false;
        }

        // Ventana de configuración
        JTextField txtKiosco = new JTextField("5000");
        JTextField txtOperador = new JTextField("5001");
        JTextField txtIpPantalla = new JTextField("127.0.0.1");
        JTextField txtPtoPantalla = new JTextField("6000");
        JTextField txtIpRespaldo = new JTextField("127.0.0.2");
        JTextField txtClave = new JTextField("12345"); // Clave numérica de encriptación
        JTextField txtIdNodo = new JTextField(esPrimario ? "Nodo1" : "Nodo2");

        String[] opcionesFormato = { "JSON", "XML", "TXT" };
        JComboBox<String> cmbFormato = new JComboBox<>(opcionesFormato);

        Object[] message = {
                "ID del Servidor (Nombre único para archivos):", txtIdNodo,
                "Puerto de Escucha para Kioscos:", txtKiosco,
                "Puerto de Escucha para Operadores:", txtOperador,
                "IP de la Pantalla (Monitor):", txtIpPantalla,
                "Puerto de la Pantalla:", txtPtoPantalla,
                "IP del Servidor de Respaldo:", txtIpRespaldo,
                "Formato de Backup (RF-05):", cmbFormato,
                "Clave de Encriptación (numérica):", txtClave
        };

        String titulo = esPrimario ? "Configuración Servidor PRIMARIO" : "Configuración Servidor RESPALDO";
        int option = JOptionPane.showConfirmDialog(null, message, titulo, JOptionPane.OK_CANCEL_OPTION);

        if (option != JOptionPane.OK_OPTION) {
            System.exit(0);
        }

        // Extraer configuraciones
        int puertoKiosco = Integer.parseInt(txtKiosco.getText().trim());
        int puertoOperador = Integer.parseInt(txtOperador.getText().trim());
        String ipPantalla = txtIpPantalla.getText().trim();
        int puertoPantalla = Integer.parseInt(txtPtoPantalla.getText().trim());
        String ipRespaldo = txtIpRespaldo.getText().trim();
        String formato = (String) cmbFormato.getSelectedItem();
        int claveEncriptacion = Integer.parseInt(txtClave.getText().trim());
        String idNodo = txtIdNodo.getText().trim();

        // Iniciar el núcleo del servidor
        ServidorCentral servidor = new ServidorCentral();
        servidor.iniciar(esPrimario, puertoKiosco, puertoOperador, ipPantalla, puertoPantalla, ipRespaldo, formato, claveEncriptacion, idNodo);
    }
}