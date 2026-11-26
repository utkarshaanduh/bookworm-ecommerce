package com.bookworm.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.bookworm.dto.Dtos.PricePreview;
import com.bookworm.exception.ApiException;
import com.bookworm.model.*;
import com.bookworm.repository.CouponRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PricingServiceTest {
    private CouponRepository coupons;
    private PricingService pricing;
    private User user;

    @BeforeEach
    void setUp() {
        coupons = Mockito.mock(CouponRepository.class);
        pricing = new PricingService(coupons, new BigDecimal("0.12"), new BigDecimal("500"), new BigDecimal("40"));
        user = new User();
        user.setGiftPoints(100);
    }

    private CartItem item(String price, int qty) {
        Book b = new Book();
        b.setPrice(new BigDecimal(price));
        CartItem c = new CartItem();
        c.setBook(b);
        c.setQuantity(qty);
        return c;
    }

    @Test
    void freeDeliveryAtOrAboveThreshold() {
        PricePreview p = pricing.price(user, List.of(item("250", 2)), null, null);
        assertEquals(0, new BigDecimal("500").compareTo(p.subtotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(p.deliveryCharge()));
        assertEquals(0, new BigDecimal("560.00").compareTo(p.total()));
    }

    @Test
    void deliveryChargedBelowThreshold() {
        PricePreview p = pricing.price(user, List.of(item("100", 1)), null, null);
        assertEquals(0, new BigDecimal("40").compareTo(p.deliveryCharge()));
        assertEquals(0, new BigDecimal("152.00").compareTo(p.total())); // 100 + 12 + 40
    }

    @Test
    void couponAndPointsReduceTotal() {
        when(coupons.findById("WELCOME100")).thenReturn(Optional.of(new Coupon("WELCOME100", new BigDecimal("100"))));
        PricePreview p = pricing.price(user, List.of(item("250", 2)), "welcome100", 50);
        assertEquals(0, new BigDecimal("100").compareTo(p.discount()));
        assertEquals(50, p.pointsRedeemed());
        assertEquals(0, new BigDecimal("410.00").compareTo(p.total())); // 560 - 100 - 50
    }

    @Test
    void invalidCouponRejected() {
        when(coupons.findById("NOPE")).thenReturn(Optional.empty());
        assertThrows(ApiException.class, () -> pricing.price(user, List.of(item("100", 1)), "NOPE", null));
    }

    @Test
    void cannotRedeemMoreThanBalance() {
        assertThrows(ApiException.class, () -> pricing.price(user, List.of(item("100", 1)), null, 500));
    }

    @Test
    void totalNeverNegative() {
        user.setGiftPoints(10_000);
        PricePreview p = pricing.price(user, List.of(item("100", 1)), null, 10_000);
        assertEquals(0, BigDecimal.ZERO.compareTo(p.total()));
    }
}
