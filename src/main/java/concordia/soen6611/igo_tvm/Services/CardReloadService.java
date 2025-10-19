package concordia.soen6611.igo_tvm.Services;

import concordia.soen6611.igo_tvm.exceptions.NetworkException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Service that encapsulates card-reading logic and fare lookup.
 */
@Service
public class CardReloadService {
    private final FareRateService fareRateService;

    public CardReloadService(FareRateService fareRateService) {
        this.fareRateService = fareRateService;
    }

    /**
     * Async card read simulation.
     * @param simulateNetworkFailure if true, completes exceptionally with a NetworkException
     * @return CompletableFuture that completes normally on success or exceptionally with AbstractCustomException
     */
    public CompletableFuture<Void> readCardAsync(boolean simulateNetworkFailure) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Simulate work / delay
                Thread.sleep(5000);
                if (simulateNetworkFailure) {
                    // simulate an IO timeout as the root cause
                    throw new NetworkException("Network unavailable while reading card", new IOException("Simulated timeout"));
                }
                // otherwise success (no return value)
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new CompletionException(ie);
            }
        });
    }

    /**
     * Delegate to FareRateService
     */
    public double getFare(String riderType, String passType) {
        return fareRateService.getRate(riderType, passType);
    }
}

