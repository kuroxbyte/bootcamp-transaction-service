package dev.bootcamp.transactionservice.infrastructure.persistence;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

/** Repositorio de Spring Data para {@link PaymentDocument}. */
public interface PaymentMongoRepository extends ReactiveMongoRepository<PaymentDocument, String> {
    /** Busca los pagos hechos al producto de crédito indicado. */
    Flux<PaymentDocument> findByTargetAndProductId(String target, String productId);
}
