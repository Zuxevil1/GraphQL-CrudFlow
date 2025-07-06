package com.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.*;
import com.google.gson.*;
import java.util.Objects;

public class ProductForm extends JFrame {
    private JTextField tfId = new JTextField();
    private JTextField tfName = new JTextField();
    private JTextField tfPrice = new JTextField();
    private JTextField tfCategory = new JTextField();

    private JTable productTable;
    private DefaultTableModel tableModel;

    public ProductForm() {
        setTitle("GraphQL Product Form");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(5, 2));
        inputPanel.add(new JLabel("ID (for Edit/Delete):"));
        inputPanel.add(tfId);
        inputPanel.add(new JLabel("Name:"));
        inputPanel.add(tfName);
        inputPanel.add(new JLabel("Price:"));
        inputPanel.add(tfPrice);
        inputPanel.add(new JLabel("Category:"));
        inputPanel.add(tfCategory);

        JButton btnAdd = new JButton("Add Product");
        JButton btnFetch = new JButton("Show All");
        JButton btnEdit = new JButton("Edit Product");
        JButton btnDelete = new JButton("Delete Product");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnFetch);

        // Table Panel
        tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Price", "Category"}, 0);
        productTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(productTable);

        // Optional: Klik tabel mengisi input form
        productTable.getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = productTable.getSelectedRow();
            if (selectedRow != -1) {
                tfId.setText(Objects.toString(tableModel.getValueAt(selectedRow, 0), ""));
                tfName.setText(Objects.toString(tableModel.getValueAt(selectedRow, 1), ""));
                tfPrice.setText(Objects.toString(tableModel.getValueAt(selectedRow, 2), ""));
                tfCategory.setText(Objects.toString(tableModel.getValueAt(selectedRow, 3), ""));
            }
        });

        add(inputPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
        add(tableScroll, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> tambahProduk());
        btnEdit.addActionListener(e -> editProduk());
        btnDelete.addActionListener(e -> hapusProduk());
        btnFetch.addActionListener(e -> ambilSemuaProduk());

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void tambahProduk() {
        try {
            String query = String.format(
                "mutation { addProduct(name: \"%s\", price: %s, category: \"%s\") { id name } }",
                tfName.getText(),
                tfPrice.getText(),
                tfCategory.getText()
            );
            String jsonRequest = new Gson().toJson(new GraphQLQuery(query));
            sendGraphQLRequest(jsonRequest);
            JOptionPane.showMessageDialog(this, "Product added");
            ambilSemuaProduk();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editProduk() {
        try {
            String query = String.format(
                "mutation { updateProduct(id: \"%s\", name: \"%s\", price: %s, category: \"%s\") { id name } }",
                tfId.getText(),
                tfName.getText(),
                tfPrice.getText(),
                tfCategory.getText()
            );
            String jsonRequest = new Gson().toJson(new GraphQLQuery(query));
            sendGraphQLRequest(jsonRequest);
            JOptionPane.showMessageDialog(this, "Product updated");
            ambilSemuaProduk();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void hapusProduk() {
        try {
            String query = String.format(
                "mutation { deleteProduct(id: \"%s\") { id name } }",
                tfId.getText()
            );
            String jsonRequest = new Gson().toJson(new GraphQLQuery(query));
            sendGraphQLRequest(jsonRequest);
            JOptionPane.showMessageDialog(this, "Product deleted");
            ambilSemuaProduk();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ambilSemuaProduk() {
        try {
            String query = "query { allProducts { id name price category } }";
            String jsonRequest = new Gson().toJson(new GraphQLQuery(query));
            String response = sendGraphQLRequest(jsonRequest);

            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
            JsonArray products = jsonObject.getAsJsonObject("data").getAsJsonArray("allProducts");

            tableModel.setRowCount(0); // clear table
            for (JsonElement elem : products) {
                JsonObject obj = elem.getAsJsonObject();
                String id = obj.get("id").getAsString();
                String name = obj.get("name").getAsString();
                double price = obj.get("price").getAsDouble();
                String category = obj.get("category").getAsString();
                tableModel.addRow(new Object[]{id, name, price, category});
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String sendGraphQLRequest(String json) throws Exception {
        URL url = new URL("http://localhost:4567/graphql");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes());
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                sb.append(line).append("\n");
            return sb.toString();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ProductForm::new);
    }

    class GraphQLQuery {
        String query;

        GraphQLQuery(String query) {
            this.query = query;
        }
    }
}
