package com.brightline.loyalty.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.brightline.loyalty.domain.Tier;
import com.brightline.loyalty.exception.UnknownTierException;
import java.util.List;
import org.junit.jupiter.api.Test;

class TierPointsRuleResolverTest {

    @Test
    void returnsTheRuleRegisteredForTheTier() {
        TierPointsRule goldRule = new GoldTierRule();
        TierPointsRuleResolver resolver =
                new TierPointsRuleResolver(List.of(new StandardTierRule(), goldRule));

        assertSame(goldRule, resolver.resolve(Tier.GOLD));
    }

    @Test
    void throwsUnknownTierExceptionWhenNoRuleIsRegisteredForTheTier() {
        TierPointsRuleResolver resolver =
                new TierPointsRuleResolver(List.of(new StandardTierRule()));

        UnknownTierException failure =
                assertThrows(UnknownTierException.class, () -> resolver.resolve(Tier.SILVER));

        assertEquals("No points rule registered for tier: SILVER", failure.getMessage());
    }

    @Test
    void refusesTwoRulesForTheSameTier() {
        List<TierPointsRule> duplicateRules = List.of(new GoldTierRule(), new GoldTierRule());

        assertThrows(IllegalStateException.class,
                () -> new TierPointsRuleResolver(duplicateRules));
    }

    @Test
    void rejectsANullTierWhenResolving() {
        TierPointsRuleResolver resolver =
                new TierPointsRuleResolver(List.of(new StandardTierRule()));

        NullPointerException failure =
                assertThrows(NullPointerException.class, () -> resolver.resolve(null));

        assertEquals("Tier must not be null", failure.getMessage());
    }
}
