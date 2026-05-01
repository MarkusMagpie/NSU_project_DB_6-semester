package controller;

import model.ComponentBatch;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BatchController {
    private final Connection connection;

    public BatchController(Connection connection) {
        this.connection = connection;
    }

    public List<ComponentBatch> getAllBatches() throws SQLException {
        List<ComponentBatch> list = new ArrayList<>();
        String sql = "SELECT Batch_id, Component_id, Receipt_date, Quantity, Component_request_id " +
                "FROM lab_drug_store.Партии_компонентов ORDER BY Batch_id";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new ComponentBatch(
                        rs.getInt("Batch_id"),
                        rs.getInt("Component_id"),
                        rs.getDate("Receipt_date"),
                        rs.getDouble("Quantity"),
                        rs.getInt("Component_request_id")
                ));
            }
        }
        return list;
    }

    public void addBatch(ComponentBatch batch) throws SQLException {
        String sql = "INSERT INTO lab_drug_store.Партии_компонентов (Batch_id, Component_id, Receipt_date, Quantity, Component_request_id) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, batch.getBatchId());
            ps.setInt(2, batch.getComponentId());
            ps.setDate(3, batch.getReceiptDate());
            ps.setDouble(4, batch.getQuantity());
            ps.setInt(5, batch.getComponentRequestId());
            ps.executeUpdate();
        }
    }

    public void updateBatchQuantity(int batchId, double newQuantity) throws SQLException {
        String sql = "UPDATE lab_drug_store.Партии_компонентов SET Quantity = ? WHERE Batch_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, newQuantity);
            ps.setInt(2, batchId);
            ps.executeUpdate();
        }
    }
}