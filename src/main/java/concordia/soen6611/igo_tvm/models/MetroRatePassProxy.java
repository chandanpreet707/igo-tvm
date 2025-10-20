// `src/main/java/concordia/soen6611/igo_tvm/cache/MetroPassRateProxy.java`
package concordia.soen6611.igo_tvm.models;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class MetroRatePassProxy {
    private final Map<String, Double> rateCache = new ConcurrentHashMap<>();
    private final double GST  = 0.05;
    private final double QST  = 0.09975;
    private final double TAX_RATE = GST + QST;
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

    public double getRate(String key) {
        return rateCache.getOrDefault(key, 0.0);
    }

    public void setRate(String key, double rate) {
        rateCache.put(key, rate);
    }

    public void clearRates() {
        rateCache.clear();
    }

    public double getGST() {
        return GST;
    }

    public double getQST() {
        return QST;
    }

    public double getTAX_RATE() {
        return TAX_RATE;
    }
}
