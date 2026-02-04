import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Arrays;

public class CryptoHelper {
    // Sunucu (ClientHandler.java) ile aynı algoritma
    private static final String ALGORITHM = "DES/ECB/PKCS5Padding";

    public static String encrypt(String plainText, String key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, getFixedKey(key));
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static String decrypt(String encryptedText, String key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, getFixedKey(key));
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
        byte[] plainBytes = cipher.doFinal(decodedBytes);
        return new String(plainBytes, "UTF-8");
    }

    // Anahtarı 8 byte'a sabitleyen metod (Sunucu uyumluluğu için şart)
    private static SecretKeySpec getFixedKey(String key) throws Exception {
        byte[] keyBytes = Arrays.copyOf(key.getBytes("UTF-8"), 8);
        return new SecretKeySpec(keyBytes, "DES");
    }
}