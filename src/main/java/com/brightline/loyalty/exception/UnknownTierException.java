package com.brightline.loyalty.exception;

import com.brightline.loyalty.domain.Tier;

public class UnknownTierException extends BrightlineLoyaltyException {

    private static final long serialVersionUID = 1L;

    public UnknownTierException(Tier tier) {
        super("No points rule registered for tier: " + tier);
    }
}
