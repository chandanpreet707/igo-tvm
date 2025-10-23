// src/main/java/concordia/soen6611/igo_tvm/Models/Payment.java
package concordia.soen6611.igo_tvm.models;

/**
 * Simple mutable model representing a payment in the kiosk flow.
 * <p>
 * A {@code Payment} tracks the selected payment method, the target amount,
 * and the current processing status. Typical lifecycle transitions are:
 * <pre>
 *   "Pending"  -> "Processing" -> "Completed"
 *                 \-------------------------> "Cancelled"
 * </pre>
 * <b>Note:</b> This class contains no business logic or validation; it is a data
 * holder used by services/controllers (e.g., to drive UI state).
 */
public class Payment {
    /** Payment method label (e.g., {@code "Cash"}, {@code "Card"}, {@code "MobileWallet"}). */
    private String method; // "Cash" or "Card"
    /** Total amount to be collected for this payment (in currency units). */
    private double amount;
    /** Processing status: e.g., {@code "Pending"}, {@code "Processing"}, {@code "Completed"}, {@code "Cancelled"}. */
    private String status; // "Pending", "Processing", "Completed", "Cancelled"

    /**
     * Constructs a new payment in {@code "Pending"} state.
     *
     * @param method human-readable payment method label (e.g., {@code "Card"})
     * @param amount amount to charge/collect
     */
    public Payment(String method, double amount) {
        this.method = method;
        this.amount = amount;
        this.status = "Pending";
    }

    /**
     * Returns the payment method.
     *
     * @return method label, such as {@code "Cash"} or {@code "Card"}
     */
    public String getMethod() { return method; }

    /**
     * Returns the target amount.
     *
     * @return amount to be charged/collected
     */
    public double getAmount() { return amount; }

    /**
     * Returns the current payment status.
     *
     * @return status string (e.g., {@code "Pending"}, {@code "Completed"})
     */
    public String getStatus() { return status; }

    /**
     * Sets the payment method label.
     *
     * @param method human-readable method (e.g., {@code "Card"})
     */
    public void setMethod(String method) { this.method = method; }

    /**
     * Sets the payment amount.
     *
     * @param amount new amount to charge/collect
     */
    public void setAmount(double amount) { this.amount = amount; }

    /**
     * Updates the payment status.
     *
     * @param status new status value (e.g., {@code "Processing"}, {@code "Cancelled"})
     */
    public void setStatus(String status) { this.status = status; }
}
