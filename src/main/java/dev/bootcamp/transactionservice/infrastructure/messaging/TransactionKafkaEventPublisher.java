package dev.bootcamp.transactionservice.infrastructure.messaging;

import dev.bootcamp.transactionservice.application.port.TransactionEventPublisher;
import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
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
 * Implementa el puerto {@link TransactionEventPublisher} publicando en el tópico
 * {@code transaction-events}. La clave de cada mensaje es el id del cliente, para que sus eventos
 * lleguen en orden. El contrato está en {@code docs/events/asyncapi.yaml} del repositorio raíz.
 *
 * <p>Si Kafka no responde ({@code max.block.ms}, 1 s en config-repo), el error se registra y la
 * transacción no falla: el evento se pierde (limitación conocida; evitarla requiere el patrón
 * outbox).
 */
@Slf4j
@Component
public class TransactionKafkaEventPublisher implements TransactionEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final Clock clock;
    private final String topic;

    /**
     * Crea el publicador con el tópico configurado.
     */
    public TransactionKafkaEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            JsonMapper jsonMapper,
            Clock clock,
            @Value("${messaging.topics.transaction-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
        this.topic = topic;
    }

    @Override
    public Completable publishPaymentRegistered(String customerId, Payment payment) {
        Map<String, Object> event = new LinkedHashMap<>();
        if (payment.target() == PaymentTarget.CREDIT) {
            event.put("eventType", "CreditPaymentRegistered");
            event.put("creditId", payment.productId());
        } else {
            event.put("eventType", "CreditCardPaymentRegistered");
            event.put("creditCardId", payment.productId());
        }
        event.put("customerId", customerId);
        event.put("payerCustomerId", payment.payerCustomerId());
        event.put("amount", payment.amount());
        event.put("date", payment.date().toString());
        return publish(customerId, event);
    }

    @Override
    public Completable publishChargeRegistered(String customerId, Charge charge) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", "CreditCardChargeRegistered");
        event.put("creditCardId", charge.creditCardId());
        event.put("customerId", customerId);
        event.put("amount", charge.amount());
        event.put("date", charge.date().toString());
        return publish(customerId, event);
    }

    private Completable publish(String customerId, Map<String, Object> event) {
        return Completable.defer(() -> {
            // Id único: debt-service lo usa para no procesar dos veces el mismo evento.
            event.put("eventId", UUID.randomUUID().toString());
            event.put("occurredAt", Instant.now(clock).toString());
            return Completable.fromCompletionStage(kafkaTemplate.send(
                    topic,
                    customerId,
                    jsonMapper.writeValueAsString(event)));
        })
                // send() puede bloquear mientras busca al broker: fuera del hilo de eventos.
                .subscribeOn(Schedulers.io())
                .doOnComplete(() -> log.info(
                        "{} published customerId={}",
                        event.get("eventType"),
                        customerId))
                .onErrorComplete(error -> {
                    log.error(
                            "{} could not be published customerId={}",
                            event.get("eventType"),
                            customerId,
                            error);
                    return true;
                });
    }
}
