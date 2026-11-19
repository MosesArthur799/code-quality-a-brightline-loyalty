package com.brightline.loyalty.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.brightline.loyalty.bonus.ThresholdBonusPolicy;
import com.brightline.loyalty.domain.Member;
import com.brightline.loyalty.domain.MemberId;
import com.brightline.loyalty.domain.Purchase;
import com.brightline.loyalty.domain.Tier;
import com.brightline.loyalty.exception.InvalidPurchaseException;
import com.brightline.loyalty.exception.MemberNotFoundException;
import com.brightline.loyalty.exception.UnknownTierException;
import com.brightline.loyalty.repository.MemberRepository;
import com.brightline.loyalty.rules.GoldTierRule;
import com.brightline.loyalty.rules.SilverTierRule;
import com.brightline.loyalty.rules.StandardTierRule;
import com.brightline.loyalty.rules.TierPointsRule;
import com.brightline.loyalty.rules.TierPointsRuleResolver;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PointsCalculatorTest {

    private static final MemberId MEMBER_ID = new MemberId("member-1");

    @Mock
    private MemberRepository repository;

    @ParameterizedTest(name = "{0} member spending GHS {1} earns {2} points")
    @MethodSource("tierAwardCases")
    void awardsTheExpectedPointsForTheTierAndAmount(Tier tier, BigDecimal amount, int expected) {
        givenMemberOnTier(tier);

        int points = calculatorWithAllRules().calculatePoints(new Purchase(MEMBER_ID, amount));

        assertEquals(expected, points);
    }

    private static Stream<Arguments> tierAwardCases() {
        return Stream.of(
                Arguments.of(Tier.STANDARD, new BigDecimal("99.99"), 9),
                Arguments.of(Tier.STANDARD, new BigDecimal("500"), 50),
                Arguments.of(Tier.STANDARD, new BigDecimal("500.01"), 100),
                Arguments.of(Tier.SILVER, new BigDecimal("10"), 1),
                Arguments.of(Tier.SILVER, new BigDecimal("50"), 7),
                Arguments.of(Tier.SILVER, new BigDecimal("200"), 30),
                Arguments.of(Tier.GOLD, new BigDecimal("250"), 50),
                Arguments.of(Tier.GOLD, new BigDecimal("500"), 100),
                Arguments.of(Tier.GOLD, new BigDecimal("500.01"), 150));
    }

    @Test
    void awardsZeroPointsForAZeroAmount() {
        givenMemberOnTier(Tier.STANDARD);

        int points = calculatorWithAllRules()
                .calculatePoints(new Purchase(MEMBER_ID, BigDecimal.ZERO));

        assertEquals(0, points);
    }

    @Test
    void rejectsANullPurchase() {
        InvalidPurchaseException failure = assertThrows(InvalidPurchaseException.class,
                () -> calculatorWithAllRules().calculatePoints(null));

        assertEquals("Purchase must not be null", failure.getMessage());
    }

    @Test
    void throwsMemberNotFoundExceptionForAnUnknownMember() {
        MemberId unknownMemberId = new MemberId("ghost");
        when(repository.findById(unknownMemberId)).thenReturn(Optional.empty());
        PointsCalculator calculator = calculatorWithAllRules();
        Purchase purchase = new Purchase(unknownMemberId, BigDecimal.TEN);

        MemberNotFoundException failure = assertThrows(MemberNotFoundException.class,
                () -> calculator.calculatePoints(purchase));

        assertEquals("Member not found: ghost", failure.getMessage());
    }

    @Test
    void throwsUnknownTierExceptionWhenTheMembersTierHasNoRule() {
        givenMemberOnTier(Tier.GOLD);
        PointsCalculator calculator = calculatorWith(new StandardTierRule(), new SilverTierRule());
        Purchase purchase = new Purchase(MEMBER_ID, BigDecimal.TEN);

        UnknownTierException failure = assertThrows(UnknownTierException.class,
                () -> calculator.calculatePoints(purchase));

        assertEquals("No points rule registered for tier: GOLD", failure.getMessage());
    }

    @ParameterizedTest(name = "{0} member spending GHS {1} is rejected as too large")
    @MethodSource("overflowCases")
    void rejectsAmountsTooLargeToAwardAndKeepsTheCause(Tier tier, BigDecimal amount) {
        givenMemberOnTier(tier);
        PointsCalculator calculator = calculatorWithAllRules();
        Purchase purchase = new Purchase(MEMBER_ID, amount);

        InvalidPurchaseException failure = assertThrows(InvalidPurchaseException.class,
                () -> calculator.calculatePoints(purchase));

        assertEquals("Purchase amount is too large to award points: " + amount,
                failure.getMessage());
        assertInstanceOf(ArithmeticException.class, failure.getCause());
    }

    private static Stream<Arguments> overflowCases() {
        return Stream.of(
                Arguments.of(Tier.STANDARD, new BigDecimal("10000000000000")),
                Arguments.of(Tier.STANDARD, new BigDecimal("21474836470")),
                Arguments.of(Tier.SILVER, new BigDecimal("20000000000")),
                Arguments.of(Tier.GOLD, new BigDecimal("15000000000")));
    }

    private void givenMemberOnTier(Tier tier) {
        when(repository.findById(MEMBER_ID))
                .thenReturn(Optional.of(new Member(MEMBER_ID, "Test Member", tier)));
    }

    private PointsCalculator calculatorWithAllRules() {
        return calculatorWith(new StandardTierRule(), new SilverTierRule(), new GoldTierRule());
    }

    private PointsCalculator calculatorWith(TierPointsRule... rules) {
        return new PointsCalculator(
                repository,
                new TierPointsRuleResolver(List.of(rules)),
                new ThresholdBonusPolicy());
    }
}
