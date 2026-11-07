package dev.bootcamp.transactionservice.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.bootcamp.transactionservice.application.port.CreditCardClient;
import dev.bootcamp.transactionservice.application.port.PaymentRepository;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.creditcard.CreditCard;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PayCreditCardUseCaseImplTest {

    private static final String CUSTOMER_ID = "100";
    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);
    private static final BigDecimal AMOUNT = new BigDecimal("250.00");
    private static final CreditCard CARD = new CreditCard("1", CUSTOMER_ID, new BigDecimal("1000"));

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CreditCardClient creditCardClient;

    @InjectMocks
    private PayCreditCardUseCaseImpl useCase;

    @Test
    void customerPaysOwnCreditCard() {
        when(creditCardClient.findById("1")).thenReturn(Optional.of(CARD));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payment payment = useCase.pay(CUSTOMER_ID, "1", AMOUNT, DATE);

        assertThat(payment).isEqualTo(new Payment(PaymentTarget.CREDIT_CARD, "1", AMOUNT, DATE));
    }

    @Test
    void rejectsPaymentToCardOfAnotherCustomer() {
        when(creditCardClient.findById("1")).thenReturn(Optional.of(CARD));

        assertThatThrownBy(() -> useCase.pay("999", "1", AMOUNT, DATE))
                .isInstanceOf(BusinessRuleException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownCard() {
        when(creditCardClient.findById("99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.pay(CUSTOMER_ID, "99", AMOUNT, DATE))
                .isInstanceOf(BusinessRuleException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidAmountBeforeQueryingCard() {
        assertThatThrownBy(() -> useCase.pay(CUSTOMER_ID, "1", BigDecimal.ZERO, DATE))
                .isInstanceOf(BusinessRuleException.class);
        verify(creditCardClient, never()).findById(any());
        verify(paymentRepository, never()).save(any());
    }
}
