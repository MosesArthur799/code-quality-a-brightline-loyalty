package com.brightline.loyalty.repository;

import com.brightline.loyalty.domain.Member;
import com.brightline.loyalty.domain.MemberId;
import com.brightline.loyalty.domain.Tier;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryMemberRepository implements MemberRepository {

    private final Map<MemberId, Member> members = new HashMap<>();

    public InMemoryMemberRepository() {
        MemberId standardMemberId = new MemberId("m1");
        MemberId goldMemberId = new MemberId("m2");
        MemberId silverMemberId = new MemberId("m3");
        members.put(standardMemberId, new Member(standardMemberId, "Kofi Mensah", Tier.STANDARD));
        members.put(goldMemberId, new Member(goldMemberId, "Ama Owusu", Tier.GOLD));
        members.put(silverMemberId, new Member(silverMemberId, "Yaa Boateng", Tier.SILVER));
    }

    @Override
    public Optional<Member> findById(MemberId memberId) {
        return Optional.ofNullable(members.get(memberId));
    }
}
