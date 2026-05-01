package model;

public class ComponentRequest {
    private int requestId;
    private int componentId;
    private int quantity;
    private String status;
    private int supplierId;
    // не официальные атрибуты сущности
    private String componentName;
    private String supplierName;

    public ComponentRequest(int requestId, int componentId, int quantity, String status, int supplierId) {
        this(requestId, componentId, quantity, status, supplierId, null, null);
    }

    // полный конструктор
    public ComponentRequest(int requestId, int componentId, int quantity, String status, int supplierId,
                            String componentName, String supplierName) {
        this.requestId = requestId;
        this.componentId = componentId;
        this.quantity = quantity;
        this.status = status;
        this.supplierId = supplierId;
        this.componentName = componentName;
        this.supplierName = supplierName;
    }

    public int  getRequestId() { return requestId; }
    public void setRequestId(int requestId) { this.requestId = requestId; }

    public int getComponentId() { return componentId; }
    public void setComponentId(int componentId) { this.componentId = componentId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
}