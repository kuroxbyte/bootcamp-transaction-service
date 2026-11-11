package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.transfer.Transfer;
import io.reactivex.rxjava3.core.Single;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Caso de uso para transferir dinero desde una cuenta bancaria del cliente a otra cuenta del banco.
 *
 * <p>Reglas:
 *
 * <ul>
 *   <li>La cuenta origen debe existir y pertenecer al cliente; la cuenta destino debe existir.
 *   <li>Origen y destino deben ser cuentas distintas.
 *   <li>Entre cuentas propias, el destino debe ser del cliente; a terceros, de otro cliente.
 *   <li>En el origen se trata como un retiro y en el destino como un depósito: aplican las reglas
 *       de cada tipo de cuenta (límite mensual del ahorro, día del plazo fijo) y cuenta para las
 *       transacciones sin comisión del mes de cada cuenta.
 *   <li>Cada cuenta paga su propia comisión si supera sus transacciones sin comisión del mes.
 *   <li>El saldo del origen debe cubrir el monto más su comisión.
 * </ul>
 *
 * <p>Es reactivo: no hace nada hasta que se suscribe al {@link Single}. Si alguna regla no se
 * cumple, termina con error y no se guarda ninguno de los dos movimientos.
 */
public interface TransferUseCase {

    /**
     * Transfiere entre dos cuentas del mismo cliente.
     *
     * @param customerId      id del cliente que transfiere
     * @param sourceAccountId cuenta origen, del cliente
     * @param targetAccountId cuenta destino, también del cliente
     * @param amount          monto a transferir
     * @param date            fecha de la transferencia
     * @return la transferencia guardada con la comisión de cada cuenta; error {@link
     *     NotFoundException} si una cuenta no existe o el origen no es del cliente; {@link
     *     BusinessRuleException} si se incumple una regla
     */
    Single<Transfer> transferToOwnAccount(
            String customerId,
            String sourceAccountId,
            String targetAccountId,
            BigDecimal amount,
            LocalDate date);

    /**
     * Transfiere a una cuenta de otro cliente del mismo banco.
     *
     * @param customerId      id del cliente que transfiere
     * @param sourceAccountId cuenta origen, del cliente
     * @param targetAccountId cuenta destino, de otro cliente
     * @param amount          monto a transferir
     * @param date            fecha de la transferencia
     * @return la transferencia guardada con la comisión de cada cuenta; error {@link
     *     NotFoundException} si una cuenta no existe o el origen no es del cliente; {@link
     *     BusinessRuleException} si se incumple una regla
     */
    Single<Transfer> transferToThirdParty(
            String customerId,
            String sourceAccountId,
            String targetAccountId,
            BigDecimal amount,
            LocalDate date);
}
