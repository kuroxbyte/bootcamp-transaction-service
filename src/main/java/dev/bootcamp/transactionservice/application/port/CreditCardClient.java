package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.creditcard.CreditCard;
import io.reactivex.rxjava3.core.Maybe;

/**
 * Consulta de tarjetas de crédito en credit-service.
 */
public interface CreditCardClient {

    /**
     * Busca el recurso por su id.
     *
     * @return la tarjeta, o vacío si no existe
     */
    Maybe<CreditCard> findById(String creditCardId);
}
