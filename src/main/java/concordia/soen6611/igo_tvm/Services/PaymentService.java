package concordia.soen6611.igo_tvm.Services;

import concordia.soen6611.igo_tvm.models.Payment;
import org.springframework.stereotype.Service;

/**
 * Service layer component responsible for orchestrating the lifecycle of a {@link Payment}.
 * <p>
 * This simple implementation holds a single in-memory {@link Payment} instance and
 * provides methods to start, process, and cancel the payment. It is primarily used
 * by controllers to drive UI state in the kiosk flow.
 * </p>
 *
 * <h3>Lifecycle</h3>
 * <pre>
 * startPayment(...) -> processPayment() -> getCurrentPayment()
 *                          \-------------------------------> cancelPayment()
 * </pre>
 *
 * <p><strong>Note:</strong> No external I/O or gateway integration is performed here;
 * status transitions are simulated and intended for demo/testing flows.</p>
 */
@Service
public class PaymentService {
    /** Currently active payment instance for the user/session. */
    private Payment currentPayment;

    /**
     * Creates a new {@link Payment} with the given method and amount and sets its
     * initial status to {@code "Pending"}.
     *
     * @param method human-readable payment method (e.g., {@code "Card"}, {@code "Cash"})
     * @param amount amount to charge/collect
     */
    public void startPayment(String method, double amount) {
        currentPayment = new Payment(method, amount);
    }

    /**
     * Returns the current {@link Payment} instance if one has been started.
     *
     * @return the active payment, or {@code null} if no payment is in progress
     */
    public Payment getCurrentPayment() {
        return currentPayment;
    }

    /**
     * Simulates payment processing by setting the status to {@code "Processing"}
     * and then immediately to {@code "Completed"}.
     * <p>
     * If no payment is active, this method is a no-op.
     * </p>
     */
    public void processPayment() {
        if (currentPayment != null) {
            currentPayment.setStatus("Processing");
            // Simulate processing
            currentPayment.setStatus("Completed");
        }
    }

    /**
     * Cancels the current payment by setting its status to {@code "Cancelled"}.
     * <p>
     * If no payment is active, this method is a no-op.
     * </p>
     */
    public void cancelPayment() {
        if (currentPayment != null) {
            currentPayment.setStatus("Cancelled");
        }
    }
}
