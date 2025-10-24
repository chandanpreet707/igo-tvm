package concordia.soen6611.igo_tvm;

import concordia.soen6611.igo_tvm.Services.PaymentSession;
import concordia.soen6611.igo_tvm.models.OrderSummary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentSessionTest {

    @Test
    void setGetClear_behavesAsExpected() {
        PaymentSession s = new PaymentSession();

        assertEquals(PaymentSession.Origin.BUY_TICKET, s.getOrigin());
        assertNull(s.getCurrentOrder());

        s.setOrigin(PaymentSession.Origin.RELOAD_CARD);
        s.setCurrentOrder(new OrderSummary("Adult", "Single Trip", 1, 2, 3.75, 7.50));

        assertEquals(PaymentSession.Origin.RELOAD_CARD, s.getOrigin());
        assertNotNull(s.getCurrentOrder());
        assertEquals(7.50, s.getCurrentOrder().getTotal(), 1e-9);

        s.clear();
        assertEquals(PaymentSession.Origin.BUY_TICKET, s.getOrigin());
        assertNull(s.getCurrentOrder());
    }
}

