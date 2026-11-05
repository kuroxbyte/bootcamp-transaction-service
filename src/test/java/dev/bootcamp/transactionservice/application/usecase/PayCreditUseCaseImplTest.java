package dev.bootcamp.transactionservice.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.bootcamp.transactionservice.application.port.CreditClient;
import dev.bootcamp.transactionservice.application.port.PaymentRepository;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.credit.Credit;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PayCreditUseCaseImplTest {

    private static final Long CUSTOMER_ID = 100L;
    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);
    private static final BigDecimal AMOUNT = new BigDecimal("250.00");

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CreditClient creditClient;

    @InjectMocks
    private PayCreditUseCaseImpl useCase;

    @Test
    void customerPaysOwnCredit() {
        when(creditClient.findById(1L)).thenReturn(Optional.of(new Credit(1L, CUSTOMER_ID)));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payment payment = useCase.pay(CUSTOMER_ID, 1L, AMOUNT, DATE);

        assertThat(payment).isEqualTo(new Payment(1L, AMOUNT, DATE));
    }

    @Test
    void rejectsPaymentToCreditOfAnotherCustomer() {
        when(creditClient.findById(1L)).thenReturn(Optional.of(new Credit(1L, CUSTOMER_ID)));

        assertThatThrownBy(() -> useCase.pay(999L, 1L, AMOUNT, DATE))
                .isInstanceOf(BusinessRuleException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownCredit() {
        when(creditClient.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.pay(CUSTOMER_ID, 99L, AMOUNT, DATE))
                .isInstanceOf(BusinessRuleException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidAmountBeforeQueryingCredit() {
        assertThatThrownBy(() -> useCase.pay(CUSTOMER_ID, 1L, BigDecimal.ZERO, DATE))
                .isInstanceOf(BusinessRuleException.class);
        verify(creditClient, never()).findById(any());
        verify(paymentRepository, never()).save(any());
    }
}
