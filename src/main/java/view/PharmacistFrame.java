package view;

import controller.OrderController;
import model.Order;
import utils.DatabaseConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;
import java.util.List;



public class PharmacistFrame extends JFrame {
    private DefaultTableModel ordersTableModel;
    private Connection connection;
    private final OrderController orderController;

    public PharmacistFrame(Connection connection) {
        this.connection = connection;
        this.orderController = new OrderController(connection);

        setTitle("Аптека - Фармацевт");
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 600);
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

        // вкладки
        JTabbedPane managementPane = new JTabbedPane();
        managementPane.addTab("Заказы", createOrdersPanel());

        JTabbedPane viewsPane = new JTabbedPane();
        viewsPane.addTab("Технологии приготовления лекарств", createTechnologiesPanel());
        viewsPane.addTab("Заказы в производстве", createOrdersInProductionPanel());
        viewsPane.addTab("Препараты для производства", createRequiredMedicinesPanel());

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, managementPane, viewsPane);
        splitPane.setResizeWeight(0.6);
        add(splitPane);

        for (int i = 0; i < viewsPane.getTabCount(); i++) {
            viewsPane.setBackgroundAt(i, new Color(165, 165, 165));
        }

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

    // --------------------------------------------------------------------------- панель заказов
    private JPanel createOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        ordersTableModel = new DefaultTableModel(new String[]{
                "ID заказа", "Дата создания", "Статус", "Время изготовления", "Цена", "Лекарство"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(ordersTableModel);
        refreshOrdersTable();

        JButton changeStatusButton = new JButton("Изменить статус");
        changeStatusButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Выберите заказ");
                return;
            }
            int orderId = (int) ordersTableModel.getValueAt(selectedRow, 0);
            String currentStatus = (String) ordersTableModel.getValueAt(selectedRow, 2);
            changeOrderStatus(orderId, currentStatus);
        });

        JPanel btnPanel = new JPanel();
        btnPanel.add(changeStatusButton);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshOrdersTable() {
        ordersTableModel.setRowCount(0);
        try {
            List<Order> orders = orderController.getAllOrders();
            for (Order o : orders) {
                ordersTableModel.addRow(new Object[]{
                        o.getOrderId(),
                        o.getCreationDate(),
                        o.getStatus(),
                        o.getCompletionTime(),
                        o.getPrice(),
                        o.getMedicineName()
                });
            }
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                int option = JOptionPane.showConfirmDialog(this,
                        "Соединение с базой данных потеряно. Переподключиться?",
                        "Ошибка соединения", JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION) reLogin();
            } else {
                JOptionPane.showMessageDialog(this, "Ошибка обновления статусов заказов: " + e.getMessage());
            }
        }
    }

    private void changeOrderStatus(int orderId, String currentStatus) {
        // фармацевт может перевести заказ из "готов к производству" в "в производстве"; из "в производстве" в "готов к выдаче"
        String[] allowedStatuses;
        if ("готов к производству".equals(currentStatus)) {
            allowedStatuses = new String[]{"в производстве"};
        } else if ("в производстве".equals(currentStatus)) {
            allowedStatuses = new String[]{"готов к выдаче"};
        } else {
            JOptionPane.showMessageDialog(this, "Для выбранного заказа нельзя изменить статус (разрешены только 'готов к производству' -> 'в производстве' -> 'готов к выдаче')");
            return;
        }
        String newStatus = (String) JOptionPane.showInputDialog(this, "Выберите новый статус:",
                "Изменение статуса", JOptionPane.QUESTION_MESSAGE, null, allowedStatuses, allowedStatuses[0]);
        if (newStatus != null && !newStatus.equals(currentStatus)) {
            try {
                orderController.updateOrderStatus(orderId, newStatus);
                refreshOrdersTable();
                JOptionPane.showMessageDialog(this, "Статус изменён на " + newStatus);
            } catch (SQLException e) {
                if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                    int option = JOptionPane.showConfirmDialog(this,
                            "Соединение с базой данных потеряно. Переподключиться?",
                            "Ошибка соединения", JOptionPane.YES_NO_OPTION);
                    if (option == JOptionPane.YES_OPTION) reLogin();
                } else {
                    JOptionPane.showMessageDialog(this, "Ошибка обновления статуса заказа: " + e.getMessage());
                }
            }
        }
    }

    // --------------------------------------------------------------------------- панели представлений
    private JPanel createTechnologiesPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

