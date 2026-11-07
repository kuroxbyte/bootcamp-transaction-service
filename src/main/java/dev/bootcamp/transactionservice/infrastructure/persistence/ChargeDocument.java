package dev.bootcamp.transactionservice.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

/** Consumo de una tarjeta de crédito, en la colección {@code charges}. */
@Getter
@AllArgsConstructor
@Document(collection = "charges")
public class ChargeDocument {
    @Id
    private final String id;
    @Indexed
    private final String creditCardId;
    @Field(targetType = FieldType.DECIMAL128)
    private final BigDecimal amount;
    private final LocalDate date;
}
