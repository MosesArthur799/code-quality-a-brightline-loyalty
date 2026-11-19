package com.brightline.loyalty.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.brightline.loyalty.domain.Member;
import com.brightline.loyalty.domain.MemberId;
import com.brightline.loyalty.domain.Tier;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class InMemoryMemberRepositoryTest {

    private final InMemoryMemberRepository repository = new InMemoryMemberRepository();

    @ParameterizedTest(name = "finds {0} ({1}, {2})")
    @MethodSource("seededMembers")
    void findsEachSeededMember(MemberId memberId, String name, Tier tier) {
        Optional<Member> found = repository.findById(memberId);

        assertEquals(Optional.of(new Member(memberId, name, tier)), found);
    }

    private static Stream<Arguments> seededMembers() {
        return Stream.of(
                Arguments.of(new MemberId("m1"), "Kofi Mensah", Tier.STANDARD),
                Arguments.of(new MemberId("m2"), "Ama Owusu", Tier.GOLD),
                Arguments.of(new MemberId("m3"), "Yaa Boateng", Tier.SILVER));
    }

    @Test
    void returnsAnEmptyOptionalForAnUnknownId() {
        assertEquals(Optional.empty(), repository.findById(new MemberId("unknown")));
    }
}
