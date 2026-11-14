package dev.bootcamp.transactionservice.infrastructure.client.account;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.account.CheckingMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.account.FixedTermMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.account.MovementPolicy;
import dev.bootcamp.transactionservice.domain.model.account.SavingsMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.account.TransactionFee;
import dev.bootcamp.transactionservice.infrastructure.client.RemoteLookup;
import dev.bootcamp.transactionservice.infrastructure.client.account.dto.AccountResponseDto;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Implementa el puerto {@link AccountClient} consultando account-service con WebClient.
 */
@Component
public class AccountWebClientAdapter implements AccountClient {

    private static final String SERVICE_NAME = "account-service";

    private final WebClient webClient;
    private final RemoteLookup remoteLookup;

    /**
     * Crea el cliente con la URL base configurada; las consultas pasan por un circuit breaker.
     */
    public AccountWebClientAdapter(
            WebClient.Builder webClientBuilder,
            @Value("${clients.account-service.base-url}") String baseUrl,
            RemoteLookup remoteLookup) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.remoteLookup = remoteLookup;
    }

    @Override
    public Maybe<Account> findById(String accountId) {
        return remoteLookup.findById(
                webClient
                        .get()
                        .uri("/api/v1/accounts/{accountId}", accountId)
                        .retrieve()
                        .bodyToMono(AccountResponseDto.class)
                        .map(AccountWebClientAdapter::toDomain),
                SERVICE_NAME,
                accountId);
    }

    @Override
    public Flowable<Account> findAll() {
        return remoteLookup.findAll(
                webClient
                        .get()
                        .uri("/api/v1/internal/accounts")
                        .retrieve()
                        .bodyToFlux(AccountResponseDto.class)
                        .map(AccountWebClientAdapter::toDomain),
                SERVICE_NAME);
    }

    /**
     * Convierte la cuenta en la vista local, con la regla de movimientos de su tipo.
     */
    private static Account toDomain(AccountResponseDto response) {
        MovementPolicy movementPolicy =
                switch (response.getType()) {
                    case SAVINGS -> new SavingsMovementPolicy(response.getMaxMonthlyMovements());
                    case CHECKING -> new CheckingMovementPolicy();
                    case FIXED_TERM ->
                            new FixedTermMovementPolicy(response.getMovementDayOfMonth());
                };
        return new Account(
                response.getId(),
                response.getCustomerId(),
                movementPolicy,
                response.getOpeningAmount(),
                response.getOpenedAt(),
                response.getMinimumDailyAverage(),
                new TransactionFee(
                        response.getFreeMonthlyTransactions(),
                        response.getTransactionFee()),
                response.getMaintenanceFee());
    }
}
