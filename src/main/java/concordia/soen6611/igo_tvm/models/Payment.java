// src/main/java/concordia/soen6611/igo_tvm/Models/Payment.java
package concordia.soen6611.igo_tvm.models;

public class Payment {
    private String method; // "Cash" or "Card"
    private double amount;
    private String status; // "Pending", "Processing", "Completed", "Cancelled"

    public Payment(String method, double amount) {
        this.method = method;
        this.amount = amount;
        this.status = "Pending";
    }

    public String getMethod() { return method; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }

    public void setMethod(String method) { this.method = method; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setStatus(String status) { this.status = status; }
}
