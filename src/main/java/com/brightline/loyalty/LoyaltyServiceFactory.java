package com.brightline.loyalty;

import com.brightline.loyalty.bonus.ThresholdBonusPolicy;
import com.brightline.loyalty.repository.InMemoryMemberRepository;
import com.brightline.loyalty.rules.GoldTierRule;
import com.brightline.loyalty.rules.SilverTierRule;
import com.brightline.loyalty.rules.StandardTierRule;
import com.brightline.loyalty.rules.TierPointsRule;
import com.brightline.loyalty.rules.TierPointsRuleResolver;
import com.brightline.loyalty.service.PointsCalculator;
import java.util.List;

public class LoyaltyServiceFactory {

    public List<TierPointsRule> productionRules() {
        return List.of(new StandardTierRule(), new SilverTierRule(), new GoldTierRule());
    }

    public PointsCalculator createCalculator() {
        return new PointsCalculator(
                new InMemoryMemberRepository(),
                new TierPointsRuleResolver(productionRules()),
                new ThresholdBonusPolicy());
    }
}
