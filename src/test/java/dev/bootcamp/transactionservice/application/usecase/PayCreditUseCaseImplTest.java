package dev.bootcamp.transactionservice.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.bootcamp.transactionservice.application.port.CreditClient;
import dev.bootcamp.transactionservice.application.port.PaymentRepository;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.credit.Credit;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PayCreditUseCaseImplTest {

    private static final String CUSTOMER_ID = "100";
    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);
    private static final BigDecimal AMOUNT = new BigDecimal("250.00");

    @Mock private PaymentRepository paymentRepository;

    @Mock private CreditClient creditClient;

    @InjectMocks private PayCreditUseCaseImpl useCase;

    @Test
    void customerPaysOwnCredit() {
        when(creditClient.findById("1")).thenReturn(Maybe.just(new Credit("1", CUSTOMER_ID)));
        when(paymentRepository.save(any())).thenAnswer(inv -> Single.just(inv.getArgument(0)));

        Payment payment = useCase.pay(CUSTOMER_ID, "1", AMOUNT, DATE).blockingGet();

        assertThat(payment).isEqualTo(new Payment(PaymentTarget.CREDIT, "1", AMOUNT, DATE));
    }

    @Test
    void rejectsPaymentToCreditOfAnotherCustomer() {
        when(creditClient.findById("1")).thenReturn(Maybe.just(new Credit("1", CUSTOMER_ID)));

        useCase.pay("999", "1", AMOUNT, DATE).test().assertError(NotFoundException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownCredit() {
        when(creditClient.findById("99")).thenReturn(Maybe.empty());

        useCase.pay(CUSTOMER_ID, "99", AMOUNT, DATE).test().assertError(NotFoundException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidAmountBeforeQueryingCredit() {
        useCase.pay(CUSTOMER_ID, "1", BigDecimal.ZERO, DATE)
                .test()
                .assertError(BusinessRuleException.class);
        verify(creditClient, never()).findById(any());
        verify(paymentRepository, never()).save(any());
    }
}
