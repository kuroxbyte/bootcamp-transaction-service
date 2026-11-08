package dev.bootcamp.transactionservice.infrastructure.client.credit;

import static org.assertj.core.api.Assertions.assertThat;

import dev.bootcamp.transactionservice.domain.model.credit.Credit;
import dev.bootcamp.transactionservice.domain.model.creditcard.CreditCard;
import dev.bootcamp.transactionservice.infrastructure.client.ServiceUnavailableException;
import dev.bootcamp.transactionservice.infrastructure.client.StubWebClient;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CreditWebClientAdaptersTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    @Test
    void returnsCreditFromCreditService() {
        StubWebClient stub =
                StubWebClient.respondingWith(
                        HttpStatus.OK, "{\"id\":\"5\",\"customerId\":\"1\",\"type\":\"PERSONAL\"}");

        Credit credit =
                new CreditWebClientAdapter(stub.builder(), "http://credit-service", TIMEOUT)
                        .findById("5")
                        .blockingGet();

        assertThat(credit).isEqualTo(new Credit("5", "1"));
        assertThat(stub.lastPath()).isEqualTo("/api/v1/credits/5");
    }

    @Test
    void returnsCreditCardWithItsLimit() {
        StubWebClient stub =
                StubWebClient.respondingWith(
                        HttpStatus.OK,
                        "{\"id\":\"7\",\"customerId\":\"1\",\"creditLimit\":5000.50}");

        CreditCard card =
                new CreditCardWebClientAdapter(stub.builder(), "http://credit-service", TIMEOUT)
                        .findById("7")
                        .blockingGet();

        assertThat(card.id()).isEqualTo("7");
        assertThat(card.customerId()).isEqualTo("1");
        assertThat(card.creditLimit()).isEqualByComparingTo(new BigDecimal("5000.50"));
        assertThat(stub.lastPath()).isEqualTo("/api/v1/credit-cards/7");
    }

    @Test
    void returnsEmptyWhenProductDoesNotExist() {
        StubWebClient stub = StubWebClient.respondingWith(HttpStatus.NOT_FOUND, "{\"status\":404}");

        new CreditWebClientAdapter(stub.builder(), "http://credit-service", TIMEOUT)
                .findById("99")
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertComplete()
                .assertNoValues();
        new CreditCardWebClientAdapter(stub.builder(), "http://credit-service", TIMEOUT)
                .findById("99")
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertComplete()
                .assertNoValues();
    }

    @Test
    void failsAsUnavailableOnServerError() {
        StubWebClient stub =
                StubWebClient.respondingWith(HttpStatus.SERVICE_UNAVAILABLE, "{\"status\":503}");

        new CreditCardWebClientAdapter(stub.builder(), "http://credit-service", TIMEOUT)
                .findById("7")
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertError(ServiceUnavailableException.class);
    }
}
