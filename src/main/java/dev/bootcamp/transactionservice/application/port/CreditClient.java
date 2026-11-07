package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.credit.Credit;
import io.reactivex.rxjava3.core.Maybe;

/** Consulta de créditos personales y empresariales en credit-service. */
public interface CreditClient {

    /**
     * @return el crédito, o vacío si no existe
     */
    Maybe<Credit> findById(String creditId);
}
