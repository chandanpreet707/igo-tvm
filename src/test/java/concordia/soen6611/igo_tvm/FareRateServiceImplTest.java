package concordia.soen6611.igo_tvm;

import concordia.soen6611.igo_tvm.Services.FareRateService;
import concordia.soen6611.igo_tvm.Services.FareRateServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FareRateServiceImplTest {

    private final FareRateService svc = new FareRateServiceImpl();

    @Test
    void getRate_knownKeys_returnPositive() {
        assertTrue(svc.getRate("Adult", "Single Trip") > 0.0);
        assertTrue(svc.getRate("Student", "Monthly Pass") > 0.0);
    }

    @Test
    void getRate_unknownKey_returnsZero() {
        assertEquals(0.0, svc.getRate("Unknown", "Nope"));
    }

    @Test
    void taxes_areConsistent() {
        double gst = svc.getGST();
        double qst = svc.getQST();
        double tax = svc.getTax();

        assertTrue(gst > 0.0);
        assertTrue(qst > 0.0);
        assertEquals(gst + qst, tax, 1e-9);
    }
}

