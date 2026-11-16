package com.promoquoter.pricing;

import com.promoquoter.pricing.rules.PercentOffCategoryRule;
import com.promoquoter.promotion.PromotionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static com.promoquoter.pricing.PricingFixtures.context;
import static com.promoquoter.pricing.PricingFixtures.item;
import static com.promoquoter.pricing.PricingFixtures.money;
import static org.assertj.core.api.Assertions.assertThat;

class AuditingPromotionRuleTest {

    private final PercentOffCategoryRule delegate =
            new PercentOffCategoryRule(UUID.randomUUID(), "Books 20%", "BOOKS", BigDecimal.valueOf(20));

    @Test
    void recordsTheTotalBeforeAndAfterTheWrappedRule() {
        PricingContext context = context(item("Novel", "BOOKS", "10.00", 2), item("Pen", "OFFICE", "1.00", 1));

        new AuditingPromotionRule(delegate).apply(context);

        assertThat(context.auditTrail()).singleElement().satisfies(entry -> {
            assertThat(entry.step()).isEqualTo(1);
            assertThat(entry.promotionId()).isEqualTo(delegate.promotionId());
            assertThat(entry.promotionName()).isEqualTo("Books 20%");
            assertThat(entry.type()).isEqualTo(PromotionType.PERCENT_OFF_CATEGORY);
            assertThat(entry.totalBefore()).isEqualTo(money("21.00"));
            assertThat(entry.totalAfter()).isEqualTo(money("17.00"));
            assertThat(entry.discount()).isEqualTo(money("4.00"));
            assertThat(entry.changedPrice()).isTrue();
        });
    }

    @Test
    void recordsRulesThatChangedNothing() {
        PricingContext context = context(item("Pen", "OFFICE", "1.00", 1));

        new AuditingPromotionRule(delegate).apply(context);

        assertThat(context.auditTrail()).singleElement()
                .satisfies(entry -> assertThat(entry.changedPrice()).isFalse());
    }

    @Test
    void numbersStepsInExecutionOrder() {
        PricingContext context = context(item("Novel", "BOOKS", "10.00", 1));
        AuditingPromotionRule audited = new AuditingPromotionRule(delegate);

        audited.apply(context);
        audited.apply(context);

        assertThat(context.auditTrail()).extracting(AuditEntry::step).containsExactly(1, 2);
    }

    @Test
    void exposesTheDelegateIdentity() {
        AuditingPromotionRule audited = new AuditingPromotionRule(delegate);

        assertThat(audited.promotionId()).isEqualTo(delegate.promotionId());
        assertThat(audited.name()).isEqualTo(delegate.name());
        assertThat(audited.type()).isEqualTo(delegate.type());
        assertThat(audited.priority()).isEqualTo(delegate.priority());
        assertThat(audited).hasToString("Audited[" + delegate + "]");
    }
}
