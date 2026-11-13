package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.BankProductClient;
import dev.bootcamp.transactionservice.application.port.ChargeRepository;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.application.port.PaymentRepository;
import dev.bootcamp.transactionservice.domain.model.report.ProductReport;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

/**
 * Implementación de {@link GenerateProductReportUseCase}.
 */
@RequiredArgsConstructor
public class GenerateProductReportUseCaseImpl implements GenerateProductReportUseCase {
    private final BankProductClient bankProductClient;
    private final MovementRepository movementRepository;
    private final PaymentRepository paymentRepository;
    private final ChargeRepository chargeRepository;

    @Override
    public Single<ProductReport> execute(LocalDate from, LocalDate to) {
        return Completable.fromAction(() -> ProductReport.validateInterval(from, to))
                .andThen(Single.defer(() -> Single.zip(
                        bankProductClient.findAll().toList(),
                        movementRepository.findByDateBetween(from, to).toList(),
                        paymentRepository.findByDateBetween(from, to).toList(),
                        chargeRepository.findByDateBetween(from, to).toList(),
                        (products, movements, payments, charges) -> ProductReport.generate(
                                from,
                                to,
                                products,
                                movements,
                                payments,
                                charges))));
    }
}
