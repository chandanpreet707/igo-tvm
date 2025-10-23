package concordia.soen6611.igo_tvm.Services;

import concordia.soen6611.igo_tvm.exceptions.NetworkException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Service that encapsulates OPUS card–reading logic and fare lookup used by the
 * Card Reload flow.
 * <p>
 * This implementation provides an asynchronous simulation of card reading
 * (useful for UI flows and error handling tests) and delegates fare retrieval
 * to {@link FareRateService}. Replace the simulation with real device I/O when
 * integrating a hardware reader.
 * </p>
 *
 * <h3>Threading</h3>
 * <p>
 * {@link #readCardAsync(boolean)} executes work on a common fork-join thread via
 * {@link CompletableFuture#runAsync(Runnable)}. Callers should switch back to the
 * JavaFX Application Thread (e.g., via {@code Platform.runLater}) before touching UI.
 * </p>
 */
@Service
public class CardReloadService {
    /** Fare lookup delegate. */
    private final FareRateService fareRateService;

    /**
     * Creates a new {@code CardReloadService}.
     *
     * @param fareRateService service used to compute or retrieve fares
     */
    public CardReloadService(FareRateService fareRateService) {
        this.fareRateService = fareRateService;
    }

    /**
     * Simulates an asynchronous OPUS card read.
     * <p>
     * The task sleeps for ~5 seconds to emulate device latency, then either completes normally
     * or fails with a {@link NetworkException} (optionally wrapping a simulated {@link IOException})
     * when {@code simulateNetworkFailure} is {@code true}. If the thread is interrupted, the method
     * propagates an {@link InterruptedException} wrapped in a {@link CompletionException}.
     * </p>
     *
     * @param simulateNetworkFailure when {@code true}, the returned future completes exceptionally
     *                               with a {@link NetworkException}; otherwise completes normally
     * @return a {@link CompletableFuture} that completes with {@code null} on success or exceptionally
     *         with an {@code AbstractCustomException} (e.g., {@link NetworkException}) or a
     *         {@link CompletionException} wrapping {@link InterruptedException}
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
     * Retrieves a fare for a given rider and pass type by delegating to {@link FareRateService}.
     *
     * @param riderType rider category (e.g., "Adult", "Student", "Senior")
     * @param passType  pass/trip type (e.g., "Single Trip", "Weekly Pass")
     * @return the base fare amount for the specified rider and pass type
     */
    public double getFare(String riderType, String passType) {
        return fareRateService.getRate(riderType, passType);
    }
}
