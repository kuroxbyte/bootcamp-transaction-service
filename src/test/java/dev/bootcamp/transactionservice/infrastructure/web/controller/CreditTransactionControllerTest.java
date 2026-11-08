package dev.bootcamp.transactionservice.infrastructure.web.controller;

import static org.mockito.Mockito.when;

import dev.bootcamp.transactionservice.application.usecase.GetProductMovementsUseCase;
import dev.bootcamp.transactionservice.application.usecase.PayCreditUseCase;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.history.ProductMovement;
import dev.bootcamp.transactionservice.domain.model.history.ProductMovementType;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
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

@WebFluxTest(CreditTransactionController.class)
class CreditTransactionControllerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);
    private static final BigDecimal AMOUNT = new BigDecimal("250.0");

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired private WebTestClient webTestClient;

    @MockitoBean private PayCreditUseCase payCreditUseCase;

    @MockitoBean private GetProductMovementsUseCase getProductMovementsUseCase;

    @Test
    void registersCreditPayment() {
        when(payCreditUseCase.pay("1", "5", AMOUNT, TODAY))
                .thenReturn(Single.just(new Payment(PaymentTarget.CREDIT, "5", AMOUNT, TODAY)));

        webTestClient
                .post()
                .uri("/api/v1/credits/5/payments")
                .header("X-Customer-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":250.0}")
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.target")
                .isEqualTo("CREDIT")
                .jsonPath("$.productId")
                .isEqualTo("5");
    }

    @Test
    void returnsNotFoundWhenCreditDoesNotExist() {
        when(payCreditUseCase.pay("1", "99", AMOUNT, TODAY))
                .thenReturn(Single.error(new NotFoundException("El crédito no existe")));

        webTestClient
                .post()
                .uri("/api/v1/credits/99/payments")
                .header("X-Customer-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":250.0}")
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void returnsCreditMovements() {
        when(getProductMovementsUseCase.getCreditMovements("1", "5"))
                .thenReturn(
                        Flowable.just(
                                new ProductMovement(ProductMovementType.PAYMENT, AMOUNT, TODAY)));

        webTestClient
                .get()
                .uri("/api/v1/credits/5/movements")
                .header("X-Customer-Id", "1")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[0].type")
                .isEqualTo("PAYMENT");
    }
}
