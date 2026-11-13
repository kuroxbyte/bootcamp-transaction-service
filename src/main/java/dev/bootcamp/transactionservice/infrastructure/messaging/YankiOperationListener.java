package dev.bootcamp.transactionservice.infrastructure.messaging;

import dev.bootcamp.transactionservice.application.usecase.ProcessYankiOperationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

/**
 * Consume el tópico {@code yanki-operations} de yanki-service: cargos y abonos sobre la cuenta
 * principal de la tarjeta de un monedero. El contrato está en {@code docs/events/asyncapi.yaml}
 * del repositorio raíz.
 *
 * <p>Ignora los tipos de evento que no conoce. Si procesar falla, la excepción hace que Kafka
 * vuelva a entregar el mensaje; un pedido repetido se responde con el resultado guardado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YankiOperationListener {

    static final String DEBIT_REQUESTED = "YankiAccountDebitRequested";
    static final String CREDIT_REQUESTED = "YankiAccountCreditRequested";

    private final ProcessYankiOperationUseCase processYankiOperationUseCase;
    private final JsonMapper jsonMapper;

    /**
     * Procesa un pedido de yanki-service.
     *
     * @param message evento en JSON
     */
    @KafkaListener(topics = "${messaging.topics.yanki-operations}")
    public void onYankiOperation(String message) {
        JsonNode event = jsonMapper.readTree(message);
        String eventType = event.path("eventType").asString();
        if (!DEBIT_REQUESTED.equals(eventType) && !CREDIT_REQUESTED.equals(eventType)) {
            log.debug("Ignoring yanki operation type={}", eventType);
            return;
        }
        String operationId = event.get("operationId").asString();
        processYankiOperationUseCase
                .execute(
                        operationId,
                        DEBIT_REQUESTED.equals(eventType),
                        event.get("accountId").asString(),
                        event.get("amount").decimalValue(),
                        LocalDate.parse(event.get("date").asString()),
                        event.path("reversal").asBoolean(false))
                .blockingAwait();
        log.info("{} processed operationId={}", eventType, operationId);
    }
}
