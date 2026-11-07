package dev.bootcamp.transactionservice.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.bootcamp.transactionservice.application.usecase.GetAvailableBalanceUseCase;
import dev.bootcamp.transactionservice.application.usecase.GetProductMovementsUseCase;
import dev.bootcamp.transactionservice.application.usecase.RegisterMovementUseCase;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.history.ProductMovement;
import dev.bootcamp.transactionservice.domain.model.history.ProductMovementType;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

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

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private RegisterMovementUseCase registerMovementUseCase;

    @MockitoBean
    private GetAvailableBalanceUseCase getAvailableBalanceUseCase;

    @MockitoBean
    private GetProductMovementsUseCase getProductMovementsUseCase;

    @Test
    void registersDepositWithServerDate() {
        when(registerMovementUseCase.deposit("1", "10", AMOUNT, TODAY))
                .thenReturn(Single.just(new Movement("10", MovementType.DEPOSIT, AMOUNT, TODAY)));

        webTestClient.post().uri("/api/v1/accounts/10/deposits")
                .header("X-Customer-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":100.0}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.accountId").isEqualTo("10")
                .jsonPath("$.type").isEqualTo("DEPOSIT")
                .jsonPath("$.date").isEqualTo("2026-11-15");
    }

    @Test
    void registersWithdrawal() {
        when(registerMovementUseCase.withdraw("1", "10", AMOUNT, TODAY))
                .thenReturn(Single.just(new Movement("10", MovementType.WITHDRAWAL, AMOUNT, TODAY)));

        webTestClient.post().uri("/api/v1/accounts/10/withdrawals")
                .header("X-Customer-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":100.0}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.type").isEqualTo("WITHDRAWAL");
    }

    @Test
    void rejectsNonPositiveAmount() {
        webTestClient.post().uri("/api/v1/accounts/10/deposits")
                .header("X-Customer-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":0}")
                .exchange()
                .expectStatus().isBadRequest();

        verify(registerMovementUseCase, never()).deposit(any(), any(), any(), any());
    }

    @Test
    void returnsUnprocessableWhenWithdrawalExceedsBalance() {
        when(registerMovementUseCase.withdraw("1", "10", AMOUNT, TODAY))
                .thenReturn(Single.error(new BusinessRuleException("Saldo insuficiente para el retiro")));

        webTestClient.post().uri("/api/v1/accounts/10/withdrawals")
                .header("X-Customer-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":100.0}")
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Saldo insuficiente para el retiro");
    }

    @Test
    void returnsBalance() {
        when(getAvailableBalanceUseCase.getAccountBalance("1", "10")).thenReturn(Single.just(new BigDecimal("350.5")));

        webTestClient.get().uri("/api/v1/accounts/10/balance")
                .header("X-Customer-Id", "1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accountId").isEqualTo("10")
                .jsonPath("$.balance").isEqualTo(350.5);
    }

    @Test
    void returnsMovements() {
        when(getProductMovementsUseCase.getAccountMovements("1", "10")).thenReturn(Flowable.just(
                new ProductMovement(ProductMovementType.DEPOSIT, AMOUNT, TODAY)));

        webTestClient.get().uri("/api/v1/accounts/10/movements")
                .header("X-Customer-Id", "1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].type").isEqualTo("DEPOSIT");
    }

    @Test
    void returnsNotFoundForMovementsOfAccountOfAnotherCustomer() {
        when(getProductMovementsUseCase.getAccountMovements("999", "10"))
                .thenReturn(Flowable.error(new NotFoundException("La cuenta no pertenece al cliente")));

        webTestClient.get().uri("/api/v1/accounts/10/movements")
                .header("X-Customer-Id", "999")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
