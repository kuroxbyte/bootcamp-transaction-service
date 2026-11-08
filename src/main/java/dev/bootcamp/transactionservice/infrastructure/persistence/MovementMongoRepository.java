package dev.bootcamp.transactionservice.infrastructure.persistence;

import java.time.LocalDate;
import org.springframework.data.domain.Range;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Repositorio de Spring Data para {@link MovementDocument}. */
public interface MovementMongoRepository extends ReactiveMongoRepository<MovementDocument, String> {
    /** Busca los depósitos y retiros de la cuenta. */
    Flux<MovementDocument> findByAccountId(String accountId);

    /**
     * Cantidad de movimientos de la cuenta con fecha dentro del rango; los límites incluidos o
     * excluidos los define el {@link Range} recibido.
     */
    Mono<Long> countByAccountIdAndDateBetween(String accountId, Range<LocalDate> dateRange);
}
