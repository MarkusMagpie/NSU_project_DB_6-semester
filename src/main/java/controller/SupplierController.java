package controller;

import model.Supplier;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupplierController {
    private Connection connection;

    public SupplierController(Connection connection) {
        this.connection = connection;
    }

    public List<String> getAllSupplierNames() throws SQLException {
        List<String> names = new ArrayList<>();

        String sql = "SELECT Full_name FROM lab_drug_store.Поставщики ORDER BY Supplier_id";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                names.add(rs.getString("Full_name"));
            }
        }

        return names;
    }

    public int getSupplierIdByName(String name) throws SQLException {
        String sql = "SELECT Supplier_id FROM lab_drug_store.Поставщики WHERE Full_name = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

            throw new SQLException("Поставщик не найден: " + name);
        }
    }

    public List<Supplier> getAllSuppliers() throws SQLException {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT Supplier_id, Full_name, Phone, Email FROM lab_drug_store.Поставщики ORDER BY Supplier_id";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Supplier(
                        rs.getInt("Supplier_id"),
                        rs.getString("Full_name"),
                        rs.getString("Phone"),
                        rs.getString("Email")
                ));
            }
        }
        return list;
    }
}