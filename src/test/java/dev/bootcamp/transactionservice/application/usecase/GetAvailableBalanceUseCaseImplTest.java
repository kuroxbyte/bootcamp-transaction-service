package dev.bootcamp.transactionservice.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.ChargeRepository;
import dev.bootcamp.transactionservice.application.port.CreditCardClient;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.account.CheckingMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.creditcard.CreditCard;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
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
        when(accountClient.findById("1")).thenReturn(Maybe.just(ACCOUNT));
        when(movementRepository.getBalance("1")).thenReturn(Single.just(new BigDecimal("350.50")));

        assertThat(useCase.getAccountBalance(CUSTOMER_ID, "1").blockingGet()).isEqualByComparingTo("350.50");
    }

    @Test
    void rejectsBalanceOfAccountOfAnotherCustomer() {
        when(accountClient.findById("1")).thenReturn(Maybe.just(ACCOUNT));

        useCase.getAccountBalance("999", "1").test().assertError(NotFoundException.class);
        verify(movementRepository, never()).getBalance(any());
    }

    @Test
    void rejectsBalanceOfUnknownAccount() {
        when(accountClient.findById("99")).thenReturn(Maybe.empty());

        useCase.getAccountBalance(CUSTOMER_ID, "99").test().assertError(NotFoundException.class);
    }

    @Test
    void returnsAvailableCreditOfOwnCard() {
        when(creditCardClient.findById("2")).thenReturn(Maybe.just(CARD));
        when(chargeRepository.getDebt("2")).thenReturn(Single.just(new BigDecimal("800")));

        assertThat(useCase.getCreditCardAvailableCredit(CUSTOMER_ID, "2").blockingGet()).isEqualByComparingTo("200");
    }

    @Test
    void returnsFullLimitWhenCardHasNoDebt() {
        when(creditCardClient.findById("2")).thenReturn(Maybe.just(CARD));
        when(chargeRepository.getDebt("2")).thenReturn(Single.just(BigDecimal.ZERO));

        assertThat(useCase.getCreditCardAvailableCredit(CUSTOMER_ID, "2").blockingGet()).isEqualByComparingTo("1000");
    }

    @Test
    void rejectsAvailableCreditOfCardOfAnotherCustomer() {
        when(creditCardClient.findById("2")).thenReturn(Maybe.just(CARD));

        useCase.getCreditCardAvailableCredit("999", "2").test().assertError(NotFoundException.class);
        verify(chargeRepository, never()).getDebt(any());
    }

    @Test
    void rejectsAvailableCreditOfUnknownCard() {
        when(creditCardClient.findById("99")).thenReturn(Maybe.empty());

        useCase.getCreditCardAvailableCredit(CUSTOMER_ID, "99").test().assertError(NotFoundException.class);
    }
}
