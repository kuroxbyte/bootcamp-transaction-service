package dev.bootcamp.transactionservice.domain.model.history;

import static org.assertj.core.api.Assertions.assertThat;

import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ProductMovementTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);

    @Test
    void mapsDepositAndWithdrawal() {
        assertThat(ProductMovement.from(new Movement("1", MovementType.DEPOSIT, BigDecimal.TEN, DATE)))
                .isEqualTo(new ProductMovement(ProductMovementType.DEPOSIT, BigDecimal.TEN, DATE));
        assertThat(ProductMovement.from(new Movement("1", MovementType.WITHDRAWAL, BigDecimal.ONE, DATE)))
                .isEqualTo(new ProductMovement(ProductMovementType.WITHDRAWAL, BigDecimal.ONE, DATE));
    }

    @Test
    void mapsPayment() {
        assertThat(ProductMovement.from(new Payment(PaymentTarget.CREDIT, "1", BigDecimal.TEN, DATE)))
                .isEqualTo(new ProductMovement(ProductMovementType.PAYMENT, BigDecimal.TEN, DATE));
    }

    @Test
    void mapsCharge() {
        assertThat(ProductMovement.from(new Charge("1", BigDecimal.TEN, DATE)))
                .isEqualTo(new ProductMovement(ProductMovementType.CHARGE, BigDecimal.TEN, DATE));
    }
}
