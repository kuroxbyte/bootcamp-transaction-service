package dev.bootcamp.transactionservice.infrastructure.client;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** WebClient de prueba que responde sin red y guarda la última petición recibida. */
public final class StubWebClient {

    private final AtomicReference<ClientRequest> lastRequest = new AtomicReference<>();
    private final WebClient.Builder builder;

    private StubWebClient(Function<ClientRequest, Mono<ClientResponse>> responder) {
        this.builder = WebClient.builder().exchangeFunction(request -> {
            lastRequest.set(request);
            return responder.apply(request);
        });
    }

    public static StubWebClient respondingWith(HttpStatus status, String json) {
        return new StubWebClient(request -> Mono.just(ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .build()));
    }

    public static StubWebClient failingWith(Throwable error) {
        return new StubWebClient(request -> Mono.error(error));
    }

    public static StubWebClient neverResponding() {
        return new StubWebClient(request -> Mono.never());
    }

    public WebClient.Builder builder() {
        return builder;
    }

    public String lastPath() {
        return lastRequest.get().url().getPath();
    }
}
