package dev.bootcamp.transactionservice.infrastructure.persistence;

import org.springframework.data.domain.Range;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

/**
 * Repositorio de Spring Data para {@link ChargeDocument}.
 */
public interface ChargeMongoRepository extends ReactiveMongoRepository<ChargeDocument, String> {
    /**
     * Busca los consumos de la tarjeta.
     */
    Flux<ChargeDocument> findByCreditCardId(String creditCardId);

    /**
     * Documentos de todo el banco con fecha dentro del rango; los límites incluidos o excluidos
     * los define el {@link Range} recibido.
     */
    Flux<ChargeDocument> findByDateBetween(Range<LocalDate> dateRange);
}
