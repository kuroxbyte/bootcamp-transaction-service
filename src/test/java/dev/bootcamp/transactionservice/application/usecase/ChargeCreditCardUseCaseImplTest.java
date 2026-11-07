package dev.bootcamp.transactionservice.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.bootcamp.transactionservice.application.port.ChargeRepository;
import dev.bootcamp.transactionservice.application.port.CreditCardClient;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import dev.bootcamp.transactionservice.domain.model.creditcard.CreditCard;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChargeCreditCardUseCaseImplTest {

    private static final String CUSTOMER_ID = "100";
    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);
    private static final CreditCard CARD = new CreditCard("1", CUSTOMER_ID, new BigDecimal("1000"));

    @Mock
    private ChargeRepository chargeRepository;

    @Mock
    private CreditCardClient creditCardClient;

    @InjectMocks
    private ChargeCreditCardUseCaseImpl useCase;

    @Test
    void customerChargesOwnCardWithinAvailableCredit() {
        when(creditCardClient.findById("1")).thenReturn(Optional.of(CARD));
        when(chargeRepository.getDebt("1")).thenReturn(new BigDecimal("800"));
        when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Charge charge = useCase.charge(CUSTOMER_ID, "1", new BigDecimal("200"), DATE);

        assertThat(charge).isEqualTo(new Charge("1", new BigDecimal("200"), DATE));
    }

    @Test
    void doesNotSaveChargeOverAvailableCredit() {
        when(creditCardClient.findById("1")).thenReturn(Optional.of(CARD));
        when(chargeRepository.getDebt("1")).thenReturn(new BigDecimal("800"));

        assertThatThrownBy(() -> useCase.charge(CUSTOMER_ID, "1", new BigDecimal("200.01"), DATE))
                .isInstanceOf(BusinessRuleException.class);
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void rejectsChargeToCardOfAnotherCustomer() {
        when(creditCardClient.findById("1")).thenReturn(Optional.of(CARD));

        assertThatThrownBy(() -> useCase.charge("999", "1", BigDecimal.TEN, DATE))
                .isInstanceOf(BusinessRuleException.class);
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownCard() {
        when(creditCardClient.findById("99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.charge(CUSTOMER_ID, "99", BigDecimal.TEN, DATE))
                .isInstanceOf(BusinessRuleException.class);
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidAmountBeforeQueryingCard() {
        assertThatThrownBy(() -> useCase.charge(CUSTOMER_ID, "1", BigDecimal.ZERO, DATE))
                .isInstanceOf(BusinessRuleException.class);
        verify(creditCardClient, never()).findById(any());
        verify(chargeRepository, never()).save(any());
    }
}
