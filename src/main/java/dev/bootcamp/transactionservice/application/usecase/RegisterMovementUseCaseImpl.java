package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public class RegisterMovementUseCaseImpl implements RegisterMovementUseCase {
    private final MovementRepository movementRepository;
    private final AccountClient accountClient;

    public RegisterMovementUseCaseImpl(MovementRepository movementRepository, AccountClient accountClient) {
        this.movementRepository = movementRepository;
        this.accountClient = accountClient;
    }

    @Override
    public Single<Movement> deposit(String customerId, String accountId, BigDecimal amount, LocalDate date) {
        return Single.fromCallable(() -> new Movement(accountId, MovementType.DEPOSIT, amount, date))
                .flatMap(movement -> register(customerId, movement));
    }

    @Override
    public Single<Movement> withdraw(String customerId, String accountId, BigDecimal amount, LocalDate date) {
        return Single.fromCallable(() -> new Movement(accountId, MovementType.WITHDRAWAL, amount, date))
                .flatMap(movement -> register(customerId, movement));
    }

    private Single<Movement> register(String customerId, Movement movement) {
        return accountClient.findById(movement.accountId())
                .switchIfEmpty(Single.error(() -> new BusinessRuleException("La cuenta no existe")))
                .map(account -> ensureBelongsTo(account, customerId))
                .flatMap(account -> Single.zip(
                        movementRepository.countByAccountIdAndMonth(movement.accountId(), YearMonth.from(movement.date())),
                        movementRepository.getBalance(movement.accountId()),
                        (movementsInMonth, balance) -> {
                            account.validateMovement(movement, movementsInMonth, balance);
                            return movement;
                        }))
                .flatMap(movementRepository::save);
    }

    private static Account ensureBelongsTo(Account account, String customerId) {
        if (!account.belongsTo(customerId)) {
            throw new BusinessRuleException("La cuenta no pertenece al cliente");
        }
        return account;
    }
}
