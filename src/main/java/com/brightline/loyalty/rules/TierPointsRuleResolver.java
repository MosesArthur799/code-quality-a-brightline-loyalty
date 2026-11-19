package com.brightline.loyalty.rules;

import com.brightline.loyalty.domain.Tier;
import com.brightline.loyalty.exception.UnknownTierException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TierPointsRuleResolver {

    private final Map<Tier, TierPointsRule> rulesByTier;

    public TierPointsRuleResolver(List<TierPointsRule> rules) {
        Objects.requireNonNull(rules, "Tier points rules must not be null");
        this.rulesByTier = rules.stream()
                .map(rule -> Objects.requireNonNull(rule, "Tier points rule must not be null"))
                .collect(
                Collectors.toUnmodifiableMap(TierPointsRule::supportedTier, Function.identity()));
    }

    public TierPointsRule resolve(Tier tier) {
        Objects.requireNonNull(tier, "Tier must not be null");
        TierPointsRule rule = rulesByTier.get(tier);
        if (rule == null) {
            throw new UnknownTierException(tier);
        }
        return rule;
    }
}
