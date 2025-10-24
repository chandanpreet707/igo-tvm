package concordia.soen6611.igo_tvm;

import concordia.soen6611.igo_tvm.Services.CardReloadService;
import concordia.soen6611.igo_tvm.Services.FareRateService;
import concordia.soen6611.igo_tvm.Services.FareRateServiceImpl;
import concordia.soen6611.igo_tvm.exceptions.NetworkException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardReloadServiceTest {

    private final FareRateService rates = new FareRateServiceImpl();
    private final CardReloadService svc = new CardReloadService(rates);

    @Test
    void readCardAsync_successCompletesNormally() throws Exception {
        CompletableFuture<Void> f = svc.readCardAsync(false);
        f.get(); // should not throw
        assertTrue(f.isDone());
    }

    @Test
    void readCardAsync_simulatedNetworkFailureThrowsNetworkException() {
        CompletableFuture<Void> f = svc.readCardAsync(true);
        ExecutionException ex = assertThrows(ExecutionException.class, f::get);
        Throwable cause = ex.getCause();
        assertTrue(cause instanceof NetworkException);
        assertTrue(cause.getMessage().toLowerCase().contains("network"));
    }

    @Test
    void getFare_delegatesToRateService() {
        double fare = svc.getFare("Adult", "Day Pass");
        assertTrue(fare > 0.0);
    }
}

