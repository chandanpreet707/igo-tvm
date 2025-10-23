package concordia.soen6611.igo_tvm.Services;

import concordia.soen6611.igo_tvm.models.OrderSummary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * Session-scoped container for the current purchase flow.
 * <p>
 * Stores the active {@link OrderSummary} and the originating flow
 * (buying a new ticket vs. reloading a card). Controllers use this
 * to pass order details across screens until the flow is completed.
 * </p>
 *
 * <h3>Lifecycle</h3>
 * <ul>
 *   <li>Set {@link #origin} at the start of a flow.</li>
 *   <li>Populate {@link #currentOrder} before navigating to payment.</li>
 *   <li>Call {@link #clear()} after success/cancel to reset defaults.</li>
 * </ul>
 */
@Service
@Scope("singleton")
public class PaymentSession {

    /**
     * Identifies which user journey created the session state.
     */
    public enum Origin { BUY_TICKET, RELOAD_CARD }

    /** The current order being processed (may be {@code null} if none). */
    private OrderSummary currentOrder;

    /** The flow that created this session; defaults to {@link Origin#BUY_TICKET}. */
    private Origin origin = Origin.BUY_TICKET; // default/fallback

    /**
     * Sets the current order summary for this session.
     *
     * @param order the active order (may be {@code null} to clear)
     */
    public void setCurrentOrder(OrderSummary order) { this.currentOrder = order; }

    /**
     * Returns the current order summary.
     *
     * @return the active {@link OrderSummary}, or {@code null} if none
     */
    public OrderSummary getCurrentOrder() { return currentOrder; }

    /**
     * Sets the originating flow for this session.
     *
     * @param origin the origin (e.g., {@link Origin#BUY_TICKET})
     */
    public void setOrigin(Origin origin) { this.origin = origin; }

    /**
     * Returns the originating flow for this session.
     *
     * @return the {@link Origin} value
     */
    public Origin getOrigin()            { return origin; }

    /**
     * Clears all session state and resets the origin to {@link Origin#BUY_TICKET}.
     * <p>
     * Call this after the user completes or cancels a transaction.
     * </p>
     */
    public void clear() {
        currentOrder = null;
        origin = Origin.BUY_TICKET;
    }
}
