package dev.bootcamp.transactionservice.infrastructure.client.account;

import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.account.CheckingMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.account.FixedTermMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.account.SavingsMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.account.TransactionFee;
import dev.bootcamp.transactionservice.infrastructure.client.ServiceUnavailableException;
import dev.bootcamp.transactionservice.infrastructure.client.StubWebClient;
import dev.bootcamp.transactionservice.infrastructure.client.TestCircuitBreakers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AccountWebClientAdapterTest {

    private static final TransactionFee FEE = new TransactionFee(3, new BigDecimal("2"));

    /**
     * Consulta la cuenta respondiendo con el JSON indicado más la comisión por transacción {@link
     * #FEE}.
     */
    private static Account fetch(String json) {
        StubWebClient stub = StubWebClient.respondingWith(
                HttpStatus.OK,
                json.replaceFirst("\\{", "{\"freeMonthlyTransactions\":3,\"transactionFee\":2,"));
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
                                + "\"maxMonthlyMovements\":5,\"openingAmount\":0}");

        assertThat(account).isEqualTo(
                new Account(
                        "10",
                        "1",
                        new SavingsMovementPolicy(5),
                        BigDecimal.ZERO,
                        null,
                        null,
                        FEE));
    }

    @Test
    void mapsOpeningDateAndMinimumDailyAverageOfVipSavingsAccount() {
        Account account =
                fetch(
                        "{\"id\":\"10\",\"customerId\":\"1\",\"type\":\"SAVINGS\","
                                + "\"maxMonthlyMovements\":5,\"openingAmount\":0,"
                                + "\"openedAt\":\"2026-11-10\",\"minimumDailyAverage\":500}");

        assertThat(account.openedAt()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(account.minimumDailyAverage()).isEqualByComparingTo("500");
    }

    @Test
    void mapsTransactionFee() {
        Account account = fetch("{\"id\":\"10\",\"customerId\":\"1\",\"type\":\"CHECKING\","
                + "\"openingAmount\":0}");

        assertThat(account.transactionFee().freeMonthlyTransactions()).isEqualTo(3);
        assertThat(account.transactionFee().amount()).isEqualByComparingTo("2");
    }

    @Test
    void mapsCheckingAccountToItsMovementPolicy() {
        Account account =
                fetch(
                        "{\"id\":\"10\",\"customerId\":\"2\",\"type\":\"CHECKING\","
                                + "\"maintenanceFee\":15.0,"
                                + "\"holderIds\":[\"h1\"],\"authorizedSignerIds\":[],"
                                + "\"openingAmount\":0}");

        assertThat(account).isEqualTo(
                new Account(
                        "10",
                        "2",
                        new CheckingMovementPolicy(),
                        BigDecimal.ZERO,
                        null,
                        null,
                        FEE,
                        new BigDecimal("15.0")));
    }

    @Test
    void mapsFixedTermAccountToItsMovementPolicy() {
        Account account =
                fetch(
                        "{\"id\":\"10\",\"customerId\":\"1\",\"type\":\"FIXED_TERM\","
                                + "\"movementDayOfMonth\":15,\"openingAmount\":0}");

        assertThat(account).isEqualTo(
                new Account(
                        "10",
                        "1",
                        new FixedTermMovementPolicy(15),
                        BigDecimal.ZERO,
                        null,
                        null,
                        FEE));
    }

    @Test
    void callsAccountByIdEndpoint() {
        StubWebClient stub =
                StubWebClient.respondingWith(
                        HttpStatus.OK,
                        "{\"id\":\"10\",\"customerId\":\"1\",\"type\":\"CHECKING\","
                                + "\"openingAmount\":0,\"freeMonthlyTransactions\":3,"
                                + "\"transactionFee\":2}");

        new AccountWebClientAdapter(
                stub.builder(),
                "http://account-service",
                TestCircuitBreakers.remoteLookup(Duration.ofSeconds(1)))
                .findById("10")
                .blockingGet();

        assertThat(stub.lastPath()).isEqualTo("/api/v1/accounts/10");
    }

    @Test
    void listsAllAccountsFromTheInternalEndpoint() {
        StubWebClient stub = StubWebClient.respondingWith(
                HttpStatus.OK,
                "[{\"id\":\"10\",\"customerId\":\"1\",\"type\":\"CHECKING\","
                        + "\"maintenanceFee\":8,\"openingAmount\":0,"
                        + "\"freeMonthlyTransactions\":3,\"transactionFee\":2},"
                        + "{\"id\":\"11\",\"customerId\":\"1\",\"type\":\"SAVINGS\","
                        + "\"maxMonthlyMovements\":5,\"openingAmount\":0,"
                        + "\"freeMonthlyTransactions\":3,\"transactionFee\":2}]");

        List<Account> accounts = new AccountWebClientAdapter(
                stub.builder(),
                "http://account-service",
                TestCircuitBreakers.remoteLookup(Duration.ofSeconds(1)))
                .findAll()
                .toList()
                .blockingGet();

        assertThat(stub.lastPath()).isEqualTo("/api/v1/internal/accounts");
        assertThat(accounts).extracting(Account::id).containsExactly("10", "11");
        assertThat(accounts.get(0).maintenanceFee()).isEqualByComparingTo("8");
        assertThat(accounts.get(1).maintenanceFee()).isEqualByComparingTo("0");
    }

    @Test
    void returnsEmptyWhenAccountDoesNotExist() {
        StubWebClient stub = StubWebClient.respondingWith(HttpStatus.NOT_FOUND, "{\"status\":404}");

        new AccountWebClientAdapter(
                stub.builder(),
                "http://account-service",
                TestCircuitBreakers.remoteLookup(Duration.ofSeconds(1)))
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

    @Test
    void mapsOpeningAmount() {
        Account account = fetch(
                "{\"id\":\"10\",\"customerId\":\"1\",\"type\":\"CHECKING\","
                        + "\"minimumOpeningAmount\":0,\"openingAmount\":250.5}");

        assertThat(account.openingAmount()).isEqualByComparingTo("250.5");
    }
}
