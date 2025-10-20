package concordia.soen6611.igo_tvm.Services;

public interface FareRateService {
    double getRate(String riderType, String passType);

    double getTax();

    double getGST();

    double getQST();
}
