import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String[] parts = inputLine.split("\\|");

                // 1. KULLANICI LİSTESİ İSTEĞİ (Manual Refresh için)
                if (parts[0].equals("GET_USERS")) {
                    sendCurrentList();
                    continue;
                }

                if (parts.length < 2) continue;

                String command = parts[0];
                switch (command) {
                    case "REGISTER_IMG":
                        if (parts.length < 3) break;
                        byte[] imageBytes = Base64.getDecoder().decode(parts[2]);
                        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                        BufferedImage receivedImg = ImageIO.read(bais);
                        
                        String extractedKey = SteganoManager.decode(receivedImg); // Key resimden çıktı
                        handleRegister(parts[1].toLowerCase(), extractedKey);
                        break;

                    case "SEND":
                        if (parts.length < 3) break;
                        handleMessage(parts[1].toLowerCase(), parts[2]);
                        break;
                }
            }
        } catch (Exception e) {
            // Bağlantı koptu
        } finally {
            if (username != null) {
                MainServer.activeClients.remove(username);
                MainServer.log("[AYRILDI] " + username);
                MainServer.broadcastUserList(); // Biri çıkınca herkese haber ver
            }
            try { socket.close(); } catch (IOException e) {}
        }
    }

    private void handleRegister(String user, String key) {
        this.username = user;
        
        MainServer.registerUser(user); // Kalıcı listeye ekle
        MainServer.userKeys.put(user, key);
        MainServer.activeClients.put(user, this);

        MainServer.log("[KAYIT] " + user + " (Anahtar: " + key + ")");
        
        // Yeni biri geldi, herkese güncel listeyi yay!
        MainServer.broadcastUserList();

        // Offline Mesajları İlet
        if (MainServer.offlineMessages.containsKey(user)) {
            List<String> messages = MainServer.offlineMessages.get(user);
            for (String msg : messages) {
                sendMessage(msg);
            }
            MainServer.offlineMessages.remove(user);
            MainServer.log("[OFFLINE] " + user + " mesajları iletildi.");
        }
    }

    private void sendCurrentList() {
        StringBuilder listBuilder = new StringBuilder();
        synchronized (MainServer.allRegisteredUsers) {
            for (String u : MainServer.allRegisteredUsers) {
                boolean isOnline = MainServer.activeClients.containsKey(u);
                listBuilder.append(u).append(":").append(isOnline ? "ONLINE" : "OFFLINE").append(",");
            }
        }
        sendMessage("USER_LIST|" + listBuilder.toString());
    }

    private void handleMessage(String target, String encryptedMsg) {
        try {
            String sKey = MainServer.userKeys.get(this.username);
            
            // Not: Basitlik için hedef offline olsa bile daha önce 1 kere girdiyse key'i hafızada varsayıyoruz.
            // Eğer server restart atılırsa ve hedef hiç girmediyse key null olur, mesaj iletilemez.
            // Bu senaryoda hedef online olmalı.
            String tKey = MainServer.userKeys.get(target); 

            MainServer.log("\n--- [ŞİFRELEME KANITI] ---");
            MainServer.log("1. Gelen Şifreli (" + username + "): " + encryptedMsg);
            
            String plainText = desAction(encryptedMsg, sKey, Cipher.DECRYPT_MODE);
            MainServer.log("2. Sunucu Çözdü (Plain): " + plainText);
            
            if (tKey == null) {
                MainServer.log("[UYARI] Hedef (" + target + ") anahtarı bellekte yok. Mesaj saklanamıyor.");
                return; // Gerçek bir uygulamada veritabanından çekilmeli
            }

            String reEncrypted = desAction(plainText, tKey, Cipher.ENCRYPT_MODE);
            MainServer.log("3. Hedef (" + target + ") İçin Şifrelendi: " + reEncrypted);
            MainServer.log("--------------------------\n");

            String formattedMsg = "MESSAGE|" + this.username + "|" + reEncrypted;

            if (MainServer.activeClients.containsKey(target)) {
                MainServer.activeClients.get(target).sendMessage(formattedMsg);
            } else {
                MainServer.offlineMessages.computeIfAbsent(target, k -> new ArrayList<>()).add(formattedMsg);
                MainServer.log("[STOK] " + target + " offline. Mesaj bekletiliyor.");
            }

        } catch (Exception e) {
            MainServer.log("[KRİPTO HATASI] " + e.getMessage());
        }
    }

    private String desAction(String data, String keyStr, int mode) throws Exception {
        byte[] keyBytes = Arrays.copyOf(keyStr.getBytes("UTF-8"), 8);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "DES");
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(mode, secretKey);

        if (mode == Cipher.ENCRYPT_MODE) {
            return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes("UTF-8")));
        } else {
            return new String(cipher.doFinal(Base64.getDecoder().decode(data)), "UTF-8");
        }
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }
}
