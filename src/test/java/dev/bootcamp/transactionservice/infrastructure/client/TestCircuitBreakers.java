package dev.bootcamp.transactionservice.infrastructure.client;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigurationProperties;

import java.time.Duration;

/**
 * Circuit breakers de prueba: se abren tras dos fallas y usan el tiempo de espera indicado.
 */
public final class TestCircuitBreakers {

    private TestCircuitBreakers() {
    }

    /**
     * Crea un {@link RemoteLookup} con su propia fábrica de circuit breakers.
     *
     * @param timeout tiempo máximo de espera de cada llamada
     * @return la consulta remota protegida
     */
    public static RemoteLookup remoteLookup(Duration timeout) {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build();
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(timeout)
                .build();
        ReactiveResilience4JCircuitBreakerFactory factory =
                new ReactiveResilience4JCircuitBreakerFactory(
                        CircuitBreakerRegistry.of(circuitBreakerConfig),
                        TimeLimiterRegistry.of(timeLimiterConfig),
                        null,
                        new Resilience4JConfigurationProperties());
        factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(circuitBreakerConfig)
                .timeLimiterConfig(timeLimiterConfig)
                .build());
        return new RemoteLookup(factory);
    }
}
