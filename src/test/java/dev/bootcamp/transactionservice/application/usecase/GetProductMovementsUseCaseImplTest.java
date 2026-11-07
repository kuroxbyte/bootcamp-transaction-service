package dev.bootcamp.transactionservice.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.ChargeRepository;
import dev.bootcamp.transactionservice.application.port.CreditCardClient;
import dev.bootcamp.transactionservice.application.port.CreditClient;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.application.port.PaymentRepository;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.account.CheckingMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import dev.bootcamp.transactionservice.domain.model.credit.Credit;
import dev.bootcamp.transactionservice.domain.model.creditcard.CreditCard;
import dev.bootcamp.transactionservice.domain.model.history.ProductMovement;
import dev.bootcamp.transactionservice.domain.model.history.ProductMovementType;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetProductMovementsUseCaseImplTest {

    private static final String CUSTOMER_ID = "100";
    private static final LocalDate DAY_1 = LocalDate.of(2026, 11, 1);
    private static final LocalDate DAY_2 = LocalDate.of(2026, 11, 2);
    private static final LocalDate DAY_3 = LocalDate.of(2026, 11, 3);

    @Mock
    private AccountClient accountClient;
    @Mock
    private CreditClient creditClient;
    @Mock
    private CreditCardClient creditCardClient;
    @Mock
    private MovementRepository movementRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ChargeRepository chargeRepository;

    @InjectMocks
    private GetProductMovementsUseCaseImpl useCase;

    @Test
    void returnsAccountMovementsSortedByDate() {
        when(accountClient.findById("1")).thenReturn(Optional.of(new Account("1", CUSTOMER_ID, new CheckingMovementPolicy())));
        when(movementRepository.findByAccountId("1")).thenReturn(List.of(
                new Movement("1", MovementType.WITHDRAWAL, BigDecimal.ONE, DAY_2),
                new Movement("1", MovementType.DEPOSIT, BigDecimal.TEN, DAY_1)));

        List<ProductMovement> movements = useCase.getAccountMovements(CUSTOMER_ID, "1");

        assertThat(movements).containsExactly(
                new ProductMovement(ProductMovementType.DEPOSIT, BigDecimal.TEN, DAY_1),
                new ProductMovement(ProductMovementType.WITHDRAWAL, BigDecimal.ONE, DAY_2));
    }

    @Test
    void returnsEmptyListWhenAccountHasNoMovements() {
        when(accountClient.findById("1")).thenReturn(Optional.of(new Account("1", CUSTOMER_ID, new CheckingMovementPolicy())));
        when(movementRepository.findByAccountId("1")).thenReturn(List.of());

        assertThat(useCase.getAccountMovements(CUSTOMER_ID, "1")).isEmpty();
    }

    @Test
    void rejectsMovementsOfAccountOfAnotherCustomer() {
        when(accountClient.findById("1")).thenReturn(Optional.of(new Account("1", CUSTOMER_ID, new CheckingMovementPolicy())));

        assertThatThrownBy(() -> useCase.getAccountMovements("999", "1"))
                .isInstanceOf(BusinessRuleException.class);
        verify(movementRepository, never()).findByAccountId(any());
    }

    @Test
    void rejectsMovementsOfUnknownAccount() {
        when(accountClient.findById("99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getAccountMovements(CUSTOMER_ID, "99"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void returnsCreditPayments() {
        when(creditClient.findById("2")).thenReturn(Optional.of(new Credit("2", CUSTOMER_ID)));
        when(paymentRepository.findByTargetAndProductId(PaymentTarget.CREDIT, "2")).thenReturn(List.of(
                new Payment(PaymentTarget.CREDIT, "2", BigDecimal.TEN, DAY_1)));

        assertThat(useCase.getCreditMovements(CUSTOMER_ID, "2")).containsExactly(
                new ProductMovement(ProductMovementType.PAYMENT, BigDecimal.TEN, DAY_1));
    }

    @Test
    void rejectsMovementsOfCreditOfAnotherCustomer() {
        when(creditClient.findById("2")).thenReturn(Optional.of(new Credit("2", CUSTOMER_ID)));

        assertThatThrownBy(() -> useCase.getCreditMovements("999", "2"))
                .isInstanceOf(BusinessRuleException.class);
        verify(paymentRepository, never()).findByTargetAndProductId(any(), any());
    }

    @Test
    void returnsCardChargesAndPaymentsMergedAndSortedByDate() {
        when(creditCardClient.findById("3"))
                .thenReturn(Optional.of(new CreditCard("3", CUSTOMER_ID, new BigDecimal("1000"))));
        when(chargeRepository.findByCreditCardId("3")).thenReturn(List.of(
                new Charge("3", new BigDecimal("300"), DAY_1),
                new Charge("3", new BigDecimal("50"), DAY_3)));
        when(paymentRepository.findByTargetAndProductId(PaymentTarget.CREDIT_CARD, "3")).thenReturn(List.of(
                new Payment(PaymentTarget.CREDIT_CARD, "3", new BigDecimal("100"), DAY_2)));

        assertThat(useCase.getCreditCardMovements(CUSTOMER_ID, "3")).containsExactly(
                new ProductMovement(ProductMovementType.CHARGE, new BigDecimal("300"), DAY_1),
                new ProductMovement(ProductMovementType.PAYMENT, new BigDecimal("100"), DAY_2),
                new ProductMovement(ProductMovementType.CHARGE, new BigDecimal("50"), DAY_3));
    }

    @Test
    void rejectsMovementsOfCardOfAnotherCustomer() {
        when(creditCardClient.findById("3"))
                .thenReturn(Optional.of(new CreditCard("3", CUSTOMER_ID, new BigDecimal("1000"))));

        assertThatThrownBy(() -> useCase.getCreditCardMovements("999", "3"))
                .isInstanceOf(BusinessRuleException.class);
        verify(chargeRepository, never()).findByCreditCardId(any());
        verify(paymentRepository, never()).findByTargetAndProductId(any(), any());
    }
}
