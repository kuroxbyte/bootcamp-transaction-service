package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.application.port.YankiOperationRepository;
import dev.bootcamp.transactionservice.application.port.YankiOperationResultPublisher;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import dev.bootcamp.transactionservice.domain.model.yanki.YankiOperation;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Implementación de {@link ProcessYankiOperationUseCase}.
 */
@RequiredArgsConstructor
public class ProcessYankiOperationUseCaseImpl implements ProcessYankiOperationUseCase {
    private final MovementRepository movementRepository;
    private final AccountClient accountClient;
    private final YankiOperationRepository operationRepository;
    private final YankiOperationResultPublisher resultPublisher;

    @Override
    public Completable execute(
            String operationId,
            boolean debit,
            String accountId,
            BigDecimal amount,
            LocalDate date,
            boolean reversal) {
        Single<YankiOperation> processed = Single.defer(() -> apply(
                        operationId,
                        debit,
                        accountId,
                        amount,
                        date,
                        reversal))
                .flatMap(operation -> operationRepository.save(operation)
                        .andThen(Single.just(operation)));
        return operationRepository.findById(operationId)
                .switchIfEmpty(processed)
                .flatMapCompletable(resultPublisher::publish);
    }

    private Single<YankiOperation> apply(
            String operationId,
            boolean debit,
            String accountId,
            BigDecimal amount,
            LocalDate date,
            boolean reversal) {
        Single<Movement> registered;
        if (reversal) {
            registered = Single.fromCallable(() -> new Movement(
                            accountId,
                            MovementType.YANKI_PAYMENT_REVERSAL,
                            amount,
                            date))
                    .flatMap(movementRepository::save);
        } else {
            MovementType type = debit
                    ? MovementType.YANKI_PAYMENT_SENT
                    : MovementType.YANKI_PAYMENT_RECEIVED;
            registered = Single.fromCallable(() -> new Movement(accountId, type, amount, date))
                    .flatMap(this::register);
        }
        return registered
                .map(movement -> YankiOperation.completed(operationId))
                .onErrorResumeNext(error ->
                        error instanceof BusinessRuleException || error instanceof NotFoundException
                                ? Single.just(YankiOperation.rejected(
                                        operationId,
                                        error.getMessage()))
                                : Single.error(error));
    }

    // Mismas reglas y comisión que un depósito o retiro del cliente.
    private Single<Movement> register(Movement movement) {
        return accountClient.findById(movement.accountId())
                .switchIfEmpty(Single.error(() -> new NotFoundException("La cuenta no existe")))
                .flatMap(account -> Single.zip(
                        movementRepository.countByAccountIdAndMonth(
                                movement.accountId(),
                                YearMonth.from(movement.date())),
                        movementRepository.getBalance(movement.accountId()),
                        (movementsInMonth, movementsBalance) -> account.prepareMovement(
                                movement,
                                movementsInMonth,
                                account.balance(movementsBalance))))
                .flatMap(movementRepository::save);
    }
}
