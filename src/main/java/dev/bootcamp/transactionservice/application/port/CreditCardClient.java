package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.creditcard.CreditCard;
import java.util.Optional;

/** Consulta de tarjetas de crédito en credit-service. */
public interface CreditCardClient {
    Optional<CreditCard> findById(String creditCardId);
}
