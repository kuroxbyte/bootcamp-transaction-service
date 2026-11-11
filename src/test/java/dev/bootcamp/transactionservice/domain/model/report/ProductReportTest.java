package dev.bootcamp.transactionservice.domain.model.report;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import dev.bootcamp.transactionservice.domain.model.history.ProductMovementType;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductReportTest {

    private static final LocalDate FROM = LocalDate.of(2026, 11, 1);
    private static final LocalDate TO = LocalDate.of(2026, 11, 30);
    private static final LocalDate DAY = LocalDate.of(2026, 11, 10);

    private static final List<BankProduct> PRODUCTS = List.of(
            new BankProduct("s1", ProductType.SAVINGS, LocalDate.of(2026, 11, 2)),
            new BankProduct("s2", ProductType.SAVINGS, LocalDate.of(2026, 10, 31)),
            new BankProduct("c1", ProductType.CHECKING, LocalDate.of(2026, 11, 30)),
            new BankProduct("k1", ProductType.PERSONAL_CREDIT, LocalDate.of(2026, 11, 5)),
            new BankProduct("t1", ProductType.CREDIT_CARD, LocalDate.of(2026, 12, 1)));

    private static ProductTypeReport summaryOf(ProductReport report, ProductType type) {
        return report.products().stream()
                .filter(product -> product.type() == type)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void countsProductsOpenedInsideTheInterval() {
        ProductReport report = ProductReport.generate(
                FROM,
                TO,
                PRODUCTS,
                List.of(),
                List.of(),
                List.of());

        assertThat(report.products()).extracting(ProductTypeReport::type)
                .containsExactly(ProductType.values());
        assertThat(summaryOf(report, ProductType.SAVINGS).openedProducts()).isEqualTo(1);
        assertThat(summaryOf(report, ProductType.CHECKING).openedProducts()).isEqualTo(1);
        assertThat(summaryOf(report, ProductType.PERSONAL_CREDIT).openedProducts()).isEqualTo(1);
        assertThat(summaryOf(report, ProductType.CREDIT_CARD).openedProducts()).isZero();
    }

    @Test
    void groupsMovementsAndCommissionsByProductType() {
        List<Movement> movements = List.of(
                new Movement("s1", MovementType.DEPOSIT, new BigDecimal("100"), DAY),
                new Movement("s2", MovementType.DEPOSIT, new BigDecimal("50"), DAY)
                        .withCommission(new BigDecimal("1")),
                new Movement("s1", MovementType.TRANSFER_OUT, new BigDecimal("30"), DAY)
                        .withCommission(new BigDecimal("1")),
                new Movement("c1", MovementType.WITHDRAWAL, new BigDecimal("20"), DAY));

        ProductReport report = ProductReport.generate(
                FROM,
                TO,
                PRODUCTS,
                movements,
                List.of(),
                List.of());

        ProductTypeReport savings = summaryOf(report, ProductType.SAVINGS);
        assertThat(savings.transactions()).isEqualTo(3);
        assertThat(savings.commissions()).isEqualByComparingTo("2");
        assertThat(savings.movements()).containsExactly(
                new MovementTotal(ProductMovementType.DEPOSIT, 2, new BigDecimal("150")),
                new MovementTotal(ProductMovementType.TRANSFER_OUT, 1, new BigDecimal("30")));
        assertThat(summaryOf(report, ProductType.CHECKING).movements()).containsExactly(
                new MovementTotal(ProductMovementType.WITHDRAWAL, 1, new BigDecimal("20")));
    }

    @Test
    void assignsPaymentsAndChargesToTheirCreditProduct() {
        List<Payment> payments = List.of(
                new Payment(PaymentTarget.CREDIT, "k1", new BigDecimal("80"), DAY),
                new Payment(PaymentTarget.CREDIT_CARD, "t1", new BigDecimal("40"), DAY));
        List<Charge> charges = List.of(new Charge("t1", new BigDecimal("200"), DAY));

        ProductReport report = ProductReport.generate(
                FROM,
                TO,
                PRODUCTS,
                List.of(),
                payments,
                charges);

        assertThat(summaryOf(report, ProductType.PERSONAL_CREDIT).movements()).containsExactly(
                new MovementTotal(ProductMovementType.PAYMENT, 1, new BigDecimal("80")));
        assertThat(summaryOf(report, ProductType.CREDIT_CARD).movements()).containsExactly(
                new MovementTotal(ProductMovementType.CHARGE, 1, new BigDecimal("200")),
                new MovementTotal(ProductMovementType.PAYMENT, 1, new BigDecimal("40")));
    }

    @Test
    void ignoresMovementsOfUnknownProducts() {
        List<Movement> movements =
                List.of(new Movement("zz", MovementType.DEPOSIT, BigDecimal.TEN, DAY));

        ProductReport report = ProductReport.generate(
                FROM,
                TO,
                PRODUCTS,
                movements,
                List.of(),
                List.of());

        assertThat(report.products()).allMatch(product -> product.transactions() == 0);
    }

    @Test
    void rejectsInvertedInterval() {
        assertThatThrownBy(() -> ProductReport.validateInterval(TO, FROM))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La fecha inicial no puede ser posterior a la fecha final");
        assertThatThrownBy(() -> new ProductReport(TO, FROM, List.of()))
                .isInstanceOf(BusinessRuleException.class);
    }
}
