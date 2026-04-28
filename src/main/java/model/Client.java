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
    public void setClientId(int clientId) {
        this.clientId = clientId;
    }
    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
}