package dev.bootcamp.transactionservice.infrastructure.messaging;

import dev.bootcamp.transactionservice.application.port.YankiOperationResultPublisher;
import dev.bootcamp.transactionservice.domain.model.yanki.YankiOperation;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementa el puerto {@link YankiOperationResultPublisher} publicando en el tópico
 * {@code yanki-operation-results}; la clave del mensaje es el id de la operación. El contrato está
 * en {@code docs/events/asyncapi.yaml} del repositorio raíz.
 *
 * <p>El error se propaga: el pedido se vuelve a entregar y se responde con el resultado guardado.
 */
@Slf4j
@Component
public class YankiOperationResultKafkaPublisher implements YankiOperationResultPublisher {

    static final String COMPLETED = "YankiOperationCompleted";
    static final String REJECTED = "YankiOperationRejected";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final Clock clock;
    private final String topic;

    /**
     * Crea el publicador con el tópico configurado.
     */
    public YankiOperationResultKafkaPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            JsonMapper jsonMapper,
            Clock clock,
            @Value("${messaging.topics.yanki-operation-results}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
        this.topic = topic;
    }

    @Override
    public Completable publish(YankiOperation operation) {
        String eventType = operation.completed() ? COMPLETED : REJECTED;
        return Completable.defer(() -> {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventType", eventType);
            event.put("eventId", UUID.randomUUID().toString());
            event.put("operationId", operation.operationId());
            if (!operation.completed()) {
                event.put("reason", operation.reason());
            }
            event.put("occurredAt", Instant.now(clock).toString());
            return Completable.fromCompletionStage(kafkaTemplate.send(
                    topic,
                    operation.operationId(),
                    jsonMapper.writeValueAsString(event)));
        })
                // send() puede bloquear mientras busca al broker: fuera del hilo de eventos.
                .subscribeOn(Schedulers.io())
                .doOnComplete(() -> log.info(
                        "{} published operationId={}",
                        eventType,
                        operation.operationId()))
                .doOnError(error -> log.error(
                        "{} could not be published operationId={}",
                        eventType,
                        operation.operationId(),
                        error));
    }
}
