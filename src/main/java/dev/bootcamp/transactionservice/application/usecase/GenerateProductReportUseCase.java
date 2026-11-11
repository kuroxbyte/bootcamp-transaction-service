package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.report.ProductReport;
import io.reactivex.rxjava3.core.Single;

import java.time.LocalDate;

/**
 * Caso de uso para generar el reporte general del banco por tipo de producto en un intervalo de
 * fechas.
 *
 * <p>Por cada tipo de producto (ahorro, corriente, plazo fijo, crédito personal, crédito
 * empresarial y tarjeta de crédito) informa los productos abiertos u otorgados en el intervalo, la
 * cantidad y el monto de cada tipo de movimiento, el total de transacciones y las comisiones
 * cobradas. Es un reporte de uso administrativo: no se filtra por cliente.
 *
 * <p>Es reactivo: no hace nada hasta que se suscribe al {@link Single}.
 */
public interface GenerateProductReportUseCase {

    /**
     * Genera el reporte del intervalo indicado.
     *
     * @param from primer día del intervalo (incluido)
     * @param to   último día del intervalo (incluido)
     * @return el reporte con un resumen por tipo de producto; error {@link BusinessRuleException}
     *     si {@code from} es posterior a {@code to}
     */
    Single<ProductReport> execute(LocalDate from, LocalDate to);
}
