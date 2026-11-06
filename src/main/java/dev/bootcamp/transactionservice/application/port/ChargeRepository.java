package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import java.math.BigDecimal;
import java.util.List;

public interface ChargeRepository {

    /**
     * Deuda de la tarjeta: suma de sus consumos menos suma de sus pagos
     * ({@link dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget#CREDIT_CARD}) (0 si no tiene ninguno).
     */
    BigDecimal getDebt(Long creditCardId);

    /** Consumos de la tarjeta. */
    List<Charge> findByCreditCardId(Long creditCardId);

    Charge save(Charge charge);
}
