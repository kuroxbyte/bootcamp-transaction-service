package dev.bootcamp.transactionservice.infrastructure.web.controller;

import dev.bootcamp.transactionservice.application.usecase.GenerateProductReportUseCase;
import dev.bootcamp.transactionservice.application.usecase.GetProductMovementsUseCase;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.history.ProductMovement;
import dev.bootcamp.transactionservice.domain.model.history.ProductMovementType;
import dev.bootcamp.transactionservice.domain.model.report.MovementTotal;
import dev.bootcamp.transactionservice.domain.model.report.ProductReport;
import dev.bootcamp.transactionservice.domain.model.report.ProductType;
import dev.bootcamp.transactionservice.domain.model.report.ProductTypeReport;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(ReportController.class)
class ReportControllerTest {

    private static final LocalDate FROM = LocalDate.of(2026, 11, 1);
    private static final LocalDate TO = LocalDate.of(2026, 11, 30);

    @Autowired private WebTestClient webTestClient;

    @MockitoBean private GenerateProductReportUseCase generateProductReportUseCase;

    @MockitoBean private GetProductMovementsUseCase getProductMovementsUseCase;

    @Test
    void returnsProductReportOfTheInterval() {
        when(generateProductReportUseCase.execute(FROM, TO)).thenReturn(Single.just(
                new ProductReport(FROM, TO, List.of(new ProductTypeReport(
                        ProductType.SAVINGS,
                        2,
                        List.of(new MovementTotal(
                                ProductMovementType.DEPOSIT,
                                3,
                                new BigDecimal("300"))),
                        new BigDecimal("1.00"))))));

        webTestClient
                .get()
                .uri("/api/v1/reports/products?from=2026-11-01&to=2026-11-30")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.from")
                .isEqualTo("2026-11-01")
                .jsonPath("$.products[0].productType")
                .isEqualTo("SAVINGS")
                .jsonPath("$.products[0].openedProducts")
                .isEqualTo(2)
                .jsonPath("$.products[0].transactions")
                .isEqualTo(3)
                .jsonPath("$.products[0].commissions")
                .isEqualTo(1.00)
                .jsonPath("$.products[0].movements[0].type")
                .isEqualTo("DEPOSIT")
                .jsonPath("$.products[0].movements[0].amount")
                .isEqualTo(300);
    }

    @Test
    void rejectsMissingOrInvalidDates() {
        webTestClient.get().uri("/api/v1/reports/products?from=2026-11-01").exchange()
                .expectStatus().isBadRequest();
        webTestClient.get().uri("/api/v1/reports/products?from=2026-13-01&to=2026-11-30")
                .exchange()
                .expectStatus().isBadRequest();

        verify(generateProductReportUseCase, never()).execute(any(), any());
    }

    @Test
    void returnsUnprocessableForInvertedInterval() {
        when(generateProductReportUseCase.execute(TO, FROM)).thenReturn(Single.error(
                new BusinessRuleException(
                        "La fecha inicial no puede ser posterior a la fecha final")));

        webTestClient
                .get()
                .uri("/api/v1/reports/products?from=2026-11-30&to=2026-11-01")
                .exchange()
                .expectStatus()
                .isEqualTo(422);
    }

    @Test
    void returnsLastCreditCardMovements() {
        when(getProductMovementsUseCase.getCreditCardLastMovements("1", "t1"))
                .thenReturn(Flowable.just(
                        new ProductMovement(ProductMovementType.PAYMENT, BigDecimal.TEN, TO),
                        new ProductMovement(ProductMovementType.CHARGE, BigDecimal.ONE, FROM)));

        webTestClient
                .get()
                .uri("/api/v1/reports/credit-cards/t1/last-movements")
                .header("X-Customer-Id", "1")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.length()")
                .isEqualTo(2)
                .jsonPath("$[0].type")
                .isEqualTo("PAYMENT");
    }
}
