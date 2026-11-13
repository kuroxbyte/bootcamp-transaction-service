package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.DebitCardRepository;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import io.reactivex.rxjava3.core.Single;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Implementación de {@link RegisterMovementUseCase}.
 */
public class RegisterMovementUseCaseImpl implements RegisterMovementUseCase {
    private final MovementRepository movementRepository;
    private final AccountClient accountClient;
    private final DebitCardRepository debitCardRepository;

    /**
     * Crea el caso de uso con los adaptadores de sus puertos.
     */
    public RegisterMovementUseCaseImpl(
            MovementRepository movementRepository,
            AccountClient accountClient,
            DebitCardRepository debitCardRepository) {
        this.movementRepository = movementRepository;
        this.accountClient = accountClient;
        this.debitCardRepository = debitCardRepository;
    }

    @Override
    public Single<Movement> payWithDebitCard(
            String customerId,
            String debitCardId,
            BigDecimal amount,
            LocalDate date) {
        return debitCardRepository.findById(debitCardId)
                .filter(card -> card.belongsTo(customerId))
                .switchIfEmpty(Single.error(() -> new NotFoundException(
                        "La tarjeta de débito no existe o no pertenece al cliente")))
                .map(card -> Movement.debitCardPayment(
                        card.primaryAccountId(),
                        debitCardId,
                        amount,
                        date))
                .flatMap(movement -> register(customerId, movement));
    }

    @Override
    public Single<Movement> deposit(
            String customerId,
            String accountId,
            BigDecimal amount,
            LocalDate date) {
        return Single.fromCallable(
                        () -> new Movement(accountId, MovementType.DEPOSIT, amount, date))
                .flatMap(movement -> register(customerId, movement));
    }

    @Override
    public Single<Movement> withdraw(
            String customerId,
            String accountId,
            BigDecimal amount,
            LocalDate date) {
        return Single.fromCallable(
                        () -> new Movement(accountId, MovementType.WITHDRAWAL, amount, date))
                .flatMap(movement -> register(customerId, movement));
    }

    private Single<Movement> register(String customerId, Movement movement) {
        return accountClient
                .findById(movement.accountId())
                .switchIfEmpty(Single.error(() -> new NotFoundException("La cuenta no existe")))
                .map(account -> ensureBelongsTo(account, customerId))
                .flatMap(
                        account ->
                                Single.zip(
                                        movementRepository.countByAccountIdAndMonth(
                                                movement.accountId(),
                                                YearMonth.from(movement.date())),
                                        movementRepository.getBalance(movement.accountId()),
                                        (movementsInMonth, movementsBalance) ->
                                                account.prepareMovement(
                                                        movement,
                                                        movementsInMonth,
                                                        account.balance(movementsBalance))))
                .flatMap(movementRepository::save);
    }

    private static Account ensureBelongsTo(Account account, String customerId) {
        if (!account.belongsTo(customerId)) {
            throw new NotFoundException("La cuenta no pertenece al cliente");
        }
        return account;
    }
}
