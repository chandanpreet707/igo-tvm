package concordia.soen6611.igo_tvm.models;

public class OrderSummary {
    private final String riderType;   // Adult / Student / Senior /
    private final String tripType;    // Single Trip / Multiple Trip / Day Pass / Monthly Pass / Weekend Pass
    private final int    multiTrips;  // 1..10 (1 for non-Multiple)
    private final int    quantity;    // number of tickets
    private final double unitPrice;   // price for ONE ticket (already scaled if Multiple Trip)
    private final double total;       // unitPrice * quantity

    public OrderSummary(String riderType, String tripType, int multiTrips, int quantity, double unitPrice) {
        this.riderType = riderType;
        this.tripType = tripType;
        this.multiTrips = multiTrips;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.total = unitPrice * quantity;
    }

    public String getRiderType() { return riderType; }
    public String getTripType() { return tripType; }
    public int getMultiTrips() { return multiTrips; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getTotal() { return total; }
}
