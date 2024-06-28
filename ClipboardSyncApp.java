import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ClipboardSyncApp {

    private static String serverUrl = "http://16.170.246.163";
    private static String accessToken = "";

    public static void main(String[] args) {
        JFrame frame = new JFrame("Clipboard Sync App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 300);

        JPanel panel = new JPanel();
        frame.add(panel);
        placeComponents(panel);

        frame.setVisible(true);
    }

    private static void placeComponents(JPanel panel) {
        panel.setLayout(null);

        JLabel userLabel = new JLabel("User");
        userLabel.setBounds(10, 20, 80, 25);
        panel.add(userLabel);

        JTextField userText = new JTextField(20);
        userText.setBounds(100, 20, 165, 25);
        panel.add(userText);

        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setBounds(10, 50, 80, 25);
        panel.add(passwordLabel);

        JPasswordField passwordText = new JPasswordField(20);
        passwordText.setBounds(100, 50, 165, 25);
        panel.add(passwordText);

        JButton loginButton = new JButton("Login");
        loginButton.setBounds(10, 80, 80, 25);
        panel.add(loginButton);

        JButton registerButton = new JButton("Register");
        registerButton.setBounds(100, 80, 100, 25);
        panel.add(registerButton);

        JButton pushButton = new JButton("Push Clipboard");
        pushButton.setBounds(10, 120, 250, 25);
        pushButton.setEnabled(false);
        panel.add(pushButton);

        JButton pullButton = new JButton("Pull Clipboard");
        pullButton.setBounds(10, 160, 250, 25);
        pullButton.setEnabled(false);
        panel.add(pullButton);

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = userText.getText();
                String password = new String(passwordText.getPassword());
                try {
                    String response = login(username, password);
                    if (response != null) {
                        accessToken = response;
                        pushButton.setEnabled(true);
                        pullButton.setEnabled(true);
                        JOptionPane.showMessageDialog(panel, "Login successful!");
                    } else {
                        JOptionPane.showMessageDialog(panel, "Login failed!");
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = userText.getText();
                String password = new String(passwordText.getPassword());
                try {
                    String response = register(username, password);
                    if (response != null) {
                        JOptionPane.showMessageDialog(panel, "Registration successful! Please log in.");
                    } else {
                        JOptionPane.showMessageDialog(panel, "Registration failed!");
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        pushButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String clipboardContent = getClipboardContents();
                    updateClipboard(clipboardContent);
                    JOptionPane.showMessageDialog(panel, "Clipboard pushed!");
                } catch (IOException | UnsupportedFlavorException ex) {
                    ex.printStackTrace();
                }
            }
        });

        pullButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String clipboardContent = fetchClipboard();
                    setClipboardContents(clipboardContent);
                    JOptionPane.showMessageDialog(panel, "Clipboard pulled!");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private static String register(String username, String password) throws IOException {
        URL url = new URL(serverUrl + "/register");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String jsonInputString = String.format("{\"username\": \"%s\", \"password\": \"%s\"}", username, password);
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 201) {
            return "Registration successful";
        } else {
            return null;
        }
    }

    private static String login(String username, String password) throws IOException {
        URL url = new URL(serverUrl + "/login");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String jsonInputString = String.format("{\"username\": \"%s\", \"password\": \"%s\"}", username, password);
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString().split(":")[1].replaceAll("[\"}]", ""); // Extracting the token from the response
            }
        } else {
            return null;
        }
    }

    private static void updateClipboard(String clipboardContent) throws IOException {
        URL url = new URL(serverUrl + "/clipboard");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setDoOutput(true);

        String jsonInputString = String.format("{\"clipboard\": \"%s\"}", clipboardContent);
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Failed to sync clipboard: " + responseCode);
        }
    }

    private static String fetchClipboard() throws IOException {
        URL url = new URL(serverUrl + "/clipboard");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString().split(":")[1].replaceAll("[\"}]", ""); // Extracting the clipboard content from the response
            }
        } else {
            throw new IOException("Failed to fetch clipboard: " + responseCode);
        }
    }

    private static String getClipboardContents() throws UnsupportedFlavorException, IOException {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        if (hasTransferableText) {
            return (String) contents.getTransferData(DataFlavor.stringFlavor);
        }
        return "";
    }

    private static void setClipboardContents(String str) {
        StringSelection stringSelection = new StringSelection(str);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }
}
