package dev.bootcamp.transactionservice.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

/** Pago a un producto de crédito, en la colección {@code payments}. */
@Getter
@AllArgsConstructor
@Document(collection = "payments")
@CompoundIndex(name = "target_product", def = "{'target': 1, 'productId': 1}")
public class PaymentDocument {
    @Id
    private final String id;
    /** CREDIT o CREDIT_CARD. */
    private final String target;
    private final String productId;
    @Field(targetType = FieldType.DECIMAL128)
    private final BigDecimal amount;
    private final LocalDate date;
}
