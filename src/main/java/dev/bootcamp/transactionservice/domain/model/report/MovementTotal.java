package dev.bootcamp.transactionservice.domain.model.report;

import dev.bootcamp.transactionservice.domain.model.history.ProductMovementType;

import java.math.BigDecimal;

/**
 * Cantidad y monto total de un tipo de movimiento dentro del reporte.
 *
 * @param type   tipo de movimiento
 * @param count  cantidad de movimientos
 * @param amount suma de sus montos (sin comisiones)
 */
public record MovementTotal(ProductMovementType type, long count, BigDecimal amount) {}
