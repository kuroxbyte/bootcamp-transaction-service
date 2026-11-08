package dev.bootcamp.transactionservice.infrastructure.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * WebClient de prueba que responde sin red y guarda la última petición recibida.
 */
public final class StubWebClient {

    private final AtomicReference<ClientRequest> lastRequest = new AtomicReference<>();
    private final WebClient.Builder builder;

    private StubWebClient(Function<ClientRequest, Mono<ClientResponse>> responder) {
        this.builder =
                WebClient.builder()
                        .exchangeFunction(
                                request -> {
                                    lastRequest.set(request);
                                    return responder.apply(request);
                                });
    }

    /**
     * Crea un stub que responde siempre con el estado y el JSON indicados.
     */
    public static StubWebClient respondingWith(HttpStatus status, String json) {
        return new StubWebClient(
                request ->
                        Mono.just(
                                ClientResponse.create(status)
                                        .header(
                                                HttpHeaders.CONTENT_TYPE,
                                                MediaType.APPLICATION_JSON_VALUE)
                                        .body(json)
                                        .build()));
    }

    /**
     * Crea un stub cuya llamada falla siempre con el error indicado.
     */
    public static StubWebClient failingWith(Throwable error) {
        return new StubWebClient(request -> Mono.error(error));
    }

    /**
     * Crea un stub que nunca responde, para probar el tiempo de espera.
     */
    public static StubWebClient neverResponding() {
        return new StubWebClient(request -> Mono.never());
    }

    /**
     * Builder de WebClient que usa este stub en lugar de la red.
     */
    public WebClient.Builder builder() {
        return builder;
    }

    /**
     * Ruta de la última petición recibida.
     */
    public String lastPath() {
        return lastRequest.get().url().getPath();
    }
}
