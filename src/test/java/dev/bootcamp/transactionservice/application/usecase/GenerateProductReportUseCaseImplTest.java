package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.BankProductClient;
import dev.bootcamp.transactionservice.application.port.ChargeRepository;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.application.port.PaymentRepository;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import dev.bootcamp.transactionservice.domain.model.report.BankProduct;
import dev.bootcamp.transactionservice.domain.model.report.ProductReport;
import dev.bootcamp.transactionservice.domain.model.report.ProductType;
import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateProductReportUseCaseImplTest {

    private static final LocalDate FROM = LocalDate.of(2026, 11, 1);
    private static final LocalDate TO = LocalDate.of(2026, 11, 30);

    @Mock private BankProductClient bankProductClient;

    @Mock private MovementRepository movementRepository;

    @Mock private PaymentRepository paymentRepository;

    @Mock private ChargeRepository chargeRepository;

    private GenerateProductReportUseCaseImpl useCase;

    @BeforeEach
    void createUseCase() {
        useCase = new GenerateProductReportUseCaseImpl(
                bankProductClient,
                movementRepository,
                paymentRepository,
                chargeRepository);
    }

    @Test
    void generatesReportWithProductsAndTransactionsOfTheInterval() {
        when(bankProductClient.findAll()).thenReturn(Flowable.just(
                new BankProduct("s1", ProductType.SAVINGS, LocalDate.of(2026, 11, 3))));
        when(movementRepository.findByDateBetween(FROM, TO)).thenReturn(Flowable.just(
                new Movement(
                        "s1",
                        MovementType.DEPOSIT,
                        BigDecimal.TEN,
                        LocalDate.of(2026, 11, 4))));
        when(paymentRepository.findByDateBetween(FROM, TO)).thenReturn(Flowable.empty());
        when(chargeRepository.findByDateBetween(FROM, TO)).thenReturn(Flowable.empty());

        ProductReport report = useCase.execute(FROM, TO).blockingGet();

        assertThat(report.products().get(0).type()).isEqualTo(ProductType.SAVINGS);
        assertThat(report.products().get(0).openedProducts()).isEqualTo(1);
        assertThat(report.products().get(0).transactions()).isEqualTo(1);
    }

    @Test
    void rejectsInvertedIntervalWithoutQueryingAnything() {
        useCase.execute(TO, FROM).test().assertError(BusinessRuleException.class);

        verifyNoInteractions(
                bankProductClient,
                movementRepository,
                paymentRepository,
                chargeRepository);
    }
}
