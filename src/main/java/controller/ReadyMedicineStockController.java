package controller;

import java.sql.*;

public class ReadyMedicineStockController {
    private Connection connection;

    public ReadyMedicineStockController(Connection connection) {
        this.connection = connection;
    }

    public void updateStock(int medicineId, int newStock) throws SQLException {
        String sql = "UPDATE lab_drug_store.Готовые_лекарства SET Остаток = ? WHERE Medicine_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, newStock);
            ps.setInt(2, medicineId);
            ps.executeUpdate();
        }
    }
}