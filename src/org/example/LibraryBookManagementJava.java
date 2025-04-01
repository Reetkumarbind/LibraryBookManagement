package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class LibraryBookManagementJava {
    private static final String URL = "jdbc:mysql://localhost:3306/mydb";
    private static final String USER = "root";
    private static final String PASSWORD = "admin";

    private static Connection conn;
    private static JFrame frame;
    private static JTextArea outputArea;
    private static JTextField bookIdField, titleField, authorField, genreField, availabilityField;
    private static JScrollPane outputScrollPane;
    private static JPanel outputPanel;
    private static JPanel buttonPanel; // Made static to access in viewBooks
    private static JButton closeButton; // Added for closing book list

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                conn = DriverManager.getConnection(URL, USER, PASSWORD);
                createTableIfNotExists(); // Add this line
                createAndShowGUI();
            } catch (SQLException | ClassNotFoundException e) {
                JOptionPane.showMessageDialog(null,
                        "Database connection error: " + e.getMessage(),
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                System.exit(1);
            }
        });
    }

    private static void createAndShowGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        frame = new JFrame("Library Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLayout(new BorderLayout(15, 15));
        frame.getContentPane().setBackground(new Color(240, 248, 255));

        // Header Panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 15));
        headerPanel.setBackground(new Color(51, 122, 183));
        headerPanel.setPreferredSize(new Dimension(1000, 80));
        JLabel titleLabel = new JLabel("Library Book Management System");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel);

        // Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 15, 15));
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(25, 30, 25, 30),
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true)
        ));
        inputPanel.setBackground(Color.WHITE);

        Font labelFont = new Font("Segoe UI", Font.BOLD, 14);
        Color labelColor = new Color(51, 122, 183);

        addInputField(inputPanel, "Book ID:", bookIdField = new JTextField(), labelFont, labelColor);
        addInputField(inputPanel, "Title:", titleField = new JTextField(), labelFont, labelColor);
        addInputField(inputPanel, "Author:", authorField = new JTextField(), labelFont, labelColor);
        addInputField(inputPanel, "Genre:", genreField = new JTextField(), labelFont, labelColor);
        addInputField(inputPanel, "Availability:", availabilityField = new JTextField(), labelFont, labelColor);

        // Button Panel
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        buttonPanel.setBackground(new Color(240, 248, 255));

        Color buttonColor = new Color(51, 122, 183);
        Font buttonFont = new Font("Segoe UI", Font.BOLD, 14);

        JButton[] buttons = {
                createStyledButton("Add Book", buttonColor, buttonFont, e -> addBook()),
                createStyledButton("View All Books", buttonColor, buttonFont, e -> viewBooks()),
                createStyledButton("Search Book", buttonColor, buttonFont, e -> searchBook()),
                createStyledButton("Update Book", buttonColor, buttonFont, e -> updateBook()),
                createStyledButton("Delete Book", new Color(178, 34, 34), buttonFont, e -> deleteBook()),
                createStyledButton("Clear Fields", new Color(60, 179, 113), buttonFont, e -> clearFields())
        };
        for (JButton button : buttons) {
            buttonPanel.add(button);
        }

        // Output Area
        outputPanel = new JPanel(new BorderLayout());
        outputArea = new JTextArea(10, 50);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        outputArea.setBackground(new Color(248, 248, 255));
        outputArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        outputScrollPane = new JScrollPane(outputArea);
        outputScrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        outputPanel.add(outputScrollPane, BorderLayout.CENTER);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(new Color(240, 248, 255));
        contentPanel.add(inputPanel, BorderLayout.NORTH);
        contentPanel.add(buttonPanel, BorderLayout.CENTER);

        frame.add(headerPanel, BorderLayout.NORTH);
        frame.add(contentPanel, BorderLayout.CENTER);
        frame.add(outputPanel, BorderLayout.SOUTH);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (conn != null && !conn.isClosed()) {
                        conn.close();
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void addInputField(JPanel panel, String labelText, JTextField field, Font font, Color color) {
        JLabel label = new JLabel(labelText, SwingConstants.RIGHT);
        label.setFont(font);
        label.setForeground(color);
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(label);
        panel.add(field);
    }

    private static JButton createStyledButton(String text, Color bgColor, Font font, ActionListener action) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(font);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(160, 45));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.addActionListener(action);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { button.setBackground(bgColor.darker()); }
            @Override
            public void mouseExited(MouseEvent e) { button.setBackground(bgColor); }
        });

        return button;
    }

    private static void addBook() {
        if (!validateRequiredFields(true)) return;

        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO books (bookId, title, author, genre, availability) VALUES (?, ?, ?, ?, ?)")) {
            String bookId = bookIdField.getText().trim();

            if (bookExists(bookId)) {
                showError("Book ID " + bookId + " already exists.");
                return;
            }

            stmt.setString(1, bookId);
            stmt.setString(2, titleField.getText().trim());
            stmt.setString(3, authorField.getText().trim());
            stmt.setString(4, genreField.getText().trim());
            stmt.setString(5, availabilityField.getText().isEmpty() ? "Available" : availabilityField.getText().trim());

            stmt.executeUpdate();
            showSuccessAnimation("Book added successfully");
            clearFields();
        } catch (SQLException e) {
            showError("Error adding book: " + e.getMessage());
        }
    }

    private static void viewBooks() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM books ORDER BY CAST(bookId AS SIGNED)");
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("%-10s %-30s %-25s %-15s %-15s\n", 
                         "Book ID", "Title", "Author", "Genre", "Availability"));
            result.append("-".repeat(95)).append("\n");
            
            boolean found = false;
            while (rs.next()) {
                found = true;
                result.append(String.format("%-10s %-30s %-25s %-15s %-15s\n",
                             rs.getString("bookId"),
                             rs.getString("title"),
                             rs.getString("author"),
                             rs.getString("genre"),
                             rs.getString("availability")));
            }
            
            if (!found) {
                result.append("No books found in the database.");
            }
            
            outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
            outputArea.setText(result.toString());
            
            rs.close();
            stmt.close();
         } catch (SQLException e) {
            showError("Error viewing books: " + e.getMessage());
         }
      }

    private static void searchBook() {
        String search = bookIdField.getText().trim().isEmpty() ? titleField.getText().trim() : bookIdField.getText().trim();
        if (search.isEmpty()) {
            showError("Please enter a Book ID or Title to search.");
            return;
        }

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM books WHERE bookId = ? OR title LIKE ?")) {
            stmt.setString(1, search);
            stmt.setString(2, "%" + search + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                displayResults(rs, "Search Results");
            }
        } catch (SQLException e) {
            showError("Error searching book: " + e.getMessage());
        }
    }

    private static void updateBook() {
        String bookId = bookIdField.getText().trim();
        if (bookId.isEmpty()) {
            showError("Book ID is required for updating.");
            return;
        }

        try {
            if (!bookExists(bookId)) {
                showError("Book with ID " + bookId + " not found.");
                return;
            }

            StringBuilder query = new StringBuilder("UPDATE books SET ");
            java.util.List<String> updates = new java.util.ArrayList<>();
            java.util.List<String> values = new java.util.ArrayList<>();

            addUpdateField(updates, values, "title", titleField.getText());
            addUpdateField(updates, values, "author", authorField.getText());
            addUpdateField(updates, values, "genre", genreField.getText());
            addUpdateField(updates, values, "availability", availabilityField.getText());

            if (updates.isEmpty()) {
                showError("Please provide at least one field to update.");
                return;
            }

            query.append(String.join(", ", updates)).append(" WHERE bookId = ?");
            try (PreparedStatement stmt = conn.prepareStatement(query.toString())) {
                for (int i = 0; i < values.size(); i++) {
                    stmt.setString(i + 1, values.get(i));
                }
                stmt.setString(values.size() + 1, bookId);

                stmt.executeUpdate();
                showSuccessAnimation("Book updated successfully");
                clearFields();
            }
        } catch (SQLException e) {
            showError("Error updating book: " + e.getMessage());
        }
    }

    private static void deleteBook() {
        String bookId = bookIdField.getText().trim();
        if (bookId.isEmpty()) {
            showError("Book ID is required for deletion.");
            return;
        }

        try {
            if (!bookExists(bookId)) {
                showError("Book with ID " + bookId + " not found.");
                return;
            }

            if (JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete this book?",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM books WHERE bookId = ?")) {
                stmt.setString(1, bookId);
                stmt.executeUpdate();
                showSuccessAnimation("Book deleted successfully");
                clearFields();
            }
        } catch (SQLException e) {
            showError("Error deleting book: " + e.getMessage());
        }
    }

    private static void clearFields() {
        bookIdField.setText("");
        titleField.setText("");
        authorField.setText("");
        genreField.setText("");
        availabilityField.setText("");
        resetOutputArea();
        bookIdField.requestFocus();
    }

    private static void addUpdateField(java.util.List<String> updates, java.util.List<String> values,
                                       String field, String value) {
        if (!value.trim().isEmpty()) {
            updates.add(field + " = ?");
            values.add(value.trim());
        }
    }

    private static boolean bookExists(String bookId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM books WHERE bookId = ?")) {
            stmt.setString(1, bookId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void displayResults(ResultSet rs, String title) throws SQLException {
        StringBuilder html = new StringBuilder("<html><body style='font-family: Segoe UI, Arial, sans-serif; background-color: #f8f9fa;'>" +
                "<h2 style='color: #2c3e50; text-align: center; font-size: 24px; margin: 20px 0;'>" + title + "</h2>" +
                "<div style='margin: 0 20px;'><table style='width: 100%; border-collapse: separate; border-spacing: 0; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>" +
                "<tr style='background-color: #337ab7; color: white;'>" +
                "<th style='padding: 12px 15px; text-align: left; font-weight: 600;'>Book ID</th>" +
                "<th style='padding: 12px 15px; text-align: left; font-weight: 600;'>Title</th>" +
                "<th style='padding: 12px 15px; text-align: left; font-weight: 600;'>Author</th>" +
                "<th style='padding: 12px 15px; text-align: left; font-weight: 600;'>Genre</th>" +
                "<th style='padding: 12px 15px; text-align: left; font-weight: 600;'>Availability</th></tr>");

        boolean alternate = false;
        boolean hasResults = false;

        while (rs.next()) {
            hasResults = true;
            String rowColor = alternate ? "#f8f9fa" : "white";
            alternate = !alternate;
            String availabilityColor = "Available".equalsIgnoreCase(rs.getString("availability")) ?
                    "#28a745" : "#dc3545";

            // Fixed the HTML string formatting - removed invalid JavaScript events and fixed quotes
            html.append(String.format(
                    "<tr style='background-color: %s;'>" +
                            "<td style='padding: 12px 15px; border-bottom: 1px solid #dee2e6;'>%s</td>" +
                            "<td style='padding: 12px 15px; border-bottom: 1px solid #dee2e6;'>%s</td>" +
                            "<td style='padding: 12px 15px; border-bottom: 1px solid #dee2e6;'>%s</td>" +
                            "<td style='padding: 12px 15px; border-bottom: 1px solid #dee2e6;'>%s</td>" +
                            "<td style='padding: 12px 15px; border-bottom: 1px solid #dee2e6;'><span style='color: %s; font-weight: 600; padding: 4px 8px; border-radius: 4px; background-color: %s20;'>%s</span></td></tr>",
                    rowColor, rs.getString("bookId"), rs.getString("title"),
                    rs.getString("author"), rs.getString("genre"),
                    availabilityColor, availabilityColor, rs.getString("availability")));
        }

        if (!hasResults) {
            html.append("<tr><td colspan='5' style='text-align:center; padding: 30px; color: #6c757d; font-style: italic;'>No books found.</td></tr>");
        }

        html.append("</table></div></body></html>");

        JEditorPane editor = new JEditorPane("text/html", html.toString());
        editor.setEditable(false);
        editor.setBackground(new Color(248, 248, 255));
        editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        outputPanel.removeAll();
        outputScrollPane = new JScrollPane(editor);
        outputScrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        outputPanel.add(outputScrollPane, BorderLayout.CENTER);
        outputPanel.revalidate();
        outputPanel.repaint();
    }

    private static void resetOutputArea() {
        outputPanel.removeAll();
        outputScrollPane = new JScrollPane(outputArea);
        outputScrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        outputPanel.add(outputScrollPane, BorderLayout.CENTER);
        if (closeButton != null) {
            buttonPanel.remove(closeButton);
            closeButton = null;
            buttonPanel.revalidate();
            buttonPanel.repaint();
        }
        outputPanel.revalidate();
        outputPanel.repaint();
        outputArea.setText("");
    }

    private static void showError(String message) {
        resetOutputArea();
        outputArea.setText(message);
    }

    private static void showSuccessAnimation(String message) {
        JDialog dialog = new JDialog(frame, false);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new JPanel() {
            private float alpha = 0f;
            private Timer timer = new Timer(16, null);
            private long startTime = System.currentTimeMillis();

            {
                setOpaque(false);
                setPreferredSize(new Dimension(400, 200));
                timer.addActionListener(e -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    if (elapsed < 500) {
                        alpha = elapsed / 500f;
                    } else if (elapsed < 1500) {
                        alpha = 1f;
                    } else if (elapsed < 2000) {
                        alpha = 1f - (elapsed - 1500) / 500f;
                    } else {
                        timer.stop();
                        dialog.dispose();
                        resetOutputArea();
                        outputArea.setText(message);
                        return;
                    }
                    repaint();
                });
                timer.start();
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

                int x = (getWidth() - 300) / 2;
                int y = (getHeight() - 100) / 2;

                g2d.setColor(new Color(0, 0, 0, 50));
                g2d.fillRoundRect(x + 5, y + 5, 300, 100, 20, 20);
                g2d.setColor(new Color(46, 125, 50));
                g2d.fillRoundRect(x, y, 300, 100, 20, 20);

                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(4));
                g2d.drawLine(x + 40, y + 50, x + 60, y + 70);
                g2d.drawLine(x + 60, y + 70, x + 100, y + 30);

                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                FontMetrics fm = g2d.getFontMetrics();
                int textX = x + (300 - fm.stringWidth(message)) / 2 + 15;
                g2d.drawString(message, textX, y + 60);
                g2d.dispose();
            }
        };

        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private static boolean validateRequiredFields(boolean isAddOperation) {
        if (!isAddOperation) return true;

        StringBuilder errors = new StringBuilder();
        if (bookIdField.getText().trim().isEmpty()) errors.append("Book ID is required.\n");
        if (titleField.getText().trim().isEmpty()) errors.append("Title is required.\n");
        if (authorField.getText().trim().isEmpty()) errors.append("Author is required.\n");

        if (errors.length() > 0) {
            showError("Validation Errors:\n" + errors);
            return false;
        }
        return true;
    }

    // Add this method after the main method
    private static void createTableIfNotExists() {
        try (Statement stmt = conn.createStatement()) {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS books (" +
                    "bookId VARCHAR(50) PRIMARY KEY," +
                    "title VARCHAR(255) NOT NULL," +
                    "author VARCHAR(255) NOT NULL," +
                    "genre VARCHAR(100)," +
                    "availability VARCHAR(50) DEFAULT 'Available'" +
                    ")";
            stmt.execute(createTableSQL);
        } catch (SQLException e) {
            showError("Error creating table: " + e.getMessage());
        }
    }
}