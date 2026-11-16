package com.promoquoter.pricing;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static com.promoquoter.pricing.PricingFixtures.money;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @ParameterizedTest
    @CsvSource({
            "0.125, 0.13",
            "0.124, 0.12",
            "0.135, 0.14",
            "2.5,   2.50",
            "-0.125, -0.13"
    })
    void roundsHalfUpToCents(String input, String expected) {
        assertThat(Money.round(money(input))).isEqualTo(money(expected));
    }

    @Test
    void calculatesAPercentageRoundedToCents() {
        assertThat(Money.percentOf(money("19.99"), money("15"))).isEqualTo(money("3.00"));
        assertThat(Money.percentOf(money("0.15"), money("10"))).isEqualTo(money("0.02"));
    }

    @Test
    void calculatesAProportionWithoutLosingPrecisionBeforeRounding() {
        assertThat(Money.proportion(money("100.00"), 1, 3)).isEqualTo(money("33.33"));
        assertThat(Money.proportion(money("100.00"), 2, 3)).isEqualTo(money("66.67"));
    }

    @Test
    void rejectsANonPositiveDenominator() {
        assertThatThrownBy(() -> Money.proportion(money("1.00"), 1, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
