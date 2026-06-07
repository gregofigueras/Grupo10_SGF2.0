package seguridad;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CifradoSimetrico implements Strategy {

    private static final String ALGORITMO = "AES";

    // Transformamos el 'int' del diagrama en una clave AES de 16 bytes
    private SecretKeySpec generarKey(int clave) {
        String claveStr = String.format("%016d", clave);
        return new SecretKeySpec(claveStr.getBytes(StandardCharsets.UTF_8), ALGORITMO);
    }

    @Override
    public String encriptar(String x, int clave) {
        try {
            SecretKeySpec secretKey = generarKey(clave);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] datosEncriptados = cipher.doFinal(x.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(datosEncriptados);
        } catch (Exception e) {
            throw new RuntimeException("Error al encriptar", e);
        }
    }

    @Override
    public String desencriptar(String x, int clave) {
        try {
            SecretKeySpec secretKey = generarKey(clave);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] bytesCifrados = Base64.getDecoder().decode(x);
            byte[] datosDesencriptados = cipher.doFinal(bytesCifrados);
            return new String(datosDesencriptados, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error al desencriptar", e);
        }
    }
}