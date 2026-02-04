import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class ChatScreen extends JFrame {
    private final Color COLOR_BG = new Color(10, 15, 15);
    private final Color COLOR_PANEL = new Color(20, 30, 30);
    private final Color COLOR_ACCENT = new Color(13, 242, 242); 
    private final Color COLOR_TEXT_MAIN = new Color(240, 255, 255);
    private final Color COLOR_TEXT_DIM = new Color(144, 203, 203);
    private final Color COLOR_FIELD_BG = new Color(15, 25, 25);

    private String username;
    private String myKey;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private JTextArea messageArea;
    private JTextField txtMessage;
    private JTextField txtTargetUser;
    private DefaultListModel<String> listModel;

    public ChatScreen(String username, String key, Socket socket) {
        this.username = username;
        this.myKey = key;
        this.socket = socket;

        setupFrame();
        initUI();
        setupNetwork();
    }

    private void setupFrame() {
        setTitle("SteganoChat - " + username);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(750, 550);
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_BG);
        setLayout(new BorderLayout(10, 10));
    }

    private void initUI() {
        // --- SOL PANEL ---
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(COLOR_PANEL);
        leftPanel.setPreferredSize(new Dimension(200, 0));
        leftPanel.setBorder(new EmptyBorder(10, 10, 10, 0));

        JLabel lblOnline = new JLabel("Users Directory");
        lblOnline.setForeground(COLOR_ACCENT);
        lblOnline.setFont(new Font("Inter", Font.BOLD, 14));
        lblOnline.setBorder(new EmptyBorder(0, 0, 10, 0));
        leftPanel.add(lblOnline, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(listModel);
        userList.setBackground(COLOR_FIELD_BG);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // ÖZEL RENDERER (RENKLENDİRME İÇİN)
        userList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String entry = (String) value;
                String[] parts = entry.split(":"); // ali:ONLINE
                
                if (parts.length > 1) {
                    label.setText(parts[0]);
                    label.setFont(new Font("Monospaced", Font.BOLD, 14));
                    if (parts[1].equals("ONLINE")) {
                        label.setForeground(Color.GREEN);
                        label.setText("● " + parts[0]);
                    } else {
                        label.setForeground(Color.RED);
                        label.setText("○ " + parts[0]);
                    }
                }
                return label;
            }
        });

        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && userList.getSelectedValue() != null) {
                String val = userList.getSelectedValue();
                txtTargetUser.setText(val.split(":")[0]);
            }
        });

        JScrollPane listScroll = new JScrollPane(userList);
        listScroll.setBorder(new LineBorder(COLOR_TEXT_DIM, 1));
        leftPanel.add(listScroll, BorderLayout.CENTER);

        JButton btnRefresh = new JButton("Force Refresh ↻");
        btnRefresh.addActionListener(e -> { if(out!=null) out.println("GET_USERS"); });
        leftPanel.add(btnRefresh, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.WEST);

        // --- ORTA PANEL ---
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBackground(COLOR_BG);
        centerPanel.setBorder(new EmptyBorder(10, 0, 10, 10));

        JPanel targetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        targetPanel.setBackground(COLOR_BG);
        JLabel lblTarget = new JLabel("Target User:");
        lblTarget.setForeground(COLOR_TEXT_DIM);
        txtTargetUser = new JTextField(12);
        targetPanel.add(lblTarget);
        targetPanel.add(txtTargetUser);
        centerPanel.add(targetPanel, BorderLayout.NORTH);

        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setBackground(COLOR_BG);
        messageArea.setForeground(COLOR_TEXT_MAIN);
        messageArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        messageArea.setLineWrap(true);
        centerPanel.add(new JScrollPane(messageArea), BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // --- ALT PANEL ---
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setBackground(COLOR_PANEL);
        bottomPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        txtMessage = new JTextField();
        txtMessage.addActionListener(this::sendMessage);
        JButton btnSend = new JButton("SEND (DES) →");
        btnSend.setBackground(COLOR_ACCENT);
        btnSend.addActionListener(this::sendMessage);

        bottomPanel.add(txtMessage, BorderLayout.CENTER);
        bottomPanel.add(btnSend, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void setupNetwork() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("GET_USERS"); 

            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        processIncomingMessage(line);
                    }
                } catch (IOException e) {
                    appendMessage("SERVER", "Bağlantı koptu!", true);
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(ActionEvent e) {
        String msg = txtMessage.getText().trim();
        String target = txtTargetUser.getText().trim();
        if (msg.isEmpty() || target.isEmpty()) return;

        try {
            String encryptedMsg = CryptoHelper.encrypt(msg, myKey);
            out.println("SEND|" + target + "|" + encryptedMsg);
            
            appendMessage("Me -> " + target, msg, false);
            System.out.println("[CLIENT LOG] Gönderilen Şifreli: " + encryptedMsg);
            txtMessage.setText("");
        } catch (Exception ex) {
            appendMessage("SYSTEM", "Hata: " + ex.getMessage(), true);
        }
    }

    private void processIncomingMessage(String rawMessage) {
        String[] parts = rawMessage.split("\\|");
        
        if (parts[0].equals("USER_LIST")) {
            SwingUtilities.invokeLater(() -> {
                listModel.clear();
                if (parts.length > 1) {
                    String[] users = parts[1].split(",");
                    for (String u : users) {
                        if (!u.split(":")[0].equalsIgnoreCase(username)) {
                            listModel.addElement(u);
                        }
                    }
                }
            });
            return;
        }

        if (parts.length >= 3 && parts[0].equals("MESSAGE")) {
            String sender = parts[1];
            String encryptedContent = parts[2];

            System.out.println("\n--- [ALICI KANIT LOGU] ---");
            System.out.println("Gelen Şifreli: " + encryptedContent);

            try {
                String decryptedContent = CryptoHelper.decrypt(encryptedContent, myKey);
                System.out.println("Çözülen Mesaj: " + decryptedContent);
                System.out.println("--------------------------");
                appendMessage(sender, decryptedContent, false);
            } catch (Exception e) {
                appendMessage("SYSTEM", "Şifre Çözülemedi!", true);
            }
        }
    }

    private void appendMessage(String sender, String msg, boolean isError) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        SwingUtilities.invokeLater(() -> {
            messageArea.append("[" + time + "] " + sender + ": " + msg + "\n");
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
        });
    }
}
