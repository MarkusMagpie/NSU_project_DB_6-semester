package controller;

import model.Order;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderController {
    private final Connection connection;

    public OrderController(Connection connection) {
        this.connection = connection;
    }

    public List<Order> getAllOrders() throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.Order_id, o.Дата_создания, o.Статус, o.Время_изготовления, o.Цена, l.Название " +
                "FROM lab_drug_store.Заказы o " +
                "JOIN lab_drug_store.Лекарства l ON o.Medicine_id = l.Medicine_id " +
                "ORDER BY o.Order_id";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                orders.add(new Order(
                        rs.getInt("Order_id"),
                        rs.getTimestamp("Дата_создания"),
                        rs.getString("Статус"),
                        rs.getTimestamp("Время_изготовления"),
                        rs.getInt("Цена"),
                        rs.getString("Название")
                ));
            }
        }

        return orders;
    }

    public void updateOrderStatus(int orderId, String newStatus) throws SQLException {
        String sql = "UPDATE lab_drug_store.Заказы SET Статус = ? WHERE Order_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    // следующий id для рецепта
    public int getNextPrescriptionId() throws SQLException {
        String sql = "SELECT COALESCE(MAX(Prescription_id), 0) + 1 FROM lab_drug_store.Рецепты";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 1;
        }
    }

    // следующий id для заказа
    public int getNextOrderId() throws SQLException {
        String sql = "SELECT COALESCE(MAX(Order_id), 0) + 1 FROM lab_drug_store.Заказы";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 1;
        }
    }

    // название лекарства по ID
    private String getMedicineNameById(int medicineId) throws SQLException {
        String sql = "SELECT Название FROM lab_drug_store.Лекарства WHERE Medicine_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, medicineId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(1);

            throw new SQLException("Лекарство не найдено");
        }
    }

    // цена лекарства по ID
    public int getMedicinePriceById(int medicineId) throws SQLException {
        String sql = "SELECT Цена FROM lab_drug_store.Лекарства WHERE Medicine_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, medicineId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
            
            throw new SQLException("Цена не найдена");
        }
    }
    
    public int createOrder(int clientId, int medicineId, String diagnosis,
                           String usageMethod, java.sql.Date issueDate, int quantity,
                           String doctorName, String doctorSignature, String doctorStamp,
                           int price) throws SQLException {
        connection.setAutoCommit(false);
        
        try {
            int nextPrescriptionId = getNextPrescriptionId();
            String medicineName = getMedicineNameById(medicineId);

            // вставка рецепта
            String sql = "INSERT INTO lab_drug_store.Рецепты " +
                    "(Prescription_id, Диагноз, Наименование_лекарства, Способ_применения, " +
                    "Дата_выписки, Количество_лекарства, ФИО_врача, Подпись_врача, Печать_врача, Client_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, nextPrescriptionId);
                ps.setString(2, diagnosis);
                ps.setString(3, medicineName);
                ps.setString(4, usageMethod);
                ps.setDate(5, issueDate);
                ps.setInt(6, quantity);
                ps.setString(7, doctorName);
                ps.setString(8, doctorSignature);
                ps.setString(9, doctorStamp);
                ps.setInt(10, clientId);
                ps.executeUpdate();
            }

            // вставка заказа
            int nextOrderId = getNextOrderId();
            String insertOrder = "INSERT INTO lab_drug_store.Заказы " +
                    "(Order_id, Дата_создания, Статус, Время_изготовления, Цена, Medicine_id, Prescription_id) " +
                    "VALUES (?, CURRENT_TIMESTAMP, NULL, NULL, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(insertOrder)) {
                ps.setInt(1, nextOrderId);
                ps.setInt(2, price);
                ps.setInt(3, medicineId);
                ps.setInt(4, nextPrescriptionId);
                ps.executeUpdate();
            }

            connection.commit();

            return nextOrderId;
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
}