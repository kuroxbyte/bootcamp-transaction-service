package dev.bootcamp.transactionservice.infrastructure.messaging;

import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionKafkaEventPublisherTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    private TransactionKafkaEventPublisher publisher;

    @BeforeEach
    void createPublisher() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);
        publisher = new TransactionKafkaEventPublisher(
                kafkaTemplate,
                jsonMapper,
                clock,
                "transaction-events");
    }

    private void givenKafkaAccepts() {
        when(kafkaTemplate.send(eq("transaction-events"), eq("c1"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    private JsonNode sentEvent() {
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("transaction-events"), eq("c1"), json.capture());
        return jsonMapper.readTree(json.getValue());
    }

    @Test
    void publishesCreditPayment() {
        givenKafkaAccepts();

        publisher.publishPaymentRegistered(
                        "c1",
                        new Payment(PaymentTarget.CREDIT, "k1", new BigDecimal("50"), DATE))
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertComplete();

        JsonNode event = sentEvent();
        assertThat(event.get("eventType").asString()).isEqualTo("CreditPaymentRegistered");
        assertThat(event.get("creditId").asString()).isEqualTo("k1");
        assertThat(event.get("customerId").asString()).isEqualTo("c1");
        assertThat(event.get("amount").decimalValue()).isEqualByComparingTo("50");
        assertThat(event.get("date").asString()).isEqualTo("2026-11-15");
        assertThat(event.get("occurredAt").asString()).isEqualTo("2026-11-15T10:00:00Z");
    }

    @Test
    void publishesCreditCardPayment() {
        givenKafkaAccepts();

        publisher.publishPaymentRegistered(
                        "c1",
                        new Payment(PaymentTarget.CREDIT_CARD, "t1", BigDecimal.TEN, DATE))
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertComplete();

        JsonNode event = sentEvent();
        assertThat(event.get("eventType").asString()).isEqualTo("CreditCardPaymentRegistered");
        assertThat(event.get("creditCardId").asString()).isEqualTo("t1");
    }

    @Test
    void publishesCreditCardCharge() {
        givenKafkaAccepts();

        publisher.publishChargeRegistered("c1", new Charge("t1", new BigDecimal("80"), DATE))
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertComplete();

        JsonNode event = sentEvent();
        assertThat(event.get("eventType").asString()).isEqualTo("CreditCardChargeRegistered");
        assertThat(event.get("creditCardId").asString()).isEqualTo("t1");
        assertThat(event.get("amount").decimalValue()).isEqualByComparingTo("80");
    }

    @Test
    void doesNotFailWhenKafkaIsUnavailable() {
        when(kafkaTemplate.send(eq("transaction-events"), eq("c1"), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("down")));

        publisher.publishChargeRegistered("c1", new Charge("t1", BigDecimal.TEN, DATE))
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertComplete();
    }
}
