package dev.bootcamp.transactionservice.infrastructure.client.report;

import dev.bootcamp.transactionservice.application.port.BankProductClient;
import dev.bootcamp.transactionservice.domain.model.report.BankProduct;
import dev.bootcamp.transactionservice.domain.model.report.ProductType;
import dev.bootcamp.transactionservice.infrastructure.client.RemoteLookup;
import dev.bootcamp.transactionservice.infrastructure.client.account.dto.AccountResponseDto;
import dev.bootcamp.transactionservice.infrastructure.client.credit.dto.CreditCardResponseDto;
import dev.bootcamp.transactionservice.infrastructure.client.credit.dto.CreditResponseDto;
import io.reactivex.rxjava3.core.Flowable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Implementa el puerto {@link BankProductClient} con los endpoints internos de account-service y
 * credit-service que listan todos sus productos.
 */
@Component
public class BankProductWebClientAdapter implements BankProductClient {

    private static final String ACCOUNT_SERVICE = "account-service";
    private static final String CREDIT_SERVICE = "credit-service";

    private final WebClient accountWebClient;
    private final WebClient creditWebClient;
    private final RemoteLookup remoteLookup;

    /**
     * Crea el cliente con las URL base configuradas; las consultas pasan por un circuit breaker.
     */
    public BankProductWebClientAdapter(
            WebClient.Builder webClientBuilder,
            @Value("${clients.account-service.base-url}") String accountBaseUrl,
            @Value("${clients.credit-service.base-url}") String creditBaseUrl,
            RemoteLookup remoteLookup) {
        this.accountWebClient = webClientBuilder.clone().baseUrl(accountBaseUrl).build();
        this.creditWebClient = webClientBuilder.clone().baseUrl(creditBaseUrl).build();
        this.remoteLookup = remoteLookup;
    }

    @Override
    public Flowable<BankProduct> findAll() {
        Flowable<BankProduct> accounts = remoteLookup.findAll(
                accountWebClient
                        .get()
                        .uri("/api/v1/internal/accounts")
                        .retrieve()
                        .bodyToFlux(AccountResponseDto.class)
                        .map(account -> new BankProduct(
                                account.getId(),
                                ProductType.valueOf(account.getType().name()),
                                account.getOpenedAt())),
                ACCOUNT_SERVICE);
        Flowable<BankProduct> credits = remoteLookup.findAll(
                creditWebClient
                        .get()
                        .uri("/api/v1/internal/credits")
                        .retrieve()
                        .bodyToFlux(CreditResponseDto.class)
                        .map(credit -> new BankProduct(
                                credit.getId(),
                                ProductType.valueOf(credit.getType().name() + "_CREDIT"),
                                credit.getGrantedAt())),
                CREDIT_SERVICE);
        Flowable<BankProduct> creditCards = remoteLookup.findAll(
                creditWebClient
                        .get()
                        .uri("/api/v1/internal/credit-cards")
                        .retrieve()
                        .bodyToFlux(CreditCardResponseDto.class)
                        .map(card -> new BankProduct(
                                card.getId(),
                                ProductType.CREDIT_CARD,
                                card.getGrantedAt())),
                CREDIT_SERVICE);
        return Flowable.concat(accounts, credits, creditCards);
    }
}
