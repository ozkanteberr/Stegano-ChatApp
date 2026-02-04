import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SteganoManager {
    private static final long STEGO_SEED = 1234567L;
    private static final String DEBUG_FILE = "stego_debug.txt"; // Kanıt Dosyası

    public static BufferedImage encode(BufferedImage sourceImage, String message) {
        // 1. KAPASİTE KONTROLÜ
        long maxCapacity = (long) sourceImage.getWidth() * sourceImage.getHeight() / 8;
        if (message.length() + 1 > maxCapacity) {
            throw new IllegalArgumentException("Resim çok küçük! Bu resmi veri gizlemek için kullanamazsınız.");
        }

        logProof("--- [ENCODE BAŞLADI] Mesaj: " + message + " ---");

        message += "\0"; // Bitiş işareti
        byte[] msgBytes = message.getBytes();
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();

        BufferedImage encoded = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        encoded.getGraphics().drawImage(sourceImage, 0, 0, null);

        List<Integer> pixelIndices = new ArrayList<>();
        for (int i = 0; i < width * height; i++) pixelIndices.add(i);
        Collections.shuffle(pixelIndices, new Random(STEGO_SEED));

        int msgIdx = 0, bitIdx = 0;
        int changedPixels = 0;

        for (int pixelIndex : pixelIndices) {
            if (msgIdx >= msgBytes.length) break;

            int x = pixelIndex % width;
            int y = pixelIndex / width;

            int rgb = encoded.getRGB(x, y);
            int originalBit = rgb & 1; // Eski LSB

            // Gizlenecek bit
            int bitToHide = (msgBytes[msgIdx] >> (7 - bitIdx)) & 1;
            
            // LSB Değişimi
            int newRgb = (rgb & 0xFFFFFFFE) | bitToHide;
            encoded.setRGB(x, y, newRgb);

            // KANIT LOGLAMA (İlk 50 değişikliği ve her değişikliği dosyaya yaz)
            if (originalBit != bitToHide) {
                changedPixels++;
                // Sadece ilk 20 değişikliği detaylı yaz, dosya şişmesin ama kanıt olsun
                if (changedPixels <= 20) {
                    logProof(String.format("[DEĞİŞİM] Piksel(%d,%d) | Eski LSB: %d -> Yeni LSB: %d | Gizlenen Bit: %d", 
                        x, y, originalBit, originalBit & 1, bitToHide));
                }
            }

            if (++bitIdx == 8) { bitIdx = 0; msgIdx++; }
        }
        
        logProof("--- [ENCODE BİTTİ] Toplam Değişen Piksel Sayısı: " + changedPixels + " ---");
        return encoded;
    }

    public static String decode(BufferedImage img) {
        logProof("--- [DECODE BAŞLADI] ---");
        StringBuilder msg = new StringBuilder();
        int width = img.getWidth();
        int height = img.getHeight();
        
        List<Integer> pixelIndices = new ArrayList<>();
        for (int i = 0; i < width * height; i++) pixelIndices.add(i);
        Collections.shuffle(pixelIndices, new Random(STEGO_SEED));

        byte currentByte = 0;
        int bitIdx = 0;

        for (int pixelIndex : pixelIndices) {
            int x = pixelIndex % width;
            int y = pixelIndex / width;

            int rgb = img.getRGB(x, y);
            int bit = rgb & 1;

            currentByte = (byte) ((currentByte << 1) | bit);
            
            if (++bitIdx == 8) {
                if (currentByte == 0) break; // \0 gördü
                msg.append((char) currentByte);
                bitIdx = 0; currentByte = 0;
            }
        }
        
        logProof("--- [DECODE BİTTİ] Çözülen Mesaj: " + msg.toString() + " ---");
        return msg.toString();
    }

    // Dosyaya log yazan yardımcı metod
    private static void logProof(String text) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        try (FileWriter fw = new FileWriter(DEBUG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println("[" + time + "] " + text);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
