package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.credit.Credit;
import dev.bootcamp.transactionservice.domain.model.credit.CreditCard;
import java.util.Optional;

/** Consulta de productos de crédito en credit-service. */
public interface CreditClient {
    Optional<Credit> findById(Long creditId);

    /**
     * Busca una tarjeta de crédito; vacío si el crédito no existe o no es una tarjeta.
     */
    Optional<CreditCard> findCreditCardById(Long creditId);
}
