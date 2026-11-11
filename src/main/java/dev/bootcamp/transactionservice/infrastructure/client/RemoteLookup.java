package dev.bootcamp.transactionservice.infrastructure.client;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Consulta un recurso por id en otro microservicio, protegida con un circuit breaker.
 *
 * <ul>
 *   <li>404: el recurso no existe; se emite vacío y no cuenta como falla del circuito.</li>
 *   <li>Error de conexión, otro código de error, tiempo de espera agotado (2 s, configurado en
 *       config-repo) o circuito abierto: {@link ServiceUnavailableException}.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteLookup {

    private final ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory;

    /**
     * Consulta un recurso por id en otro microservicio.
     *
     * @param request     llamada HTTP que obtiene el recurso
     * @param serviceName nombre del microservicio consultado; también nombra su circuit breaker
     * @param resourceId  id del recurso consultado, para logs
     * @return el recurso, vacío si no existe, o error {@link ServiceUnavailableException}
     */
    public <T> Maybe<T> findById(Mono<T> request, String serviceName, String resourceId) {
        Mono<T> call = request
                .doOnSubscribe(subscription -> log.debug(
                        "Calling {} resourceId={}", serviceName, resourceId))
                .onErrorResume(WebClientResponseException.NotFound.class, notFound -> {
                    log.debug("{} returned 404 resourceId={}", serviceName, resourceId);
                    return Mono.empty();
                });
        Mono<T> protectedCall = circuitBreakerFactory.create(serviceName)
                .run(call, error -> {
                    log.error("{} call failed resourceId={}", serviceName, resourceId, error);
                    return Mono.error(new ServiceUnavailableException(
                            serviceName + " no está disponible", error));
                });
        return Maybe.fromPublisher(protectedCall);
    }

    /**
     * Consulta una lista de recursos en otro microservicio, protegida con el mismo circuit breaker.
     *
     * @param request     llamada HTTP que obtiene la lista
     * @param serviceName nombre del microservicio consultado; también nombra su circuit breaker
     * @return los recursos, o error {@link ServiceUnavailableException} si la llamada falla, se
     *     agota el tiempo de espera o el circuito está abierto
     */
    public <T> Flowable<T> findAll(Flux<T> request, String serviceName) {
        Flux<T> call = request.doOnSubscribe(
                subscription -> log.debug("Listing resources from {}", serviceName));
        Flux<T> protectedCall = circuitBreakerFactory.create(serviceName)
                .run(call, error -> {
                    log.error("{} list call failed", serviceName, error);
                    return Flux.error(new ServiceUnavailableException(
                            serviceName + " no está disponible", error));
                });
        return Flowable.fromPublisher(protectedCall);
    }
}
