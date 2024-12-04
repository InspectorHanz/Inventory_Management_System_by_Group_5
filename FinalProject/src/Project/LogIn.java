package Project;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LogIn {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Login System");
        frame.setSize(400, 350);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(null);

        JLabel titleLabel = new JLabel("Login");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setBounds(150, 39, 73, 30);

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setBounds(50, 104, 100, 25);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setBounds(50, 139, 100, 25);

        JTextField usernameField = new JTextField();
        usernameField.setBounds(150, 104, 180, 25);

        JPasswordField passwordField = new JPasswordField();
        passwordField.setBounds(150, 139, 180, 25);

        JButton loginButton = new JButton("Login");
        loginButton.setBounds(109, 193, 100, 30);

        JButton signUpButton = new JButton("Sign Up");
        signUpButton.setBounds(240, 193, 100, 30);

        JButton cancelButton = new JButton("Exit");
        cancelButton.setBounds(285, 273, 91, 30);

        // Login Button Action for Regular Users
        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            if (authenticate(username, password, "user")) {
                JOptionPane.showMessageDialog(frame, "Login successful! Welcome, " + username + "!");
                frame.dispose();

                Inventory inventory = new Inventory(); // Load inventory
                new Order(inventory).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        signUpButton.addActionListener(e -> new SignUpWindow().setVisible(true));

        cancelButton.addActionListener(e -> System.exit(0));

        frame.getContentPane().add(titleLabel);
        frame.getContentPane().add(usernameLabel);
        frame.getContentPane().add(passwordLabel);
        frame.getContentPane().add(usernameField);
        frame.getContentPane().add(passwordField);
        frame.getContentPane().add(loginButton);
        frame.getContentPane().add(signUpButton);
        frame.getContentPane().add(cancelButton);

        // Admin Button Action
        JButton btnAdmin = new JButton("Admin");
        btnAdmin.setBounds(10, 273, 100, 30);
        btnAdmin.addActionListener(e -> {
        	frame.dispose();
            new Admin().setVisible(true);
            
        });
        frame.getContentPane().add(btnAdmin);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    static boolean authenticate(String username, String password, String userType) {
        boolean isAuthenticated = false;
        String dbURL, tableName;

        if (userType.equals("admin")) {
            dbURL = "jdbc:mysql://localhost:3306/admin_authentication";
            tableName = "admins";
        } else {
            dbURL = "jdbc:mysql://localhost:3306/user_authentication";
            tableName = "users";
        }

        String dbUser = "root";
        String dbPassword = "1234";

        try (Connection conn = DriverManager.getConnection(dbURL, dbUser, dbPassword)) {
            String query = "SELECT * FROM " + tableName + " WHERE username = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            isAuthenticated = rs.next();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database connection error: " + e.getMessage());
        }

        return isAuthenticated;
    }

    @SuppressWarnings("serial")
    static class SignUpWindow extends JFrame {
        public SignUpWindow() {
            setTitle("Sign Up");
            setSize(400, 300);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setLayout(null);

            JLabel titleLabel = new JLabel("Sign Up");
            titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
            titleLabel.setBounds(150, 20, 100, 30);

            JLabel usernameLabel = new JLabel("Username:");
            usernameLabel.setBounds(50, 80, 100, 25);

            JLabel passwordLabel = new JLabel("Password:");
            passwordLabel.setBounds(50, 120, 100, 25);

            JTextField usernameField = new JTextField();
            usernameField.setBounds(150, 80, 180, 25);

            JPasswordField passwordField = new JPasswordField();
            passwordField.setBounds(150, 120, 180, 25);

            JButton registerButton = new JButton("Register");
            registerButton.setBounds(150, 160, 100, 30);

            JButton cancelButton = new JButton("Cancel");
            cancelButton.setBounds(260, 160, 100, 30);

            registerButton.addActionListener(e -> {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                if (registerUser(username, password, "user")) { // Default to user database
                    JOptionPane.showMessageDialog(this, "Registration successful! You can now log in.");
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Registration failed. Username might already exist.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            cancelButton.addActionListener(e -> dispose());

            add(titleLabel);
            add(usernameLabel);
            add(passwordLabel);
            add(usernameField);
            add(passwordField);
            add(registerButton);
            add(cancelButton);

            setLocationRelativeTo(null);
        }
    }

    private static boolean registerUser(String username, String password, String userType) {
        boolean isRegistered = false;
        String dbURL = "jdbc:mysql://localhost:3306/user_authentication";
        String tableName = "users";

        if (userType.equals("admin")) {
            dbURL = "jdbc:mysql://localhost:3306/admin_authentication";
            tableName = "admins";
        }

        String dbUser = "root";
        String dbPassword = "1234";

        try (Connection conn = DriverManager.getConnection(dbURL, dbUser, dbPassword)) {
            String query = "INSERT INTO " + tableName + " (username, password) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.executeUpdate();
            isRegistered = true;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database connection error: " + e.getMessage());
        }

        return isRegistered;
    }
}
