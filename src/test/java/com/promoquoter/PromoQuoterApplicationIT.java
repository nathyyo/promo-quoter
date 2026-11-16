package com.promoquoter;

import com.promoquoter.pricing.PromotionRuleProvider;
import com.promoquoter.promotion.Promotion;
import com.promoquoter.promotion.PromotionType;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PromoQuoterApplicationIT {

    @Autowired
    private List<PromotionRuleProvider<?>> ruleProviders;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void registersExactlyOneRuleProviderPerPromotionType() {
        Set<Class<?>> promotionEntities = entityManagerFactory.getMetamodel().getEntities().stream()
                .map(EntityType::getJavaType)
                .filter(type -> Promotion.class.isAssignableFrom(type) && type != Promotion.class)
                .collect(Collectors.toSet());

        assertThat(promotionEntities).hasSize(PromotionType.values().length);
        List<Class<?>> providedClasses = ruleProviders.stream()
                .<Class<?>>map(PromotionRuleProvider::promotionClass)
                .toList();
        assertThat(providedClasses).containsExactlyInAnyOrderElementsOf(promotionEntities);
    }
}
