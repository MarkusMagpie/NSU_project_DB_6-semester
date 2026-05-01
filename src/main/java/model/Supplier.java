package model;

public class Supplier {
    private int supplierId;
    private String fullName;
    private String phone;
    private String email;

    public Supplier(int supplierId, String fullName, String phone, String email) {
        this.supplierId = supplierId;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
    }

    public int getSupplierId() { return supplierId; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
}