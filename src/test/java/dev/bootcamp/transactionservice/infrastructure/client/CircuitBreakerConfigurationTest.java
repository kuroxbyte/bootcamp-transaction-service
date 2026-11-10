package dev.bootcamp.transactionservice.infrastructure.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que los circuit breakers usen el timeout de 2 segundos definido en config-repo.
 */
@SpringBootTest
class CircuitBreakerConfigurationTest {

    @Autowired
    private ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Test
    void callsTimeOutAfterTwoSeconds() {
        long start = System.nanoTime();

        String result = circuitBreakerFactory.create("config-check")
                .run(Mono.<String>never(), error -> Mono.just("timeout"))
                .block(Duration.ofSeconds(5));

        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
        assertThat(result).isEqualTo("timeout");
        assertThat(elapsed).isBetween(Duration.ofMillis(1900), Duration.ofMillis(3000));
    }
}
