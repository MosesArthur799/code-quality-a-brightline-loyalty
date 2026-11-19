package com.brightline.loyalty.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MemberIdTest {

    @Test
    void retainsItsValue() {
        assertEquals("m1", new MemberId("m1").value());
    }

    @Test
    void rejectsANullValue() {
        IllegalArgumentException failure =
                assertThrows(IllegalArgumentException.class, () -> new MemberId(null));

        assertEquals("Member id must not be blank", failure.getMessage());
    }

    @ParameterizedTest(name = "rejects blank member id \"{0}\"")
    @ValueSource(strings = {"", " ", "\t"})
    void rejectsBlankValues(String blankValue) {
        IllegalArgumentException failure =
                assertThrows(IllegalArgumentException.class, () -> new MemberId(blankValue));

        assertEquals("Member id must not be blank", failure.getMessage());
    }
}
