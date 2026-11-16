package com.promoquoter.pricing;

import com.promoquoter.promotion.Promotion;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory that turns persisted promotion definitions into executable {@link PromotionRule}s by
 * delegating to the registered {@link PromotionRuleProvider} for each definition class.
 */
@Component
public class PromotionRuleFactory {

    private final Map<Class<?>, PromotionRuleProvider<?>> providers = new HashMap<>();

    public PromotionRuleFactory(List<PromotionRuleProvider<?>> providers) {
        for (PromotionRuleProvider<?> provider : providers) {
            PromotionRuleProvider<?> existing = this.providers.putIfAbsent(provider.promotionClass(), provider);
            if (existing != null) {
                throw new IllegalStateException("Two rule providers registered for "
                        + provider.promotionClass().getSimpleName() + ": "
                        + existing.getClass().getName() + " and " + provider.getClass().getName());
            }
        }
    }

    public PromotionRule create(Promotion promotion) {
        PromotionRuleProvider<?> provider = providers.get(promotion.getClass());
        if (provider == null) {
            // Fallback for subclasses such as persistence proxies.
            provider = providers.values().stream()
                    .filter(candidate -> candidate.promotionClass().isInstance(promotion))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No rule provider registered for " + promotion.getClass().getName()));
        }
        return createWith(provider, promotion);
    }

    private static <P extends Promotion> PromotionRule createWith(PromotionRuleProvider<P> provider, Promotion promotion) {
        return provider.create(provider.promotionClass().cast(promotion));
    }
}
