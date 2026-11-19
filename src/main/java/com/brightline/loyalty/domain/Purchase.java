package com.brightline.loyalty.domain;

import com.brightline.loyalty.exception.InvalidPurchaseException;
import java.math.BigDecimal;

public record Purchase(MemberId memberId, BigDecimal amount) {

    public Purchase {
        if (memberId == null) {
            throw new InvalidPurchaseException("Member id must not be blank");
        }
        if (amount == null) {
            throw new InvalidPurchaseException("Purchase amount must not be null");
        }
        if (amount.signum() < 0) {
            throw new InvalidPurchaseException("Purchase amount must not be negative: " + amount);
        }
    }
}
