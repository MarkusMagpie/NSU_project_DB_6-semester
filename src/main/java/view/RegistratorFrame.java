package view;

import controller.ClientController;
import controller.MedicineController;
import controller.OrderController;
import model.Client;
import model.Order;
import utils.DatabaseConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;
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
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);

        JMenuBar menuBar = new JMenuBar();
        JMenu systemMenu = new JMenu("Система");
        JMenuItem reconnectItem = new JMenuItem("Сменить пользователя");
        reconnectItem.addActionListener(e -> reLogin());
        systemMenu.add(reconnectItem);
        JMenuItem checkConnectionItem = new JMenuItem("Проверить соединение");
        checkConnectionItem.addActionListener(e -> checkConnection());
        systemMenu.add(checkConnectionItem);

        menuBar.add(systemMenu);
        setJMenuBar(menuBar);

        // вкладки для управления
        JTabbedPane managementPane = new JTabbedPane();
        managementPane.addTab("Больные клиенты", createClientsPanel());
        managementPane.addTab("Заказы", createOrdersPanel());
        managementPane.addTab("Создать заказ", createCreateOrderPanel());
        managementPane.addTab("Лекарства", createMedicinesPanel());
        managementPane.addTab("Состав изготавливаемых лекарств", createCompositionPanel());
        // вкладки - представления
        JTabbedPane viewsPane = new JTabbedPane();
        viewsPane.addTab("Незабранные заказы", createUnclaimedOrdersPanel());
        viewsPane.addTab("Ожидающие компоненты", createWaitingCustomersPanel());
        viewsPane.addTab("Заказы в производстве", createOrdersInProductionPanel());
        viewsPane.addTab("Препараты для производства", createRequiredMedicinesPanel());

        JPanel mainPanel = new JPanel(new GridLayout(2, 1));
        mainPanel.add(managementPane);
        mainPanel.add(viewsPane);

        add(mainPanel);

        for (int i = 0; i < viewsPane.getTabCount(); i++) {
            viewsPane.setBackgroundAt(i, new Color(165, 165, 165));
        }

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, managementPane, viewsPane);
        splitPane.setResizeWeight(0.5);
        add(splitPane);

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
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "ФИО", "Телефон", "Адрес"}, 0)
        {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
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

        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Название", "Тип", "Способ применения", "Цена"}, 0)
        {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
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

    // --------------------------------------------------------------------------- Состав лекарств (просмотр)
    private JPanel createCompositionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel(new String[]{"Компонент", "Изготавливаемое лекарство",
                "Количество (ед.)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(model);

        // загрузка данных о составе лекарств
        String sql =
                "SELECT c.Name AS component_name, l.Название AS medicine_name, r.Количество AS quantity " +
                        "FROM lab_drug_store.Изготавливаемые_лекарства as im " +
                        "JOIN lab_drug_store.Технологические_карты as t ON im.Medicine_id = t.Medicine_id " +
                        "JOIN lab_drug_store.Рецептуры as r ON t.Technology_id = r.Технологическая_карта " +

                        "JOIN lab_drug_store.Компоненты as c ON r.Компоненты = c.Component_id " +

                        "JOIN lab_drug_store.Лекарства as l ON im.Medicine_id = l.Medicine_id " +
                        "ORDER BY l.Название, c.Name";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("component_name"),
                        rs.getString("medicine_name"),
                        rs.getBigDecimal("quantity")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки состава лекарств: " + e.getMessage());
        }

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }

    // --------------------------------------------------------------------------- заказы (просмотр, создание)
    private JPanel createOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        ordersTableModel = new DefaultTableModel(new String[]{
                "ID заказа", "Дата создания", "Статус", "Время изготовления", "Цена", "Лекарство"
        }, 0)
        {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(ordersTableModel);
        refreshOrdersTable(); // метод для загрузки данных

        JButton statusButton = new JButton("Изменить статус");
        statusButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Перед изменением статуса заказа нужно его выбрать");
                return;
            }

            int orderId = (int) ordersTableModel.getValueAt(selectedRow, 0);
            String currentStatus = (String) ordersTableModel.getValueAt(selectedRow, 2);
            changeOrderStatus(orderId, currentStatus);
        });

        JPanel btnPanel = new JPanel();
        btnPanel.add(statusButton);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

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
            if (newStatus == null) return;
            if (newStatus.equals(currentStatus)) {
                JOptionPane.showMessageDialog(this, "Вы выбрали текущий статус");
                return;
            }
            try {
                orderController.updateOrderStatus(orderId, newStatus);
                refreshOrdersTable();
                JOptionPane.showMessageDialog(this, "Статус изменен на " + newStatus);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Ошибка обновления статуса: " + e.getMessage());
            }
        }
    }

    // вкладка "Создать заказ"
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
        gbc.gridx=0; gbc.gridy=5;
        panel.add(new JLabel("Количество лекарства:"), gbc);
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
                int price = orderController.getMedicinePriceById(medicineId) * quantity;

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

    // --------------------------------------------------------------------------- представления
    private JPanel createUnclaimedOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JLabel text = new JLabel("Сведения о покупателях, не забравших заказ (просрочка более 1 часа).");
        text.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(text);

        // панель фильтров
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField nameSearchField = new JTextField(15);
        JTextField overdueHoursField = new JTextField(3);
        JComboBox<String> sortCombo = new JComboBox<>(new String[]{
                "по времени готовности (возр.)",
                "по времени готовности (убыв.)",
                "по просрочке (возр.)",
                "по просрочке (убыв.)",
                "по клиенту (А-Я)",
                "по клиенту (Я-А)"
        });
        JButton refreshButton = new JButton("Обновить");

        filterPanel.add(new JLabel("ФИО клиента:"));
        filterPanel.add(nameSearchField);
        filterPanel.add(new JLabel("Просрочка > часов:"));
        filterPanel.add(overdueHoursField);
        filterPanel.add(new JLabel("Сортировка:"));
        filterPanel.add(sortCombo);
        filterPanel.add(refreshButton);

        topPanel.add(filterPanel);
        panel.add(topPanel, BorderLayout.NORTH);

        // таблица
        DefaultTableModel model = new DefaultTableModel(new String[]{
                "ID клиента", "ФИО", "Телефон", "Адрес", "ID заказа", "Время готовности", "Просрочка (интервал)"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        Runnable loadData = () -> {
            String nameSearch = nameSearchField.getText().trim();
            String overdueHoursStr = overdueHoursField.getText().trim();
            String sort = (String) sortCombo.getSelectedItem();
            refreshUnclaimedOrdersTable(model, nameSearch, overdueHoursStr, sort);
        };

        refreshButton.addActionListener(e -> loadData.run());
        SwingUtilities.invokeLater(loadData);

        return panel;
    }

    private void refreshUnclaimedOrdersTable(DefaultTableModel model, String nameSearch, String overdueHoursStr, String sortOption) {
        model.setRowCount(0);
        StringBuilder sql = new StringBuilder(
                "SELECT client_id, full_name, phone, address, order_id, completion_time, overdue_interval " +
                        "FROM lab_drug_store.v_unclaimed_orders"
        );

        boolean hasWhere = false;
        if (!nameSearch.isEmpty()) {
            sql.append(" WHERE full_name ILIKE ?");
            hasWhere = true;
        }
        if (!overdueHoursStr.isEmpty()) {
            try {
                if (hasWhere)
                    sql.append(" AND");
                else
                    sql.append(" WHERE");
                sql.append(" EXTRACT(EPOCH FROM overdue_interval)/3600 > ?");
            } catch (NumberFormatException ignored) {}
        }

        sql.append(switch (sortOption) {
            case "по времени готовности (возр.)" -> " ORDER BY completion_time ASC";
            case "по времени готовности (убыв.)" -> " ORDER BY completion_time DESC";
            case "по просрочке (возр.)" -> " ORDER BY overdue_interval ASC";
            case "по просрочке (убыв.)" -> " ORDER BY overdue_interval DESC";
            case "по клиенту (А-Я)" -> " ORDER BY full_name ASC";
            case "по клиенту (Я-А)" -> " ORDER BY full_name DESC";
            default -> " ORDER BY completion_time ASC";
        });

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (!nameSearch.isEmpty()) {
                ps.setString(paramIndex++, "%" + nameSearch + "%");
            }
            if (!overdueHoursStr.isEmpty()) {
                try {
                    int hours = Integer.parseInt(overdueHoursStr);
                    ps.setDouble(paramIndex++, (double) hours);
                } catch (NumberFormatException ignored) {}
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("client_id"),
                        rs.getString("full_name"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getInt("order_id"),
                        rs.getTimestamp("completion_time"),
                        rs.getObject("overdue_interval") // Interval тип
                });
            }
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                int option = JOptionPane.showConfirmDialog(this,
                        "Соединение потеряно. Переподключиться?",
                        "Ошибка", JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION) reLogin();
            } else {
                JOptionPane.showMessageDialog(this, "Ошибка загрузки данных: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }



    private JPanel createWaitingCustomersPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JLabel text = new JLabel("Покупатели, ожидающие прибытия медикаментов на склад");
        text.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(text);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField nameSearchField = new JTextField(15);
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Все типы", "готовое", "изготавливаемое"});
        JComboBox<String> sortCombo = new JComboBox<>(new String[]{
                "по клиенту (А-Я)",
                "по клиенту (Я-А)",
                "по лекарству (А-Я)",
                "по лекарству (Я-А)"
        });
        JButton refreshButton = new JButton("Обновить");

        filterPanel.add(new JLabel("ФИО клиента:"));
        filterPanel.add(nameSearchField);
        filterPanel.add(new JLabel("Тип лекарства:"));
        filterPanel.add(typeCombo);
        filterPanel.add(new JLabel("Сортировка:"));
        filterPanel.add(sortCombo);
        filterPanel.add(refreshButton);

        topPanel.add(filterPanel);
        panel.add(topPanel, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(new String[]{
                "ID клиента", "ФИО", "Телефон", "Адрес", "ID заказа", "Лекарство", "Тип"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        Runnable loadData = () -> {
            String nameSearch = nameSearchField.getText().trim();
            String type = (String) typeCombo.getSelectedItem();
            String sort = (String) sortCombo.getSelectedItem();
            refreshWaitingCustomersTable(model, nameSearch, type, sort);
        };

        refreshButton.addActionListener(e -> loadData.run());
        SwingUtilities.invokeLater(loadData);

        return panel;
    }

    private void refreshWaitingCustomersTable(DefaultTableModel model, String nameSearch, String type, String sortOption) {
        model.setRowCount(0);
        StringBuilder sql = new StringBuilder(
                "SELECT client_id, full_name, phone, address, order_id, medicine_name, medicine_type " +
                        "FROM lab_drug_store.v_waiting_customers"
        );

        boolean hasWhere = false;
        if (!nameSearch.isEmpty()) {
            sql.append(" WHERE full_name ILIKE ?");
            hasWhere = true;
        }
        if (!"Все типы".equals(type)) {
            if (hasWhere) sql.append(" AND");
            else sql.append(" WHERE");
            sql.append(" medicine_type = ?");
        }

        sql.append(switch (sortOption) {
            case "по клиенту (А-Я)" -> " ORDER BY full_name ASC";
            case "по клиенту (Я-А)" -> " ORDER BY full_name DESC";
            case "по лекарству (А-Я)" -> " ORDER BY medicine_name ASC";
            case "по лекарству (Я-А)" -> " ORDER BY medicine_name DESC";
            default -> " ORDER BY full_name ASC";
        });

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (!nameSearch.isEmpty()) {
                ps.setString(paramIndex++, "%" + nameSearch + "%");
            }
            if (!"Все типы".equals(type)) {
                ps.setString(paramIndex++, "готовое".equals(type) ? "готовое" : "изготавливаемое");
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("client_id"),
                        rs.getString("full_name"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getInt("order_id"),
                        rs.getString("medicine_name"),
                        rs.getString("medicine_type")
                });
            }
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                int option = JOptionPane.showConfirmDialog(this,
                        "Соединение потеряно. Переподключиться?",
                        "Ошибка", JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION) reLogin();
            } else {
                JOptionPane.showMessageDialog(this, "Ошибка загрузки данных: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }



    private JPanel createOrdersInProductionPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JLabel text = new JLabel("Заказы, находящиеся в производстве");
        text.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(text);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField medicineSearchField = new JTextField(15);
        JTextField customerSearchField = new JTextField(15);
        JComboBox<String> sortCombo = new JComboBox<>(new String[]{
                "по дате создания (возр.)",
                "по дате создания (убыв.)",
                "по лекарству (А-Я)",
                "по лекарству (Я-А)",
                "по клиенту (А-Я)",
                "по клиенту (Я-А)"
        });
        JButton refreshButton = new JButton("Обновить");

        filterPanel.add(new JLabel("Лекарство:"));
        filterPanel.add(medicineSearchField);
        filterPanel.add(new JLabel("Клиент:"));
        filterPanel.add(customerSearchField);
        filterPanel.add(new JLabel("Сортировка:"));
        filterPanel.add(sortCombo);
        filterPanel.add(refreshButton);

        topPanel.add(filterPanel);
        panel.add(topPanel, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(new String[]{
                "ID заказа", "Статус", "Дата создания", "Лекарство", "Клиент"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        Runnable loadData = () -> {
            String medicine = medicineSearchField.getText().trim();
            String customer = customerSearchField.getText().trim();
            String sort = (String) sortCombo.getSelectedItem();
            refreshOrdersInProductionTable(model, medicine, customer, sort);
        };

        refreshButton.addActionListener(e -> loadData.run());
        SwingUtilities.invokeLater(loadData);

        return panel;
    }

    private void refreshOrdersInProductionTable(DefaultTableModel model, String medicine, String customer, String sortOption) {
        model.setRowCount(0);
        StringBuilder sql = new StringBuilder(
                "SELECT order_id, status, creation_date, medicine_name, customer_name " +
                        "FROM lab_drug_store.v_orders_in_production"
        );

        boolean hasWhere = false;
        if (!medicine.isEmpty()) {
            sql.append(" WHERE medicine_name ILIKE ?");
            hasWhere = true;
        }
        if (!customer.isEmpty()) {
            if (hasWhere) sql.append(" AND");
            else sql.append(" WHERE");
            sql.append(" customer_name ILIKE ?");
        }

        sql.append(switch (sortOption) {
            case "по дате создания (возр.)" -> " ORDER BY creation_date ASC";
            case "по дате создания (убыв.)" -> " ORDER BY creation_date DESC";
            case "по лекарству (А-Я)" -> " ORDER BY medicine_name ASC";
            case "по лекарству (Я-А)" -> " ORDER BY medicine_name DESC";
            case "по клиенту (А-Я)" -> " ORDER BY customer_name ASC";
            case "по клиенту (Я-А)" -> " ORDER BY customer_name DESC";
            default -> " ORDER BY creation_date ASC";
        });

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (!medicine.isEmpty()) {
                ps.setString(paramIndex++, "%" + medicine + "%");
            }
            if (!customer.isEmpty()) {
                ps.setString(paramIndex++, "%" + customer + "%");
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("order_id"),
                        rs.getString("status"),
                        rs.getTimestamp("creation_date"),
                        rs.getString("medicine_name"),
                        rs.getString("customer_name")
                });
            }
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                int option = JOptionPane.showConfirmDialog(this,
                        "Соединение потеряно. Переподключиться?",
                        "Ошибка", JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION) reLogin();
            } else {
                JOptionPane.showMessageDialog(this, "Ошибка загрузки данных: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }



    private JPanel createRequiredMedicinesPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JLabel text = new JLabel("Препараты, требующиеся для заказов, находящихся в производстве");
        text.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(text);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField orderIdField = new JTextField(10);
        JTextField medicineSearchField = new JTextField(15);
        JComboBox<String> sortCombo = new JComboBox<>(new String[]{
                "по ID заказа (возр.)",
                "по ID заказа (убыв.)",
                "по лекарству (А-Я)",
                "по лекарству (Я-А)",
                "по количеству (возр.)",
                "по количеству (убыв.)"
        });
        JButton refreshButton = new JButton("Обновить");

        filterPanel.add(new JLabel("ID заказа:"));
        filterPanel.add(orderIdField);
        filterPanel.add(new JLabel("Лекарство:"));
        filterPanel.add(medicineSearchField);
        filterPanel.add(new JLabel("Сортировка:"));
        filterPanel.add(sortCombo);
        filterPanel.add(refreshButton);

        topPanel.add(filterPanel);
        panel.add(topPanel, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(new String[]{
                "ID заказа", "Лекарство", "Требуемое количество (ед.)"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        Runnable loadData = () -> {
            String orderIdStr = orderIdField.getText().trim();
            String medicine = medicineSearchField.getText().trim();
            String sort = (String) sortCombo.getSelectedItem();
            refreshRequiredMedicinesTable(model, orderIdStr, medicine, sort);
        };

        refreshButton.addActionListener(e -> loadData.run());
        SwingUtilities.invokeLater(loadData);

        return panel;
    }

    private void refreshRequiredMedicinesTable(DefaultTableModel model, String orderIdStr, String medicine, String sortOption) {
        model.setRowCount(0);
        StringBuilder sql = new StringBuilder(
                "SELECT order_id, medicine_name, required_quantity FROM lab_drug_store.v_required_medicines_for_production"
        );

        boolean hasWhere = false;
        if (!orderIdStr.isEmpty()) {
            try {
                sql.append(" WHERE order_id = ?");
                hasWhere = true;
            } catch (NumberFormatException ignored) {}
        }
        if (!medicine.isEmpty()) {
            if (hasWhere)
                sql.append(" AND");
            else
                sql.append(" WHERE");
            sql.append(" medicine_name ILIKE ?");
        }

        sql.append(switch (sortOption) {
            case "по ID заказа (возр.)" -> " ORDER BY order_id ASC";
            case "по ID заказа (убыв.)" -> " ORDER BY order_id DESC";
            case "по лекарству (А-Я)" -> " ORDER BY medicine_name ASC";
            case "по лекарству (Я-А)" -> " ORDER BY medicine_name DESC";
            case "по количеству (возр.)" -> " ORDER BY required_quantity ASC";
            case "по количеству (убыв.)" -> " ORDER BY required_quantity DESC";
            default -> " ORDER BY order_id ASC";
        });

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (!orderIdStr.isEmpty()) {
                try {
                    int orderId = Integer.parseInt(orderIdStr);
                    ps.setInt(paramIndex++, orderId);
                } catch (NumberFormatException ignored) {}
            }

            if (!medicine.isEmpty()) {
                ps.setString(paramIndex++, "%" + medicine + "%");
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("order_id"),
                        rs.getString("medicine_name"),
                        rs.getBigDecimal("required_quantity")
                });
            }
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                int option = JOptionPane.showConfirmDialog(this,
                        "Соединение потеряно. Переподключиться?",
                        "Ошибка", JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION) reLogin();
            } else {
                JOptionPane.showMessageDialog(this, "Ошибка загрузки данных: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    // --------------------------------------------------------------------------- соединение с бд
    private void reLogin() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        new LoginDialog(null).setVisible(true);

        dispose();
    }

    private void checkConnection() {
        boolean alive = DatabaseConnection.isConnectionAlive(connection);
        String msg = alive ? "Соединение с БД активно" : "Соединение с БД потеряно";
        System.out.println(msg);
        JOptionPane.showMessageDialog(this, msg, "Статус соединения",
                alive ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);

        if (!alive) {
            int option = JOptionPane.showConfirmDialog(this,
                    "Соединение потеряно. Переподключиться?",
                    "Ошибка", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) reLogin();
        }
    }
}