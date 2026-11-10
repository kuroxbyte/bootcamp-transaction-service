package dev.bootcamp.transactionservice.infrastructure.client.credit;

import dev.bootcamp.transactionservice.application.port.CreditCardClient;
import dev.bootcamp.transactionservice.domain.model.creditcard.CreditCard;
import dev.bootcamp.transactionservice.infrastructure.client.RemoteLookup;
import dev.bootcamp.transactionservice.infrastructure.client.credit.dto.CreditCardResponseDto;
import io.reactivex.rxjava3.core.Maybe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Implementa el puerto {@link CreditCardClient} consultando credit-service con WebClient.
 */
@Component
public class CreditCardWebClientAdapter implements CreditCardClient {

    private static final String SERVICE_NAME = "credit-service";

    private final WebClient webClient;
    private final RemoteLookup remoteLookup;

    /**
     * Crea el cliente con la URL base configurada; las consultas pasan por un circuit breaker.
     */
    public CreditCardWebClientAdapter(
            WebClient.Builder webClientBuilder,
            @Value("${clients.credit-service.base-url}") String baseUrl,
            RemoteLookup remoteLookup) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.remoteLookup = remoteLookup;
    }

    @Override
    public Maybe<CreditCard> findById(String creditCardId) {
        return remoteLookup.findById(
                webClient
                        .get()
                        .uri("/api/v1/credit-cards/{creditCardId}", creditCardId)
                        .retrieve()
                        .bodyToMono(CreditCardResponseDto.class)
                        .map(
                                response ->
                                        new CreditCard(
                                                response.getId(),
                                                response.getCustomerId(),
                                                response.getCreditLimit())),
                SERVICE_NAME,
                creditCardId);
    }
}
