package dev.bootcamp.transactionservice.infrastructure.persistence;

import dev.bootcamp.transactionservice.application.port.ChargeRepository;
import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Implementa el puerto {@link ChargeRepository} con MongoDB reactivo. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChargePersistenceAdapter implements ChargeRepository {

    private final ChargeMongoRepository chargeMongoRepository;
    private final PaymentMongoRepository paymentMongoRepository;

    /** Suma los consumos de la tarjeta y resta los pagos hechos a ella. */
    @Override
    public Single<BigDecimal> getDebt(String creditCardId) {
        Single<BigDecimal> charges =
                Flowable.fromPublisher(chargeMongoRepository.findByCreditCardId(creditCardId))
                        .map(ChargeDocument::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        Single<BigDecimal> payments =
                Flowable.fromPublisher(
                                paymentMongoRepository.findByTargetAndProductId(
                                        PaymentTarget.CREDIT_CARD.name(), creditCardId))
                        .map(PaymentDocument::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Single.zip(charges, payments, BigDecimal::subtract);
    }

    @Override
    public Flowable<Charge> findByCreditCardId(String creditCardId) {
        return Flowable.fromPublisher(chargeMongoRepository.findByCreditCardId(creditCardId))
                .map(TransactionPersistenceMapper::toDomain);
    }

    @Override
    public Single<Charge> save(Charge charge) {
        return Single.fromPublisher(
                        chargeMongoRepository.save(TransactionPersistenceMapper.toDocument(charge)))
                .doOnSuccess(
                        saved ->
                                log.debug(
                                        "Charge saved chargeId={} creditCardId={}",
                                        saved.getId(),
                                        saved.getCreditCardId()))
                .map(TransactionPersistenceMapper::toDomain);
    }
}
