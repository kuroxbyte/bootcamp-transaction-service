package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
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
    public Movement deposit(Long customerId, Long accountId, BigDecimal amount, LocalDate date) {
        return register(customerId, new Movement(accountId, MovementType.DEPOSIT, amount, date));
    }

    @Override
    public Movement withdraw(Long customerId, Long accountId, BigDecimal amount, LocalDate date) {
        return register(customerId, new Movement(accountId, MovementType.WITHDRAWAL, amount, date));
    }

    private Movement register(Long customerId, Movement movement) {
        Account account = accountClient.findById(movement.accountId())
                .orElseThrow(() -> new BusinessRuleException("La cuenta no existe"));
        if (!account.belongsTo(customerId)) {
            throw new BusinessRuleException("La cuenta no pertenece al cliente");
        }
        long movementsInMonth = movementRepository.countByAccountIdAndMonth(
                movement.accountId(), YearMonth.from(movement.date()));
        BigDecimal balance = movementRepository.getBalance(movement.accountId());
        account.validateMovement(movement, movementsInMonth, balance);
        return movementRepository.save(movement);
    }
}
