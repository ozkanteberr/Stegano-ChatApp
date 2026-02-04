import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MainServer {
    private static final int PORT = 5555;
    private static final String LOG_FILE = "server_logs.txt";
    private static final String USERS_FILE = "registered_users.txt"; // Kalıcı kullanıcı listesi

    // Aktif (Online) olanlar
    public static Map<String, ClientHandler> activeClients = new ConcurrentHashMap<>();
    
    // Anlık Anahtarlar (RAM'de tutulur)
    public static Map<String, String> userKeys = new ConcurrentHashMap<>();
    
    // Offline Mesajlar
    public static Map<String, List<String>> offlineMessages = new ConcurrentHashMap<>();
    
    // Tüm Kayıtlı Kullanıcılar (Online + Offline)
    public static Set<String> allRegisteredUsers = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        loadRegisteredUsers(); // Dosyadan eski kullanıcıları yükle
        log("--- SUNUCU BAŞLATILIYOR (Port: " + PORT + ") ---");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            log("SUNUCU HATASI: " + e.getMessage());
        }
    }

    public static synchronized void registerUser(String username) {
        if (!allRegisteredUsers.contains(username)) {
            allRegisteredUsers.add(username);
            // Dosyaya da ekle
            try (FileWriter fw = new FileWriter(USERS_FILE, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                pw.println(username);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void loadRegisteredUsers() {
        File file = new File(USERS_FILE);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    allRegisteredUsers.add(line.trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcastUserList() {
        // Tüm clientlara güncel listeyi gönderir
        StringBuilder listBuilder = new StringBuilder();
        synchronized (allRegisteredUsers) {
            for (String user : allRegisteredUsers) {
                boolean isOnline = activeClients.containsKey(user);
                listBuilder.append(user).append(":").append(isOnline ? "ONLINE" : "OFFLINE").append(",");
            }
        }
        
        String listMsg = "USER_LIST|" + listBuilder.toString();
        for (ClientHandler client : activeClients.values()) {
            client.sendMessage(listMsg);
        }
    }

    public static synchronized void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String logEntry = "[" + timestamp + "] " + message;
        System.out.println(logEntry);
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(logEntry);
        } catch (IOException e) {
            System.err.println("Log yazılamadı: " + e.getMessage());
        }
    }
}
