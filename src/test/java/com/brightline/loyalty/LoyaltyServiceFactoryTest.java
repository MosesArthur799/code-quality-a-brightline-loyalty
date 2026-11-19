package com.brightline.loyalty;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.brightline.loyalty.domain.MemberId;
import com.brightline.loyalty.domain.Purchase;
import com.brightline.loyalty.domain.Tier;
import com.brightline.loyalty.rules.TierPointsRuleResolver;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LoyaltyServiceFactoryTest {

    private final LoyaltyServiceFactory factory = new LoyaltyServiceFactory();

    @ParameterizedTest(name = "production wiring has a rule for {0}")
    @EnumSource(Tier.class)
    void everyTierHasARegisteredRule(Tier tier) {
        TierPointsRuleResolver resolver = new TierPointsRuleResolver(factory.productionRules());

        assertEquals(tier, resolver.resolve(tier).supportedTier());
    }

    @Test
    void createsACalculatorThatAwardsPointsToASeededMember() {
        int points = factory.createCalculator()
                .calculatePoints(new Purchase(new MemberId("m1"), new BigDecimal("100")));

        assertEquals(10, points);
    }
}
