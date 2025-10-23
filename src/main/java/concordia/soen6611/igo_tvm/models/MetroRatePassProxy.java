// `src/main/java/concordia/soen6611/igo_tvm/cache/MetroPassRateProxy.java`
package concordia.soen6611.igo_tvm.models;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * In-memory proxy/cache for metro pass base rates and tax constants.
 * <p>
 * This class provides a simple, thread-safe rate lookup backed by a
 * {@link ConcurrentHashMap}. It is intended as a lightweight stand-in for
 * a data source such as a database or remote API. Seed data is populated
 * in the constructor and can be updated at runtime via {@link #setRate(String, double)}.
 * </p>
 *
 * <h3>Key format</h3>
 * <p>
 * Rate keys follow the pattern:
 * <pre>
 *   &lt;RiderType&gt;_&lt;PassType&gt;
 * </pre>
 * Examples:
 * <ul>
 *   <li>{@code "Adult_Single Trip"}</li>
 *   <li>{@code "Student_Monthly Pass"}</li>
 *   <li>{@code "Senior_Weekend Pass"}</li>
 * </ul>
 * </p>
 *
 * <h3>Thread safety</h3>
 * <p>
 * The underlying cache is a {@link ConcurrentHashMap}, so concurrent reads and
 * writes are supported. There is no additional synchronization beyond that.
 * </p>
 *
 * <h3>Taxes</h3>
 * <p>
 * The proxy also exposes GST and QST constants and their combined tax rate.
 * These values are fixed within the lifetime of the instance.
 * </p>
 */
public class MetroRatePassProxy {

    /** Thread-safe cache of rates keyed by {@code "<RiderType>_<PassType>"}. */
    private final Map<String, Double> rateCache = new ConcurrentHashMap<>();

    /** Federal Goods and Services Tax (GST). */
    private final double GST  = 0.05;

    /** Quebec Sales Tax (QST). */
    private final double QST  = 0.09975;

    /** Combined tax rate (GST + QST). */
    private final double TAX_RATE = GST + QST;

    /**
     * Creates a new proxy with sample seed data.
     * <p>
     * Replace or augment these values by fetching from a persistent data source
     * (e.g., DB/API) as needed in your environment.
     * </p>
     */
    public MetroRatePassProxy() {
        // Sample data; replace with DB/API fetch
        rateCache.put("Adult_Single Trip", 3.75);
        rateCache.put("Adult_Day Pass", 11.00);
        rateCache.put("Adult_Monthly Pass", 94.00);
        rateCache.put("Adult_Weekly Pass", 25.00);
        rateCache.put("Adult_Weekend Pass", 14.00);

        rateCache.put("Student_Single Trip", 3.00);
        rateCache.put("Student_Day Pass", 8.00);
        rateCache.put("Student_Monthly Pass", 70.00);
        rateCache.put("Student_Weekly Pass", 20.00);
        rateCache.put("Student_Weekend Pass", 10.00);

        rateCache.put("Senior_Single Trip", 2.50);
        rateCache.put("Senior_Day Pass", 7.00);
        rateCache.put("Senior_Monthly Pass", 60.00);
        rateCache.put("Student_Weekly Pass", 20.00);
        rateCache.put("Senior_Weekend Pass", 9.00);

        rateCache.put("Tourist_Single Trip", 4.00);
        rateCache.put("Tourist_Day Pass", 12.00);
        rateCache.put("Tourist_Monthly Pass", 99.00);
        rateCache.put("Tourist_Weekly Pass", 30.00);
        rateCache.put("Tourist_Weekend Pass", 16.00);
    }

    /**
     * Retrieves a base rate for the given key.
     *
     * @param key rate key in the form {@code "<RiderType>_<PassType>"}
     * @return the rate if present, otherwise {@code 0.0}
     */
    public double getRate(String key) {
        return rateCache.getOrDefault(key, 0.0);
    }

    /**
     * Inserts or updates a rate for the given key.
     *
     * @param key   rate key in the form {@code "<RiderType>_<PassType>"}
     * @param rate  non-negative base price to associate with the key
     */
    public void setRate(String key, double rate) {
        rateCache.put(key, rate);
    }

    /**
     * Clears all cached rates.
     * <p>
     * Useful when reloading rates from an external source.
     * </p>
     */
    public void clearRates() {
        rateCache.clear();
    }

    /**
     * @return the GST fraction (e.g., {@code 0.05} for 5%)
     */
    public double getGST() {
        return GST;
    }

    /**
     * @return the QST fraction (e.g., {@code 0.09975} for 9.975%)
     */
    public double getQST() {
        return QST;
    }

    /**
     * @return the combined tax fraction ({@code GST + QST})
     */
    public double getTAX_RATE() {
        return TAX_RATE;
    }
}
