package dev.bootcamp.transactionservice.infrastructure.persistence;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface PaymentMongoRepository extends ReactiveMongoRepository<PaymentDocument, String> {
    Flux<PaymentDocument> findByTargetAndProductId(String target, String productId);
}
