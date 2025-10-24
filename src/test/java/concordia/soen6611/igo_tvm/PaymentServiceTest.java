package concordia.soen6611.igo_tvm;

import concordia.soen6611.igo_tvm.Services.PaymentService;
import concordia.soen6611.igo_tvm.models.Payment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PaymentServiceTest {

    @Test
    void startProcessCancel_flowUpdatesStatus() {
        PaymentService ps = new PaymentService();

        ps.startPayment("Card", 12.34);
        Payment p = ps.getCurrentPayment();
        assertNotNull(p);
        assertEquals("Card", p.getMethod());
        assertEquals(12.34, p.getAmount(), 1e-9);
        assertEquals("Pending", p.getStatus());

        ps.processPayment();
        assertEquals("Completed", ps.getCurrentPayment().getStatus());

        ps.cancelPayment();
        assertEquals("Cancelled", ps.getCurrentPayment().getStatus());
    }
}

