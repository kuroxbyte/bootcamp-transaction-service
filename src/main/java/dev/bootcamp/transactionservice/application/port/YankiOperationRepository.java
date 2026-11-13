package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.yanki.YankiOperation;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;

/**
 * Puerto de persistencia de los resultados de las operaciones Yanki ya procesadas.
 */
public interface YankiOperationRepository {

    /**
     * Guarda el resultado.
     */
    Completable save(YankiOperation operation);

    /**
     * Busca el resultado de una operación.
     *
     * @return el resultado, o vacío si la operación todavía no se procesó
     */
    Maybe<YankiOperation> findById(String operationId);
}
