
package concordia.soen6611.igo_tvm.Services;

import concordia.soen6611.igo_tvm.models.Payment;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    private Payment currentPayment;

    public void startPayment(String method, double amount) {
        currentPayment = new Payment(method, amount);
    }

    public Payment getCurrentPayment() {
        return currentPayment;
    }

    public void processPayment() {
        if (currentPayment != null) {
            currentPayment.setStatus("Processing");
            // Simulate processing
            currentPayment.setStatus("Completed");
        }
    }

    public void cancelPayment() {
        if (currentPayment != null) {
            currentPayment.setStatus("Cancelled");
        }
    }
}