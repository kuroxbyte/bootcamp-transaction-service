package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.ChargeRepository;
import dev.bootcamp.transactionservice.application.port.CreditCardClient;
import dev.bootcamp.transactionservice.application.port.TransactionEventPublisher;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import dev.bootcamp.transactionservice.domain.model.creditcard.CreditCard;
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
class ChargeCreditCardUseCaseImplTest {

    private static final String CUSTOMER_ID = "100";
    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);
    private static final CreditCard CARD = new CreditCard("1", CUSTOMER_ID, new BigDecimal("1000"));

    @Mock private ChargeRepository chargeRepository;

    @Mock private CreditCardClient creditCardClient;

    @Mock private TransactionEventPublisher transactionEventPublisher;

    @InjectMocks private ChargeCreditCardUseCaseImpl useCase;

    /**
     * Publicar siempre termina bien, salvo en las pruebas que verifican lo contrario.
     */
    @BeforeEach
    void eventsArePublished() {
        lenient().when(transactionEventPublisher.publishChargeRegistered(any(), any()))
                .thenReturn(Completable.complete());
    }

    @Test
    void customerChargesOwnCardWithinAvailableCredit() {
        when(creditCardClient.findById("1")).thenReturn(Maybe.just(CARD));
        when(chargeRepository.getDebt("1")).thenReturn(Single.just(new BigDecimal("800")));
        when(chargeRepository.save(any())).thenAnswer(inv -> Single.just(inv.getArgument(0)));

        Charge charge = useCase.charge(CUSTOMER_ID, "1", new BigDecimal("200"), DATE).blockingGet();

        assertThat(charge).isEqualTo(new Charge("1", new BigDecimal("200"), DATE));
    }

    @Test
    void doesNotSaveChargeOverAvailableCredit() {
        when(creditCardClient.findById("1")).thenReturn(Maybe.just(CARD));
        when(chargeRepository.getDebt("1")).thenReturn(Single.just(new BigDecimal("800")));

        useCase.charge(CUSTOMER_ID, "1", new BigDecimal("200.01"), DATE)
                .test()
                .assertError(BusinessRuleException.class);
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void rejectsChargeToCardOfAnotherCustomer() {
        when(creditCardClient.findById("1")).thenReturn(Maybe.just(CARD));

        useCase.charge("999", "1", BigDecimal.TEN, DATE)
                .test()
                .assertError(NotFoundException.class);
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownCard() {
        when(creditCardClient.findById("99")).thenReturn(Maybe.empty());

        useCase.charge(CUSTOMER_ID, "99", BigDecimal.TEN, DATE)
                .test()
                .assertError(NotFoundException.class);
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidAmountBeforeQueryingCard() {
        useCase.charge(CUSTOMER_ID, "1", BigDecimal.ZERO, DATE)
                .test()
                .assertError(BusinessRuleException.class);
        verify(creditCardClient, never()).findById(any());
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void publishesTheChargeOnlyWhenSaved() {
        when(creditCardClient.findById("1")).thenReturn(Maybe.just(CARD));
        when(chargeRepository.getDebt("1")).thenReturn(Single.just(BigDecimal.ZERO));
        when(chargeRepository.save(any())).thenAnswer(inv -> Single.just(inv.getArgument(0)));

        Charge charge = useCase.charge(CUSTOMER_ID, "1", BigDecimal.TEN, DATE).blockingGet();

        verify(transactionEventPublisher).publishChargeRegistered(CUSTOMER_ID, charge);
    }
}
