package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.account.DailyAverageBalance;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Implementación de {@link GetDailyAverageBalanceUseCase}.
 */
@RequiredArgsConstructor
public class GetDailyAverageBalanceUseCaseImpl implements GetDailyAverageBalanceUseCase {
    private final AccountClient accountClient;
    private final MovementRepository movementRepository;
    private final Clock clock;

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
