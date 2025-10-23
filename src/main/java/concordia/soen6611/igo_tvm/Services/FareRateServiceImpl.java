package concordia.soen6611.igo_tvm.Services;

import concordia.soen6611.igo_tvm.models.MetroRatePassProxy;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link FareRateService} that retrieves fares and tax
 * fractions from an in-memory {@link MetroRatePassProxy}.
 * <p>
 * This implementation composes a lookup key in the form
 * {@code "<riderType>_<passType>"} (e.g., {@code "Adult_Single Trip"}) and
 * delegates all values to the proxy. It is suitable for demos or as a simple
 * cache-backed provider; replace the proxy with a DB/API-backed source for
 * production.
 * </p>
 */
@Service
public class FareRateServiceImpl implements FareRateService {

    /** In-memory proxy/cache used to serve rate and tax data. */
    private final MetroRatePassProxy rateProxy = new MetroRatePassProxy();

    /**
     * Returns the base fare for the given rider and pass type by composing a key
     * {@code "<riderType>_<passType>"} and querying {@link MetroRatePassProxy}.
     *
     * @param riderType rider category (e.g., {@code "Adult"}, {@code "Student"}, {@code "Senior"})
     * @param passType  pass/trip type (e.g., {@code "Single Trip"}, {@code "Weekly Pass"})
     * @return base fare amount, or {@code 0.0} if the key is unknown
     */
    @Override
    public double getRate(String riderType, String passType) {
        // Compose key, e.g. "Adult_SingleTrip"
        String key = riderType + "_" + passType;
        return rateProxy.getRate(key);
    }

    /**
     * Returns the combined sales tax fraction (e.g., {@code GST + QST}).
     *
     * @return total tax fraction
     */
    @Override
    public double getTax(){
        return rateProxy.getTAX_RATE();
    }

    /**
     * Returns the Goods and Services Tax (GST) fraction.
     *
     * @return GST as a fraction (e.g., {@code 0.05})
     */
    @Override
    public double getGST() {
        return rateProxy.getGST();
    }

    /**
     * Returns the Quebec Sales Tax (QST) fraction.
     *
     * @return QST as a fraction (e.g., {@code 0.09975})
     */
    @Override
    public double getQST() {
        return rateProxy.getQST();
    }
}
