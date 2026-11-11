package dev.bootcamp.transactionservice.domain.model.report;

/**
 * Tipo de producto del banco, para agrupar el reporte general por producto.
 */
public enum ProductType {
    /**
     * Cuenta de ahorro (incluye la de ahorro VIP).
     */
    SAVINGS,
    /**
     * Cuenta corriente.
     */
    CHECKING,
    /**
     * Cuenta a plazo fijo.
     */
    FIXED_TERM,
    /**
     * Crédito personal.
     */
    PERSONAL_CREDIT,
    /**
     * Crédito empresarial.
     */
    BUSINESS_CREDIT,
    /**
     * Tarjeta de crédito.
     */
    CREDIT_CARD
}
