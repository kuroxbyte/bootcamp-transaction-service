package dev.bootcamp.transactionservice.infrastructure.web.controller;

import dev.bootcamp.transactionservice.application.usecase.RegisterMovementUseCase;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
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
import java.time.ZoneOffset;

import static org.mockito.Mockito.when;

@WebFluxTest(DebitCardTransactionController.class)
class DebitCardTransactionControllerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);
    private static final BigDecimal AMOUNT = new BigDecimal("40.0");

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired private WebTestClient webTestClient;

    @MockitoBean private RegisterMovementUseCase registerMovementUseCase;

    private WebTestClient.ResponseSpec pay(String debitCardId) {
        return webTestClient.post()
                .uri("/api/v1/debit-cards/" + debitCardId + "/payments")
                .header("X-Customer-Id", "c1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":40.0}")
                .exchange();
    }

    @Test
    void registersPaymentFromThePrimaryAccount() {
        when(registerMovementUseCase.payWithDebitCard("c1", "d1", AMOUNT, TODAY))
                .thenReturn(Single.just(Movement.debitCardPayment("a1", "d1", AMOUNT, TODAY)));

        pay("d1")
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.accountId")
                .isEqualTo("a1")
                .jsonPath("$.type")
                .isEqualTo("DEBIT_CARD_PAYMENT")
                .jsonPath("$.debitCardId")
                .isEqualTo("d1")
                .jsonPath("$.commission")
                .isEqualTo(0);
    }

    @Test
    void returnsNotFoundAndUnprocessable() {
        when(registerMovementUseCase.payWithDebitCard("c1", "d9", AMOUNT, TODAY))
                .thenReturn(Single.error(new NotFoundException("no existe")));
        when(registerMovementUseCase.payWithDebitCard("c1", "d1", AMOUNT, TODAY))
                .thenReturn(Single.error(new BusinessRuleException("saldo insuficiente")));

        pay("d9").expectStatus().isNotFound();
        pay("d1").expectStatus().isEqualTo(422);
    }
}
