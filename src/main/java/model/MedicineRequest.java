package model;

public class MedicineRequest {
    private int requestId;
    private int medicineId;
    private int quantity;
    private String status;
    private int supplierId;
    // как и в ComponentRequest.java ниже - не официальные атрибуты сущности
    private String medicineName;
    private String supplierName;

    // конструктор для вставки (без доп имен)
    public MedicineRequest(int requestId, int medicineId, int quantity, String status, int supplierId) {
        this(requestId, medicineId, quantity, status, supplierId, null, null);
    }

    public MedicineRequest(int requestId, int medicineId, int quantity, String status, int supplierId,
                           String medicineName, String supplierName) {
        this.requestId = requestId;
        this.medicineId = medicineId;
        this.quantity = quantity;
        this.status = status;
        this.supplierId = supplierId;
        this.medicineName = medicineName;
        this.supplierName = supplierName;
    }

    public int getRequestId() { return requestId; }
    public int getMedicineId() { return medicineId; }
    public int getQuantity() { return quantity; }
    public String getStatus() { return status; }
    public int getSupplierId() { return supplierId; }
    public String getMedicineName() { return medicineName; }
    public String getSupplierName() { return supplierName; }
}