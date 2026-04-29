package view;

import controller.ClientController;
import controller.MedicineController;
import controller.OrderController;
import model.Client;
import model.Order;

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
    private final OrderController orderController;
    private final MedicineController medicineController;
    private DefaultTableModel ordersTableModel;

    public RegistratorFrame(Connection connection) {
        this.connection = connection;
        this.clientController = new ClientController(connection);
        this.orderController = new OrderController(connection);
        this.medicineController = new MedicineController(connection);

        setTitle("Аптека - Регистратор");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // вкладки
        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Больные клиенты", createClientsPanel());
        tabbedPane.addTab("Заказы", createOrdersPanel());
        tabbedPane.addTab("Создать заказ", createCreateOrderPanel());
        tabbedPane.addTab("Лекарства", createMedicinesPanel());
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
        ordersTableModel = new DefaultTableModel(new String[]{
                "ID заказа", "Дата создания", "Статус", "Время изготовления", "Цена", "Лекарство"
        }, 0);
        JTable table = new JTable(ordersTableModel);
        refreshOrdersTable(); // метод для загрузки данных

        // попка изменения статуса
        JButton statusButton = new JButton("Изменить статус");
        statusButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Выберите заказ");
                return;
            }

            int orderId = (int) ordersTableModel.getValueAt(selectedRow, 0);
            String currentStatus = (String) ordersTableModel.getValueAt(selectedRow, 2);
            changeOrderStatus(orderId, currentStatus);
        });

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(statusButton, BorderLayout.SOUTH);

        return panel;
    }

    // обновить таблицу заказа: при создании вкладки, при добавлении нового заказа
    private void refreshOrdersTable() {
        ordersTableModel.setRowCount(0);

        try {
            List<Order> orders = orderController.getAllOrders();
            for (Order o : orders) {
                ordersTableModel.addRow(new Object[]{o.getOrderId(), o.getCreationDate(), o.getStatus(),
                        o.getCompletionTime(), o.getPrice(), o.getMedicineName()});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки заказов: " + e.getMessage());
        }
    }

    private void changeOrderStatus(int orderId, String currentStatus) {
        String[] allowedStatuses = {"готов к выдаче", "выполнен"};

        if ("выполнен".equals(currentStatus)) {
            JOptionPane.showMessageDialog(this, "Нельзя изменить статус выполненного заказа");
            return;
        }

        // панель с двумя строками
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Изменять статус можно только у изготавливаемых лекарств"), gbc);

        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Новый статус:"), gbc);

        gbc.gridx = 1;
        JComboBox<String> combo = new JComboBox<>(allowedStatuses);
        panel.add(combo, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Изменение статуса",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String newStatus = (String) combo.getSelectedItem();
            assert newStatus != null;
            if (!newStatus.equals(currentStatus)) {
                try {
                    orderController.updateOrderStatus(orderId, newStatus);
                    refreshOrdersTable();
                    JOptionPane.showMessageDialog(this, "Статус изменен на " + newStatus);
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Ошибка обновления статуса: " + e.getMessage());
                }
            }
        }
    }

    private JPanel createCreateOrderPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // клиент
        gbc.gridx=0; gbc.gridy=0; panel.add(new JLabel("Клиент:"), gbc);
        gbc.gridx=1; JComboBox<String> clientCombo = new JComboBox<>(); panel.add(clientCombo, gbc);
        try {
            List<Client> clients = clientController.getAllClients();
            for (Client c : clients) {
                clientCombo.addItem(c.getClientId() + " - " + c.getFullName());
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка поиска больного клиента: " + e.getMessage());
        }

        // лекарство
        gbc.gridx=0; gbc.gridy=1; panel.add(new JLabel("Лекарство:"), gbc);
        gbc.gridx=1; JComboBox<String> medicineCombo = new JComboBox<>(); panel.add(medicineCombo, gbc);
        try {
            List<String> meds = medicineController.getAllMedicineNames();
            for (String m : meds) medicineCombo.addItem(m);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка поиска лекарства: " + e.getMessage());
        }

        // поля рецепта
        gbc.gridx=0; gbc.gridy=2; panel.add(new JLabel("Диагноз:"), gbc);
        gbc.gridx=1; JTextField diagnosisField = new JTextField(20); panel.add(diagnosisField, gbc);
        gbc.gridx=0; gbc.gridy=3; panel.add(new JLabel("Способ применения:"), gbc);
        gbc.gridx=1; JTextField usageField = new JTextField(20); panel.add(usageField, gbc);
        gbc.gridx=0; gbc.gridy=4; panel.add(new JLabel("Дата выписки (ГГГГ-ММ-ДД):"), gbc);
        gbc.gridx=1; JTextField dateField = new JTextField(10); panel.add(dateField, gbc);
        gbc.gridx=0; gbc.gridy=5; panel.add(new JLabel("Количество лекарства:"), gbc);
        gbc.gridx=1; JTextField quantityField = new JTextField(10); panel.add(quantityField, gbc);
        gbc.gridx=0; gbc.gridy=6; panel.add(new JLabel("ФИО врача:"), gbc);
        gbc.gridx=1; JTextField doctorField = new JTextField(20); panel.add(doctorField, gbc);
        gbc.gridx=0; gbc.gridy=7; panel.add(new JLabel("Подпись врача:"), gbc);
        gbc.gridx=1; JTextField signatureField = new JTextField(10); panel.add(signatureField, gbc);
        gbc.gridx=0; gbc.gridy=8; panel.add(new JLabel("Печать врача:"), gbc);
        gbc.gridx=1; JTextField stampField = new JTextField(10); panel.add(stampField, gbc);

        JButton createButton = new JButton("Создать заказ");
        gbc.gridx=0; gbc.gridy=9; gbc.gridwidth=2;
        panel.add(createButton, gbc);

        createButton.addActionListener(e -> {
            try {
                String selectedClient = (String) clientCombo.getSelectedItem();
                assert selectedClient != null;
                int clientId = Integer.parseInt(selectedClient.split(" - ")[0]);
                String medicineName = (String) medicineCombo.getSelectedItem();
                int medicineId = medicineController.getMedicineIdByName(medicineName);
                String diagnosis = diagnosisField.getText().trim();
                String usage = usageField.getText().trim();
                java.sql.Date issueDate = java.sql.Date.valueOf(dateField.getText().trim());
                int quantity = Integer.parseInt(quantityField.getText().trim());
                String doctorName = doctorField.getText().trim();
                String signature = signatureField.getText().trim();
                String stamp = stampField.getText().trim();
                int price = orderController.getMedicinePriceById(medicineId);

                int newOrderId = orderController.createOrder(clientId, medicineId, diagnosis,
                        usage, issueDate, quantity, doctorName, signature, stamp, price);
                refreshOrdersTable();
                JOptionPane.showMessageDialog(this, "Заказ создан!\nНомер заказа: " + newOrderId);

                // очистка полей
                diagnosisField.setText("");
                usageField.setText("");
                dateField.setText("");
                quantityField.setText("");
                doctorField.setText("");
                signatureField.setText("");
                stampField.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка: " + ex.getMessage());
            }
        });

        return panel;
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