package dev.bootcamp.transactionservice.domain.model.report;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import dev.bootcamp.transactionservice.domain.model.history.ProductMovementType;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reporte general del banco por tipo de producto en un intervalo de fechas.
 *
 * @param from     primer día del intervalo (incluido)
 * @param to       último día del intervalo (incluido)
 * @param products un resumen por cada tipo de producto, en el orden de {@link ProductType}
 */
public record ProductReport(LocalDate from, LocalDate to, List<ProductTypeReport> products) {

    /**
     * Valida que el intervalo no esté invertido.
     *
     * @throws BusinessRuleException si {@code from} es posterior a {@code to}
     */
    public ProductReport {
        validateInterval(from, to);
    }

    /**
     * Valida el intervalo de un reporte antes de generarlo.
     *
     * @throws BusinessRuleException si {@code from} es posterior a {@code to}
     */
    public static void validateInterval(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessRuleException(
                    "La fecha inicial no puede ser posterior a la fecha final");
        }
    }

    /**
     * Genera el reporte agrupando por tipo de producto.
     *
     * <ul>
     *   <li>Productos abiertos: los que se abrieron u otorgaron dentro del intervalo.
     *   <li>Movimientos: depósitos, retiros y transferencias de cuentas, pagos a créditos y
     *       tarjetas, y consumos de tarjetas, con fecha dentro del intervalo; cada uno se asigna al
     *       tipo del producto al que pertenece.
     *   <li>Comisiones: suma de las comisiones por transacción de esos movimientos de cuentas.
     * </ul>
     *
     * <p>Los movimientos se reciben ya filtrados por el intervalo. Se ignoran los que pertenecen a
     * un producto que no está en {@code products}.
     *
     * @param products todos los productos del banco
     * @throws BusinessRuleException si {@code from} es posterior a {@code to}
     */
    public static ProductReport generate(
            LocalDate from,
            LocalDate to,
            List<BankProduct> products,
            List<Movement> movements,
            List<Payment> payments,
            List<Charge> charges) {
        Map<String, ProductType> typeById = products.stream()
                .collect(Collectors.toMap(BankProduct::id, BankProduct::type, (a, b) -> a));
        Map<ProductType, Builder> builders = new EnumMap<>(ProductType.class);
        for (ProductType type : ProductType.values()) {
            builders.put(type, new Builder());
        }
        products.stream()
                .filter(product -> product.openedAt() != null
                        && !product.openedAt().isBefore(from)
                        && !product.openedAt().isAfter(to))
                .forEach(product -> builders.get(product.type()).opened++);
        for (Movement movement : movements) {
            Builder builder = builderFor(builders, typeById, movement.accountId());
            if (builder != null) {
                builder.add(
                        ProductMovementType.valueOf(movement.type().name()),
                        movement.amount());
                builder.commissions = builder.commissions.add(movement.commission());
            }
        }
        for (Payment payment : payments) {
            Builder builder = builderFor(builders, typeById, payment.productId());
            if (builder != null) {
                builder.add(ProductMovementType.PAYMENT, payment.amount());
            }
        }
        for (Charge charge : charges) {
            Builder builder = builderFor(builders, typeById, charge.creditCardId());
            if (builder != null) {
                builder.add(ProductMovementType.CHARGE, charge.amount());
            }
        }
        List<ProductTypeReport> summaries = new ArrayList<>();
        builders.forEach((type, builder) -> summaries.add(builder.build(type)));
        return new ProductReport(from, to, summaries);
    }

    private static Builder builderFor(
            Map<ProductType, Builder> builders,
            Map<String, ProductType> typeById,
            String productId) {
        ProductType type = typeById.get(productId);
        return type == null ? null : builders.get(type);
    }

    /**
     * Acumula los datos de un tipo de producto mientras se genera el reporte.
     */
    private static final class Builder {
        private long opened;
        private BigDecimal commissions = BigDecimal.ZERO;
        private final Map<ProductMovementType, MovementTotal> totals =
                new EnumMap<>(ProductMovementType.class);

        private void add(ProductMovementType type, BigDecimal amount) {
            totals.merge(
                    type,
                    new MovementTotal(type, 1, amount),
                    (total, one) -> new MovementTotal(
                            type,
                            total.count() + 1,
                            total.amount().add(amount)));
        }

        private ProductTypeReport build(ProductType type) {
            return new ProductTypeReport(
                    type,
                    opened,
                    List.copyOf(totals.values()),
                    commissions);
        }
    }
}
