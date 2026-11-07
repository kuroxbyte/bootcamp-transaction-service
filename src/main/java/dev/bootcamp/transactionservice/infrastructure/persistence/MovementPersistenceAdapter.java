package dev.bootcamp.transactionservice.infrastructure.persistence;

import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.stereotype.Component;

/** Implementa el puerto {@link MovementRepository} con MongoDB reactivo. */
@Slf4j
@Component
@RequiredArgsConstructor
public class MovementPersistenceAdapter implements MovementRepository {

    private final MovementMongoRepository mongoRepository;

    @Override
    public Single<Long> countByAccountIdAndMonth(String accountId, YearMonth month) {
        // [primer día del mes, primer día del mes siguiente)
        Range<LocalDate> monthRange = Range.rightOpen(month.atDay(1), month.plusMonths(1).atDay(1));
        return Single.fromPublisher(mongoRepository.countByAccountIdAndDateBetween(accountId, monthRange));
    }

    /**
     * Suma los depósitos y resta los retiros de la cuenta.
     */
    @Override
    public Single<BigDecimal> getBalance(String accountId) {
        return findByAccountId(accountId)
                .reduce(BigDecimal.ZERO, (balance, movement) -> movement.type() == MovementType.DEPOSIT
                        ? balance.add(movement.amount())
                        : balance.subtract(movement.amount()));
    }

    @Override
    public Flowable<Movement> findByAccountId(String accountId) {
        return Flowable.fromPublisher(mongoRepository.findByAccountId(accountId))
                .map(TransactionPersistenceMapper::toDomain);
    }

    @Override
    public Single<Movement> save(Movement movement) {
        return Single.fromPublisher(mongoRepository.save(TransactionPersistenceMapper.toDocument(movement)))
                .doOnSuccess(saved -> log.debug("Movement saved movementId={} accountId={}",
                        saved.getId(), saved.getAccountId()))
                .map(TransactionPersistenceMapper::toDomain);
    }
}
