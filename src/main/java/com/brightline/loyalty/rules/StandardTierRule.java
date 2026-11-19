package com.brightline.loyalty.rules;

import com.brightline.loyalty.domain.Tier;

public class StandardTierRule implements TierPointsRule {

    private static final int MULTIPLIER = 1;

    @Override
    public Tier supportedTier() {
        return Tier.STANDARD;
    }

    @Override
    public int applyTo(int basePoints) {
        return Math.multiplyExact(basePoints, MULTIPLIER);
    }
}
