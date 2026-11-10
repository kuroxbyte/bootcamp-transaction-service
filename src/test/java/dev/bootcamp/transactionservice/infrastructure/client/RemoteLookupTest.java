package dev.bootcamp.transactionservice.infrastructure.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comportamiento del circuit breaker en las consultas a otros servicios.
 */
class RemoteLookupTest {

    private final RemoteLookup remoteLookup =
            TestCircuitBreakers.remoteLookup(Duration.ofMillis(300));

    private static Mono<String> call(StubWebClient stub) {
        WebClient client = stub.builder().baseUrl("http://remote").build();
        return client.get().uri("/resource").retrieve().bodyToMono(String.class);
    }

    @Test
    void returnsResourceWhenServiceResponds() {
        StubWebClient stub = StubWebClient.respondingWith(HttpStatus.OK, "ok");

        remoteLookup.findById(call(stub), "remote-service", "1").test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertValue("ok");
    }

    @Test
    void failsAsUnavailableWhenCallExceedsTimeout() {
        long start = System.nanoTime();

        remoteLookup.findById(call(StubWebClient.neverResponding()), "slow-service", "1").test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertError(ServiceUnavailableException.class);

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(1));
    }

    @Test
    void opensCircuitAfterRepeatedFailuresAndStopsCallingTheService() {
        StubWebClient stub = StubWebClient.respondingWith(HttpStatus.INTERNAL_SERVER_ERROR, "{}");

        for (int i = 0; i < 2; i++) {
            remoteLookup.findById(call(stub), "failing-service", "1").test()
                    .awaitDone(2, TimeUnit.SECONDS)
                    .assertError(ServiceUnavailableException.class);
        }
        remoteLookup.findById(call(stub), "failing-service", "1").test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertError(ServiceUnavailableException.class);

        assertThat(stub.requestCount()).isEqualTo(2);
    }

    @Test
    void notFoundDoesNotOpenCircuit() {
        StubWebClient stub = StubWebClient.respondingWith(HttpStatus.NOT_FOUND, "{}");

        for (int i = 0; i < 3; i++) {
            remoteLookup.findById(call(stub), "lookup-service", "1").test()
                    .awaitDone(2, TimeUnit.SECONDS)
                    .assertComplete()
                    .assertNoValues();
        }

        assertThat(stub.requestCount()).isEqualTo(3);
    }
}
