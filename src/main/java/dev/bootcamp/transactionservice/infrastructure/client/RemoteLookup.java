package dev.bootcamp.transactionservice.infrastructure.client;

import io.reactivex.rxjava3.core.Maybe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Reglas comunes para consultar un recurso por id en otro microservicio.
 *
 * <ul>
 *   <li>404: el recurso no existe; se emite vacío.
 *   <li>Tiempo de espera agotado, error de conexión u otro código de error: {@link
 *       ServiceUnavailableException}.
 * </ul>
 */
@Slf4j
public final class RemoteLookup {

    private RemoteLookup() {}

    /**
     * Consulta un recurso por id en otro microservicio.
     *
     * @param request llamada HTTP que obtiene el recurso
     * @param timeout tiempo máximo de espera
     * @param serviceName nombre del microservicio consultado, para logs y errores
     * @param resourceId id del recurso consultado, para logs
     * @return el recurso, vacío si no existe, o error {@link ServiceUnavailableException}
     */
    public static <T> Maybe<T> findById(
            Mono<T> request,
            Duration timeout,
            String serviceName,
            String resourceId) {
        Mono<T> lookup =
                request.timeout(timeout)
                        .doOnSubscribe(
                                subscription ->
                                        log.debug(
                                                "Calling {} resourceId={}",
                                                serviceName,
                                                resourceId))
                        .onErrorResume(
                                WebClientResponseException.NotFound.class,
                                notFound -> {
                                    log.debug(
                                            "{} returned 404 resourceId={}",
                                            serviceName,
                                            resourceId);
                                    return Mono.empty();
                                })
                        .onErrorMap(
                                error -> {
                                    log.error(
                                            "{} call failed resourceId={}",
                                            serviceName,
                                            resourceId,
                                            error);
                                    return new ServiceUnavailableException(
                                            serviceName + " no está disponible", error);
                                });
        return Maybe.fromPublisher(lookup);
    }
}
