package dev.bootcamp.transactionservice.infrastructure.client.credit;

import dev.bootcamp.transactionservice.application.port.CreditClient;
import dev.bootcamp.transactionservice.domain.model.credit.Credit;
import dev.bootcamp.transactionservice.infrastructure.client.RemoteLookup;
import dev.bootcamp.transactionservice.infrastructure.client.credit.dto.CreditResponseDto;
import io.reactivex.rxjava3.core.Maybe;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Implementa el puerto {@link CreditClient} consultando credit-service con WebClient. */
@Component
public class CreditWebClientAdapter implements CreditClient {

    private static final String SERVICE_NAME = "credit-service";

    private final WebClient webClient;
    private final Duration timeout;

    /** Crea el cliente con la URL base y el tiempo de espera configurados. */
    public CreditWebClientAdapter(
            WebClient.Builder webClientBuilder,
            @Value("${clients.credit-service.base-url}") String baseUrl,
            @Value("${clients.credit-service.timeout}") Duration timeout) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.timeout = timeout;
    }

    @Override
    public Maybe<Credit> findById(String creditId) {
        return RemoteLookup.findById(
                webClient
                        .get()
                        .uri("/api/v1/credits/{creditId}", creditId)
                        .retrieve()
                        .bodyToMono(CreditResponseDto.class)
                        .map(response -> new Credit(response.getId(), response.getCustomerId())),
                timeout,
                SERVICE_NAME,
                creditId);
    }
}
