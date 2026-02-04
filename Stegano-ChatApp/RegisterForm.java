import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.Base64;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class RegisterForm extends JFrame {
    // Görsel Renk Paleti
    private final Color COLOR_BG = new Color(10, 15, 15);
    private final Color COLOR_CARD = new Color(20, 30, 30, 230);
    private final Color COLOR_ACCENT = new Color(13, 242, 242); // Neon Cyan
    private final Color COLOR_TEXT_MAIN = new Color(240, 255, 255);
    private final Color COLOR_TEXT_DIM = new Color(144, 203, 203);
    private final Color COLOR_FIELD_BG = new Color(15, 25, 25);
    private final Color BORDER_COLOR = new Color(49, 104, 104);

    private JTextField txtUser;
    private JPasswordField txtKey;
    private JLabel lblUploadInfo;
    private File selectedFile;
    private JPanel uploadArea;

    public RegisterForm() {
        setupFrame();
        initUI();
    }

    private void setupFrame() {
        setTitle("SteganoAuth - Create Identity");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(520, 820);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(COLOR_BG);
        setLayout(new GridBagLayout());
    }

    private void initUI() {
        // Ana Kart - Glassmorphism Efekti
        JPanel card = new RoundedPanel(40, COLOR_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(50, 45, 50, 45));
        card.setPreferredSize(new Dimension(460, 740));

        // 1. Header (İkon ve Başlıklar)
        JLabel lblShield = new JLabel("🛡");
        lblShield.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 64));
        lblShield.setForeground(COLOR_ACCENT);
        lblShield.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblShield);
        card.add(Box.createRigidArea(new Dimension(0, 15)));

        JLabel lblTitle = new JLabel("Create Identity");
        lblTitle.setFont(new Font("Inter", Font.BOLD, 32));
        lblTitle.setForeground(COLOR_TEXT_MAIN);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblTitle);
        card.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel lblSub = new JLabel("Initialize secure steganography channel");
        lblSub.setFont(new Font("Inter", Font.PLAIN, 13));
        lblSub.setForeground(COLOR_TEXT_DIM);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblSub);
        card.add(Box.createRigidArea(new Dimension(0, 50)));

        // 2. Input Alanları - BÜYÜTÜLMÜŞ
        card.add(createInputLabel("CODENAME"));
        txtUser = createCustomField("Enter your alias", false);
        card.add(txtUser);
        card.add(Box.createRigidArea(new Dimension(0, 25)));

        card.add(createInputLabel("SECRET KEY"));
        txtKey = (JPasswordField) createCustomField("Min. 8 characters", true);
        card.add(txtKey);
        card.add(Box.createRigidArea(new Dimension(0, 30)));

        // 3. Resim Alanı
        card.add(createInputLabel("COVER IMAGE"));
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        uploadArea = createUploadZone();
        card.add(uploadArea);
        card.add(Box.createRigidArea(new Dimension(0, 45)));

        // 4. Kayıt Butonu - BÜYÜTÜLMÜŞ
        JButton btnAction = new JButton("ENCRYPT & REGISTER →");
        btnAction.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        btnAction.setBackground(COLOR_ACCENT);
        btnAction.setForeground(COLOR_BG);
        btnAction.setFont(new Font("Inter", Font.BOLD, 15));
        btnAction.setFocusPainted(false);
        btnAction.setBorderPainted(false);
        btnAction.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAction.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnAction.addActionListener(e -> sendRegisterRequest());
        card.add(btnAction);

        add(card);
    }

    private JLabel createInputLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(COLOR_ACCENT); // Neon cyan rengi
        l.setFont(new Font("Inter", Font.BOLD, 14)); // Daha büyük font
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        l.setBorder(new EmptyBorder(0, 0, 8, 0));
        return l;
    }

    private JTextField createCustomField(String placeholder, boolean isPass) {
        JTextField f = isPass ? new JPasswordField() : new JTextField();
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50)); // Daha yüksek
        f.setBackground(COLOR_FIELD_BG);
        f.setForeground(COLOR_TEXT_MAIN);
        f.setFont(new Font("Inter", Font.PLAIN, 16)); // Daha büyük yazı
        f.setHorizontalAlignment(JTextField.CENTER);
        f.setCaretColor(COLOR_ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(40, 60, 60), 2), // Daha kalın border
            new EmptyBorder(0, 15, 0, 15)
        ));
        
        // Placeholder efekti
        if (!isPass) {
            f.setText(placeholder);
            f.setForeground(COLOR_TEXT_DIM);
            f.addFocusListener(new java.awt.event.FocusAdapter() {
                public void focusGained(java.awt.event.FocusEvent e) {
                    if (f.getText().equals(placeholder)) {
                        f.setText("");
                        f.setForeground(COLOR_TEXT_MAIN);
                    }
                }
                public void focusLost(java.awt.event.FocusEvent e) {
                    if (f.getText().isEmpty()) {
                        f.setText(placeholder);
                        f.setForeground(COLOR_TEXT_DIM);
                    }
                }
            });
        }
        
        return f;
    }

    private JPanel createUploadZone() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float[] dash = {8.0f};
                g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
            }
        };
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150)); // Daha büyük
        p.setLayout(new BorderLayout());
        
        lblUploadInfo = new JLabel("<html><center><div style='font-size:14px;'>📁 Click to select cover image</div><br><font size='3' color='#90CBCB'>PNG Carrier Required</font></center></html>");
        lblUploadInfo.setForeground(COLOR_TEXT_DIM);
        lblUploadInfo.setHorizontalAlignment(JLabel.CENTER);
        p.add(lblUploadInfo, BorderLayout.CENTER);
        
        p.setCursor(new Cursor(Cursor.HAND_CURSOR));
        p.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                JFileChooser jfc = new JFileChooser();
                jfc.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
                jfc.setAcceptAllFileFilterUsed(false);
    
                if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    selectedFile = jfc.getSelectedFile();
                    previewImage();
                }
            }
        });
        return p;
    }

    private void previewImage() {
        try {
            BufferedImage img = ImageIO.read(selectedFile);
            Image scaled = img.getScaledInstance(140, 90, Image.SCALE_SMOOTH);
            lblUploadInfo.setIcon(new ImageIcon(scaled));
            lblUploadInfo.setText("<html><center><div style='font-size:13px; color:#0DF2F2;'>✓ " + selectedFile.getName() + "</div></center></html>");
            lblUploadInfo.setVerticalTextPosition(JLabel.BOTTOM);
            lblUploadInfo.setHorizontalTextPosition(JLabel.CENTER);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void sendRegisterRequest() {
        String user = txtUser.getText().trim();
        String key = new String(txtKey.getPassword()).trim();

        // Placeholder kontrolü
        if (user.equals("Enter your alias")) user = "";

        if (user.isEmpty() || key.isEmpty() || selectedFile == null) {
            String missing = "";
            if (user.isEmpty()) missing += "• Codename\n";
            if (key.isEmpty()) missing += "• Secret Key\n";
            if (selectedFile == null) missing += "• Image Carrier";
            
            JOptionPane.showMessageDialog(this, 
                "⚠️ Security Breach: Missing fields:\n\n" + missing, 
                "Validation Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (key.length() < 8) {
            JOptionPane.showMessageDialog(this, 
                "⚠️ Secret Key must be at least 8 characters!", 
                "Weak Key", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            BufferedImage img = ImageIO.read(selectedFile);
            BufferedImage stego = SteganoManager.encode(img, key);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(stego, "png", baos);
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());

            Socket s = new Socket("localhost", 5555);
            PrintWriter out = new PrintWriter(s.getOutputStream(), true);
            
            out.println("REGISTER_IMG|" + user + "|" + base64);
            
            JOptionPane.showMessageDialog(this, 
                "✓ Identity '" + user + "' registered successfully!", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
            
            this.dispose();
            new ChatScreen(user, key, s).setVisible(true);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "⚠️ Connection Error: Server is offline!\n" + ex.getMessage(),
                "Network Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    class RoundedPanel extends JPanel {
        private int radius;
        private Color color;
        public RoundedPanel(int r, Color c) {
            this.radius = r;
            this.color = c;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.setColor(BORDER_COLOR);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, radius, radius);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RegisterForm().setVisible(true));
    }
}