package dev.bootcamp.transactionservice.infrastructure.persistence;

import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Implementa el puerto {@link MovementRepository} con MongoDB reactivo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MovementPersistenceAdapter implements MovementRepository {

    private static final List<String> NOT_CUSTOMER_TRANSACTIONS = Arrays.stream(
                    MovementType.values())
            .filter(type -> !type.isCustomerTransaction())
            .map(MovementType::name)
            .toList();

    private static final List<String> MONTHLY_FEES = Stream.of(
                    MovementType.MAINTENANCE_FEE,
                    MovementType.MINIMUM_AVERAGE_FEE)
            .map(MovementType::name)
            .toList();

    private final MovementMongoRepository mongoRepository;

    @Override
    public Single<Long> countByAccountIdAndMonth(String accountId, YearMonth month) {
        // [primer día del mes, primer día del mes siguiente)
        Range<LocalDate> monthRange = Range.rightOpen(month.atDay(1), month.plusMonths(1).atDay(1));
        // Las devoluciones de pagos Yanki y las comisiones mensuales no cuentan.
        return Single.fromPublisher(mongoRepository.countByAccountIdAndDateBetweenAndTypeNotIn(
                accountId,
                monthRange,
                NOT_CUSTOMER_TRANSACTIONS));
    }

    @Override
    public Single<Boolean> hasMonthlyCharges(String accountId, LocalDate chargeDate) {
        return Single.fromPublisher(mongoRepository.existsByAccountIdAndDateAndTypeIn(
                accountId,
                chargeDate,
                MONTHLY_FEES));
    }

    /**
     * Suma los depósitos y resta los retiros y las comisiones de la cuenta.
     */
    @Override
    public Single<BigDecimal> getBalance(String accountId) {
        return findByAccountId(accountId)
                .reduce(
                        BigDecimal.ZERO,
                        (balance, movement) -> balance.add(movement.balanceEffect()));
    }

    @Override
    public Flowable<Movement> findByAccountId(String accountId) {
        return Flowable.fromPublisher(mongoRepository.findByAccountId(accountId))
                .map(TransactionPersistenceMapper::toDomain);
    }

    @Override
    public Flowable<Movement> findByDebitCardId(String debitCardId) {
        return Flowable.fromPublisher(mongoRepository.findByDebitCardId(debitCardId))
                .map(TransactionPersistenceMapper::toDomain);
    }

    @Override
    public Single<Movement> save(Movement movement) {
        return Single.fromPublisher(
                        mongoRepository.save(TransactionPersistenceMapper.toDocument(movement)))
                .doOnSuccess(
                        saved ->
                                log.debug(
                                        "Movement saved movementId={} accountId={}",
                                        saved.getId(),
                                        saved.getAccountId()))
                .map(TransactionPersistenceMapper::toDomain);
    }

    @Override
    public Flowable<Movement> saveAll(List<Movement> movements) {
        return Flowable.fromPublisher(mongoRepository.saveAll(
                        movements.stream().map(TransactionPersistenceMapper::toDocument).toList()))
                .map(TransactionPersistenceMapper::toDomain);
    }

    @Override
    public Flowable<Movement> findByDateBetween(LocalDate from, LocalDate to) {
        return Flowable.fromPublisher(mongoRepository.findByDateBetween(Range.closed(from, to)))
                .map(TransactionPersistenceMapper::toDomain);
    }
}
