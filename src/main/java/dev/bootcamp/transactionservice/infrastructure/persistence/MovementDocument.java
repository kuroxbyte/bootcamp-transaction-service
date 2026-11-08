package dev.bootcamp.transactionservice.infrastructure.persistence;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Depósito o retiro de una cuenta bancaria, en la colección {@code movements}.
 */
@Getter
@AllArgsConstructor
@Document(collection = "movements")
@CompoundIndex(name = "account_date", def = "{'accountId': 1, 'date': 1}")
public class MovementDocument {
    @Id private final String id;
    private final String accountId;

    /**
     * DEPOSIT o WITHDRAWAL.
     */
    private final String type;

    @Field(targetType = FieldType.DECIMAL128)
    private final BigDecimal amount;

    private final LocalDate date;
}
