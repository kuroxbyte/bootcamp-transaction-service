package dev.bootcamp.transactionservice.infrastructure.persistence;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface ChargeMongoRepository extends ReactiveMongoRepository<ChargeDocument, String> {
    Flux<ChargeDocument> findByCreditCardId(String creditCardId);
}
