package concordia.soen6611.igo_tvm;

import concordia.soen6611.igo_tvm.models.OrderSummary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderSummaryTest {

    @Test
    void gettersReturnConstructorValues() {
        OrderSummary os = new OrderSummary("Student", "Weekly Pass", 1, 3, 20.0, 60.0);
        assertEquals("Student", os.getRiderType());
        assertEquals("Weekly Pass", os.getTripType());
        assertEquals(1, os.getMultiTrips());
        assertEquals(3, os.getQuantity());
        assertEquals(20.0, os.getUnitPrice(), 1e-9);
        assertEquals(60.0, os.getTotal(), 1e-9);
    }
}
