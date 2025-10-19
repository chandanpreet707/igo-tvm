package concordia.soen6611.igo_tvm.Services;

import concordia.soen6611.igo_tvm.models.OrderSummary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("singleton")
public class PaymentSession {
    public enum Origin { BUY_TICKET, RELOAD_CARD }
    private OrderSummary currentOrder;
    private Origin origin = Origin.BUY_TICKET; // default/fallback

    public void setCurrentOrder(OrderSummary order) { this.currentOrder = order; }
    public OrderSummary getCurrentOrder() { return currentOrder; }
    public void setOrigin(Origin origin) { this.origin = origin; }
    public Origin getOrigin()            { return origin; }
    public void clear() {
        currentOrder = null;
        origin = Origin.BUY_TICKET;
    }
}
