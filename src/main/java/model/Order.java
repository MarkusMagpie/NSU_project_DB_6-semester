package model;

import java.sql.Timestamp;

public class Order {
    private int orderId;
    private Timestamp creationDate;
    private String status;
    private Timestamp completionTime;
    private int price;
    private String medicineName;

    public Order(int orderId, Timestamp creationDate, String status, Timestamp completionTime, int price, String medicineName) {
        this.orderId = orderId;
        this.creationDate = creationDate;
        this.status = status;
        this.completionTime = completionTime;
        this.price = price;
        this.medicineName = medicineName;
    }

    public int getOrderId() { return orderId; }
    public Timestamp getCreationDate() { return creationDate; }
    public String getStatus() { return status; }
    public Timestamp getCompletionTime() { return completionTime; }
    public int getPrice() { return price; }
    public String getMedicineName() { return medicineName; }
}