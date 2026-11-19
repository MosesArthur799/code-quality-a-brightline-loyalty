package com.brightline.loyalty.bonus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.brightline.loyalty.domain.MemberId;
import com.brightline.loyalty.domain.Purchase;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ThresholdBonusPolicyTest {

    private final ThresholdBonusPolicy policy = new ThresholdBonusPolicy();

    @ParameterizedTest(name = "GHS {0} earns a bonus of {1}")
    @CsvSource({
        "0, 0",
        "499.99, 0",
        "500, 0",
        "500.01, 50",
        "1000, 50"
    })
    void paysTheBonusOnlyWhenTheAmountIsStrictlyAboveTheThreshold(BigDecimal amount, int expected) {
        assertEquals(expected, policy.bonusFor(new Purchase(new MemberId("m1"), amount)));
    }
}
