package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.ChargeRepository;
import dev.bootcamp.transactionservice.application.port.CreditCardClient;
import dev.bootcamp.transactionservice.application.port.CreditClient;
import dev.bootcamp.transactionservice.application.port.DebitCardRepository;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.application.port.PaymentRepository;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.account.CheckingMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import dev.bootcamp.transactionservice.domain.model.credit.Credit;
import dev.bootcamp.transactionservice.domain.model.creditcard.CreditCard;
import dev.bootcamp.transactionservice.domain.model.debitcard.DebitCard;
import dev.bootcamp.transactionservice.domain.model.history.ProductMovement;
import dev.bootcamp.transactionservice.domain.model.history.ProductMovementType;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProductMovementsUseCaseImplTest {

    private static final String CUSTOMER_ID = "100";
    private static final LocalDate DAY_1 = LocalDate.of(2026, 11, 1);
    private static final LocalDate DAY_2 = LocalDate.of(2026, 11, 2);
    private static final LocalDate DAY_3 = LocalDate.of(2026, 11, 3);

    @Mock private AccountClient accountClient;
    @Mock private CreditClient creditClient;
    @Mock private CreditCardClient creditCardClient;
    @Mock private MovementRepository movementRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ChargeRepository chargeRepository;
    @Mock private DebitCardRepository debitCardRepository;

    @InjectMocks private GetProductMovementsUseCaseImpl useCase;

    @Test
    void returnsAccountMovementsSortedByDate() {
        when(accountClient.findById("1"))
                .thenReturn(
                        Maybe.just(
                                new Account(
                                        "1",
                                        CUSTOMER_ID,
                                        new CheckingMovementPolicy(),
                                        BigDecimal.ZERO,
                                        null,
                                        null,
                                        null)));
        when(movementRepository.findByAccountId("1"))
                .thenReturn(
                        Flowable.fromIterable(
                                List.of(
                                        new Movement(
                                                "1",
                                                MovementType.WITHDRAWAL,
                                                BigDecimal.ONE,
                                                DAY_2),
                                        new Movement(
                                                "1",
                                                MovementType.DEPOSIT,
                                                BigDecimal.TEN,
                                                DAY_1))));

        List<ProductMovement> movements =
                useCase.getAccountMovements(CUSTOMER_ID, "1").toList().blockingGet();

        assertThat(movements)
                .containsExactly(
                        new ProductMovement(
                                ProductMovementType.DEPOSIT,
                                BigDecimal.TEN,
                                DAY_1,
                                BigDecimal.ZERO),
                        new ProductMovement(
                                ProductMovementType.WITHDRAWAL,
                                BigDecimal.ONE,
                                DAY_2,
                                BigDecimal.ZERO));
    }

    @Test
    void returnsEmptyListWhenAccountHasNoMovements() {
        when(accountClient.findById("1"))
                .thenReturn(
                        Maybe.just(
                                new Account(
                                        "1",
                                        CUSTOMER_ID,
                                        new CheckingMovementPolicy(),
                                        BigDecimal.ZERO,
                                        null,
                                        null,
                                        null)));
        when(movementRepository.findByAccountId("1")).thenReturn(Flowable.fromIterable(List.of()));

        assertThat(useCase.getAccountMovements(CUSTOMER_ID, "1").toList().blockingGet()).isEmpty();
    }

    @Test
    void rejectsMovementsOfAccountOfAnotherCustomer() {
        when(accountClient.findById("1"))
                .thenReturn(
                        Maybe.just(
                                new Account(
                                        "1",
                                        CUSTOMER_ID,
                                        new CheckingMovementPolicy(),
                                        BigDecimal.ZERO,
                                        null,
                                        null,
                                        null)));

        useCase.getAccountMovements("999", "1").test().assertError(NotFoundException.class);
        verify(movementRepository, never()).findByAccountId(any());
    }

    @Test
    void rejectsMovementsOfUnknownAccount() {
        when(accountClient.findById("99")).thenReturn(Maybe.empty());

        useCase.getAccountMovements(CUSTOMER_ID, "99").test().assertError(NotFoundException.class);
    }

    @Test
    void returnsCreditPayments() {
        when(creditClient.findById("2")).thenReturn(Maybe.just(new Credit("2", CUSTOMER_ID)));
        when(paymentRepository.findByTargetAndProductId(PaymentTarget.CREDIT, "2"))
                .thenReturn(
                        Flowable.fromIterable(
                                List.of(
                                        new Payment(
                                                PaymentTarget.CREDIT,
                                                "2",
                                                BigDecimal.TEN,
                                                DAY_1,
                                                CUSTOMER_ID))));

        assertThat(useCase.getCreditMovements(CUSTOMER_ID, "2").toList().blockingGet())
                .containsExactly(
                        new ProductMovement(ProductMovementType.PAYMENT, BigDecimal.TEN, DAY_1));
    }

    @Test
    void rejectsMovementsOfCreditOfAnotherCustomer() {
        when(creditClient.findById("2")).thenReturn(Maybe.just(new Credit("2", CUSTOMER_ID)));

        useCase.getCreditMovements("999", "2").test().assertError(NotFoundException.class);
        verify(paymentRepository, never()).findByTargetAndProductId(any(), any());
    }

    @Test
    void returnsCardChargesAndPaymentsMergedAndSortedByDate() {
        when(creditCardClient.findById("3"))
                .thenReturn(Maybe.just(new CreditCard("3", CUSTOMER_ID, new BigDecimal("1000"))));
        when(chargeRepository.findByCreditCardId("3"))
                .thenReturn(
                        Flowable.fromIterable(
                                List.of(
                                        new Charge("3", new BigDecimal("300"), DAY_1),
                                        new Charge("3", new BigDecimal("50"), DAY_3))));
        when(paymentRepository.findByTargetAndProductId(PaymentTarget.CREDIT_CARD, "3"))
                .thenReturn(
                        Flowable.fromIterable(
                                List.of(
                                        new Payment(
                                                PaymentTarget.CREDIT_CARD,
                                                "3",
                                                new BigDecimal("100"),
                                                DAY_2,
                                                CUSTOMER_ID))));

        assertThat(useCase.getCreditCardMovements(CUSTOMER_ID, "3").toList().blockingGet())
                .containsExactly(
                        new ProductMovement(
                                ProductMovementType.CHARGE, new BigDecimal("300"), DAY_1),
                        new ProductMovement(
                                ProductMovementType.PAYMENT, new BigDecimal("100"), DAY_2),
                        new ProductMovement(
                                ProductMovementType.CHARGE, new BigDecimal("50"), DAY_3));
    }

    @Test
    void rejectsMovementsOfCardOfAnotherCustomer() {
        when(creditCardClient.findById("3"))
                .thenReturn(Maybe.just(new CreditCard("3", CUSTOMER_ID, new BigDecimal("1000"))));

        useCase.getCreditCardMovements("999", "3").test().assertError(NotFoundException.class);
        verify(chargeRepository, never()).findByCreditCardId(any());
        verify(paymentRepository, never()).findByTargetAndProductId(any(), any());
    }

    @Test
    void returnsLastTenCardMovementsNewestFirst() {
        when(creditCardClient.findById("3"))
                .thenReturn(Maybe.just(new CreditCard("3", CUSTOMER_ID, new BigDecimal("1000"))));
        List<Charge> charges = new ArrayList<>();
        for (int day = 1; day <= 12; day++) {
            charges.add(new Charge("3", BigDecimal.valueOf(day), LocalDate.of(2026, 11, day)));
        }
        when(chargeRepository.findByCreditCardId("3")).thenReturn(Flowable.fromIterable(charges));
        when(paymentRepository.findByTargetAndProductId(PaymentTarget.CREDIT_CARD, "3"))
                .thenReturn(Flowable.empty());

        List<ProductMovement> last =
                useCase.getCreditCardLastMovements(CUSTOMER_ID, "3").toList().blockingGet();

        assertThat(last).hasSize(10);
        assertThat(last.get(0).date()).isEqualTo(LocalDate.of(2026, 11, 12));
        assertThat(last.get(9).date()).isEqualTo(LocalDate.of(2026, 11, 3));
    }

    @Test
    void rejectsLastMovementsOfCardOfAnotherCustomer() {
        when(creditCardClient.findById("3"))
                .thenReturn(Maybe.just(new CreditCard("3", "other", new BigDecimal("1000"))));

        useCase.getCreditCardLastMovements(CUSTOMER_ID, "3").test()
                .assertError(NotFoundException.class);
    }

    @Test
    void returnsLastTenDebitCardPaymentsNewestFirst() {
        when(debitCardRepository.findById("d1"))
                .thenReturn(Maybe.just(new DebitCard("d1", CUSTOMER_ID, "a1")));
        List<Movement> payments = new ArrayList<>();
        for (int day = 1; day <= 12; day++) {
            payments.add(Movement.debitCardPayment(
                    "a1",
                    "d1",
                    BigDecimal.valueOf(day),
                    LocalDate.of(2026, 11, day)));
        }
        when(movementRepository.findByDebitCardId("d1"))
                .thenReturn(Flowable.fromIterable(payments));

        List<ProductMovement> last =
                useCase.getDebitCardLastMovements(CUSTOMER_ID, "d1").toList().blockingGet();

        assertThat(last).hasSize(10);
        assertThat(last.get(0).type()).isEqualTo(ProductMovementType.DEBIT_CARD_PAYMENT);
        assertThat(last.get(0).date()).isEqualTo(LocalDate.of(2026, 11, 12));
        assertThat(last.get(9).date()).isEqualTo(LocalDate.of(2026, 11, 3));
    }

    @Test
    void rejectsLastMovementsOfDebitCardOfAnotherCustomer() {
        when(debitCardRepository.findById("d1"))
                .thenReturn(Maybe.just(new DebitCard("d1", "otro", "a1")));

        useCase.getDebitCardLastMovements(CUSTOMER_ID, "d1").test()
                .assertError(NotFoundException.class);
    }
}
