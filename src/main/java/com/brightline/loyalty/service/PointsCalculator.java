package com.brightline.loyalty.service;

import com.brightline.loyalty.bonus.BonusPolicy;
import com.brightline.loyalty.domain.Member;
import com.brightline.loyalty.domain.MemberId;
import com.brightline.loyalty.domain.Purchase;
import com.brightline.loyalty.exception.InvalidPurchaseException;
import com.brightline.loyalty.exception.MemberNotFoundException;
import com.brightline.loyalty.repository.MemberRepository;
import com.brightline.loyalty.rules.TierPointsRuleResolver;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class PointsCalculator {

    private static final BigDecimal GHS_PER_POINT = BigDecimal.TEN;

    private final MemberRepository repository;
    private final TierPointsRuleResolver ruleResolver;
    private final BonusPolicy bonusPolicy;

    public PointsCalculator(MemberRepository repository,
                            TierPointsRuleResolver ruleResolver,
                            BonusPolicy bonusPolicy) {
        this.repository = Objects.requireNonNull(repository, "Member repository must not be null");
        this.ruleResolver = Objects.requireNonNull(ruleResolver, "Rule resolver must not be null");
        this.bonusPolicy = Objects.requireNonNull(bonusPolicy, "Bonus policy must not be null");
    }

    public int calculatePoints(Purchase purchase) {
        if (purchase == null) {
            throw new InvalidPurchaseException("Purchase must not be null");
        }
        Member member = findMember(purchase.memberId());
        try {
            int tierPoints = ruleResolver.resolve(member.tier()).applyTo(basePointsFor(purchase));
            return Math.addExact(tierPoints, bonusPolicy.bonusFor(purchase));
        } catch (ArithmeticException arithmeticException) {
            throw new InvalidPurchaseException(
                    "Purchase amount is too large to award points: " + purchase.amount(),
                    arithmeticException);
        }
    }

    private Member findMember(MemberId memberId) {
        return repository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
    }

    private int basePointsFor(Purchase purchase) {
        return purchase.amount()
                .divide(GHS_PER_POINT, 0, RoundingMode.DOWN)
                .intValueExact();
    }
}
