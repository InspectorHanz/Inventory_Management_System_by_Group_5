package Project;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.*;

@SuppressWarnings("serial")
public class Order extends JFrame {
    private Inventory inventory;
    private JTextField quantityField;
    private JComboBox<String> productDropdown;
    private JTextArea cartArea, outputArea;
    private JButton addToCartButton, checkoutButton, removeButton;

    private Map<String, Integer> cart; // Stores product name and quantity

    @SuppressWarnings("static-access")
    public Order(Inventory inventory) {
        this.inventory = inventory;
        this.cart = new LinkedHashMap<>(); // Maintain order of added items

        // Frame setup
        setTitle("Customer Shopping System");
        setSize(700, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(null);

        // UI Components
        JLabel lblProduct = new JLabel("Select Product:");
        lblProduct.setFont(new Font("Tahoma", Font.PLAIN, 15));
        lblProduct.setBounds(20, 20, 120, 25);
        add(lblProduct);

        productDropdown = new JComboBox<>();
        productDropdown.setBounds(150, 20, 200, 25);
        add(productDropdown);

        JLabel lblQuantity = new JLabel("Quantity:");
        lblQuantity.setFont(new Font("Tahoma", Font.PLAIN, 15));
        lblQuantity.setBounds(20, 70, 120, 25);
        add(lblQuantity);

        quantityField = new JTextField();
        quantityField.setBounds(150, 70, 200, 25);
        add(quantityField);

        addToCartButton = new JButton("Add to Cart");
        addToCartButton.setFont(new Font("Tahoma", Font.PLAIN, 15));
        addToCartButton.setBounds(20, 120, 150, 30);
        add(addToCartButton);

        removeButton = new JButton("Remove Item");
        removeButton.setFont(new Font("Tahoma", Font.PLAIN, 15));
        removeButton.setBounds(200, 120, 150, 30);
        add(removeButton);

        checkoutButton = new JButton("Checkout");
        checkoutButton.setFont(new Font("Tahoma", Font.PLAIN, 15));
        checkoutButton.setBounds(20, 170, 150, 30);
        add(checkoutButton);

        cartArea = new JTextArea();
        cartArea.setEditable(false);
        JScrollPane cartScrollPane = new JScrollPane(cartArea);
        cartScrollPane.setBounds(20, 220, 300, 300);
        add(cartScrollPane);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        outputScrollPane.setBounds(350, 20, 320, 500);
        add(outputScrollPane);

        JButton backButton = new JButton("Back");
        backButton.setFont(new Font("Tahoma", Font.PLAIN, 15));
        backButton.setBounds(580, 530, 100, 30);
        add(backButton);

        // Populate product dropdown
        populateProductDropdown();

        // Action Listeners
        addToCartButton.addActionListener(e -> addToCart());
        removeButton.addActionListener(e -> removeFromCart());
        checkoutButton.addActionListener(e -> checkout());
        backButton.addActionListener(e -> {
            dispose(); // Close Admin Login frame
            SwingUtilities.invokeLater(() -> new LogIn().main(null)); // Redirect to Login System
        });

        setVisible(true);
    }

    private void populateProductDropdown() {
        productDropdown.removeAllItems();
        try (Connection conn = inventory.connectToDatabase()) {
            if (conn != null) {
                String query = "SELECT name FROM products";
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                    while (rs.next()) {
                        productDropdown.addItem(rs.getString("name"));
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading products: " + e.getMessage());
        }
    }

    private void addToCart() {
        String selectedProduct = (String) productDropdown.getSelectedItem();
        if (selectedProduct == null) {
            JOptionPane.showMessageDialog(this, "Please select a product.");
            return;
        }

        String quantityText = quantityField.getText().trim();
        if (!isNumeric(quantityText)) {
            JOptionPane.showMessageDialog(this, "Please enter a valid quantity.");
            return;
        }

        int quantity = Integer.parseInt(quantityText);
        if (quantity <= 0) {
            JOptionPane.showMessageDialog(this, "Quantity must be greater than zero.");
            return;
        }

        // Add or update quantity in cart
        cart.put(selectedProduct, cart.getOrDefault(selectedProduct, 0) + quantity);
        updateCartDisplay();
    }

    private void removeFromCart() {
        String selectedProduct = (String) productDropdown.getSelectedItem();
        if (selectedProduct == null || !cart.containsKey(selectedProduct)) {
            JOptionPane.showMessageDialog(this, "Product not in cart.");
            return;
        }

        cart.remove(selectedProduct);
        updateCartDisplay();
    }

    private void updateCartDisplay() {
        cartArea.setText("Cart:\n");
        cart.forEach((product, quantity) -> cartArea.append(product + " x " + quantity + "\n"));
    }

    private void checkout() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Your cart is empty.");
            return;
        }

        double totalCost = 0;

        try (Connection conn = inventory.connectToDatabase()) {
            if (conn != null) {
                // Validate stock availability
                for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                    String product = entry.getKey();
                    int quantity = entry.getValue();

                    String query = "SELECT quantity, price FROM products WHERE name = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(query)) {
                        stmt.setString(1, product);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                int availableQuantity = rs.getInt("quantity");
                                double price = rs.getDouble("price");

                                if (quantity > availableQuantity) {
                                    JOptionPane.showMessageDialog(this, "Insufficient stock for " + product);
                                    return;
                                }

                                totalCost += price * quantity;
                            } else {
                                JOptionPane.showMessageDialog(this, "Product not found: " + product);
                                return;
                            }
                        }
                    }
                }

