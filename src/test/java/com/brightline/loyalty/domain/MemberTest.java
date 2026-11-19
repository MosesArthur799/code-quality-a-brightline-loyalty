package com.brightline.loyalty.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MemberTest {

    @Test
    void keepsTheValuesItWasCreatedWith() {
        MemberId memberId = new MemberId("m9");
        Member member = new Member(memberId, "Efua Asante", Tier.SILVER);

        assertAll(
                () -> assertEquals(memberId, member.id()),
                () -> assertEquals("Efua Asante", member.name()),
                () -> assertEquals(Tier.SILVER, member.tier()));
    }

    @Test
    void rejectsANullId() {
        NullPointerException failure = assertThrows(NullPointerException.class,
                () -> new Member(null, "Efua Asante", Tier.GOLD));

        assertEquals("Member id must not be null", failure.getMessage());
    }

    @Test
    void rejectsANullName() {
        NullPointerException failure = assertThrows(NullPointerException.class,
                () -> new Member(new MemberId("m9"), null, Tier.GOLD));

        assertEquals("Member name must not be null", failure.getMessage());
    }

    @Test
    void rejectsANullTier() {
        NullPointerException failure = assertThrows(NullPointerException.class,
                () -> new Member(new MemberId("m9"), "Efua Asante", null));

        assertEquals("Member tier must not be null", failure.getMessage());
    }
}
