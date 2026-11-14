package dev.bootcamp.transactionservice.infrastructure.scheduling;

import dev.bootcamp.transactionservice.application.usecase.ChargeMonthlyFeesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;

/**
 * Cobro mensual de comisiones: al empezar cada mes cobra las del mes anterior. El horario se
 * configura con {@code fees.monthly.cron}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyFeeCharge {

    private final ChargeMonthlyFeesUseCase chargeMonthlyFeesUseCase;
    private final Clock clock;

    /**
     * Cobra las comisiones del mes anterior a todas las cuentas.
     */
    @Scheduled(cron = "${fees.monthly.cron}")
    public void chargePreviousMonth() {
        YearMonth month = YearMonth.now(clock).minusMonths(1);
        log.info("Monthly fee charge started month={}", month);
        chargeMonthlyFeesUseCase.execute(month)
                .count()
                .doOnSuccess(charges -> log.info(
                        "Monthly fee charge finished month={} charges={}", month, charges))
                .ignoreElement()
                .onErrorComplete(error -> {
                    log.error("Monthly fee charge failed month={}", month, error);
                    return true;
                })
                .blockingAwait();
    }
}
