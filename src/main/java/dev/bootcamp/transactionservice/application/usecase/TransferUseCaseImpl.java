package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.transfer.Transfer;
import dev.bootcamp.transactionservice.domain.model.transfer.TransferType;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Implementación de {@link TransferUseCase}.
 */
@RequiredArgsConstructor
public class TransferUseCaseImpl implements TransferUseCase {
    private final AccountClient accountClient;
    private final MovementRepository movementRepository;

    @Override
    public Single<Transfer> transferToOwnAccount(
            String customerId,
            String sourceAccountId,
            String targetAccountId,
            BigDecimal amount,
            LocalDate date) {
        return transfer(
                TransferType.OWN,
                customerId,
                sourceAccountId,
                targetAccountId,
                amount,
                date);
    }

    @Override
    public Single<Transfer> transferToThirdParty(
            String customerId,
            String sourceAccountId,
            String targetAccountId,
            BigDecimal amount,
            LocalDate date) {
        return transfer(
                TransferType.THIRD_PARTY,
                customerId,
                sourceAccountId,
                targetAccountId,
                amount,
                date);
    }

    private Single<Transfer> transfer(
            TransferType type,
            String customerId,
            String sourceAccountId,
            String targetAccountId,
            BigDecimal amount,
            LocalDate date) {
        return Single.zip(
                        findAccount(sourceAccountId, "La cuenta origen no existe"),
                        findAccount(targetAccountId, "La cuenta destino no existe"),
                        (source, target) -> charge(
                                Transfer.between(type, customerId, source, target, amount, date),
                                source,
                                target))
                .flatMap(charged -> charged)
                .flatMap(this::save);
    }

    private Single<Account> findAccount(String accountId, String notFoundMessage) {
        return accountClient
                .findById(accountId)
                .switchIfEmpty(Single.error(() -> new NotFoundException(notFoundMessage)));
    }

    // Aplica a cada movimiento las reglas de su cuenta y le asigna su comisión.
    private Single<Transfer> charge(Transfer transfer, Account source, Account target) {
        return Single.zip(
                prepare(source, transfer.outgoing()),
                prepare(target, transfer.incoming()),
                transfer::withMovements);
    }

    private Single<Movement> prepare(Account account, Movement movement) {
        return Single.zip(
                movementRepository.countByAccountIdAndMonth(
                        account.id(),
                        YearMonth.from(movement.date())),
                movementRepository.getBalance(account.id()),
                (movementsInMonth, movementsBalance) -> account.prepareMovement(
                        movement,
                        movementsInMonth,
                        account.balance(movementsBalance)));
    }

    // Guarda los dos movimientos en una sola operación.
    private Single<Transfer> save(Transfer transfer) {
        return movementRepository
                .saveAll(List.of(transfer.outgoing(), transfer.incoming()))
                .toList()
                .map(saved -> transfer.withMovements(saved.get(0), saved.get(1)));
    }
}
