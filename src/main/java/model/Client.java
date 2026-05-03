package model;

public class Client {
    private int clientId;
    private String fullName;
    private String phone;
    private String address;

    public Client(int clientId, String fullName, String phone, String address) {
        this.clientId = clientId;
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
    }

    public int getClientId() {
        return clientId;
    }
    public String getFullName() {
        return fullName;
    }
    public String getPhone() {
        return phone;
    }
    public String getAddress() {
        return address;
    }
}