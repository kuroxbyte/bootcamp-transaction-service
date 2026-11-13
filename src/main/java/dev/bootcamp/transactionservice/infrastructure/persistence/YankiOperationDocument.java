package dev.bootcamp.transactionservice.infrastructure.persistence;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Resultado de una operación Yanki tal como se guarda en la colección {@code yanki_operations};
 * su id es el de la operación en yanki-service.
 */
@Getter
@AllArgsConstructor
@Document(collection = "yanki_operations")
public class YankiOperationDocument {
    @Id private final String id;
    private final boolean completed;
    private final String reason;
}
