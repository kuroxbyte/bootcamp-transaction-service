package dev.bootcamp.transactionservice.infrastructure.client.account;

import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.account.CheckingMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.account.FixedTermMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.account.SavingsMovementPolicy;
import dev.bootcamp.transactionservice.infrastructure.client.ServiceUnavailableException;
import dev.bootcamp.transactionservice.infrastructure.client.StubWebClient;
import dev.bootcamp.transactionservice.infrastructure.client.TestCircuitBreakers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AccountWebClientAdapterTest {

    private static Account fetch(String json) {
        StubWebClient stub = StubWebClient.respondingWith(HttpStatus.OK, json);
        return new AccountWebClientAdapter(
                        stub.builder(), "http://account-service", TestCircuitBreakers.remoteLookup(Duration.ofSeconds(1)))
                .findById("10")
                .blockingGet();
    }

    @Test
    void mapsSavingsAccountToItsMovementPolicy() {
        Account account =
                fetch(
                        "{\"id\":\"10\",\"customerId\":\"1\",\"type\":\"SAVINGS\","
                                + "\"maxMonthlyMovements\":5}");

        assertThat(account).isEqualTo(new Account("10", "1", new SavingsMovementPolicy(5)));
    }

    @Test
    void mapsCheckingAccountToItsMovementPolicy() {
        Account account =
                fetch(
                        "{\"id\":\"10\",\"customerId\":\"2\",\"type\":\"CHECKING\","
                                + "\"maintenanceFee\":15.0,"
                                + "\"holderIds\":[\"h1\"],\"authorizedSignerIds\":[]}");

        assertThat(account).isEqualTo(new Account("10", "2", new CheckingMovementPolicy()));
    }

    @Test
    void mapsFixedTermAccountToItsMovementPolicy() {
        Account account =
                fetch(
                        "{\"id\":\"10\",\"customerId\":\"1\",\"type\":\"FIXED_TERM\","
                                + "\"movementDayOfMonth\":15}");

        assertThat(account).isEqualTo(new Account("10", "1", new FixedTermMovementPolicy(15)));
    }

    @Test
    void callsAccountByIdEndpoint() {
        StubWebClient stub =
                StubWebClient.respondingWith(
                        HttpStatus.OK,
                        "{\"id\":\"10\",\"customerId\":\"1\",\"type\":\"CHECKING\"}");

        new AccountWebClientAdapter(stub.builder(), "http://account-service", TestCircuitBreakers.remoteLookup(Duration.ofSeconds(1)))
                .findById("10")
                .blockingGet();

        assertThat(stub.lastPath()).isEqualTo("/api/v1/accounts/10");
    }

    @Test
    void returnsEmptyWhenAccountDoesNotExist() {
        StubWebClient stub = StubWebClient.respondingWith(HttpStatus.NOT_FOUND, "{\"status\":404}");

        new AccountWebClientAdapter(stub.builder(), "http://account-service", TestCircuitBreakers.remoteLookup(Duration.ofSeconds(1)))
                .findById("99")
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertComplete()
                .assertNoValues();
    }

    @Test
    void failsAsUnavailableOnTimeout() {
        new AccountWebClientAdapter(
                        StubWebClient.neverResponding().builder(),
                        "http://account-service",
                        TestCircuitBreakers.remoteLookup(Duration.ofMillis(100)))
                .findById("10")
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertError(ServiceUnavailableException.class);
    }
}
