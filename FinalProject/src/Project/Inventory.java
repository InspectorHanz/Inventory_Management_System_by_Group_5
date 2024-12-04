package Project;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressWarnings("serial")
public class Inventory extends JFrame {
    public Inventory() {
        // Set up the frame
        setTitle("Computer Part Inventory Management System");
        setSize(570, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        // Button setup
        JButton addButton = new JButton("Add Product");
        addButton.setFont(new Font("Tahoma", Font.PLAIN, 15));
        addButton.setBounds(49, 58, 168, 67);
        getContentPane().add(addButton);

        JButton viewButton = new JButton("View Inventory");
        viewButton.setFont(new Font("Tahoma", Font.PLAIN, 15));
        viewButton.setBounds(313, 176, 168, 67);
        getContentPane().add(viewButton);

        JButton removeButton = new JButton("Remove Product");
        removeButton.setFont(new Font("Tahoma", Font.PLAIN, 15));
        removeButton.setBounds(313, 58, 168, 67);
        getContentPane().add(removeButton);

        JButton restockButton = new JButton("Restock Product");
        restockButton.setFont(new Font("Tahoma", Font.PLAIN, 15));
        restockButton.setBounds(49, 176, 168, 67);
        getContentPane().add(restockButton);

        JButton stockMovementButton = new JButton("Stock Movement");
        stockMovementButton.setFont(new Font("Tahoma", Font.PLAIN, 15));
        stockMovementButton.setBounds(184, 287, 168, 67);
        getContentPane().add(stockMovementButton);

        JButton backButton = new JButton("Back");
        backButton.setFont(new Font("Tahoma", Font.PLAIN, 15));
        backButton.setBounds(419, 450, 89, 33);
        backButton.addActionListener(e -> {
            setVisible(false); // Close the Inventory window
            new Admin().setVisible(true); // Return to Admin Login
        });
        getContentPane().add(backButton);

        // Button actions
        viewButton.addActionListener(e -> showInventory());
        addButton.addActionListener(e -> openAddProductDialog());
        removeButton.addActionListener(e -> openRemoveProductDialog());
        restockButton.addActionListener(e -> openRestockProductDialog());
        stockMovementButton.addActionListener(e -> showStockMovement());
    }

    private void showInventory() {
        JDialog inventoryDialog = new JDialog(this, "Inventory", true);

        String[] columnNames = {"Product ID", "Name", "Quantity", "Price"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        JTable inventoryTable = new JTable(tableModel);

        try (Connection conn = connectToDatabase()) {
            if (conn != null) {
                String query = "SELECT * FROM products";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query);

                while (rs.next()) {
                    int productId = rs.getInt("product_id");
                    String name = rs.getString("name");
                    int quantity = rs.getInt("quantity");
                    double price = rs.getDouble("price");

                    tableModel.addRow(new Object[]{productId, name, quantity, price});
                }

                if (tableModel.getRowCount() == 0) {
                    JOptionPane.showMessageDialog(this, "No products in inventory.");
                    inventoryDialog.dispose();
                    return;
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error retrieving inventory: " + e.getMessage());
        }

        JScrollPane scrollPane = new JScrollPane(inventoryTable);
        inventoryDialog.getContentPane().add(scrollPane, BorderLayout.CENTER);

        inventoryDialog.setSize(500, 400);
        inventoryDialog.setLocationRelativeTo(this);
        inventoryDialog.setVisible(true);
    }

    
    // Add Product dialog
    private void openAddProductDialog() {
        JDialog addProductDialog = new JDialog(this, "Add Product", true);
        addProductDialog.getContentPane().setLayout(new GridLayout(5, 2, 10, 10));

        JTextField nameField = new JTextField();
        JTextField quantityField = new JTextField();
        JTextField priceField = new JTextField();

        addProductDialog.getContentPane().add(new JLabel("Product Name:"));
        addProductDialog.getContentPane().add(nameField);
        addProductDialog.getContentPane().add(new JLabel("Quantity:"));
        addProductDialog.getContentPane().add(quantityField);
        addProductDialog.getContentPane().add(new JLabel("Price:"));
        addProductDialog.getContentPane().add(priceField);

        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String quantityStr = quantityField.getText().trim();
            String priceStr = priceField.getText().trim();

            if (name.isEmpty() || quantityStr.isEmpty() || priceStr.isEmpty()) {
                JOptionPane.showMessageDialog(addProductDialog, "Please fill all fields.");
                return;
            }

            try (Connection conn = connectToDatabase()) {
                if (conn != null) {
                    int quantity = Integer.parseInt(quantityStr);
                    double price = Double.parseDouble(priceStr);

                    String query = "INSERT INTO products (name, quantity, price) VALUES (?, ?, ?)";
                    PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                    stmt.setString(1, name);
                    stmt.setInt(2, quantity);
                    stmt.setDouble(3, price);
                    stmt.executeUpdate();

                    // Get the product ID of the newly inserted product
                    ResultSet rs = stmt.getGeneratedKeys();
                    if (rs.next()) {
                        int productId = rs.getInt(1);

                        // Add stock movement record
                        String movementQuery = "INSERT INTO stock_movement (product_id, movement_type, quantity, movement_date) VALUES (?, ?, ?, ?)";
                        PreparedStatement movementStmt = conn.prepareStatement(movementQuery);
                        movementStmt.setInt(1, productId);
                        movementStmt.setString(2, "Added");
                        movementStmt.setInt(3, quantity);
                        movementStmt.setString(4, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                        movementStmt.executeUpdate();
                    }

                    JOptionPane.showMessageDialog(this, "Product added successfully.");
                    addProductDialog.dispose();
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(addProductDialog, "Error adding product: " + ex.getMessage());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(addProductDialog, "Please enter valid numbers for quantity and price.");
            }
        });

        addProductDialog.getContentPane().add(new JLabel());
        addProductDialog.getContentPane().add(addButton);

        addProductDialog.setSize(300, 200);
        addProductDialog.setLocationRelativeTo(this);
        addProductDialog.setVisible(true);
    }

 // Remove Product dialog
    private void openRemoveProductDialog() {
        JDialog removeProductDialog = new JDialog(this, "Remove Product", true);
        removeProductDialog.getContentPane().setLayout(new GridLayout(3, 2, 10, 10));

        JTextField idField = new JTextField();
        JTextField quantityField = new JTextField();

        removeProductDialog.getContentPane().add(new JLabel("Product ID:"));
        removeProductDialog.getContentPane().add(idField);
        removeProductDialog.getContentPane().add(new JLabel("Quantity to Remove:"));
        removeProductDialog.getContentPane().add(quantityField);

        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> {
            String idStr = idField.getText().trim();
            String quantityStr = quantityField.getText().trim();

            if (idStr.isEmpty() || quantityStr.isEmpty()) {
                JOptionPane.showMessageDialog(removeProductDialog, "Please fill in all fields.");
                return;
            }

            try (Connection conn = connectToDatabase()) {
                if (conn != null) {
                    int productId = Integer.parseInt(idStr);
                    int quantityToRemove = Integer.parseInt(quantityStr);

                    if (quantityToRemove <= 0) {
                        JOptionPane.showMessageDialog(removeProductDialog, "Quantity must be greater than 0.");
                        return;
                    }

                    // Check if the product exists and has enough stock
                    String checkQuery = "SELECT quantity FROM products WHERE product_id = ?";
                    PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
                    checkStmt.setInt(1, productId);

                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next()) {
                        int currentQuantity = rs.getInt("quantity");
                        if (currentQuantity < quantityToRemove) {
                            JOptionPane.showMessageDialog(removeProductDialog, "Insufficient stock to remove.");
                            return;
                        }

                        // Update the product's quantity in the database
                        String updateQuery = "UPDATE products SET quantity = quantity - ? WHERE product_id = ?";
                        PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                        updateStmt.setInt(1, quantityToRemove);
                        updateStmt.setInt(2, productId);

                        int rowsAffected = updateStmt.executeUpdate();
                        if (rowsAffected > 0) {
                            // Log the removal in stock movement
                            String movementQuery = "INSERT INTO stock_movement (product_id, movement_type, quantity, movement_date) VALUES (?, ?, ?, ?)";
                            PreparedStatement movementStmt = conn.prepareStatement(movementQuery);
                            movementStmt.setInt(1, productId);
                            movementStmt.setString(2, "Removed");
                            movementStmt.setInt(3, quantityToRemove);
                            movementStmt.setString(4, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                            movementStmt.executeUpdate();

                            JOptionPane.showMessageDialog(this, "Product quantity removed successfully.");
                        } else {
                            JOptionPane.showMessageDialog(this, "Failed to remove product.");
                        }
                        removeProductDialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(removeProductDialog, "Product not found.");
                    }
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(removeProductDialog, "Error removing product: " + ex.getMessage());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(removeProductDialog, "Please enter valid numbers for Product ID and Quantity.");
            }
        });

        removeProductDialog.getContentPane().add(new JLabel());
        removeProductDialog.getContentPane().add(removeButton);

        removeProductDialog.setSize(300, 200);
        removeProductDialog.setLocationRelativeTo(this);
        removeProductDialog.setVisible(true);
    }

    // Restock Product dialog
    private void openRestockProductDialog() {
        JDialog restockDialog = new JDialog(this, "Restock Product", true);
        restockDialog.getContentPane().setLayout(new GridLayout(3, 2, 10, 10));

        JTextField idField = new JTextField();
        JTextField quantityField = new JTextField();

        restockDialog.getContentPane().add(new JLabel("Product ID:"));
        restockDialog.getContentPane().add(idField);
        restockDialog.getContentPane().add(new JLabel("Quantity to Restock:"));
        restockDialog.getContentPane().add(quantityField);

        JButton restockButton = new JButton("Restock");
        restockButton.addActionListener(e -> {
            String idStr = idField.getText().trim();
            String quantityStr = quantityField.getText().trim();

            if (idStr.isEmpty() || quantityStr.isEmpty()) {
                JOptionPane.showMessageDialog(restockDialog, "Please fill in all fields.");
                return;
            }

            try (Connection conn = connectToDatabase()) {
                if (conn != null) {
                    int productId = Integer.parseInt(idStr);
                    int quantity = Integer.parseInt(quantityStr);

                    if (quantity <= 0) {
                        JOptionPane.showMessageDialog(restockDialog, "Quantity must be greater than 0.");
                        return;
                    }

                    // Update the product's quantity in the database
                    String updateQuery = "UPDATE products SET quantity = quantity + ? WHERE product_id = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                    updateStmt.setInt(1, quantity);
                    updateStmt.setInt(2, productId);

                    int rowsAffected = updateStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        JOptionPane.showMessageDialog(this, "Product restocked successfully.");

                        // Optionally log restock operation (stock movement tracking)
                        String movementQuery = "INSERT INTO stock_movement (product_id, movement_type, quantity, movement_date) VALUES (?, ?, ?, ?)";
                        PreparedStatement movementStmt = conn.prepareStatement(movementQuery);
                        movementStmt.setInt(1, productId);
                        movementStmt.setString(2, "Restocked");
                        movementStmt.setInt(3, quantity);
                        movementStmt.setString(4, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                        movementStmt.executeUpdate();
                    } else {
                        JOptionPane.showMessageDialog(this, "Product not found.");
                    }

                    restockDialog.dispose();
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(restockDialog, "Error restocking product: " + ex.getMessage());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(restockDialog, "Please enter valid numbers for Product ID and Quantity.");
            }
        });

        restockDialog.getContentPane().add(new JLabel());
        restockDialog.getContentPane().add(restockButton);

        restockDialog.setSize(300, 200);
        restockDialog.setLocationRelativeTo(this);
        restockDialog.setVisible(true);
    }
    
    // Show Stock Movement history
    private void showStockMovement() {
        JDialog stockMovementDialog = new JDialog(this, "Stock Movement History", true);

        String[] columnNames = {"Movement ID", "Product ID", "Product Name", "Type", "Quantity", "Date"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        JTable movementTable = new JTable(tableModel);

        try (Connection conn = connectToDatabase()) {
            if (conn != null) {
                String query = "SELECT sm.movement_id, sm.product_id, p.name, sm.movement_type, sm.quantity, sm.movement_date " +
                               "FROM stock_movement sm " +
                               "JOIN products p ON sm.product_id = p.product_id " +
                               "ORDER BY sm.movement_date DESC";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query);

                while (rs.next()) {
                    int movementId = rs.getInt("movement_id");
                    int productId = rs.getInt("product_id");
                    String productName = rs.getString("name");
                    String movementType = rs.getString("movement_type");
                    int quantity = rs.getInt("quantity");
                    String movementDate = rs.getString("movement_date");

                    tableModel.addRow(new Object[]{movementId, productId, productName, movementType, quantity, movementDate});
                }

                if (tableModel.getRowCount() == 0) {
                    JOptionPane.showMessageDialog(this, "No stock movements recorded.");
                    stockMovementDialog.dispose();
                    return;
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error retrieving stock movements: " + e.getMessage());
        }

        JScrollPane scrollPane = new JScrollPane(movementTable);
        stockMovementDialog.getContentPane().add(scrollPane, BorderLayout.CENTER);

        stockMovementDialog.setSize(600, 400);
        stockMovementDialog.setLocationRelativeTo(this);
        stockMovementDialog.setVisible(true);
    }

    // Connect to database
    Connection connectToDatabase() {
        try {
            String url = "jdbc:mysql://localhost:3306/inventorydb";
            String user = "root";
            String password = "1234";
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database connection failed: " + ex.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Inventory inventory = new Inventory();
            inventory.setVisible(true);
        });
    }
}
