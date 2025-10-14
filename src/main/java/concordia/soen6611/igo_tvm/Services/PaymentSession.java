package concordia.soen6611.igo_tvm.Services;

import concordia.soen6611.igo_tvm.models.OrderSummary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("singleton")
public class PaymentSession {
    private OrderSummary currentOrder;

    public void setCurrentOrder(OrderSummary order) { this.currentOrder = order; }
    public OrderSummary getCurrentOrder() { return currentOrder; }

    public void clear() { currentOrder = null; }
}
