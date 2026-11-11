package dev.bootcamp.transactionservice.domain.model.report;

import java.time.LocalDate;

/**
 * Producto del banco visto por el reporte: cuenta, crédito o tarjeta de crédito.
 *
 * @param id       id del producto en account-service o credit-service
 * @param type     tipo de producto
 * @param openedAt fecha de apertura (cuentas) u otorgamiento (créditos y tarjetas)
 */
public record BankProduct(String id, ProductType type, LocalDate openedAt) {}
