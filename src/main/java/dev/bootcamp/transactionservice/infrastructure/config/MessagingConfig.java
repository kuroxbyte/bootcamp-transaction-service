package dev.bootcamp.transactionservice.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Configuración de la mensajería: transaction-service es dueño de los tópicos de pagos y consumos
 * de productos de crédito y de respuestas a las operaciones Yanki.
 */
@Configuration
public class MessagingConfig {

    /**
     * Tópico de pagos y consumos; la clave de cada mensaje es el id del cliente.
     */
    @Bean
    public NewTopic transactionEventsTopic(
            @Value("${messaging.topics.transaction-events}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    /**
     * Tópico de respuestas a las operaciones Yanki; la clave de cada mensaje es el id de la
     * operación.
     */
    @Bean
    public NewTopic yankiOperationResultsTopic(
            @Value("${messaging.topics.yanki-operation-results}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}
