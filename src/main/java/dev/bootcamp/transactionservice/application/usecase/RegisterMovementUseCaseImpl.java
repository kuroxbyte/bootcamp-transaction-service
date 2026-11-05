package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
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
    public Movement execute(Long accountId, MovementType type, LocalDate date) {
        Account account = accountClient.findById(accountId)
                .orElseThrow(() -> new BusinessRuleException("La cuenta no existe"));
        Movement movement = new Movement(accountId, type, date);
        long movementsInMonth = movementRepository.countByAccountIdAndMonth(accountId, YearMonth.from(date));
        account.validateMovement(movement, movementsInMonth);
        return movementRepository.save(movement);
    }
}
