package com.brightline.loyalty.exception;

import com.brightline.loyalty.domain.MemberId;

public class MemberNotFoundException extends BrightlineLoyaltyException {

    private static final long serialVersionUID = 1L;

    public MemberNotFoundException(MemberId memberId) {
        super("Member not found: " + memberId.value());
    }
}
