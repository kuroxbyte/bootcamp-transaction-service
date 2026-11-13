package dev.bootcamp.transactionservice.infrastructure.persistence;

import dev.bootcamp.transactionservice.application.port.YankiOperationRepository;
import dev.bootcamp.transactionservice.domain.model.yanki.YankiOperation;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementa el puerto {@link YankiOperationRepository} con MongoDB reactivo.
 */
@Component
@RequiredArgsConstructor
public class YankiOperationPersistenceAdapter implements YankiOperationRepository {

    private final YankiOperationMongoRepository mongoRepository;

    @Override
    public Completable save(YankiOperation operation) {
        return Completable.fromPublisher(mongoRepository.save(new YankiOperationDocument(
                operation.operationId(),
                operation.completed(),
                operation.reason())));
    }

    @Override
    public Maybe<YankiOperation> findById(String operationId) {
        return Maybe.fromPublisher(mongoRepository.findById(operationId))
                .map(document -> new YankiOperation(
                        document.getId(),
                        document.isCompleted(),
                        document.getReason()));
    }
}
