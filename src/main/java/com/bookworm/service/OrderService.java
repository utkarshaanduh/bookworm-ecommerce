package com.bookworm.service;

import com.bookworm.dto.Dtos.*;
import com.bookworm.exception.ApiException;
import com.bookworm.model.*;
import com.bookworm.repository.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {
    private final OrderRepository orders;
    private final CartItemRepository carts;
    private final AddressRepository addresses;
    private final UserRepository users;
    private final BookRepository books;
    private final PricingService pricing;
    private final int cancelWindowHours;

    public OrderService(OrderRepository orders, CartItemRepository carts, AddressRepository addresses,
                        UserRepository users, BookRepository books, PricingService pricing,
                        @Value("${bookworm.cancel-window-hours}") int cancelWindowHours) {
        this.orders = orders;
        this.carts = carts;
        this.addresses = addresses;
        this.users = users;
        this.books = books;
        this.pricing = pricing;
        this.cancelWindowHours = cancelWindowHours;
    }

    @Transactional(readOnly = true)
    public PricePreview preview(User user, String coupon, Integer points) {
        return pricing.price(user, carts.findByUserIdOrderByIdAsc(user.getId()), coupon, points);
    }

    public OrderDto checkout(User user, CheckoutRequest r) {
        List<CartItem> cart = carts.findByUserIdOrderByIdAsc(user.getId());
        if (cart.isEmpty()) throw ApiException.badRequest("Cart is empty");

        User fresh = users.findById(user.getId()).orElseThrow(() -> ApiException.notFound("User"));
        Address addr = resolveAddress(fresh, r);
        PricePreview p = pricing.price(fresh, cart, r.couponCode(), r.pointsToRedeem());

        Order o = new Order();
        o.setUser(fresh);
        o.setSubtotal(p.subtotal());
        o.setTax(p.tax());
        o.setDeliveryCharge(p.deliveryCharge());
        o.setDiscount(p.discount());
        o.setPointsRedeemed(p.pointsRedeemed());
        o.setTotal(p.total());
        o.setCouponCode(r.couponCode() == null || r.couponCode().isBlank() ? null : r.couponCode().trim().toUpperCase());
        o.setShippingAddress(addr.toDisplayString());
        int maxDays = 0;
        for (CartItem c : cart) {
            OrderItem oi = new OrderItem();
            oi.setOrder(o);
            oi.setBook(c.getBook());
            oi.setQuantity(c.getQuantity());
            oi.setUnitPrice(c.getBook().getPrice());
            o.getItems().add(oi);
            c.getBook().setCopiesSold(c.getBook().getCopiesSold() + c.getQuantity());
            books.save(c.getBook());
            maxDays = Math.max(maxDays, c.getBook().getDeliveryDays());
        }
        o.setEstimatedDelivery(LocalDate.now().plusDays(maxDays));

        // Simulated gateway: always succeeds. Replace with a real gateway call for production.
        Payment pay = new Payment();
        pay.setOrder(o);
        pay.setMethod(r.paymentMethod());
        pay.setStatus(Payment.Status.SUCCESS);
        pay.setAmount(p.total());
        pay.setTransactionRef("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        o.setPayment(pay);

        // Redeem points, and earn 1 point per 10 rupees paid.
        fresh.setGiftPoints(fresh.getGiftPoints() - p.pointsRedeemed() + p.total().intValue() / 10);
        users.save(fresh);

        Order saved = orders.save(o);
        carts.deleteByUserId(user.getId());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> history(User user) {
        return orders.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OrderDto get(User user, Long id) {
        return toDto(owned(user, id));
    }

    public OrderDto cancel(User user, Long id) {
        Order o = owned(user, id);
        if (o.getStatus() == Order.Status.CANCELLED) throw ApiException.conflict("Order already cancelled");
        if (!cancellable(o)) {
            throw ApiException.badRequest("Orders can only be cancelled within " + cancelWindowHours + " hours");
        }
        o.setStatus(Order.Status.CANCELLED);
        if (o.getPayment() != null) o.getPayment().setStatus(Payment.Status.REFUNDED);
        for (OrderItem i : o.getItems()) {
            i.getBook().setCopiesSold(Math.max(0, i.getBook().getCopiesSold() - i.getQuantity()));
        }
        // Reverse points: give back redeemed, remove earned.
        User u = users.findById(user.getId()).orElseThrow();
        u.setGiftPoints(Math.max(0, u.getGiftPoints() + o.getPointsRedeemed() - o.getTotal().intValue() / 10));
        users.save(u);
        return toDto(orders.save(o));
    }

    /** "Buy it again": puts every book from a past order back into the cart. */
    public void buyAgain(User user, Long orderId) {
        Order o = owned(user, orderId);
        for (OrderItem i : o.getItems()) {
            CartItem c = carts.findByUserIdAndBookId(user.getId(), i.getBook().getId()).orElseGet(() -> {
                CartItem n = new CartItem();
                n.setUser(user);
                n.setBook(i.getBook());
                n.setQuantity(0);
                return n;
            });
            c.setQuantity(Math.min(20, c.getQuantity() + i.getQuantity()));
            carts.save(c);
        }
    }

    // ---------- addresses ----------
    @Transactional(readOnly = true)
    public List<AddressDto> addresses(User user) {
        return addresses.findByUserId(user.getId()).stream().map(AddressDto::of).toList();
    }

    public AddressDto saveAddress(User user, AddressRequest r) {
        return AddressDto.of(addresses.save(build(user, r)));
    }

    public void deleteAddress(User user, Long id) {
        Address a = addresses.findById(id).filter(x -> x.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> ApiException.notFound("Address"));
        addresses.delete(a);
    }

    // ---------- helpers ----------
    private Address resolveAddress(User user, CheckoutRequest r) {
        if (r.addressId() != null) {
            return addresses.findById(r.addressId()).filter(a -> a.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> ApiException.notFound("Address"));
        }
        if (r.address() != null) return addresses.save(build(user, r.address()));
        throw ApiException.badRequest("Provide addressId or address");
    }

    private Address build(User user, AddressRequest r) {
        Address a = new Address();
        a.setUser(user);
        a.setFirstName(r.firstName());
        a.setLastName(r.lastName());
        a.setLine1(r.line1());
        a.setEmail(r.email());
        a.setCity(r.city());
        a.setPin(r.pin());
        a.setState(r.state());
        if (r.country() != null && !r.country().isBlank()) a.setCountry(r.country());
        a.setPhone(r.phone());
        return a;
    }

    private Order owned(User user, Long id) {
        Order o = orders.findById(id).orElseThrow(() -> ApiException.notFound("Order"));
        if (!o.getUser().getId().equals(user.getId())) throw ApiException.notFound("Order");
        return o;
    }

    private boolean cancellable(Order o) {
        return o.getStatus() == Order.Status.PLACED
                && Duration.between(o.getCreatedAt(), LocalDateTime.now()).toHours() < cancelWindowHours;
    }

    private OrderDto toDto(Order o) {
        List<OrderItemDto> items = o.getItems().stream()
                .map(i -> new OrderItemDto(BookDto.of(i.getBook()), i.getQuantity(), i.getUnitPrice())).toList();
        Payment p = o.getPayment();
        return new OrderDto(o.getId(), o.getStatus().name(), items, o.getSubtotal(), o.getTax(), o.getDeliveryCharge(),
                o.getDiscount(), o.getPointsRedeemed(), o.getTotal(), o.getCouponCode(), o.getShippingAddress(),
                o.getCreatedAt(), o.getEstimatedDelivery(), p == null ? null : p.getMethod().name(),
                p == null ? null : p.getStatus().name(), p == null ? null : p.getTransactionRef(), cancellable(o));
    }
}
