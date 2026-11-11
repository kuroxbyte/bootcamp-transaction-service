package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.report.BankProduct;
import io.reactivex.rxjava3.core.Flowable;

/**
 * Consulta de todos los productos del banco: cuentas en account-service, y créditos y tarjetas de
 * crédito en credit-service.
 */
public interface BankProductClient {

    /**
     * Todos los productos del banco con su tipo y su fecha de apertura u otorgamiento.
     */
    Flowable<BankProduct> findAll();
}
