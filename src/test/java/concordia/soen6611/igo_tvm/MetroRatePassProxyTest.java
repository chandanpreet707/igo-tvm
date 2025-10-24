package concordia.soen6611.igo_tvm;

import concordia.soen6611.igo_tvm.models.MetroRatePassProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetroRatePassProxyTest {

    @Test
    void knownRates_presentAndPositive() {
        MetroRatePassProxy proxy = new MetroRatePassProxy();
        assertTrue(proxy.getRate("Adult_Single Trip") > 0.0);
        assertTrue(proxy.getRate("Senior_Day Pass") > 0.0);
    }

    @Test
    void unknownRate_defaultsToZero() {
        MetroRatePassProxy proxy = new MetroRatePassProxy();
        assertEquals(0.0, proxy.getRate("Unknown_Key"));
    }

    @Test
    void taxes_constantsAreExposed() {
        MetroRatePassProxy proxy = new MetroRatePassProxy();
        assertTrue(proxy.getGST() > 0.0);
        assertTrue(proxy.getQST() > 0.0);
        assertEquals(proxy.getGST() + proxy.getQST(), proxy.getTAX_RATE(), 1e-9);
    }
}

