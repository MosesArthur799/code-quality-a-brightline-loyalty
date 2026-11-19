package com.brightline.loyalty.rules;

import com.brightline.loyalty.domain.Tier;

public class GoldTierRule implements TierPointsRule {

    private static final int MULTIPLIER = 2;

    @Override
    public Tier supportedTier() {
        return Tier.GOLD;
    }

    @Override
    public int applyTo(int basePoints) {
        return Math.multiplyExact(basePoints, MULTIPLIER);
    }
}
