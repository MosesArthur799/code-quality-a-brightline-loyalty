package com.brightline.loyalty.bonus;

import com.brightline.loyalty.domain.Purchase;

public interface BonusPolicy {

    int bonusFor(Purchase purchase);
}
