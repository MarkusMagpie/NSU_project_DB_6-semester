package controller;

import model.ComponentRequest;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ComponentRequestController {
    private Connection connection;

    public ComponentRequestController(Connection connection) {
        this.connection = connection;
    }

    public List<ComponentRequest> getAllRequests() throws SQLException {
        List<ComponentRequest> list = new ArrayList<>();

        String sql = "SELECT r.Component_request_id, r.Component_id, r.Quantity, r.Status, r.Supplier_id, c.Name, s.Full_name " +
                "FROM lab_drug_store.Заявки_на_пополнение_компонентов as r " +
                "JOIN lab_drug_store.Компоненты as c ON r.Component_id = c.Component_id " +
                "JOIN lab_drug_store.Поставщики as s ON r.Supplier_id = s.Supplier_id " +
                "ORDER BY r.Component_request_id";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new ComponentRequest(
                        rs.getInt("Component_request_id"),
                        rs.getInt("Component_id"),
                        rs.getInt("Quantity"),
                        rs.getString("Status"),
                        rs.getInt("Supplier_id"),
                        rs.getString("Name"), // эти два поля - новинка
                        rs.getString("Full_name")
                        ));
            }
        }
        return list;
    }

    public void addRequest(ComponentRequest request) throws SQLException {
        int nextId;
        String idSql = "SELECT COALESCE(MAX(Component_request_id), 0) + 1 FROM lab_drug_store.Заявки_на_пополнение_компонентов";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(idSql)) {

            nextId = rs.next() ? rs.getInt(1) : 1;
        }

        String sql = "INSERT INTO lab_drug_store.Заявки_на_пополнение_компонентов " +
                "(Component_request_id, Component_id, Quantity, Status, Supplier_id) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, nextId);
            ps.setInt(2, request.getComponentId());
            ps.setInt(3, request.getQuantity());
            ps.setString(4, request.getStatus());
            ps.setInt(5, request.getSupplierId());

            ps.executeUpdate();
        }
    }

    public void updateRequestStatus(int requestId, String newStatus) throws SQLException {
        String sql = "UPDATE lab_drug_store.Заявки_на_пополнение_компонентов SET Status = ? WHERE Component_request_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, requestId);

            ps.executeUpdate();
        }
    }
}