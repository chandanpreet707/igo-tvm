package concordia.soen6611.igo_tvm.models;

/**
 * Immutable value object summarizing a user's ticket/pass purchase.
 * <p>
 * Captures the rider category, trip/pass type, multiplicity (for multi-trip products),
 * quantity, unit price, and computed total for the order at the moment it is created.
 * This model is typically stored in session and consumed by payment flows and receipts.
 *
 * <h3>Field semantics</h3>
 * <ul>
 *   <li><b>riderType</b> — e.g., "Adult", "Student", "Senior", "Tourist".</li>
 *   <li><b>tripType</b> — e.g., "Single Trip", "Day Pass", "Monthly Pass", "Weekend Pass".</li>
 *   <li><b>multiTrips</b> — number of trips for multi-trip products (use {@code 1} for non-multiple).</li>
 *   <li><b>quantity</b> — number of items purchased (tickets/passes).</li>
 *   <li><b>unitPrice</b> — price for one item; already scaled if using a multi-trip product.</li>
 *   <li><b>total</b> — total price for the order (typically {@code unitPrice * quantity}).</li>
 * </ul>
 *
 * <p>All fields are final; instances are thread-safe after construction.</p>
 */
public class OrderSummary {
    /** Rider category (e.g., Adult / Student / Senior / Tourist). */
    private final String riderType;   // Adult / Student / Senior / Tourist
    /** Trip/pass type (e.g., Single Trip / Day Pass / Monthly Pass / Weekend Pass). */
    private final String tripType;    // Single Trip / Multiple Trip / Day Pass / Monthly Pass / Weekend Pass
    /** Number of trips for multi-trip products; {@code 1} for non-multiple. */
    private final int    multiTrips;  // 1..10 (1 for non-Multiple)
    /** Number of items (tickets/passes) purchased. */
    private final int    quantity;    // number of tickets
    /** Price for one item; for multi-trip products this is the already-scaled per-item price. */
    private final double unitPrice;   // price for ONE ticket (already scaled if Multiple Trip)
    /** Total order amount; typically {@code unitPrice * quantity}. */
    private final double total;       // unitPrice * quantity

    /**
     * Constructs an immutable summary of a purchase.
     *
     * @param riderType  rider category (e.g., {@code "Adult"}, {@code "Student"})
     * @param tripType   trip/pass type (e.g., {@code "Single Trip"}, {@code "Monthly Pass"})
     * @param multiTrips number of trips for a multi-trip product; use {@code 1} for others
     * @param quantity   number of items purchased (must be {@code >= 1})
     * @param unitPrice  price for a single item (already scaled if multi-trip)
     * @param total      total cost for the order (usually {@code unitPrice * quantity})
     */
    public OrderSummary(String riderType, String tripType, int multiTrips, int quantity, double unitPrice, double total) {
        this.riderType = riderType;
        this.tripType = tripType;
        this.multiTrips = multiTrips;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.total = total;
    }

    /** @return the rider category (e.g., Adult, Student) */
    public String getRiderType() { return riderType; }

    /** @return the trip/pass type (e.g., Single Trip, Monthly Pass) */
    public String getTripType() { return tripType; }

    /** @return number of trips for multi-trip products; {@code 1} for non-multiple */
    public int getMultiTrips() { return multiTrips; }

    /** @return quantity of items in the order */
    public int getQuantity() { return quantity; }

    /** @return unit price for a single item (already scaled if multi-trip) */
    public double getUnitPrice() { return unitPrice; }

    /** @return total order amount */
    public double getTotal() { return total; }
}
