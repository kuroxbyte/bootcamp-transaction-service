package dev.bootcamp.transactionservice.infrastructure.persistence;

import dev.bootcamp.transactionservice.application.port.PaymentRepository;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Implementa el puerto {@link PaymentRepository} con MongoDB reactivo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentPersistenceAdapter implements PaymentRepository {

    private final PaymentMongoRepository mongoRepository;

    @Override
    public Flowable<Payment> findByTargetAndProductId(PaymentTarget target, String productId) {
        return Flowable.fromPublisher(
                        mongoRepository.findByTargetAndProductId(target.name(), productId))
                .map(TransactionPersistenceMapper::toDomain);
    }

    @Override
    public Single<Payment> save(Payment payment) {
        return Single.fromPublisher(
                        mongoRepository.save(TransactionPersistenceMapper.toDocument(payment)))
                .doOnSuccess(
                        saved ->
                                log.debug(
                                        "Payment saved paymentId={} target={} productId={}",
                                        saved.getId(),
                                        saved.getTarget(),
                                        saved.getProductId()))
                .map(TransactionPersistenceMapper::toDomain);
    }

    @Override
    public Flowable<Payment> findByDateBetween(LocalDate from, LocalDate to) {
        return Flowable.fromPublisher(mongoRepository.findByDateBetween(Range.closed(from, to)))
                .map(TransactionPersistenceMapper::toDomain);
    }
}
