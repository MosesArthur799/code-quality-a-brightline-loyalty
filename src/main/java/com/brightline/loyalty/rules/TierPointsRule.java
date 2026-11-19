package com.brightline.loyalty.rules;

import com.brightline.loyalty.domain.Tier;

public interface TierPointsRule {

    Tier supportedTier();

    int applyTo(int basePoints);
}
