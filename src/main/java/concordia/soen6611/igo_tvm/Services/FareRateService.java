package concordia.soen6611.igo_tvm.Services;

/**
 * Abstraction for retrieving fare pricing and tax information used by the kiosk.
 * <p>
 * Typical implementations may source rates from an in-memory cache, a database,
 * or a remote API. Consumers (controllers/services) use this interface to:
 * <ul>
 *   <li>Obtain the base fare for a rider/pass combination.</li>
 *   <li>Retrieve applicable sales tax fractions (GST, QST) and their combined rate.</li>
 * </ul>
 * <p>
 * <strong>Units &amp; semantics:</strong>
 * <ul>
 *   <li>All monetary values are in the kiosk's currency (e.g., CAD) unless otherwise noted.</li>
 *   <li>Tax getters return fractional rates (e.g., {@code 0.05} for 5%).</li>
 * </ul>
 */
public interface FareRateService {

    /**
     * Returns the base fare for a given rider and pass type.
     *
     * @param riderType rider category (e.g., {@code "Adult"}, {@code "Student"}, {@code "Senior"})
     * @param passType  pass/trip type (e.g., {@code "Single Trip"}, {@code "Weekly Pass"})
     * @return the base fare amount; implementations may return {@code 0.0} if unknown
     */
    double getRate(String riderType, String passType);

    /**
     * Returns the combined sales tax fraction applied to fares.
     * <p>Typically {@code GST + QST} (e.g., {@code 0.14975} for 14.975%).</p>
     *
     * @return total tax fraction
     */
    double getTax();

    /**
     * Returns the Goods and Services Tax (GST) fraction.
     *
     * @return GST as a fraction (e.g., {@code 0.05} for 5%)
     */
    double getGST();

    /**
     * Returns the Quebec Sales Tax (QST) fraction.
     *
     * @return QST as a fraction (e.g., {@code 0.09975} for 9.975%)
     */
    double getQST();
}
