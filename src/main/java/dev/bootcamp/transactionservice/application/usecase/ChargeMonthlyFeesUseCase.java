package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import io.reactivex.rxjava3.core.Flowable;

import java.time.YearMonth;

/**
 * Cobra las comisiones mensuales de todas las cuentas del banco por un mes terminado: la comisión
 * de mantenimiento de las cuentas corrientes y la comisión de las cuentas de ahorro VIP cuyo
 * promedio diario no alcanzó el mínimo.
 */
public interface ChargeMonthlyFeesUseCase {

    /**
     * Cobra las comisiones del mes. Es idempotente: una cuenta que ya tiene las comisiones de ese
     * mes no se vuelve a cobrar. Si una cuenta falla, se registra el error y se sigue con las
     * demás.
     *
     * @param month mes terminado que se cobra
     * @return las comisiones guardadas
     */
    Flowable<Movement> execute(YearMonth month);
}
