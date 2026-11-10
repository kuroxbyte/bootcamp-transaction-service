package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.account.DailyAverageBalance;
import io.reactivex.rxjava3.core.Single;

import java.time.YearMonth;

/**
 * Caso de uso para que un cliente consulte el saldo promedio diario de una cuenta en un mes.
 *
 * <p>Reglas:
 *
 * <ul>
 *   <li>La cuenta debe existir y pertenecer al cliente.
 *   <li>Se promedian los saldos al final de cada día del mes en que la cuenta estuvo abierta,
 *       hasta hoy.
 *   <li>Si la cuenta exige un mínimo de promedio diario (ahorro VIP), se indica si lo alcanza. Es
 *       informativo: no bloquea ni cobra nada.
 * </ul>
 *
 * <p>Es reactivo: no hace nada hasta que se suscribe al {@link Single}.
 */
public interface GetDailyAverageBalanceUseCase {

    /**
     * Calcula el saldo promedio diario de la cuenta en el mes indicado.
     *
     * @param customerId id del cliente que consulta
     * @param accountId  id de la cuenta en account-service
     * @param month      mes consultado
     * @return el promedio del mes y el mínimo que exige la cuenta; error {@link NotFoundException}
     *     si la cuenta no existe o no pertenece al cliente; {@link BusinessRuleException} si la
     *     cuenta no estuvo abierta ningún día del mes hasta hoy
     */
    Single<DailyAverageBalance> execute(String customerId, String accountId, YearMonth month);
}
