package model;

import java.sql.Date;

public class ComponentBatch {
    private int batchId;
    private int componentId;
    private Date receiptDate;
    private double quantity;
    private int componentRequestId;

    public ComponentBatch(int batchId, int componentId, Date receiptDate, double quantity, int componentRequestId) {
        this.batchId = batchId;
        this.componentId = componentId;
        this.receiptDate = receiptDate;
        this.quantity = quantity;
        this.componentRequestId = componentRequestId;
    }

    public int getBatchId() { return batchId; }
    public void setBatchId(int batchId) { this.batchId = batchId; }
    public int getComponentId() { return componentId; }
    public void setComponentId(int componentId) { this.componentId = componentId; }
    public Date getReceiptDate() { return receiptDate; }
    public void setReceiptDate(Date receiptDate) { this.receiptDate = receiptDate; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public int getComponentRequestId() { return componentRequestId; }
    public void setComponentRequestId(int componentRequestId) { this.componentRequestId = componentRequestId; }
}