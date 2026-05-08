package view;

import controller.*;
import model.ComponentBatch;
import model.ComponentRequest;
import model.MedicineRequest;
import utils.DatabaseConnection;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;
import java.util.List;

public class StorekeeperFrame extends JFrame {
    private final Connection connection;
    private final BatchController batchController;
    private final ComponentRequestController componentRequestController;
    private final ComponentController componentController;
    private final SupplierController supplierController;
    private MedicineRequestController medicineRequestController;
    private ReadyMedicineStockController readyMedicineStockController;
    private MedicineController medicineController;
    private DefaultTableModel requestsTableModel;
    private DefaultTableModel batchesTableModel;

    public StorekeeperFrame(Connection connection) {
        this.connection = connection;
        this.batchController = new BatchController(connection);
        this.componentRequestController = new ComponentRequestController(connection);
        this.componentController = new ComponentController(connection);
        this.supplierController = new SupplierController(connection);
        this.medicineRequestController = new MedicineRequestController(connection);
        this.readyMedicineStockController = new ReadyMedicineStockController(connection);
        this.medicineController = new MedicineController(connection);
        this.requestsTableModel = new DefaultTableModel();
        this.batchesTableModel = new DefaultTableModel();

        setTitle("Аптека - Кладовщик");
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
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
        managementPane.addTab("Партии компонентов", createBatchesPanel());
        managementPane.addTab("Заявки на компоненты", createComponentRequestsPanel());
        managementPane.addTab("Заявки на готовые лекарства", createReadyMedicineRequestsPanel());
        managementPane.addTab("Остатки готовых лекарств", createReadyMedicinesStockPanel());
        // вкладки - представления
        JTabbedPane viewsPane = new JTabbedPane();
        viewsPane.addTab("Критические лекарства", createCriticalMedicinesPanel());
        viewsPane.addTab("Остатки лекарств", createMedicineStockPanel());
        viewsPane.addTab("Использованные компоненты", createUsedComponentsPanel());

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

    // --------------------------------------------------------------------------- вкладка "Партии компонентов" (просмотр, вставка)
    private JPanel createBatchesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        batchesTableModel = new DefaultTableModel(new String[]{"ID партии", "ID компонента", "Дата поступления",
                "Количество", "ID заявки"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(batchesTableModel);
        refreshBatchesTable(batchesTableModel);

        JButton addButton = new JButton("Добавить партию");
        JButton updateButton = new JButton("Изменить количество");
        JPanel btnPanel = new JPanel();
        btnPanel.add(addButton);
        btnPanel.add(updateButton);

        addButton.addActionListener(e -> {
            showAddBatchDialog(batchesTableModel);
        });

        updateButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Выберите партию");
                return;
            }

            int batchId = (int) batchesTableModel.getValueAt(selectedRow, 0);
            double currentQty = (double) batchesTableModel.getValueAt(selectedRow, 3);
            String input = JOptionPane.showInputDialog(this, "Новое количество:", currentQty);
            if (input != null) {
                try {
                    double newQty = Double.parseDouble(input);
                    batchController.updateBatchQuantity(batchId, newQty);

                    refreshBatchesTable(batchesTableModel);
                    JOptionPane.showMessageDialog(this, "Количество компонентов партии обновлено");
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Неверный формат числа");
                } catch (SQLException ex) {
                    if (ex.getSQLState() != null && ex.getSQLState().startsWith("08")) {
                        int option = JOptionPane.showConfirmDialog(this,
                                "Соединение потеряно. Переподключиться?",
                                "Ошибка", JOptionPane.YES_NO_OPTION);
                        if (option == JOptionPane.YES_OPTION) reLogin();
                    } else {
                        JOptionPane.showMessageDialog(this, "Ошибка обновления: " + ex.getMessage());
                    }
                }
            }
        });

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshBatchesTable(DefaultTableModel model) {
        model.setRowCount(0);

        try {
            List<ComponentBatch> batches = batchController.getAllBatches();
            for (ComponentBatch b : batches) {
                model.addRow(new Object[]{
                        b.getBatchId(), b.getComponentId(), b.getReceiptDate(), b.getQuantity(),
                        b.getComponentRequestId()
                });
            }
        } catch (SQLException e) {
//            JOptionPane.showMessageDialog(this, "Ошибка загрузки партий: " + e.getMessage());
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                int option = JOptionPane.showConfirmDialog(this,
                        "Соединение с базой данных потеряно. Переподключиться?",
                        "Ошибка соединения", JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION) reLogin();
            } else {
                JOptionPane.showMessageDialog(this, "Ошибка загрузки партий: " + e.getMessage());
            }
        }
    }

    private static class ComponentItem {
        private final int id;
        private final String name;

        public ComponentItem(int id, String name) {
            this.id = id;
            this.name = name;
        }
        public int getId() { return id; }

        @Override
        public String toString() {
            return name;
        }
    }

    private void loadComponentsToCombo(JComboBox<ComponentItem> combo) throws SQLException {
        String sql = "SELECT Component_id, Name FROM lab_drug_store.Компоненты ORDER BY Name";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                combo.addItem(new ComponentItem(rs.getInt("Component_id"), rs.getString("Name")));
            }
        }
    }

    private static class RequestItem {
        int id; String desc;
        RequestItem(int id, String desc) { this.id = id; this.desc = desc; }
        int getId() { return id; }
        @Override public String toString() { return desc; }
    }

    private void loadRequestsToCombo(JComboBox<RequestItem> combo, int componentId) throws SQLException {
        combo.removeAllItems();
        String sql = "SELECT Component_request_id, Status FROM lab_drug_store.Заявки_на_пополнение_компонентов " +
                "WHERE Component_id = ? AND Status IN ('новая', 'отправлена') ORDER BY Component_request_id";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, componentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                combo.addItem(new RequestItem(rs.getInt("Component_request_id"),
                        rs.getInt("Component_request_id") + " (" + rs.getString("Status") + ")"));
            }
            combo.setEnabled(combo.getItemCount() > 0);
        }
    }

    private void showAddBatchDialog(DefaultTableModel model) {
        int nextId;
        try {
            String idSql = "SELECT COALESCE(MAX(Batch_id), 0) + 1 FROM lab_drug_store.Партии_компонентов";
            try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(idSql)) {
                nextId = rs.next() ? rs.getInt(1) : 1;
            }
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                int option = JOptionPane.showConfirmDialog(this,
                        "Соединение потеряно. Переподключиться?",
                        "Ошибка", JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION) reLogin();
            } else {
                JOptionPane.showMessageDialog(this, "Ошибка получения следующего ID партии: " + e.getMessage());
            }
            return;
        }

        JComboBox<ComponentItem> componentCombo = new JComboBox<>();
        try {
            loadComponentsToCombo(componentCombo);
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                int option = JOptionPane.showConfirmDialog(this,
                        "Соединение потеряно. Переподключиться?",
                        "Ошибка", JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION) reLogin();
            } else {
                JOptionPane.showMessageDialog(this, "Ошибка загрузки компонентов: " + e.getMessage());
            }
            return;
        }

        JComboBox<RequestItem> requestCombo = new JComboBox<>();
        requestCombo.setEnabled(false);
        componentCombo.addActionListener(e -> {
            ComponentItem selected = (ComponentItem) componentCombo.getSelectedItem();
            if (selected != null) {
                try {
                    loadRequestsToCombo(requestCombo, selected.getId());
                } catch (SQLException ex) {
                    if (ex.getSQLState() != null && ex.getSQLState().startsWith("08")) {
                        int option = JOptionPane.showConfirmDialog(StorekeeperFrame.this,
                                "Соединение потеряно. Переподключиться?",
                                "Ошибка", JOptionPane.YES_NO_OPTION);
                        if (option == JOptionPane.YES_OPTION) reLogin();
                    } else {
                        JOptionPane.showMessageDialog(StorekeeperFrame.this, "Ошибка загрузки заявок: " + ex.getMessage());
                    }
                }
            } else {
                requestCombo.removeAllItems();
                requestCombo.setEnabled(false);
            }
        });

        JTextField receiptDateField = new JTextField(10);
        JTextField quantityField = new JTextField(10);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridwidth = 2;
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("ID новой партии компонентов: " + nextId), gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        panel.add(new JLabel("Компонент:"), gbc);
        gbc.gridx = 1;
        panel.add(componentCombo, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        panel.add(new JLabel("Заявка:"), gbc);
        gbc.gridx = 1;
        panel.add(requestCombo, gbc);

        gbc.gridy = 3; gbc.gridx = 0;
        panel.add(new JLabel("Дата поступления (ГГГГ-ММ-ДД):"), gbc);
        gbc.gridx = 1;
        panel.add(receiptDateField, gbc);

        gbc.gridy = 4; gbc.gridx = 0;
        panel.add(new JLabel("Количество:"), gbc);
        gbc.gridx = 1;
        panel.add(quantityField, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Новая партия компонентов",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                ComponentItem comp = (ComponentItem) componentCombo.getSelectedItem();
                if (comp == null) throw new Exception("Не выбран компонент");
                RequestItem req = (RequestItem) requestCombo.getSelectedItem();
                if (req == null) throw new Exception("Не выбрана заявка");
                int componentId = comp.getId();
                int requestId = req.getId();
                Date receiptDate = Date.valueOf(receiptDateField.getText().trim());
                double quantity = Double.parseDouble(quantityField.getText().trim());

                ComponentBatch batch = new ComponentBatch(nextId, componentId, receiptDate, quantity, requestId);
                batchController.addBatch(batch);
                refreshBatchesTable(batchesTableModel); // обновить таблицу партий
                refreshRequestsTable(requestsTableModel); // и запросов на комопненты
                JOptionPane.showMessageDialog(this, "Партия компонентов добавлена");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            }
        }
    }

    // --------------------------------------------------------------------------- вкладка "Заявки на пополнение компонентов" (просмотр, вставка)
    private JPanel createComponentRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        requestsTableModel = new DefaultTableModel(new String[]{"ID заявки", "Компонент", "Количество", "Статус",
                "Поставщик"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(requestsTableModel);
        refreshRequestsTable(requestsTableModel);

        JButton addButton = new JButton("Создать заявку");
        JButton statusButton = new JButton("Изменить статус");
        JPanel btnPanel = new JPanel();
        btnPanel.add(addButton);
        btnPanel.add(statusButton);

        addButton.addActionListener(e -> showAddRequestDialog(requestsTableModel));

        statusButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Сначала выберите заявку");
                return;
            }

            int requestId = (int) requestsTableModel.getValueAt(selectedRow, 0);
            String currentStatus = (String) requestsTableModel.getValueAt(selectedRow, 3);
            changeRequestStatus(requestId, currentStatus, requestsTableModel);
        });

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshRequestsTable(DefaultTableModel model) {
        model.setRowCount(0);
        try {
            List<ComponentRequest> requests = componentRequestController.getAllRequests();
            for (ComponentRequest r : requests) {
                model.addRow(new Object[]{
                        r.getRequestId(),
                        r.getComponentName(),
                        r.getQuantity(),
                        r.getStatus(),
                        r.getSupplierName()
                });
            }
        } catch (SQLException e) {
//            JOptionPane.showMessageDialog(this, "Ошибка загрузки заявок на пополнение компонентов: " + e.getMessage());
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                int option = JOptionPane.showConfirmDialog(this,
                        "Соединение с базой данных потеряно. Переподключиться?",
                        "Ошибка соединения", JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION) reLogin();
            } else {
                JOptionPane.showMessageDialog(this, "Ошибка загрузки заявок на пополнение компонентов: " + e.getMessage());
            }
        }
    }

    private void changeRequestStatus(int requestId, String currentStatus, DefaultTableModel model) {
        String[] allowedStatuses = {"новая", "отправлена", "получена"};

        String newStatus = (String) JOptionPane.showInputDialog(this, "Выберите новый статус:",
                "Изменение статуса", JOptionPane.QUESTION_MESSAGE, null, allowedStatuses, currentStatus);
        if (newStatus != null && !newStatus.equals(currentStatus)) {
            try {
                componentRequestController.updateRequestStatus(requestId, newStatus);
                refreshRequestsTable(model);
                JOptionPane.showMessageDialog(this, "Статус заявки изменен на " + newStatus);
            } catch (SQLException e) {
                if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                    int option = JOptionPane.showConfirmDialog(this,
                            "Соединение потеряно. Переподключиться?",
                            "Ошибка", JOptionPane.YES_NO_OPTION);
                    if (option == JOptionPane.YES_OPTION) reLogin();
                } else {
                    JOptionPane.showMessageDialog(this, "Ошибка добавления партии: " + e.getMessage());
                }
            }
        }
    }

    private void showAddRequestDialog(DefaultTableModel model) {
        // сначала нужно получить списки компонентов и поставщиков через их контроллеры
        // JComboBox с загруженными данными
        JComboBox<String> componentCombo = new JComboBox<>();
        loadComponents(componentCombo);
        JComboBox<String> supplierCombo = new JComboBox<>();
        loadSuppliers(supplierCombo);

        JTextField quantityField = new JTextField(10);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Компонент:"), gbc);
        gbc.gridx = 1;
        panel.add(componentCombo, gbc);

        gbc.gridy = 1; gbc.gridx = 0;
        panel.add(new JLabel("Количество:"), gbc);
        gbc.gridx = 1;
        panel.add(quantityField, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        panel.add(new JLabel("Поставщик:"), gbc);
        gbc.gridx = 1;
        panel.add(supplierCombo, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Новая заявка", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String selectedComponent = (String) componentCombo.getSelectedItem();
                int componentId = componentController.getComponentIdByName(selectedComponent);
                String selectedSupplier = (String) supplierCombo.getSelectedItem();
                int supplierId = supplierController.getSupplierIdByName(selectedSupplier);
                int quantity = Integer.parseInt(quantityField.getText().trim());

                ComponentRequest request = new ComponentRequest(0, componentId, quantity, "новая", supplierId);
                componentRequestController.addRequest(request);

                refreshRequestsTable(model);
                JOptionPane.showMessageDialog(this, "Заявка создана");
            } catch (SQLException e) {
                if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                    int option = JOptionPane.showConfirmDialog(this,
                            "Соединение потеряно. Переподключиться?",
                            "Ошибка", JOptionPane.YES_NO_OPTION);
                    if (option == JOptionPane.YES_OPTION) reLogin();
                } else {
                    JOptionPane.showMessageDialog(this, "Ошибка добавления партии: " + e.getMessage());
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            }
        }
    }

    private void loadComponents(JComboBox<String> combo) {
        try {
            List<String> names = componentController.getAllComponentNames();
            for (String n : names) combo.addItem(n);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadSuppliers(JComboBox<String> combo) {
        try {
            List<String> names = supplierController.getAllSupplierNames();
            for (String n : names) combo.addItem(n);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --------------------------------------------------------------------------- вкладка "Заявки на пополнение готовых лек-в" (просмотр, вставка)
    private JPanel createReadyMedicineRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID заявки", "Лекарство", "Количество", "Статус",
                "Поставщик"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(model);
        refreshReadyRequestsTable(model);

        JButton addButton = new JButton("Создать заявку");
        JButton statusButton = new JButton("Изменить статус");
        JPanel btnPanel = new JPanel();
        btnPanel.add(addButton);
        btnPanel.add(statusButton);

        addButton.addActionListener(e -> showAddReadyRequestDialog(model));

        statusButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Выберите заявку");
                return;
            }

            int requestId = (int) model.getValueAt(selectedRow, 0);
            String currentStatus = (String) model.getValueAt(selectedRow, 3);
            changeReadyRequestStatus(requestId, currentStatus, model);
        });

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshReadyRequestsTable(DefaultTableModel model) {
        model.setRowCount(0);

        try {
            List<MedicineRequest> requests = medicineRequestController.getAllRequests();
            for (MedicineRequest r : requests) {
                model.addRow(new Object[]{r.getRequestId(), r.getMedicineName(), r.getQuantity(), r.getStatus(),
                        r.getSupplierName()
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки заявок: " + e.getMessage());
        }
    }

    private void showAddReadyRequestDialog(DefaultTableModel model) {
        JComboBox<String> medicineCombo = new JComboBox<>();
        JComboBox<String> supplierCombo = new JComboBox<>();
        try {
            for (String m : medicineController.getAllMedicineNames()) medicineCombo.addItem(m);
            for (String s : supplierController.getAllSupplierNames()) supplierCombo.addItem(s);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки списков: " + e.getMessage());
            return;
        }
        JTextField quantityField = new JTextField(10);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Лекарство:"), gbc);
        gbc.gridx = 1; panel.add(medicineCombo, gbc);
        gbc.gridy = 1; gbc.gridx = 0; panel.add(new JLabel("Количество:"), gbc);
        gbc.gridx = 1; panel.add(quantityField, gbc);
        gbc.gridy = 2; gbc.gridx = 0; panel.add(new JLabel("Поставщик:"), gbc);
        gbc.gridx = 1; panel.add(supplierCombo, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Новая заявка на готовое лекарство", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String medName = (String) medicineCombo.getSelectedItem();
                int medicineId = medicineController.getMedicineIdByName(medName);
                int quantity = Integer.parseInt(quantityField.getText().trim());
                String suppName = (String) supplierCombo.getSelectedItem();
                int supplierId = supplierController.getSupplierIdByName(suppName);
                MedicineRequest request = new MedicineRequest(0, medicineId, quantity, "новая", supplierId);
                medicineRequestController.addRequest(request);
                refreshReadyRequestsTable(model);
                JOptionPane.showMessageDialog(this, "Заявка создана");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Ошибка создания заявки на пополнение готовых лекарств: " + e.getMessage());
            }
        }
    }

    private void changeReadyRequestStatus(int requestId, String currentStatus, DefaultTableModel model) {
        String[] allowedStatuses = {"новая", "отправлена", "получена"};
        String newStatus = (String) JOptionPane.showInputDialog(this, "Выберите новый статус:",
                "Изменение статуса", JOptionPane.QUESTION_MESSAGE, null, allowedStatuses, currentStatus);

        if (newStatus != null && !newStatus.equals(currentStatus)) {
            try {
                medicineRequestController.updateRequestStatus(requestId, newStatus);
                refreshReadyRequestsTable(model);
                JOptionPane.showMessageDialog(this, "Статус изменен на " + newStatus);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            }
        }
    }

    // --------------------------------------------------------------------------- изменение остатка готовых лекарств
    private JPanel createReadyMedicinesStockPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID лекарства", "Название", "Остаток"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 2; // только остаток редактирую
            }
        };
        JTable table = new JTable(model);
        refreshReadyMedicinesStock(model);

        // сохранение изменений при завершении редактирования
        table.getModel().addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                if (row >= 0) {
                    int medicineId = (int) model.getValueAt(row, 0);
                    int newStock;
                    try {
                        newStock = Integer.parseInt(model.getValueAt(row, 2).toString());
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Введено неверное число");
                        refreshReadyMedicinesStock(model);
                        return;
                    }

                    try {
                        readyMedicineStockController.updateStock(medicineId, newStock);
                        JOptionPane.showMessageDialog(this, "Остаток обновлен");
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(this, "Ошибка обновления: " + ex.getMessage());
                        refreshReadyMedicinesStock(model);
                    }
                }
            }
        });

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void refreshReadyMedicinesStock(DefaultTableModel model) {
        model.setRowCount(0);
        String sql = "SELECT g.Medicine_id, l.Название, g.Остаток FROM lab_drug_store.Готовые_лекарства AS g " +
                "JOIN lab_drug_store.Лекарства AS l ON g.Medicine_id = l.Medicine_id " +
                "ORDER BY g.Medicine_id";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("Medicine_id"),
                        rs.getString("Название"),
                        rs.getInt("Остаток")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки: " + e.getMessage());
        }
    }

    // --------------------------------------------------------------------------- вкладки представлений
    private JPanel createCriticalMedicinesPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JLabel text = new JLabel("Получить перечень и типы лекарств, достигших своей критической нормы или закончившихся");
        text.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(text);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField searchField = new JTextField(20);
        JComboBox<String> sortCombo = new JComboBox<>(new String[]{
                "по названию лекарства (А-Я)",
                "по названию лекарства (Я-А)",
                "по остатку компонента (возр.)",
                "по остатку компонента (убыв.)",
                "по критической норме (возр.)",
                "по критической норме (убыв.)"
        });
        JButton refreshButton = new JButton("Обновить");

        filterPanel.add(new JLabel("Поиск:"));
        filterPanel.add(searchField);
        filterPanel.add(new JLabel("Сортировка:"));
        filterPanel.add(sortCombo);
        filterPanel.add(refreshButton);

        topPanel.add(filterPanel);

        panel.add(topPanel, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(new String[]{
                "ID лекарства", "Лекарство", "Компонент", "Остаток компонента",
                "Критическая норма", "Способ применения", "Тип лекарства"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);

        panel.add(scroll, BorderLayout.CENTER);

        // загрузка данных с учетом фильтров
        Runnable loadData = () -> {
            String search = searchField.getText().trim();
            String sortOption = (String) sortCombo.getSelectedItem();
            refreshCriticalMedicinesTable(model, search, sortOption); // sql query based on users input
        };

        refreshButton.addActionListener(e -> loadData.run());

        // первоначальная загрузка таблицы когда окна пустые
        SwingUtilities.invokeLater(loadData);

        return panel;
    }

    private void refreshCriticalMedicinesTable(DefaultTableModel model, String search, String sortOption) {
        model.setRowCount(0);
        StringBuilder sql = new StringBuilder("SELECT medicine_id, medicine_name, component_name, component_stock, " +
                "critical_level, application_method, medicine_type " +
                "FROM lab_drug_store.v_critical_medicines"
        );

        // поиск по названию лекарства
        if (!search.isEmpty()) {
            sql.append(" WHERE medicine_name ILIKE ?");
        }

        // сортировка
        sql.append(switch (sortOption) {
            case "по названию лекарства (А-Я)" -> " ORDER BY medicine_name ASC";
            case "по названию лекарства (Я-А)" -> " ORDER BY medicine_name DESC";
            case "по остатку компонента (возр.)" -> " ORDER BY component_stock ASC";
            case "по остатку компонента (убыв.)" -> " ORDER BY component_stock DESC";
            case "по критической норме (возр.)" -> " ORDER BY critical_level ASC";
            case "по критической норме (убыв.)" -> " ORDER BY critical_level DESC";
            default -> " ORDER BY medicine_name ASC";
        });

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            if (!search.isEmpty()) {
                ps.setString(1, "%" + search + "%");
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("medicine_id"),
                        rs.getString("medicine_name"),
                        rs.getString("component_name"),
                        rs.getBigDecimal("component_stock"),
                        rs.getBigDecimal("critical_level"),
                        rs.getString("application_method"),
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



    private JPanel createMedicineStockPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // текстовая метка
        JLabel text = new JLabel("Перечень лекарств с запасом на складе (готовые - остаток; изготавливаемые - возможное количество которое можно изготовить)");
        text.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(text);

        // панель фильтров
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField searchField = new JTextField(15);
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"все типы", "готовое", "изготавливаемое"});
        JComboBox<String> sortCombo = new JComboBox<>(new String[]{
                "по названию (А-Я)",
                "по названию (Я-А)",
                "по запасу (возр.)",
                "по запасу (убыв.)"
        });
        JButton refreshButton = new JButton("Обновить");

        filterPanel.add(new JLabel("Поиск:"));
        filterPanel.add(searchField);
        filterPanel.add(new JLabel("Тип:"));
        filterPanel.add(typeCombo);
        filterPanel.add(new JLabel("Сортировка:"));
        filterPanel.add(sortCombo);
        filterPanel.add(refreshButton);

        topPanel.add(filterPanel);
        panel.add(topPanel, BorderLayout.NORTH);

        // статичная таблица
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID лекарства", "Название", "Тип", "Запас (ед.)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);
        panel.add(scroll, BorderLayout.CENTER);

        Runnable loadData = () -> {
            String search = searchField.getText().trim();
            String type = (String) typeCombo.getSelectedItem();
            String sort = (String) sortCombo.getSelectedItem();
            refreshMedicineStockTable(model, search, type, sort);
        };

        refreshButton.addActionListener(e -> loadData.run());
        SwingUtilities.invokeLater(loadData);

        return panel;
    }

    private void refreshMedicineStockTable(DefaultTableModel model, String search, String type, String sort) {
        model.setRowCount(0);
        StringBuilder sql = new StringBuilder("SELECT medicine_id, medicine_name, medicine_type, stock_quantity " +
                "FROM lab_drug_store.v_medicine_stock"
        );

        boolean hasWhere = false;
        if (!search.isEmpty()) {
            sql.append(" WHERE medicine_name ILIKE ?");
            hasWhere = true;
        }
        if (!"все типы".equals(type)) {
            if (hasWhere) {
                sql.append(" AND medicine_type = ?");
            } else {
                sql.append(" WHERE medicine_type = ?");
            }
        }

        sql.append(switch (sort) {
            case "по запасу (возр.)" -> " ORDER BY stock_quantity ASC";
            case "по запасу (убыв.)" -> " ORDER BY stock_quantity DESC";
            case "по названию (А-Я)" -> " ORDER BY medicine_name ASC";
            case "по названию (Я-А)" -> " ORDER BY medicine_name DESC";
            default -> " ORDER BY stock_quantity ASC";
        });

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (!search.isEmpty()) {
                ps.setString(paramIndex++, "%" + search + "%");
            }
            if (!"все типы".equals(type)) {
                String typeFilter = "готовое".equals(type) ? "готовое" : "изготавливаемое";
                ps.setString(paramIndex++, typeFilter);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("medicine_id"),
                        rs.getString("medicine_name"),
                        rs.getString("medicine_type"),
                        rs.getBigDecimal("stock_quantity")
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



    private JPanel createUsedComponentsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JLabel text = new JLabel("Объем использованных компонентов за указанный период времени");
        text.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(text);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField startDateField = new JTextField(10);
        JTextField endDateField = new JTextField(10);
        JTextField componentSearchField = new JTextField(15);
        JComboBox<String> sortCombo = new JComboBox<>(new String[]{
                "по дате заказа (возр.)",
                "по дате заказа (убыв.)",
                "по количеству (возр.)",
                "по количеству (убыв.)",
                "по компоненту (А-Я)",
                "по компоненту (Я-А)"
        });
        JButton refreshButton = new JButton("Обновить");

        filterPanel.add(new JLabel("дата от (ГГГГ-ММ-ДД):"));
        filterPanel.add(startDateField);
        filterPanel.add(new JLabel("до:"));
        filterPanel.add(endDateField);
        filterPanel.add(new JLabel("компонент:"));
        filterPanel.add(componentSearchField);
        filterPanel.add(new JLabel("сортировка:"));
        filterPanel.add(sortCombo);
        filterPanel.add(refreshButton);

        topPanel.add(filterPanel);
        panel.add(topPanel, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(new String[]{"ID компонента", "Компонент", "ID заказа",
                "Использовано, ед.", "Дата заказа"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);
        panel.add(scroll, BorderLayout.CENTER);

        Runnable loadData = () -> {
            String start = startDateField.getText().trim();
            String end = endDateField.getText().trim();
            String compSearch = componentSearchField.getText().trim();
            String sort = (String) sortCombo.getSelectedItem();
            refreshUsedComponentsTable(model, start, end, compSearch, sort);
        };

        refreshButton.addActionListener(e -> loadData.run());
        SwingUtilities.invokeLater(loadData);

        return panel;
    }

    private void refreshUsedComponentsTable(DefaultTableModel model, String startDate, String endDate, String compSearch, String sortOption) {
        model.setRowCount(0);
        StringBuilder sql = new StringBuilder("SELECT component_id, component_name, order_id, used_quantity, order_date " +
                "FROM lab_drug_store.v_used_components"
        );

        if (!startDate.isEmpty() || !endDate.isEmpty() || !compSearch.isEmpty()) {
            sql.append(" WHERE");
        }
        if (!startDate.isEmpty()) {
            sql.append(" order_date >= ?");
        }
        if (!endDate.isEmpty()) {
            if (!startDate.isEmpty()) sql.append(" AND");
            sql.append(" order_date <= ?");
        }
        if (!compSearch.isEmpty()) {
            if (!startDate.isEmpty() || !endDate.isEmpty()) sql.append(" AND");
            sql.append(" component_name ILIKE ?");
        }

        sql.append(switch (sortOption) {
            case "по дате заказа (возр.)" -> " ORDER BY order_date ASC";
            case "по дате заказа (убыв.)" -> " ORDER BY order_date DESC";
            case "по количеству (возр.)" -> " ORDER BY used_quantity ASC";
            case "по количеству (убыв.)" -> " ORDER BY used_quantity DESC";
            case "по компоненту (А-Я)" -> " ORDER BY component_name ASC";
            case "по компоненту (Я-А)" -> " ORDER BY component_name DESC";
            default -> " ORDER BY order_date ASC";
        });

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (!startDate.isEmpty()) {
                ps.setDate(paramIndex++, java.sql.Date.valueOf(startDate));
            }
            if (!endDate.isEmpty()) {
                ps.setDate(paramIndex++, java.sql.Date.valueOf(endDate));
            }
            if (!compSearch.isEmpty()) {
                ps.setString(paramIndex++, "%" + compSearch + "%");
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("component_id"),
                        rs.getString("component_name"),
                        rs.getInt("order_id"),
                        rs.getBigDecimal("used_quantity"),
                        rs.getTimestamp("order_date")
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