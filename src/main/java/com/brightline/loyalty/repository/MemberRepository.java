package com.brightline.loyalty.repository;

import com.brightline.loyalty.domain.Member;
import com.brightline.loyalty.domain.MemberId;
import java.util.Optional;

public interface MemberRepository {

    Optional<Member> findById(MemberId id);
}
