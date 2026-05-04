package controller;

import model.MedicineRequest;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicineRequestController {
    private Connection connection;

    public MedicineRequestController(Connection connection) {
        this.connection = connection;
    }

    public List<MedicineRequest> getAllRequests() throws SQLException {
        List<MedicineRequest> list = new ArrayList<>();
        String sql = "SELECT r.Medicine_request_id, r.Medicine_id, r.Quantity, r.Status, r.Supplier_id, " +
                "m.Название AS medicine_name, s.Full_name AS supplier_name " +
                "FROM lab_drug_store.Заявки_на_пополнение_готовых_лекарств as r " +

                "JOIN lab_drug_store.Готовые_лекарства as g ON r.Medicine_id = g.Medicine_id " +
                "JOIN lab_drug_store.Лекарства as m ON g.Medicine_id = m.Medicine_id " +
                "JOIN lab_drug_store.Поставщики as s ON r.Supplier_id = s.Supplier_id " +
                "ORDER BY r.Medicine_request_id";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new MedicineRequest(
                        rs.getInt("Medicine_request_id"),
                        rs.getInt("Medicine_id"),
                        rs.getInt("Quantity"),
                        rs.getString("Status"),
                        rs.getInt("Supplier_id"),
                        rs.getString("medicine_name"),
                        rs.getString("supplier_name")
                ));
            }
        }
        return list;
    }

    public void addRequest(MedicineRequest request) throws SQLException {
        int nextId;
        String idSql = "SELECT COALESCE(MAX(Medicine_request_id), 0) + 1 FROM lab_drug_store.Заявки_на_пополнение_готовых_лекарств";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(idSql)) {
            nextId = rs.next() ? rs.getInt(1) : 1;
        }

        String sql = "INSERT INTO lab_drug_store.Заявки_на_пополнение_готовых_лекарств " +
                "(Medicine_request_id, Medicine_id, Quantity, Status, Supplier_id) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, nextId);
            ps.setInt(2, request.getMedicineId());
            ps.setInt(3, request.getQuantity());
            ps.setString(4, request.getStatus());
            ps.setInt(5, request.getSupplierId());

            ps.executeUpdate();
        }
    }

    public void updateRequestStatus(int requestId, String newStatus) throws SQLException {
        String sql = "UPDATE lab_drug_store.Заявки_на_пополнение_готовых_лекарств SET Status = ? WHERE Medicine_request_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, requestId);
            ps.executeUpdate();
        }
    }
}