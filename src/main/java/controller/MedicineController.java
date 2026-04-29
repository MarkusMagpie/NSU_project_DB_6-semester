package controller;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicineController {
    private final Connection connection;

    public MedicineController(Connection connection) {
        this.connection = connection;
    }

    public List<String> getAllMedicineNames() throws SQLException {
        List<String> names = new ArrayList<>();
        String sql = "SELECT Название FROM lab_drug_store.Лекарства ORDER BY Medicine_id";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                names.add(rs.getString("Название"));
            }
        }

        return names;
    }

    public int getMedicineIdByName(String name) throws SQLException {
        String sql = "SELECT Medicine_id FROM lab_drug_store.Лекарства WHERE Название = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);

            throw new SQLException("Лекарство не найдено: " + name);
        }
    }
}