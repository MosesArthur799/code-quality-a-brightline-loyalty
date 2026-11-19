package com.brightline.loyalty.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.brightline.loyalty.exception.InvalidPurchaseException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PurchaseTest {

    private static final MemberId MEMBER_ID = new MemberId("m1");

    @Test
    void keepsTheValuesItWasCreatedWith() {
        Purchase purchase = new Purchase(MEMBER_ID, new BigDecimal("100.50"));

        assertAll(
                () -> assertEquals(MEMBER_ID, purchase.memberId()),
                () -> assertEquals(new BigDecimal("100.50"), purchase.amount()));
    }

    @Test
    void acceptsAZeroAmount() {
        Purchase purchase = new Purchase(MEMBER_ID, BigDecimal.ZERO);

        assertEquals(BigDecimal.ZERO, purchase.amount());
    }

    @Test
    void rejectsANullMemberId() {
        InvalidPurchaseException failure = assertThrows(InvalidPurchaseException.class,
                () -> new Purchase(null, BigDecimal.TEN));

        assertEquals("Member id must not be blank", failure.getMessage());
    }

    @Test
    void rejectsANullAmount() {
        InvalidPurchaseException failure = assertThrows(InvalidPurchaseException.class,
                () -> new Purchase(MEMBER_ID, null));

        assertEquals("Purchase amount must not be null", failure.getMessage());
    }

    @Test
    void rejectsANegativeAmount() {
        InvalidPurchaseException failure = assertThrows(InvalidPurchaseException.class,
                () -> new Purchase(MEMBER_ID, new BigDecimal("-50")));

        assertEquals("Purchase amount must not be negative: -50", failure.getMessage());
    }
}
