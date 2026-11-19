package com.brightline.loyalty.domain;

public record MemberId(String value) {

    public MemberId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Member id must not be blank");
        }
    }
}
