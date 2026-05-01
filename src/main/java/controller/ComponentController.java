package controller;

import model.Component;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ComponentController {
    private Connection connection;

    public ComponentController(Connection connection) {
        this.connection = connection;
    }

    public List<String> getAllComponentNames() throws SQLException {
        List<String> names = new ArrayList<>();

        String sql = "SELECT Name FROM lab_drug_store.Компоненты ORDER BY Component_id";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                names.add(rs.getString("Name"));
            }
        }

        return names;
    }

    public int getComponentIdByName(String name) throws SQLException {
        String sql = "SELECT Component_id FROM lab_drug_store.Компоненты WHERE Name = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

            throw new SQLException("Компонент не найден: " + name);
        }
    }

    public List<Component> getAllComponents() throws SQLException {
        List<Component> list = new ArrayList<>();
        String sql = "SELECT Component_id, Name, Shelf_life, Critical_level, Price FROM lab_drug_store.Компоненты ORDER BY Component_id";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Component(
                        rs.getInt("Component_id"),
                        rs.getString("Name"),
                        rs.getInt("Shelf_life"),
                        rs.getDouble("Critical_level"),
                        rs.getDouble("Price")
                ));
            }
        }
        return list;
    }
}