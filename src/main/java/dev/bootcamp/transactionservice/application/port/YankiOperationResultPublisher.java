package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.yanki.YankiOperation;
import io.reactivex.rxjava3.core.Completable;

/**
 * Publicación de la respuesta a cada operación Yanki para yanki-service.
 */
public interface YankiOperationResultPublisher {

    /**
     * Publica el resultado.
     *
     * @return termina cuando se publicó; error si no se pudo (el pedido se vuelve a procesar y se
     *     responde con el resultado guardado)
     */
    Completable publish(YankiOperation operation);
}
