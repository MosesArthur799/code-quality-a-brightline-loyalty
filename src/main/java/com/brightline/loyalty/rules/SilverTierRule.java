package com.brightline.loyalty.rules;

import com.brightline.loyalty.domain.Tier;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class SilverTierRule implements TierPointsRule {

    private static final BigDecimal MULTIPLIER = new BigDecimal("1.5");

    @Override
    public Tier supportedTier() {
        return Tier.SILVER;
    }

    @Override
    public int applyTo(int basePoints) {
        return BigDecimal.valueOf(basePoints)
                .multiply(MULTIPLIER)
                .setScale(0, RoundingMode.DOWN)
                .intValueExact();
    }
}
