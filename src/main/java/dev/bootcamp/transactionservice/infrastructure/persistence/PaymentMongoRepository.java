package dev.bootcamp.transactionservice.infrastructure.persistence;

import org.springframework.data.domain.Range;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

/**
 * Repositorio de Spring Data para {@link PaymentDocument}.
 */
public interface PaymentMongoRepository extends ReactiveMongoRepository<PaymentDocument, String> {
    /**
     * Busca los pagos hechos al producto de crédito indicado.
     */
    Flux<PaymentDocument> findByTargetAndProductId(String target, String productId);

    /**
     * Documentos de todo el banco con fecha dentro del rango; los límites incluidos o excluidos
     * los define el {@link Range} recibido.
     */
    Flux<PaymentDocument> findByDateBetween(Range<LocalDate> dateRange);
}
