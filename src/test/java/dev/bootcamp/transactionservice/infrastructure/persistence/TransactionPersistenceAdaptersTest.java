package dev.bootcamp.transactionservice.infrastructure.persistence;

import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Testcontainers
@DataMongoTest
@Import({
    MovementPersistenceAdapter.class,
    PaymentPersistenceAdapter.class,
    ChargePersistenceAdapter.class
})
class TransactionPersistenceAdaptersTest {

    @Container @ServiceConnection static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired private MovementPersistenceAdapter movementAdapter;

    @Autowired private PaymentPersistenceAdapter paymentAdapter;

    @Autowired private ChargePersistenceAdapter chargeAdapter;

    @Autowired private MovementMongoRepository movementMongoRepository;

    @Autowired private PaymentMongoRepository paymentMongoRepository;

    @Autowired private ChargeMongoRepository chargeMongoRepository;

    @BeforeEach
    void cleanCollections() {
        movementMongoRepository.deleteAll().block();
        paymentMongoRepository.deleteAll().block();
        chargeMongoRepository.deleteAll().block();
    }

    private void saveMovement(String accountId, MovementType type, String amount, LocalDate date) {
        movementAdapter
                .save(new Movement(accountId, type, new BigDecimal(amount), date))
                .blockingGet();
    }

    @Test
    void savesMovementKeepingAmountAndDate() {
        Movement movement =
                new Movement(
                        "a1",
                        MovementType.DEPOSIT,
                        new BigDecimal("100.25"),
                        LocalDate.of(2026, 11, 15));

        movementAdapter.save(movement).test().awaitDone(5, TimeUnit.SECONDS).assertValue(movement);
        movementAdapter
                .findByAccountId("a1")
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertValues(movement);
    }

    @Test
    void countsMovementsOfTheMonthOnly() {
        saveMovement("a1", MovementType.DEPOSIT, "10", LocalDate.of(2026, 10, 31));
        saveMovement("a1", MovementType.DEPOSIT, "10", LocalDate.of(2026, 11, 1));
        saveMovement("a1", MovementType.WITHDRAWAL, "5", LocalDate.of(2026, 11, 30));
        saveMovement("a1", MovementType.DEPOSIT, "10", LocalDate.of(2026, 12, 1));
        saveMovement("a2", MovementType.DEPOSIT, "10", LocalDate.of(2026, 11, 15));
        // Las devoluciones de pagos Yanki no cuentan como transacciones del mes.
        saveMovement("a1", MovementType.YANKI_PAYMENT_REVERSAL, "10", LocalDate.of(2026, 11, 20));

        movementAdapter
                .countByAccountIdAndMonth("a1", YearMonth.of(2026, 11))
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertNoErrors()
                .assertValue(2L);
    }

    @Test
    void balanceIsDepositsMinusWithdrawals() {
        saveMovement("a1", MovementType.DEPOSIT, "100.50", LocalDate.of(2026, 11, 1));
        saveMovement("a1", MovementType.DEPOSIT, "50", LocalDate.of(2026, 11, 2));
        saveMovement("a1", MovementType.WITHDRAWAL, "30.25", LocalDate.of(2026, 11, 3));

        movementAdapter
                .getBalance("a1")
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertValue(balance -> balance.compareTo(new BigDecimal("120.25")) == 0);
        movementAdapter
                .getBalance("empty")
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertValue(balance -> balance.compareTo(BigDecimal.ZERO) == 0);
    }

    @Test
    void findsPaymentsByTargetAndProduct() {
        LocalDate date = LocalDate.of(2026, 11, 15);
        paymentAdapter
                .save(new Payment(PaymentTarget.CREDIT, "p1", BigDecimal.TEN, date, "c1"))
                .blockingGet();
        paymentAdapter
                .save(new Payment(PaymentTarget.CREDIT_CARD, "p1", BigDecimal.ONE, date, "c1"))
                .blockingGet();

        paymentAdapter
                .findByTargetAndProductId(PaymentTarget.CREDIT, "p1")
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertValues(new Payment(PaymentTarget.CREDIT, "p1", BigDecimal.TEN, date, "c1"));
    }

