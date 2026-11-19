package com.brightline.loyalty.domain;

import java.util.Objects;

public record Member(MemberId id, String name, Tier tier) {

    public Member {
        Objects.requireNonNull(id, "Member id must not be null");
        Objects.requireNonNull(name, "Member name must not be null");
        Objects.requireNonNull(tier, "Member tier must not be null");
    }
}
