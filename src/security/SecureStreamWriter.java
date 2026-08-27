package src.security;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class SecureStreamWriter {
    private static final String SECRET_KEY = "1234567890123456";
    private static final String INIT_VECTOR = "1234567890123456";

    public static DataOutputStream getEncryptedStream(OutputStream rawOutputStream) throws Exception {
        IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR.getBytes(StandardCharsets.UTF_8));
        SecretKeySpec skeySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");

        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
        CipherOutputStream cos = new CipherOutputStream(rawOutputStream, cipher);

        return new DataOutputStream(cos);
    }
}