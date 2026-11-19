package com.brightline.loyalty.bonus;

import com.brightline.loyalty.domain.Purchase;
import java.math.BigDecimal;

public class ThresholdBonusPolicy implements BonusPolicy {

    private static final BigDecimal THRESHOLD = new BigDecimal("500");
    private static final int BONUS_POINTS = 50;
    private static final int NO_BONUS_POINTS = 0;

    @Override
    public int bonusFor(Purchase purchase) {
        return purchase.amount().compareTo(THRESHOLD) > 0 ? BONUS_POINTS : NO_BONUS_POINTS;
    }
}
