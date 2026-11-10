package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.account.DailyAverageBalance;
import io.reactivex.rxjava3.core.Single;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Implementación de {@link GetDailyAverageBalanceUseCase}.
 */
public class GetDailyAverageBalanceUseCaseImpl implements GetDailyAverageBalanceUseCase {
    private final AccountClient accountClient;
    private final MovementRepository movementRepository;
    private final Clock clock;

    /**
     * Crea el caso de uso con los adaptadores de sus puertos y el reloj que da la fecha actual.
     */
    public GetDailyAverageBalanceUseCaseImpl(
            AccountClient accountClient,
            MovementRepository movementRepository,
            Clock clock) {
        this.accountClient = accountClient;
        this.movementRepository = movementRepository;
        this.clock = clock;
    }

    @Override
    public Single<DailyAverageBalance> execute(
            String customerId,
            String accountId,
            YearMonth month) {
        return accountClient
                .findById(accountId)
                .switchIfEmpty(Single.error(() -> new NotFoundException("La cuenta no existe")))
                .flatMap(account -> {
                    if (!account.belongsTo(customerId)) {
                        return Single.error(
                                new NotFoundException("La cuenta no pertenece al cliente"));
                    }
                    return movementRepository
                            .findByAccountId(accountId)
                            .toList()
                            .map(movements -> account.dailyAverageBalance(
                                    month,
                                    LocalDate.now(clock),
                                    movements));
                });
    }
}
