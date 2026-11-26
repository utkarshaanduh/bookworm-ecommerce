package com.bookworm.service;

import com.bookworm.dto.Dtos.PricePreview;
import com.bookworm.exception.ApiException;
import com.bookworm.model.CartItem;
import com.bookworm.model.Coupon;
import com.bookworm.model.User;
import com.bookworm.repository.CouponRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Single source of truth for cart/checkout totals. 1 gift point = 1 rupee. */
@Service
public class PricingService {
    private final CouponRepository coupons;
    private final BigDecimal taxRate;
    private final BigDecimal freeThreshold;
    private final BigDecimal deliveryCharge;

    public PricingService(CouponRepository coupons,
                          @Value("${bookworm.tax-rate}") BigDecimal taxRate,
                          @Value("${bookworm.free-delivery-threshold}") BigDecimal freeThreshold,
                          @Value("${bookworm.delivery-charge}") BigDecimal deliveryCharge) {
        this.coupons = coupons;
        this.taxRate = taxRate;
        this.freeThreshold = freeThreshold;
        this.deliveryCharge = deliveryCharge;
    }

    public PricePreview price(User user, List<CartItem> items, String couponCode, Integer pointsToRedeem) {
        BigDecimal subtotal = items.stream()
                .map(i -> i.getBook().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal delivery = items.isEmpty() || subtotal.compareTo(freeThreshold) >= 0 ? BigDecimal.ZERO : deliveryCharge;
        BigDecimal payable = subtotal.add(tax).add(delivery);

        BigDecimal discount = BigDecimal.ZERO;
        if (couponCode != null && !couponCode.isBlank()) {
            Coupon c = coupons.findById(couponCode.trim().toUpperCase())
                    .filter(Coupon::isActive)
                    .orElseThrow(() -> ApiException.badRequest("Invalid or expired coupon"));
            discount = c.getDiscountAmount().min(payable);
        }
        int points = 0;
        if (pointsToRedeem != null && pointsToRedeem > 0) {
            if (pointsToRedeem > user.getGiftPoints()) throw ApiException.badRequest("Not enough gift points");
            BigDecimal remaining = payable.subtract(discount);
            points = Math.min(pointsToRedeem, remaining.setScale(0, RoundingMode.FLOOR).intValue());
        }
        BigDecimal total = payable.subtract(discount).subtract(BigDecimal.valueOf(points)).max(BigDecimal.ZERO);
        return new PricePreview(subtotal, tax, delivery, discount, points, total);
    }
}
