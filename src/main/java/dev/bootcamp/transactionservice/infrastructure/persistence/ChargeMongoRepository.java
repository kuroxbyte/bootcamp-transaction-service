package dev.bootcamp.transactionservice.infrastructure.persistence;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

/**
 * Repositorio de Spring Data para {@link ChargeDocument}.
 */
public interface ChargeMongoRepository extends ReactiveMongoRepository<ChargeDocument, String> {
    /**
     * Busca los consumos de la tarjeta.
     */
    Flux<ChargeDocument> findByCreditCardId(String creditCardId);
}
