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
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.account.CheckingMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.creditcard.CreditCard;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetAvailableBalanceUseCaseImplTest {

    private static final String CUSTOMER_ID = "100";
    private static final Account ACCOUNT = new Account("1", CUSTOMER_ID, new CheckingMovementPolicy());
    private static final CreditCard CARD = new CreditCard("2", CUSTOMER_ID, new BigDecimal("1000"));

    @Mock
    private AccountClient accountClient;

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private CreditCardClient creditCardClient;

    @Mock
    private ChargeRepository chargeRepository;

    @InjectMocks
    private GetAvailableBalanceUseCaseImpl useCase;

    @Test
    void returnsBalanceOfOwnAccount() {
        when(accountClient.findById("1")).thenReturn(Optional.of(ACCOUNT));
        when(movementRepository.getBalance("1")).thenReturn(new BigDecimal("350.50"));

        assertThat(useCase.getAccountBalance(CUSTOMER_ID, "1")).isEqualByComparingTo("350.50");
    }

    @Test
    void rejectsBalanceOfAccountOfAnotherCustomer() {
        when(accountClient.findById("1")).thenReturn(Optional.of(ACCOUNT));

        assertThatThrownBy(() -> useCase.getAccountBalance("999", "1"))
                .isInstanceOf(BusinessRuleException.class);
        verify(movementRepository, never()).getBalance(any());
    }

    @Test
    void rejectsBalanceOfUnknownAccount() {
        when(accountClient.findById("99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getAccountBalance(CUSTOMER_ID, "99"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void returnsAvailableCreditOfOwnCard() {
        when(creditCardClient.findById("2")).thenReturn(Optional.of(CARD));
        when(chargeRepository.getDebt("2")).thenReturn(new BigDecimal("800"));

        assertThat(useCase.getCreditCardAvailableCredit(CUSTOMER_ID, "2")).isEqualByComparingTo("200");
    }

    @Test
    void returnsFullLimitWhenCardHasNoDebt() {
        when(creditCardClient.findById("2")).thenReturn(Optional.of(CARD));
        when(chargeRepository.getDebt("2")).thenReturn(BigDecimal.ZERO);

        assertThat(useCase.getCreditCardAvailableCredit(CUSTOMER_ID, "2")).isEqualByComparingTo("1000");
    }

    @Test
    void rejectsAvailableCreditOfCardOfAnotherCustomer() {
        when(creditCardClient.findById("2")).thenReturn(Optional.of(CARD));

        assertThatThrownBy(() -> useCase.getCreditCardAvailableCredit("999", "2"))
                .isInstanceOf(BusinessRuleException.class);
        verify(chargeRepository, never()).getDebt(any());
    }

    @Test
    void rejectsAvailableCreditOfUnknownCard() {
        when(creditCardClient.findById("99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getCreditCardAvailableCredit(CUSTOMER_ID, "99"))
                .isInstanceOf(BusinessRuleException.class);
    }
}
