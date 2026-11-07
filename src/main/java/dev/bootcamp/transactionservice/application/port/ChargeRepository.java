package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;

public interface ChargeRepository {

    /**
     * Deuda de la tarjeta: suma de sus consumos menos suma de sus pagos
     * ({@link dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget#CREDIT_CARD}) (0 si no tiene ninguno).
     */
    Single<BigDecimal> getDebt(String creditCardId);

    /** Consumos de la tarjeta. */
    Flowable<Charge> findByCreditCardId(String creditCardId);

    Single<Charge> save(Charge charge);
}
