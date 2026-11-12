package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.CreditCardClient;
import dev.bootcamp.transactionservice.application.port.PaymentRepository;
import dev.bootcamp.transactionservice.application.port.TransactionEventPublisher;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.creditcard.CreditCard;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayCreditCardUseCaseImplTest {

    private static final String CUSTOMER_ID = "100";
    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);
    private static final BigDecimal AMOUNT = new BigDecimal("250.00");
    private static final CreditCard CARD = new CreditCard("1", CUSTOMER_ID, new BigDecimal("1000"));

    @Mock private PaymentRepository paymentRepository;

    @Mock private CreditCardClient creditCardClient;

    @Mock private TransactionEventPublisher transactionEventPublisher;

    @InjectMocks private PayCreditCardUseCaseImpl useCase;

    /**
     * Publicar siempre termina bien, salvo en las pruebas que verifican lo contrario.
     */
    @BeforeEach
    void eventsArePublished() {
        lenient().when(transactionEventPublisher.publishPaymentRegistered(any(), any()))
                .thenReturn(Completable.complete());
    }

    @Test
    void customerPaysOwnCreditCard() {
        when(creditCardClient.findById("1")).thenReturn(Maybe.just(CARD));
        when(paymentRepository.save(any())).thenAnswer(inv -> Single.just(inv.getArgument(0)));

        Payment payment = useCase.pay(CUSTOMER_ID, "1", AMOUNT, DATE).blockingGet();

        assertThat(payment).isEqualTo(
                new Payment(PaymentTarget.CREDIT_CARD, "1", AMOUNT, DATE, CUSTOMER_ID));
    }

    @Test
    void rejectsPaymentToCardOfAnotherCustomer() {
        when(creditCardClient.findById("1")).thenReturn(Maybe.just(CARD));

        useCase.pay("999", "1", AMOUNT, DATE).test().assertError(NotFoundException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownCard() {
        when(creditCardClient.findById("99")).thenReturn(Maybe.empty());

        useCase.pay(CUSTOMER_ID, "99", AMOUNT, DATE).test().assertError(NotFoundException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidAmountBeforeQueryingCard() {
        useCase.pay(CUSTOMER_ID, "1", BigDecimal.ZERO, DATE)
                .test()
                .assertError(BusinessRuleException.class);
        verify(creditCardClient, never()).findById(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void publishesTheCardPaymentWithItsCustomer() {
        when(creditCardClient.findById("1")).thenReturn(Maybe.just(
                new CreditCard("1", CUSTOMER_ID, new BigDecimal("1000"))));
        when(paymentRepository.save(any())).thenAnswer(inv -> Single.just(inv.getArgument(0)));

        Payment payment = useCase.pay(CUSTOMER_ID, "1", AMOUNT, DATE).blockingGet();

        verify(transactionEventPublisher).publishPaymentRegistered(CUSTOMER_ID, payment);
    }

    @Test
    void customerPaysCardOfAnotherCustomer() {
        when(creditCardClient.findById("9")).thenReturn(Maybe.just(
                new CreditCard("9", "otro-cliente", new BigDecimal("1000"))));
        when(paymentRepository.save(any())).thenAnswer(inv -> Single.just(inv.getArgument(0)));

        Payment payment = useCase.payThirdParty(CUSTOMER_ID, "9", AMOUNT, DATE).blockingGet();

        assertThat(payment.payerCustomerId()).isEqualTo(CUSTOMER_ID);
        verify(transactionEventPublisher).publishPaymentRegistered("otro-cliente", payment);
    }

    @Test
    void thirdPartyPaymentRejectsOwnCard() {
        when(creditCardClient.findById("1")).thenReturn(Maybe.just(CARD));

        useCase.payThirdParty(CUSTOMER_ID, "1", AMOUNT, DATE).test()
                .assertError(BusinessRuleException.class);
        verify(paymentRepository, never()).save(any());
    }
}
