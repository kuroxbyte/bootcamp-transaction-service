package dev.bootcamp.transactionservice.infrastructure.client.report;

import dev.bootcamp.transactionservice.domain.model.report.BankProduct;
import dev.bootcamp.transactionservice.domain.model.report.ProductType;
import dev.bootcamp.transactionservice.infrastructure.client.ServiceUnavailableException;
import dev.bootcamp.transactionservice.infrastructure.client.StubWebClient;
import dev.bootcamp.transactionservice.infrastructure.client.TestCircuitBreakers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class BankProductWebClientAdapterTest {

    private static BankProductWebClientAdapter adapter(StubWebClient stub) {
        return new BankProductWebClientAdapter(
                stub.builder(),
                "http://account-service",
                "http://credit-service",
                TestCircuitBreakers.remoteLookup(Duration.ofSeconds(1)));
    }

    @Test
    void listsAccountsCreditsAndCreditCardsWithTheirType() {
        StubWebClient stub = StubWebClient.respondingByPath(Map.of(
                "/api/v1/internal/accounts",
                "[{\"id\":\"a1\",\"customerId\":\"1\",\"type\":\"CHECKING\","
                        + "\"openedAt\":\"2026-11-01\"}]",
                "/api/v1/internal/credits",
                "[{\"id\":\"k1\",\"customerId\":\"1\",\"type\":\"BUSINESS\","
                        + "\"grantedAt\":\"2026-11-02\"}]",
                "/api/v1/internal/credit-cards",
                "[{\"id\":\"t1\",\"customerId\":\"1\",\"creditLimit\":1000,"
                        + "\"grantedAt\":\"2026-11-03\"}]"));

        adapter(stub).findAll()
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertValues(
                        new BankProduct("a1", ProductType.CHECKING, LocalDate.of(2026, 11, 1)),
                        new BankProduct(
                                "k1",
                                ProductType.BUSINESS_CREDIT,
                                LocalDate.of(2026, 11, 2)),
                        new BankProduct("t1", ProductType.CREDIT_CARD, LocalDate.of(2026, 11, 3)));
    }

    @Test
    void failsAsUnavailableWhenServiceFails() {
        StubWebClient stub =
                StubWebClient.respondingWith(HttpStatus.INTERNAL_SERVER_ERROR, "{\"status\":500}");

        adapter(stub).findAll()
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertError(ServiceUnavailableException.class);
    }
}
