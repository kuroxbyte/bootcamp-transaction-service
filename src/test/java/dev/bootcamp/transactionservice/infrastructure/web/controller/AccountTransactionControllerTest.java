package dev.bootcamp.transactionservice.infrastructure.web.controller;

import dev.bootcamp.transactionservice.application.usecase.GetAvailableBalanceUseCase;
import dev.bootcamp.transactionservice.application.usecase.GetDailyAverageBalanceUseCase;
import dev.bootcamp.transactionservice.application.usecase.GetProductMovementsUseCase;
import dev.bootcamp.transactionservice.application.usecase.RegisterMovementUseCase;
import dev.bootcamp.transactionservice.application.usecase.TransferUseCase;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.account.DailyAverageBalance;
import dev.bootcamp.transactionservice.domain.model.history.ProductMovement;
import dev.bootcamp.transactionservice.domain.model.history.ProductMovementType;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import dev.bootcamp.transactionservice.domain.model.transfer.Transfer;
import dev.bootcamp.transactionservice.domain.model.transfer.TransferType;
import dev.bootcamp.transactionservice.infrastructure.client.ServiceUnavailableException;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(AccountTransactionController.class)
class AccountTransactionControllerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);
    private static final BigDecimal AMOUNT = new BigDecimal("100.0");

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired private WebTestClient webTestClient;

    @MockitoBean private RegisterMovementUseCase registerMovementUseCase;

    @MockitoBean private GetAvailableBalanceUseCase getAvailableBalanceUseCase;

    @MockitoBean private GetProductMovementsUseCase getProductMovementsUseCase;

    @MockitoBean private GetDailyAverageBalanceUseCase getDailyAverageBalanceUseCase;

    @MockitoBean private TransferUseCase transferUseCase;

    @Test
    void transfersToOwnAccountWithServerDate() {
        when(transferUseCase.transferToOwnAccount("1", "10", "11", AMOUNT, TODAY))
                .thenReturn(Single.just(new Transfer(
                        TransferType.OWN,
                        new Movement("10", MovementType.TRANSFER_OUT, AMOUNT, TODAY),
                        new Movement("11", MovementType.TRANSFER_IN, AMOUNT, TODAY)
                                .withCommission(new BigDecimal("2")))));

        webTestClient
                .post()
                .uri("/api/v1/accounts/10/transfers/own")
                .header("X-Customer-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"targetAccountId\":\"11\",\"amount\":100.0}")
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.type")
                .isEqualTo("OWN")
                .jsonPath("$.sourceAccountId")
                .isEqualTo("10")
                .jsonPath("$.targetAccountId")
                .isEqualTo("11")
                .jsonPath("$.date")
                .isEqualTo("2026-11-15")
                .jsonPath("$.sourceCommission")
                .isEqualTo(0)
                .jsonPath("$.targetCommission")
                .isEqualTo(2);
    }

    @Test
    void transfersToThirdParty() {
        when(transferUseCase.transferToThirdParty("1", "10", "20", AMOUNT, TODAY))
                .thenReturn(Single.just(new Transfer(
                        TransferType.THIRD_PARTY,
                        new Movement("10", MovementType.TRANSFER_OUT, AMOUNT, TODAY),
                        new Movement("20", MovementType.TRANSFER_IN, AMOUNT, TODAY))));

        webTestClient
                .post()
                .uri("/api/v1/accounts/10/transfers/third-party")
                .header("X-Customer-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"targetAccountId\":\"20\",\"amount\":100.0}")
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.type")
                .isEqualTo("THIRD_PARTY");
    }

    @Test
    void rejectsTransferWithoutTargetAccount() {
        webTestClient
                .post()
                .uri("/api/v1/accounts/10/transfers/own")
                .header("X-Customer-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":100.0}")
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(transferUseCase, never()).transferToOwnAccount(any(), any(), any(), any(), any());
    }

    @Test
    void returnsUnprocessableWhenTransferBreaksRule() {
        when(transferUseCase.transferToThirdParty("1", "10", "11", AMOUNT, TODAY))
                .thenReturn(Single.error(new BusinessRuleException(
                        "La cuenta destino es del cliente; use la transferencia entre cuentas"
                                + " propias")));

        webTestClient
                .post()
                .uri("/api/v1/accounts/10/transfers/third-party")
                .header("X-Customer-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"targetAccountId\":\"11\",\"amount\":100.0}")
                .exchange()
                .expectStatus()
                .isEqualTo(422);
    }

    @Test
    void returnsDailyAverageOfTheMonth() {
        when(getDailyAverageBalanceUseCase.execute("1", "10", YearMonth.of(2026, 11)))
                .thenReturn(Single.just(new DailyAverageBalance(
                        YearMonth.of(2026, 11),
                        15,
                        new BigDecimal("640.50"),
                        new BigDecimal("500"))));

        webTestClient
                .get()
                .uri("/api/v1/accounts/10/daily-average?month=2026-11")
                .header("X-Customer-Id", "1")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.accountId")
                .isEqualTo("10")
                .jsonPath("$.month")
                .isEqualTo("2026-11")
                .jsonPath("$.days")
                .isEqualTo(15)
                .jsonPath("$.averageDailyBalance")
                .isEqualTo(640.50)
                .jsonPath("$.minimumDailyAverage")
                .isEqualTo(500)
                .jsonPath("$.meetsMinimum")
                .isEqualTo(true);
    }

    @Test
    void rejectsInvalidMonth() {
        webTestClient
                .get()
                .uri("/api/v1/accounts/10/daily-average?month=2026-13")
                .header("X-Customer-Id", "1")
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(getDailyAverageBalanceUseCase, never()).execute(any(), any(), any());
    }

    @Test
    void returnsUnprocessableWhenAccountWasNotOpenInTheMonth() {
        when(getDailyAverageBalanceUseCase.execute("1", "10", YearMonth.of(2026, 1)))
                .thenReturn(Single.error(new BusinessRuleException(
                        "La cuenta no estuvo abierta ningún día del mes consultado")));

        webTestClient
                .get()
                .uri("/api/v1/accounts/10/daily-average?month=2026-01")
                .header("X-Customer-Id", "1")
                .exchange()
                .expectStatus()
                .isEqualTo(422);
    }

    @Test
    void registersDepositWithServerDate() {
        when(registerMovementUseCase.deposit("1", "10", AMOUNT, TODAY))
                .thenReturn(Single.just(new Movement("10", MovementType.DEPOSIT, AMOUNT, TODAY)));

        webTestClient
                .post()
                .uri("/api/v1/accounts/10/deposits")
                .header("X-Customer-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":100.0}")
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.accountId")
                .isEqualTo("10")
                .jsonPath("$.type")
                .isEqualTo("DEPOSIT")
                .jsonPath("$.date")
                .isEqualTo("2026-11-15")
                .jsonPath("$.commission")
                .isEqualTo(0);
    }

    @Test
    void registersWithdrawal() {
        when(registerMovementUseCase.withdraw("1", "10", AMOUNT, TODAY))
                .thenReturn(
                        Single.just(new Movement("10", MovementType.WITHDRAWAL, AMOUNT, TODAY)));

        webTestClient
                .post()
                .uri("/api/v1/accounts/10/withdrawals")
                .header("X-Customer-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":100.0}")
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.type")
                .isEqualTo("WITHDRAWAL");
    }

    @Test
    void rejectsNonPositiveAmount() {
        webTestClient
                .post()
                .uri("/api/v1/accounts/10/deposits")
                .header("X-Customer-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":0}")
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(registerMovementUseCase, never()).deposit(any(), any(), any(), any());
    }

    @Test
    void returnsUnprocessableWhenWithdrawalExceedsBalance() {
        when(registerMovementUseCase.withdraw("1", "10", AMOUNT, TODAY))
                .thenReturn(
                        Single.error(
                                new BusinessRuleException("Saldo insuficiente para el retiro")));

        webTestClient
                .post()
                .uri("/api/v1/accounts/10/withdrawals")
                .header("X-Customer-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":100.0}")
                .exchange()
                .expectStatus()
                .isEqualTo(422)
                .expectBody()
                .jsonPath("$.detail")
                .isEqualTo("Saldo insuficiente para el retiro");
    }

    @Test
    void returnsBalance() {
        when(getAvailableBalanceUseCase.getAccountBalance("1", "10"))
                .thenReturn(Single.just(new BigDecimal("350.5")));

        webTestClient
                .get()
                .uri("/api/v1/accounts/10/balance")
                .header("X-Customer-Id", "1")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.accountId")
                .isEqualTo("10")
                .jsonPath("$.balance")
                .isEqualTo(350.5);
    }

    @Test
    void returnsMovements() {
        when(getProductMovementsUseCase.getAccountMovements("1", "10"))
                .thenReturn(
                        Flowable.just(
                                new ProductMovement(ProductMovementType.DEPOSIT, AMOUNT, TODAY)));

        webTestClient
                .get()
                .uri("/api/v1/accounts/10/movements")
                .header("X-Customer-Id", "1")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[0].type")
                .isEqualTo("DEPOSIT");
    }

    @Test
    void returnsNotFoundForMovementsOfAccountOfAnotherCustomer() {
        when(getProductMovementsUseCase.getAccountMovements("999", "10"))
                .thenReturn(
                        Flowable.error(new NotFoundException("La cuenta no pertenece al cliente")));

        webTestClient
                .get()
                .uri("/api/v1/accounts/10/movements")
                .header("X-Customer-Id", "999")
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectHeader()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void returnsServiceUnavailableWhenAccountServiceIsDown() {
        when(getAvailableBalanceUseCase.getAccountBalance("1", "10"))
                .thenReturn(
                        Single.error(
                                new ServiceUnavailableException(
                                        "account-service no está disponible",
                                        new RuntimeException())));

        webTestClient
                .get()
                .uri("/api/v1/accounts/10/balance")
                .header("X-Customer-Id", "1")
                .exchange()
                .expectStatus()
                .isEqualTo(503)
                .expectHeader()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
