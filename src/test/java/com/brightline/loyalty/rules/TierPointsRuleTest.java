package com.brightline.loyalty.rules;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.brightline.loyalty.domain.Tier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TierPointsRuleTest {

    @Test
    void eachRuleDeclaresTheTierItHandles() {
        assertAll(
                () -> assertEquals(Tier.STANDARD, new StandardTierRule().supportedTier()),
                () -> assertEquals(Tier.SILVER, new SilverTierRule().supportedTier()),
                () -> assertEquals(Tier.GOLD, new GoldTierRule().supportedTier()));
    }

    @ParameterizedTest(name = "standard: {0} base points gives {1}")
    @CsvSource({"0, 0", "9, 9", "100, 100"})
    void standardRuleKeepsTheBasePoints(int basePoints, int expected) {
        assertEquals(expected, new StandardTierRule().applyTo(basePoints));
    }

    @ParameterizedTest(name = "silver: {0} base points gives {1}")
    @CsvSource({"0, 0", "1, 1", "3, 4", "5, 7", "20, 30"})
    void silverRuleAppliesOnePointFiveAndTruncates(int basePoints, int expected) {
        assertEquals(expected, new SilverTierRule().applyTo(basePoints));
    }

    @ParameterizedTest(name = "gold: {0} base points gives {1}")
    @CsvSource({"0, 0", "25, 50", "100, 200"})
    void goldRuleDoublesTheBasePoints(int basePoints, int expected) {
        assertEquals(expected, new GoldTierRule().applyTo(basePoints));
    }

    @Test
    void silverRuleFailsLoudlyWhenTheResultDoesNotFitAnInt() {
        assertThrows(ArithmeticException.class,
                () -> new SilverTierRule().applyTo(2_000_000_000));
    }

    @Test
    void goldRuleFailsLoudlyWhenTheResultDoesNotFitAnInt() {
        assertThrows(ArithmeticException.class,
                () -> new GoldTierRule().applyTo(Integer.MAX_VALUE));
    }
}
