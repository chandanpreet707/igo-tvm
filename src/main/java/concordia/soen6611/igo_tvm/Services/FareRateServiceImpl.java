package concordia.soen6611.igo_tvm.Services;

import concordia.soen6611.igo_tvm.models.MetroRatePassProxy;
import org.springframework.stereotype.Service;

@Service
public class FareRateServiceImpl implements FareRateService {
    private final MetroRatePassProxy rateProxy = new MetroRatePassProxy();

    @Override
    public double getRate(String riderType, String passType) {
        // Compose key, e.g. "Adult_SingleTrip"
        String key = riderType + "_" + passType;
        return rateProxy.getRate(key);
    }
}
