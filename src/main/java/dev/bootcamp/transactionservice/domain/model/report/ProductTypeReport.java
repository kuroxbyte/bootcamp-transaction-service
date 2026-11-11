package dev.bootcamp.transactionservice.domain.model.report;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resumen de un tipo de producto en el intervalo del reporte.
 *
 * @param type           tipo de producto
 * @param openedProducts productos abiertos u otorgados en el intervalo
 * @param movements      cantidad y monto por tipo de movimiento; solo los tipos que tuvieron
 *     movimientos
 * @param commissions    suma de las comisiones por transacción cobradas
 */
public record ProductTypeReport(
        ProductType type,
        long openedProducts,
        List<MovementTotal> movements,
        BigDecimal commissions) {

    /**
     * Cantidad total de transacciones de este tipo de producto en el intervalo.
     */
    public long transactions() {
        return movements.stream().mapToLong(MovementTotal::count).sum();
    }
}
