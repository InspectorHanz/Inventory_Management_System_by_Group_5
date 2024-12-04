package Project;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial")
public class Admin extends JFrame {
    @SuppressWarnings("static-access")
	public Admin() {
        // Set up the frame
        setTitle("Admin Login");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);

        JLabel titleLabel = new JLabel("Admin Login");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setBounds(120, 20, 160, 30);

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setBounds(50, 80, 100, 25);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setBounds(50, 120, 100, 25);

        JTextField usernameField = new JTextField();
        usernameField.setBounds(150, 80, 180, 25);

        JPasswordField passwordField = new JPasswordField();
        passwordField.setBounds(150, 120, 180, 25);

        JButton loginButton = new JButton("Login");
        loginButton.setBounds(100, 170, 100, 30);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBounds(220, 170, 100, 30);

        // Login Button Action
        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            if (LogIn.authenticate(username, password, "admin")) {
                JOptionPane.showMessageDialog(this, "Admin Login Successful!");
                dispose(); // Close Admin Login frame
                Inventory inventory = new Inventory(); // Load Inventory system
                inventory.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid admin credentials.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Cancel Button Action
        cancelButton.addActionListener(e -> {
            dispose(); // Close Admin Login frame
            SwingUtilities.invokeLater(() -> new LogIn().main(null)); // Redirect to Login System
        });

        add(titleLabel);
        add(usernameLabel);
        add(passwordLabel);
        add(usernameField);
        add(passwordField);
        add(loginButton);
        add(cancelButton);

        setLocationRelativeTo(null);
    }
}
