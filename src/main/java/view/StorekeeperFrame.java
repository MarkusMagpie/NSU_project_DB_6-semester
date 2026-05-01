package view;

import controller.*;
import model.ComponentBatch;
import model.ComponentRequest;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;
import java.util.List;
import java.util.Vector;

public class StorekeeperFrame extends JFrame {
    private final Connection connection;
    private final BatchController batchController;
    private final ComponentRequestController componentRequestController;
    private final MedicineController medicineController;
    private final ComponentController componentController;
    private final SupplierController supplierController;

    public StorekeeperFrame(Connection connection) {
        this.connection = connection;
        this.batchController = new BatchController(connection);
        this.medicineController = new MedicineController(connection);
        this.componentRequestController = new ComponentRequestController(connection);
        this.componentController = new ComponentController(connection);
        this.supplierController = new SupplierController(connection);

        setTitle("Аптека - Кладовщик");
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        // вкладки для управления
        tabbedPane.addTab("Партии компонентов", createBatchesPanel());
        tabbedPane.addTab("Заявки на компоненты", createComponentRequestsPanel());

        // вкладки - представления
        tabbedPane.addTab("Критические лекарства", createViewPanel("v_critical_medicines"));
        tabbedPane.addTab("Остатки лекарств", createViewPanel("v_medicine_stock"));
        tabbedPane.addTab("Использованные компоненты", createViewPanel("v_used_components"));

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

    // --------------------------------------------------------------------------- вкладка "Партии компонентов" (просмотр, вставка)
    private JPanel createBatchesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID партии", "ID компонента", "Дата поступления",
                "Количество", "ID заявки"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(model);
        refreshBatchesTable(model);

        JButton addButton = new JButton("Добавить партию");
        JButton updateButton = new JButton("Изменить количество");
        JPanel btnPanel = new JPanel();
        btnPanel.add(addButton);
        btnPanel.add(updateButton);

        addButton.addActionListener(e -> {
            try {
                showAddBatchDialog(model);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });

        updateButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Выберите партию");
                return;
            }

            int batchId = (int) model.getValueAt(selectedRow, 0);
            double currentQty = (double) model.getValueAt(selectedRow, 3);
            String input = JOptionPane.showInputDialog(this, "Новое количество:", currentQty);
            if (input != null) {
                try {
                    double newQty = Double.parseDouble(input);
                    batchController.updateBatchQuantity(batchId, newQty);

                    refreshBatchesTable(model);
                    JOptionPane.showMessageDialog(this, "Количество компонентов партии обновлено");
                } catch (NumberFormatException | SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка: " + ex.getMessage());
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
            JOptionPane.showMessageDialog(this, "Ошибка загрузки партий: " + e.getMessage());
        }
    }

    private void showAddBatchDialog(DefaultTableModel model) throws SQLException {
        int nextId;
        String idSql = "SELECT COALESCE(MAX(Batch_id), 0) + 1 FROM lab_drug_store.Партии_компонентов";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(idSql)) {
            nextId = rs.next() ? rs.getInt(1) : 1;
        }

        JTextField componentIdField = new JTextField(10);
        JTextField receiptDateField = new JTextField(10);
        JTextField quantityField = new JTextField(10);
        JTextField requestIdField = new JTextField(10);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridwidth = 2;
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel idLabel = new JLabel("ID новой партии компонентов: " + nextId);
        panel.add(idLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        panel.add(new JLabel("ID компонента:"), gbc);
        gbc.gridx = 1;
        panel.add(componentIdField, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        panel.add(new JLabel("Дата поступления (ГГГГ-ММ-ДД):"), gbc);
        gbc.gridx = 1;
        panel.add(receiptDateField, gbc);

        gbc.gridy = 3; gbc.gridx = 0;
        panel.add(new JLabel("Количество:"), gbc);
        gbc.gridx = 1;
        panel.add(quantityField, gbc);

        gbc.gridy = 4; gbc.gridx = 0;
        panel.add(new JLabel("ID заявки:"), gbc);
        gbc.gridx = 1;
        panel.add(requestIdField, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Новая партия компонентов",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                ComponentBatch batch = new ComponentBatch(
                        nextId,
                        Integer.parseInt(componentIdField.getText().trim()),
                        Date.valueOf(receiptDateField.getText().trim()),
                        Double.parseDouble(quantityField.getText().trim()),
                        Integer.parseInt(requestIdField.getText().trim())
                );
                batchController.addBatch(batch);
                refreshBatchesTable(model);
                JOptionPane.showMessageDialog(this, "Партия компонентов добавлена");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Ошибка добавления партии: " + e.getMessage());
            }
        }
    }

    // --------------------------------------------------------------------------- вкладка "Заявки на пополнение компонентов" (просмотр, вставка)
    private JPanel createComponentRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        DefaultTableModel model = new DefaultTableModel(new String[]{"ID заявки", "Компонент", "Количество",
                "Статус", "Поставщик"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(model);
        refreshRequestsTable(model);

        JButton addButton = new JButton("Создать заявку");
        JButton statusButton = new JButton("Изменить статус");
        JPanel btnPanel = new JPanel();
        btnPanel.add(addButton);
        btnPanel.add(statusButton);

        addButton.addActionListener(e -> showAddRequestDialog(model));

        statusButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Сначала выберите заявку");
                return;
            }

            int requestId = (int) model.getValueAt(selectedRow, 0);
            String currentStatus = (String) model.getValueAt(selectedRow, 3);
            changeRequestStatus(requestId, currentStatus, model);
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
            JOptionPane.showMessageDialog(this, "Ошибка загрузки заявок на пополнение компонентов: " + e.getMessage());
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
                JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
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

    // --------------------------------------------------------------------------- представления
    private JPanel createViewPanel(String viewName) {
        JPanel panel = new JPanel(new BorderLayout());
        JTable table = new JTable();
        JScrollPane scroll = new JScrollPane(table);
        panel.add(scroll, BorderLayout.CENTER);

        // загрузка данных в отдельном потоке
        SwingUtilities.invokeLater(() -> refreshViewTable(table, viewName));
        return panel;
    }

    // загружает данные из представления и заполняет ими table
    private void refreshViewTable(JTable table, String viewName) {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM lab_drug_store." + viewName)) {

            // метаданные - количество столбцов и их имена
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            Vector<String> columnNames = new Vector<>(); // имена столбцов - будут заголовками таблицы
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(meta.getColumnName(i));
            }

            // сбор строк данных
            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getObject(i)); // запись значений ячейки в row
                }
                data.add(row);
            }

            // модель таблицы = данные + заголовки столбцов
            DefaultTableModel model = new DefaultTableModel(data, columnNames) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            table.setModel(model);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка загрузки представления " + viewName + ": " + e.getMessage());
        }
    }
}