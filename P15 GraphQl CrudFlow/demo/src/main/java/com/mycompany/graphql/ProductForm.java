package com.mycompany.graphql;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import com.google.gson.*;
import javax.swing.table.DefaultTableModel;

public class ProductForm extends JFrame {
    private JTextField tfName = new JTextField();
    private JTextField tfPrice = new JTextField();
    private JTextField tfCategory = new JTextField();
    private JTable table;
    private DefaultTableModel tableModel;

    public ProductForm() {
        setTitle("GraphQL Product Form");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(5, 2));

        inputPanel.add(new JLabel("Name:"));
        inputPanel.add(tfName);
        inputPanel.add(new JLabel("Price:"));
        inputPanel.add(tfPrice);
        inputPanel.add(new JLabel("Category:"));
        inputPanel.add(tfCategory);

        JButton btnAdd = new JButton("Add Product");
        JButton btnFetch = new JButton("Show All");
        JButton btnEdit = new JButton("Edit");
        JButton btnDelete = new JButton("Delete");

        inputPanel.add(btnAdd);
        inputPanel.add(btnFetch);
        inputPanel.add(btnEdit);
        inputPanel.add(btnDelete);

        add(inputPanel, BorderLayout.NORTH);

        String[] columnNames = {"ID", "Name", "Price", "Category"};
        tableModel = new DefaultTableModel(columnNames, 0);
        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        btnAdd.addActionListener(e -> tambahProduk());
        btnFetch.addActionListener(e -> ambilSemuaProduk());
        btnEdit.addActionListener(e -> editProduk());
        btnDelete.addActionListener(e -> hapusProduk());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow != -1) {
                    tfName.setText(tableModel.getValueAt(selectedRow, 1).toString());
                    tfPrice.setText(tableModel.getValueAt(selectedRow, 2).toString());
                    tfCategory.setText(tableModel.getValueAt(selectedRow, 3).toString());
                } else {
                    clearInputFields();
                }
            }
        });

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        ambilSemuaProduk();
    }

    private void tambahProduk() {
        try {
            String query = String.format(
                "mutation { addProduct(name: \"%s\", price: %s, category: \"%s\") { id name price category } }",
                tfName.getText(),
                tfPrice.getText(),
                tfCategory.getText()
            );
            String jsonRequest = new Gson().toJson(new GraphQLQuery(query));
            String response = sendGraphQLRequest(jsonRequest);
            System.out.println("Product added!\n\n" + response);

            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
            JsonElement dataElement = jsonObject.get("data");

            if (dataElement == null || dataElement.isJsonNull()) {
                System.out.println("No data received after adding product.");
                return;
            }

            JsonObject addedProduct = dataElement.getAsJsonObject().getAsJsonObject("addProduct");

            if (addedProduct != null) {
                tableModel.setRowCount(0);

                Long id = addedProduct.get("id").getAsLong();
                String name = addedProduct.get("name").getAsString();
                double price = addedProduct.get("price").getAsDouble();
                String category = addedProduct.get("category").getAsString();
                tableModel.addRow(new Object[]{id, name, price, category});
            }


        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        clearInputFields();
    }

    private void ambilSemuaProduk() {
        try {
            String query = "query { allProducts { id name price category } }";
            String jsonRequest = new Gson().toJson(new GraphQLQuery(query));
            String response = sendGraphQLRequest(jsonRequest);

            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
            JsonElement dataElement = jsonObject.get("data");

            if (dataElement == null || dataElement.isJsonNull()) {
                System.out.println("No data received from GraphQL.");
                tableModel.setRowCount(0);
                return;
            }

            JsonArray products = dataElement.getAsJsonObject().getAsJsonArray("allProducts");

            tableModel.setRowCount(0);

            if (products != null) {
                for (JsonElement element : products) {
                    JsonObject product = element.getAsJsonObject();
                    Object id = null;
                    if (product.has("id")) {
                        try {
                            id = product.get("id").getAsLong();
                        } catch (NumberFormatException e) {
                        }
                    }
                    String name = product.has("name") ? product.get("name").getAsString() : "";
                    double price = product.has("price") ? product.get("price").getAsDouble() : 0.0;
                    String category = product.has("category") ? product.get("category").getAsString() : "";
                    tableModel.addRow(new Object[]{id, name, price, category});
                }
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void editProduk() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih produk yang ingin anda edit.", "Error", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Object idObject = tableModel.getValueAt(selectedRow, 0);
        Long id = null;
        if (idObject instanceof Integer) {
            id = ((Integer) idObject).longValue();
        } else if (idObject instanceof Long) {
            id = (Long) idObject;
        } else {
            JOptionPane.showMessageDialog(this, "Error: ID bukan angka valid..", "Data Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String name = tfName.getText();
        Double price = Double.parseDouble(tfPrice.getText());
        String category = tfCategory.getText();

        try {
            if (name.isEmpty() || category.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Isi semua field!..", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (price <= 0) {
                JOptionPane.showMessageDialog(this, "Bukan sedekah! harga harus diatas 0..", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String query = String.format(
                "mutation { updateProduct(id: %d, name: \"%s\", price: %s, category: \"%s\") { id name price category } }",
                id, name, price, category
            );
            String jsonRequest = new Gson().toJson(new GraphQLQuery(query));
            String response = sendGraphQLRequest(jsonRequest);
            System.out.println("Product diedit!\n\n" + response);
            ambilSemuaProduk();
            clearInputFields();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error edit produk: " + e.getMessage(), "GraphQL Error", JOptionPane.ERROR_MESSAGE);
        }
        ambilSemuaProduk();
    }

    private void hapusProduk() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih produk yang ingin dihapus..");
            return;
        }

        Object idObject = tableModel.getValueAt(selectedRow, 0);
        Long idToDelete = null;
        if (idObject instanceof Integer) {
            idToDelete = ((Integer) idObject).longValue();
        } else if (idObject instanceof Long) {
            idToDelete = (Long) idObject;
        } else {
            JOptionPane.showMessageDialog(this, "Error: ID bukan angka valid..");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Yakin ingin menghapus produk ber-ID: " + idToDelete + "?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String query = String.format(
                    "mutation { deleteProduct(id: %d) }",
                    idToDelete
                );
                String jsonRequest = new Gson().toJson(new GraphQLQuery(query));
                String response = sendGraphQLRequest(jsonRequest);
                System.out.println("Produk dihapus!\n\n" + response);
                ambilSemuaProduk();
                clearInputFields();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error hapus produk: " + e.getMessage(), "GraphQL Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        ambilSemuaProduk();
    }

    private void clearInputFields() {
        tfName.setText("");
        tfPrice.setText("");
        tfCategory.setText("");
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
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
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