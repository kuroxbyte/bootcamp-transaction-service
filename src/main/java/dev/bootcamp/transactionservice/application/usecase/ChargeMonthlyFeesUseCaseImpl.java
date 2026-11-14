package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import io.reactivex.rxjava3.core.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Implementación de {@link ChargeMonthlyFeesUseCase}: las comisiones de cada cuenta las calcula
 * {@link Account#monthlyCharges}.
 */
@Slf4j
@RequiredArgsConstructor
public class ChargeMonthlyFeesUseCaseImpl implements ChargeMonthlyFeesUseCase {
    private final AccountClient accountClient;
    private final MovementRepository movementRepository;
    private final BigDecimal minimumAverageFee;

    @Override
    public Flowable<Movement> execute(YearMonth month) {
        return accountClient.findAll()
                .concatMap(account -> chargeAccount(account, month)
                        .onErrorResumeNext(error -> {
                            log.error(
                                    "Monthly fees failed accountId={} month={}",
                                    account.id(),
                                    month,
                                    error);
                            return Flowable.empty();
                        }));
    }

    private Flowable<Movement> chargeAccount(Account account, YearMonth month) {
        return movementRepository
                .hasMonthlyCharges(account.id(), month.plusMonths(1).atDay(1))
                .flatMapPublisher(alreadyCharged -> alreadyCharged
                        ? Flowable.<Movement>empty()
                        : movementRepository.findByAccountId(account.id())
                                .toList()
                                .map(movements -> account.monthlyCharges(
                                        month,
                                        movements,
                                        minimumAverageFee))
                                .flatMapPublisher(charges -> charges.isEmpty()
                                        ? Flowable.<Movement>empty()
                                        : movementRepository.saveAll(charges)))
                .doOnNext(charge -> log.info(
                        "Monthly fee charged accountId={} type={} amount={}",
                        charge.accountId(),
                        charge.type(),
                        charge.amount()));
    }
}
