package view;

import controller.ClientController;
import model.Client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;
import java.util.Vector;
import java.util.List;



public class RegistratorFrame extends JFrame {
    private final Connection connection;
    private final ClientController clientController;

    public RegistratorFrame(Connection connection) {
        this.connection = connection;
        this.clientController = new ClientController(connection);

        setTitle("Аптека – Регистратор");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // вкладки
        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Больные клиенты", createClientsPanel());
        tabbedPane.addTab("Лекарства", createMedicinesPanel());
        tabbedPane.addTab("Заказы", createOrdersPanel());
//        tabbedPane.addTab("Незабранные заказы", createViewPanel("v_unclaimed_orders"));
//        tabbedPane.addTab("Ожидающие компоненты", createViewPanel("v_waiting_customers"));
//        tabbedPane.addTab("Заказы в производстве", createViewPanel("v_orders_in_production"));
//        tabbedPane.addTab("Препараты для производства", createViewPanel(
//                "v_required_medicines_for_production"));

        add(tabbedPane);

        // закытие окна -> закрытие соединения
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (connection != null && !connection.isClosed()) {
                        connection.close();
                    }
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    // --------------------------------------------------------------------------- вкладка "Больные клиенты" (просмотр, создание)
    private JPanel createClientsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "ФИО", "Телефон", "Адрес"}, 0);
        JTable table = new JTable(model);
        refreshClientsTable(model);

        JButton addButton = new JButton("Добавить клиента");
        addButton.addActionListener(e -> showAddClientDialog(model));

        JPanel btnPanel = new JPanel();
        btnPanel.add(addButton);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshClientsTable(DefaultTableModel model) {
        model.setRowCount(0);
//        try (Statement st = connection.createStatement();
//             ResultSet rs = st.executeQuery("SELECT Client_id, ФИО, Телефон, Адрес FROM lab_drug_store.Больные_клиенты ORDER BY Client_id")) {
//            while (rs.next()) {
//                model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)});
//            }
//        } catch (SQLException e) {
//            JOptionPane.showMessageDialog(this, "Ошибка загрузки клиентов: " + e.getMessage());
//        }

        try {
            List<Client> clients = clientController.getAllClients();
            for (Client c : clients) {
                model.addRow(new Object[]{c.getClientId(), c.getFullName(), c.getPhone(), c.getAddress()});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки клиентов: " + e.getMessage());
        }
    }

    private void showAddClientDialog(DefaultTableModel model) {
        JTextField nameField = new JTextField("Сорокин Матвей Павлович", 20);
        JTextField phoneField = new JTextField(15);
        JTextField addressField = new JTextField(20);

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("ФИО:"));
        panel.add(nameField);
        panel.add(new JLabel("Телефон:"));
        panel.add(phoneField);
        panel.add(new JLabel("Адрес:"));
        panel.add(addressField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Новый клиент", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                // 1 - процедура add_client
//                CallableStatement cs = connection.prepareCall("CALL lab_drug_store.add_client(?, ?, ?)");
//                cs.setString(1, nameField.getText().trim());
//                cs.setString(2, phoneField.getText().trim());
//                cs.setString(3, addressField.getText().trim());
//                cs.execute();

                // 2 - запрос
//                String sql = "INSERT INTO lab_drug_store.Больные_клиенты (Client_id, ФИО, Телефон, Адрес) " +
//                        "VALUES ((SELECT COALESCE(MAX(Client_id), 0) + 1 FROM lab_drug_store.Больные_клиенты), ?, ?, ?)";
//                try (PreparedStatement ps = connection.prepareStatement(sql)) {
//                    ps.setString(1, nameField.getText().trim());
//                    ps.setString(2, phoneField.getText().trim());
//                    ps.setString(3, addressField.getText().trim());
//                    ps.executeUpdate();
//                }

                // 3 - вызов метода контроллера
                Client newClient = new Client(
                        0,
                        nameField.getText().trim(),
                        phoneField.getText().trim(),
                        addressField.getText().trim()
                );
                clientController.addClient(newClient);

                refreshClientsTable(model);
                JOptionPane.showMessageDialog(this, "Клиент добавлен");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            }
        }
    }

    // --------------------------------------------------------------------------- лекарства (просмотр)
    private JPanel createMedicinesPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Название", "Тип", "Способ применения", "Цена"}, 0);
        JTable table = new JTable(model);

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT Medicine_id, Название, Тип, Способ_применения, Цена FROM lab_drug_store.Лекарства")) {
                while (rs.next()) {
                    model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getBigDecimal(5)});
                }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки лекарств: " + e.getMessage());
        }
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }

    // --------------------------------------------------------------------------- заказы (просмотр, создание)
    private JPanel createOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // модель таблицы
        DefaultTableModel model = new DefaultTableModel(new String[]{
                "ID заказа", "Дата создания", "Статус", "Время изготовления", "Цена", "Лекарство"
        }, 0);
        JTable table = new JTable(model);
        refreshOrdersTable(model); // метод для загрузки данных

        // попка изменения статуса
        JButton statusButton = new JButton("Изменить статус");
        statusButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Выберите заказ");
                return;
            }
            int orderId = (int) model.getValueAt(selectedRow, 0);
            String currentStatus = (String) model.getValueAt(selectedRow, 2);
            changeOrderStatus(orderId, currentStatus, model);
        });

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(statusButton, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshOrdersTable(DefaultTableModel model) {
        model.setRowCount(0);
        String sql = "SELECT o.Order_id, o.Дата_создания, o.Статус, o.Время_изготовления, o.Цена, l.Название " +
                "FROM lab_drug_store.Заказы AS o " +
                "JOIN lab_drug_store.Лекарства AS l ON o.Medicine_id = l.Medicine_id " +
                "ORDER BY o.Order_id";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("Order_id"),
                        rs.getTimestamp("Дата_создания"),
                        rs.getString("Статус"),
                        rs.getTimestamp("Время_изготовления"),
                        rs.getBigDecimal("Цена"),
                        rs.getString("Название")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки заказов: " + e.getMessage());
        }
    }

    private void changeOrderStatus(int orderId, String currentStatus, DefaultTableModel model) {
        String[] allowedStatuses = {"готов к выдаче", "выполнен"};

        if ("выполнен".equals(currentStatus)) {
            JOptionPane.showMessageDialog(this, "Нельзя изменить статус выполненного заказа");
            return;
        }

        String newStatus = (String) JOptionPane.showInputDialog(this, "Выберите новый статус:",
                "Изменение статуса", JOptionPane.QUESTION_MESSAGE, null, allowedStatuses, allowedStatuses[0]);
        if (newStatus != null && !newStatus.equals(currentStatus)) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE lab_drug_store.Заказы SET Статус = ? WHERE Order_id = ?")) {
                ps.setString(1, newStatus);
                ps.setInt(2, orderId);

                ps.executeUpdate();
                refreshOrdersTable(model);
                JOptionPane.showMessageDialog(this, "Статус изменен на " + newStatus);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Ошибка обновления статуса: " + e.getMessage());
            }
        }
    }

    // --------------------------------------------------------------------------- представления (4 вкладки)
    private JPanel createViewPanel(String viewName) {
        JPanel panel = new JPanel(new BorderLayout());
        JTable table = new JTable();
        JScrollPane scroll = new JScrollPane(table);
        panel.add(scroll, BorderLayout.CENTER);

        // загрузка данных в отдельном потоке
        SwingUtilities.invokeLater(() -> refreshViewTable(table, viewName));
        return panel;
    }

    private void refreshViewTable(JTable table, String viewName) {
        try (Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM lab_drug_store." + viewName)) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                Vector<String> columnNames = new Vector<>();
                for (int i = 1; i <= columnCount; i++) {
                    columnNames.add(meta.getColumnName(i));
                }
                Vector<Vector<Object>> data = new Vector<>();
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.add(rs.getObject(i));
                    }
                    data.add(row);
                }
                table.setModel(new DefaultTableModel(data, columnNames));
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка загрузки представления " + viewName + ": " + e.getMessage());
        }
    }
}