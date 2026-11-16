package com.promoquoter.cart;

import com.promoquoter.order.Order;
import com.promoquoter.promotion.CustomerSegment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A confirmed order: its id plus the same itemised breakdown a quote returns.
 */
public record ConfirmResponse(
        UUID orderId,
        Instant createdAt,
        CustomerSegment customerSegment,
        List<QuoteResponse.Line> lines,
        List<QuoteResponse.Applied> appliedPromotions,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal total) {

    static ConfirmResponse from(Order order, QuoteResponse price) {
        return new ConfirmResponse(order.getId(), order.getCreatedAt(), price.customerSegment(), price.lines(),
                price.appliedPromotions(), price.subtotal(), price.discountTotal(), price.total());
    }
}
