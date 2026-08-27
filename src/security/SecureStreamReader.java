package src.security;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SecureStreamReader {
    private static final String SECRET_KEY = "1234567890123456";
    private static final String INIT_VECTOR = "1234567890123456";

    public static DataInputStream getDecryptedStream(InputStream rawInputStream) throws Exception {
        IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR.getBytes(StandardCharsets.UTF_8));
        SecretKeySpec skeySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");

        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
        CipherInputStream cis = new CipherInputStream(rawInputStream, cipher);

        return new DataInputStream(cis);
    }
}