//        JLabel descriptionLabel = new JLabel(
//                "<html>Технологии приготовления лекарств указанных типов, конкретных лекарств,<br>" +
//                        "лекарств, находящихся в заказах в производстве.</html>"
//        );
//        descriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
//        topPanel.add(descriptionLabel);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField searchField = new JTextField(15);
        JComboBox<String> sortCombo = new JComboBox<>(new String[]{
                "по лекарству (А-Я)",
                "по лекарству (Я-А)",
                "по технологии (А-Я)",
                "по технологии (Я-А)"
        });
        JButton refreshButton = new JButton("Обновить");

        filterPanel.add(new JLabel("Поиск (название лекарства):"));
        filterPanel.add(searchField);
        filterPanel.add(new JLabel("Сортировка:"));
        filterPanel.add(sortCombo);
        filterPanel.add(refreshButton);

        topPanel.add(filterPanel);
        panel.add(topPanel, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(new String[]{
                "ID технологии", "Технология", "Описание", "ID лекарства", "Лекарство", "Тип лекарства"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        Runnable loadData = () -> {
            String search = searchField.getText().trim();
            String sort = (String) sortCombo.getSelectedItem();
            refreshTechnologiesTable(model, search, sort);
        };
        refreshButton.addActionListener(e -> loadData.run());
        SwingUtilities.invokeLater(loadData);

        return panel;
    }

    private void refreshTechnologiesTable(DefaultTableModel model, String search, String sortOption) {
        model.setRowCount(0);
        StringBuilder sql = new StringBuilder(
                "SELECT technology_id, technology_name, preparation_description, medicine_id, medicine_name, medicine_type " +
                        "FROM lab_drug_store.v_technologies"
        );
        if (!search.isEmpty()) {
            sql.append(" WHERE medicine_name ILIKE ?");
        }
        sql.append(switch (sortOption) {
            case "по лекарству (А-Я)" -> " ORDER BY medicine_name ASC";
            case "по лекарству (Я-А)" -> " ORDER BY medicine_name DESC";
            case "по технологии (А-Я)" -> " ORDER BY technology_name ASC";
            case "по технологии (Я-А)" -> " ORDER BY technology_name DESC";
            default -> " ORDER BY technology_name ASC";
        });

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            if (!search.isEmpty()) {
                ps.setString(1, "%" + search + "%");
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("technology_id"),
                        rs.getString("technology_name"),
                        rs.getString("preparation_description"),
                        rs.getInt("medicine_id"),
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

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField medicineField = new JTextField(15);
        JTextField customerField = new JTextField(15);
        JComboBox<String> sortCombo = new JComboBox<>(new String[]{
                "по дате (возр.)", "по дате (убыв.)",
                "по лекарству (А-Я)", "по лекарству (Я-А)",
                "по клиенту (А-Я)", "по клиенту (Я-А)"
        });
        JButton refreshButton = new JButton("Обновить");

        filterPanel.add(new JLabel("Лекарство:"));
        filterPanel.add(medicineField);
        filterPanel.add(new JLabel("Клиент:"));
        filterPanel.add(customerField);
        filterPanel.add(new JLabel("Сортировка:"));
        filterPanel.add(sortCombo);
        filterPanel.add(refreshButton);

        DefaultTableModel model = new DefaultTableModel(new String[]{
                "ID заказа", "Статус", "Дата создания", "Лекарство", "Клиент"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(model);
        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        Runnable loadData = () -> {
            String medicine = medicineField.getText().trim();
            String customer = customerField.getText().trim();
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
            case "по дате (возр.)" -> " ORDER BY creation_date ASC";
            case "по дате (убыв.)" -> " ORDER BY creation_date DESC";
            case "по лекарству (А-Я)" -> " ORDER BY medicine_name ASC";
            case "по лекарству (Я-А)" -> " ORDER BY medicine_name DESC";
            case "по клиенту (А-Я)" -> " ORDER BY customer_name ASC";
            case "по клиенту (Я-А)" -> " ORDER BY customer_name DESC";
            default -> " ORDER BY creation_date ASC";
        });

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int idx = 1;
            if (!medicine.isEmpty()) {
                ps.setString(idx++, "%" + medicine + "%");
            }
            if (!customer.isEmpty()) {
                ps.setString(idx++, "%" + customer + "%");
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

        JLabel description = new JLabel(
                "Полный перечень и общее число препаратов, требующихся для заказов, находящихся в производстве"
        );
        description.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(description);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField orderIdField = new JTextField(10);
        JTextField medicineField = new JTextField(15);
        JComboBox<String> sortCombo = new JComboBox<>(new String[]{
                "по ID заказа (возр.)", "по ID заказа (убыв.)",
                "по лекарству (А-Я)", "по лекарству (Я-А)",
                "по количеству (возр.)", "по количеству (убыв.)"
        });
        JButton refreshButton = new JButton("Обновить");

        filterPanel.add(new JLabel("ID заказа:"));
        filterPanel.add(orderIdField);
        filterPanel.add(new JLabel("Лекарство:"));
        filterPanel.add(medicineField);
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
            String medicine = medicineField.getText().trim();
            String sort = (String) sortCombo.getSelectedItem();
            refreshRequiredMedicinesTable(model, orderIdStr, medicine, sort);
        };
        refreshButton.addActionListener(e -> loadData.run());
        SwingUtilities.invokeLater(loadData);

        return panel;
    }

    private void refreshRequiredMedicinesTable(DefaultTableModel model, String orderIdStr, String medicine, String sortOption) {
        model.setRowCount(0);
        StringBuilder sql = new StringBuilder("SELECT order_id, medicine_name, required_quantity " +
                        "FROM lab_drug_store.v_required_medicines_for_production"
        );

        boolean hasWhere = false;
        if (!orderIdStr.isEmpty()) {
            try {
                sql.append(" WHERE order_id = ?");
                hasWhere = true;
            } catch (NumberFormatException ignored) {}
        }
        if (!medicine.isEmpty()) {
            if (hasWhere) sql.append(" AND");
            else sql.append(" WHERE");
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