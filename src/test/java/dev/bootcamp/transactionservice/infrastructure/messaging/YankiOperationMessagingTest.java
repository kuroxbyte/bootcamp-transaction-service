package dev.bootcamp.transactionservice.infrastructure.messaging;

import dev.bootcamp.transactionservice.application.usecase.ProcessYankiOperationUseCase;
import dev.bootcamp.transactionservice.domain.model.yanki.YankiOperation;
import io.reactivex.rxjava3.core.Completable;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YankiOperationMessagingTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Mock private ProcessYankiOperationUseCase useCase;

    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void listenerProcessesDebitAndCreditRequests() {
        YankiOperationListener listener = new YankiOperationListener(useCase, jsonMapper);
        when(useCase.execute(any(), anyBoolean(), any(), any(), any(), anyBoolean()))
                .thenReturn(Completable.complete());

        listener.onYankiOperation("{\"eventType\":\"YankiAccountDebitRequested\","
                + "\"operationId\":\"payment:p1:debit\",\"accountId\":\"a1\",\"amount\":30.5,"
                + "\"date\":\"2026-11-15\",\"reversal\":false}");
        listener.onYankiOperation("{\"eventType\":\"YankiAccountCreditRequested\","
                + "\"operationId\":\"payment:p1:refund\",\"accountId\":\"a1\",\"amount\":30.5,"
                + "\"date\":\"2026-11-15\",\"reversal\":true}");
        listener.onYankiOperation("{\"eventType\":\"Other\"}");

        verify(useCase).execute(
                "payment:p1:debit", true, "a1", new BigDecimal("30.5"), DATE, false);
        verify(useCase).execute(
                "payment:p1:refund", false, "a1", new BigDecimal("30.5"), DATE, true);
    }

    @Test
    void listenerIgnoresUnknownEvents() {
        new YankiOperationListener(useCase, jsonMapper).onYankiOperation("{\"eventType\":\"X\"}");

        verify(useCase, never()).execute(any(), anyBoolean(), any(), any(), any(), anyBoolean());
    }

    @Test
    void publishesCompletedAndRejectedResultsAndPropagatesErrors() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);
        YankiOperationResultKafkaPublisher publisher = new YankiOperationResultKafkaPublisher(
                kafkaTemplate, jsonMapper, clock, "yanki-operation-results");
        when(kafkaTemplate.send(eq("yanki-operation-results"), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(kafkaTemplate.send(eq("yanki-operation-results"), eq("payment:p9:debit"), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("down")));

        publisher.publish(YankiOperation.completed("link:l1:transfer")).test()
                .awaitDone(2, TimeUnit.SECONDS).assertComplete();
        publisher.publish(YankiOperation.rejected("payment:p1:debit", "Saldo insuficiente")).test()
                .awaitDone(2, TimeUnit.SECONDS).assertComplete();
        publisher.publish(YankiOperation.completed("payment:p9:debit")).test()
                .awaitDone(2, TimeUnit.SECONDS).assertError(IllegalStateException.class);

        ArgumentCaptor<String> completed = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate)
                .send(eq("yanki-operation-results"), eq("link:l1:transfer"), completed.capture());
        JsonNode completedEvent = jsonMapper.readTree(completed.getValue());
        assertThat(completedEvent.get("eventType").asString()).isEqualTo("YankiOperationCompleted");
        assertThat(completedEvent.has("reason")).isFalse();
        ArgumentCaptor<String> rejected = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate)
                .send(eq("yanki-operation-results"), eq("payment:p1:debit"), rejected.capture());
        JsonNode rejectedEvent = jsonMapper.readTree(rejected.getValue());
        assertThat(rejectedEvent.get("eventType").asString()).isEqualTo("YankiOperationRejected");
        assertThat(rejectedEvent.get("reason").asString()).isEqualTo("Saldo insuficiente");
    }
}