                // Prompt for payment
                String paymentMethod = showPaymentDialog(totalCost);
                if (paymentMethod == null) {
                    JOptionPane.showMessageDialog(this, "Payment cancelled.");
                    return;
                }

                // Update stock and insert receipt
                for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                    String product = entry.getKey();
                    int quantity = entry.getValue();

                    String updateQuery = "UPDATE products SET quantity = quantity - ? WHERE name = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                        updateStmt.setInt(1, quantity);
                        updateStmt.setString(2, product);
                        updateStmt.executeUpdate();
                    }
                }

                // Insert receipt
                int receiptId = insertReceipt(conn, totalCost, paymentMethod);
                if (receiptId != -1) {
                    displayReceipt(totalCost, paymentMethod);
                    cart.clear();
                    updateCartDisplay();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to generate receipt.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Log error for debugging
            JOptionPane.showMessageDialog(this, "Error processing checkout: " + e.getMessage());
        }
    }

    private String showPaymentDialog(double totalCost) {
        String[] paymentOptions = {"Credit Card", "PayPal", "Cash"};
        String paymentMethod = (String) JOptionPane.showInputDialog(
                this,
                "Total Cost: $" + totalCost + "\n\nSelect Payment Method:",
                "Payment",
                JOptionPane.QUESTION_MESSAGE,
                null,
                paymentOptions,
                paymentOptions[0]
        );

        if ("Cash".equals(paymentMethod)) {
            boolean validPayment = false;
            while (!validPayment) {
                String cashInput = JOptionPane.showInputDialog(
                        this,
                        "Total Cost: $" + totalCost + "\n\nEnter Cash Amount:",
                        "Cash Payment",
                        JOptionPane.PLAIN_MESSAGE
                );

                if (cashInput == null) {
                    JOptionPane.showMessageDialog(this, "Payment cancelled.");
                    return null; // Cancel payment process
                }

                if (isNumeric(cashInput)) {
                    double cashAmount = Double.parseDouble(cashInput);
                    if (cashAmount >= totalCost) {
                        double change = cashAmount - totalCost;
                        JOptionPane.showMessageDialog(this, "Payment successful. Change: $" + change);
                        validPayment = true;
                    } else {
                        JOptionPane.showMessageDialog(this, "Insufficient cash. Please enter an amount equal to or greater than the total cost.");
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid input. Please enter a numeric value.");
                }
            }
        } else if (paymentMethod != null) {
            JOptionPane.showMessageDialog(this, "You selected: " + paymentMethod);
        }
        return paymentMethod;
    }

    private int insertReceipt(Connection conn, double totalCost, String paymentMethod) throws SQLException {
        String insertReceiptQuery = "INSERT INTO receipts (total_cost, payment_method, amount_paid, changes, receipt_date) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertReceiptQuery, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setDouble(1, totalCost);
            stmt.setString(2, paymentMethod);
            stmt.setDouble(3, totalCost);  // assuming full amount paid for simplicity
            stmt.setDouble(4, 0); // assuming no change for simplicity
            stmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }
        return -1; // Return invalid ID if insert failed
    }

    private void displayReceipt(double totalCost, String paymentMethod) {
        outputArea.setText("Receipt:\nTotal Cost: $" + totalCost + "\nPayment Method: " + paymentMethod);
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        // Assuming Inventory instance is available
        Inventory inventory = new Inventory();
        new Order(inventory);
    }
}
