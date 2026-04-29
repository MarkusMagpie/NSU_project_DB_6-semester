package controller;

import model.Client;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClientController {
    private final Connection connection;

    public ClientController(Connection connection) {
        this.connection = connection;
    }

    public List<Client> getAllClients() throws SQLException {
        List<Client> list = new ArrayList<>();
        String sql = "SELECT Client_id, ФИО, Телефон, Адрес FROM lab_drug_store.Больные_клиенты ORDER BY Client_id";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Client c = new Client(
                        rs.getInt("Client_id"),
                        rs.getString("ФИО"),
                        rs.getString("Телефон"),
                        rs.getString("Адрес")
                );
                list.add(c);
            }
        }

        return list;
    }

    public void addClient(Client client) throws SQLException {
        String fio = client.getFullName();
        String phone =  client.getPhone();
        String address =  client.getAddress();

        String call = "CALL lab_drug_store.add_client(?, ?, ?)";
        try (CallableStatement cs = connection.prepareCall(call)) {
            cs.setString(1, fio);
            cs.setString(2, phone);
            cs.setString(3, address);
            cs.execute();
        }
    }
}