package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.credit.Credit;
import java.util.Optional;

/** Consulta de productos de crédito en credit-service. */
public interface CreditClient {
    Optional<Credit> findById(Long creditId);
}