    @Test
    void debtIsChargesMinusCardPayments() {
        LocalDate date = LocalDate.of(2026, 11, 15);
        chargeAdapter.save(new Charge("card1", new BigDecimal("300"), date)).blockingGet();
        chargeAdapter.save(new Charge("card1", new BigDecimal("200.50"), date)).blockingGet();
        paymentAdapter
                .save(
                        new Payment(
                                PaymentTarget.CREDIT_CARD,
                                "card1",
                                new BigDecimal("100"),
                                date,
                                "c1"))
                .blockingGet();
        // Un pago a un crédito con el mismo id no reduce la deuda de la tarjeta.
        paymentAdapter
                .save(new Payment(PaymentTarget.CREDIT, "card1", new BigDecimal("999"), date, "c1"))
                .blockingGet();

        chargeAdapter
                .getDebt("card1")
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertValue(debt -> debt.compareTo(new BigDecimal("400.50")) == 0);
        chargeAdapter
                .findByCreditCardId("card1")
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertValueCount(2);
    }

    @Test
    void balanceDiscountsCommissions() {
        LocalDate date = LocalDate.of(2026, 11, 1);
        movementAdapter.save(new Movement(
                "a1",
                MovementType.DEPOSIT,
                new BigDecimal("100"),
                date,
                new BigDecimal("2"))).blockingGet();
        movementAdapter.save(new Movement(
                "a1",
                MovementType.WITHDRAWAL,
                new BigDecimal("30"),
                date,
                new BigDecimal("2"))).blockingGet();

        movementAdapter
                .getBalance("a1")
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertValue(balance -> balance.compareTo(new BigDecimal("66")) == 0);
    }

    @Test
    void savesBothMovementsOfTransfer() {
        LocalDate date = LocalDate.of(2026, 11, 1);
        Movement outgoing = new Movement(
                "a1",
                MovementType.TRANSFER_OUT,
                new BigDecimal("40"),
                date,
                new BigDecimal("2"));
        Movement incoming = new Movement(
                "a2",
                MovementType.TRANSFER_IN,
                new BigDecimal("40"),
                date);

        movementAdapter.saveAll(List.of(outgoing, incoming))
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertValues(outgoing, incoming);
        movementAdapter.getBalance("a1").test().awaitDone(5, TimeUnit.SECONDS)
                .assertValue(balance -> balance.compareTo(new BigDecimal("-42")) == 0);
        movementAdapter.getBalance("a2").test().awaitDone(5, TimeUnit.SECONDS)
                .assertValue(balance -> balance.compareTo(new BigDecimal("40")) == 0);
    }

    @Test
    void findsTransactionsOfTheWholeBankInsideTheInterval() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 30);
        saveMovement("a1", MovementType.DEPOSIT, "10", LocalDate.of(2026, 10, 31));
        saveMovement("a1", MovementType.DEPOSIT, "10", from);
        saveMovement("a2", MovementType.WITHDRAWAL, "5", to);
        paymentAdapter.save(new Payment(PaymentTarget.CREDIT, "k1", BigDecimal.ONE, to, "c1"))
                .blockingGet();
        paymentAdapter.save(new Payment(
                PaymentTarget.CREDIT,
                "k1",
                BigDecimal.ONE,
                LocalDate.of(2026, 12, 1),
                "c1")).blockingGet();
        chargeAdapter.save(new Charge("t1", BigDecimal.TEN, from)).blockingGet();

        movementAdapter.findByDateBetween(from, to).test().awaitDone(5, TimeUnit.SECONDS)
                .assertValueCount(2);
        paymentAdapter.findByDateBetween(from, to).test().awaitDone(5, TimeUnit.SECONDS)
                .assertValueCount(1);
        chargeAdapter.findByDateBetween(from, to).test().awaitDone(5, TimeUnit.SECONDS)
                .assertValueCount(1);
    }

    @Test
    void findsPaymentsOfDebitCardKeepingItsId() {
        LocalDate date = LocalDate.of(2026, 11, 15);
        movementAdapter.save(Movement.debitCardPayment("a1", "d1", BigDecimal.TEN, date))
                .blockingGet();
        movementAdapter.save(new Movement("a1", MovementType.WITHDRAWAL, BigDecimal.ONE, date))
                .blockingGet();

        movementAdapter.findByDebitCardId("d1").test().awaitDone(5, TimeUnit.SECONDS)
                .assertValue(movement -> "d1".equals(movement.debitCardId())
                        && movement.type() == MovementType.DEBIT_CARD_PAYMENT);
    }
}
