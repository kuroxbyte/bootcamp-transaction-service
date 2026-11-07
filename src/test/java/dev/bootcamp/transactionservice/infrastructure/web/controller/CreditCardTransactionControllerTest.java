package dev.bootcamp.transactionservice.infrastructure.web.controller;

import static org.mockito.Mockito.when;

import dev.bootcamp.transactionservice.application.usecase.ChargeCreditCardUseCase;
import dev.bootcamp.transactionservice.application.usecase.GetAvailableBalanceUseCase;
import dev.bootcamp.transactionservice.application.usecase.GetProductMovementsUseCase;
import dev.bootcamp.transactionservice.application.usecase.PayCreditCardUseCase;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.charge.Charge;
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

@WebFluxTest(CreditCardTransactionController.class)
class CreditCardTransactionControllerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);
    private static final BigDecimal AMOUNT = new BigDecimal("80.5");

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
    private ChargeCreditCardUseCase chargeCreditCardUseCase;

    @MockitoBean
    private PayCreditCardUseCase payCreditCardUseCase;

    @MockitoBean
    private GetAvailableBalanceUseCase getAvailableBalanceUseCase;

    @MockitoBean
    private GetProductMovementsUseCase getProductMovementsUseCase;

    @Test
    void registersCharge() {
        when(chargeCreditCardUseCase.charge("1", "7", AMOUNT, TODAY))
                .thenReturn(Single.just(new Charge("7", AMOUNT, TODAY)));

        webTestClient.post().uri("/api/v1/credit-cards/7/charges")
                .header("X-Customer-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":80.5}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.creditCardId").isEqualTo("7")
                .jsonPath("$.amount").isEqualTo(80.5);
    }

    @Test
    void returnsUnprocessableWhenChargeExceedsAvailableCredit() {
        when(chargeCreditCardUseCase.charge("1", "7", AMOUNT, TODAY)).thenReturn(Single.error(
                new BusinessRuleException("El consumo supera el crédito disponible de la tarjeta")));

        webTestClient.post().uri("/api/v1/credit-cards/7/charges")
                .header("X-Customer-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":80.5}")
                .exchange()
                .expectStatus().isEqualTo(422);
    }

    @Test
    void registersCardPayment() {
        when(payCreditCardUseCase.pay("1", "7", AMOUNT, TODAY))
                .thenReturn(Single.just(new Payment(PaymentTarget.CREDIT_CARD, "7", AMOUNT, TODAY)));

        webTestClient.post().uri("/api/v1/credit-cards/7/payments")
                .header("X-Customer-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":80.5}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.target").isEqualTo("CREDIT_CARD");
    }

    @Test
    void returnsAvailableCredit() {
        when(getAvailableBalanceUseCase.getCreditCardAvailableCredit("1", "7"))
                .thenReturn(Single.just(new BigDecimal("4200")));

        webTestClient.get().uri("/api/v1/credit-cards/7/available-credit")
                .header("X-Customer-Id", "1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.availableCredit").isEqualTo(4200);
    }

    @Test
    void returnsCardMovements() {
        when(getProductMovementsUseCase.getCreditCardMovements("1", "7")).thenReturn(Flowable.just(
                new ProductMovement(ProductMovementType.CHARGE, AMOUNT, TODAY),
                new ProductMovement(ProductMovementType.PAYMENT, AMOUNT, TODAY)));

        webTestClient.get().uri("/api/v1/credit-cards/7/movements")
                .header("X-Customer-Id", "1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);
    }
}
