package src.utility;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

public class CryptoUtil {
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    public static byte[] encrypt(byte[] plaintext, SecretKey key) throws GeneralSecurityException {
        byte[] iv = new byte[IV_LENGTH];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH * 1, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] ciphertext = cipher.doFinal(plaintext);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            output.write(iv);
            output.write(ciphertext);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return output.toByteArray();
    }

    public static byte[] decrypt(byte[] encryptedData, SecretKey key) throws GeneralSecurityException {
        byte[] iv = Arrays.copyOfRange(encryptedData, 0, IV_LENGTH);
        byte[] ciphertext = Arrays.copyOfRange(encryptedData, IV_LENGTH, encryptedData.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        return cipher.doFinal(ciphertext);
    }
}