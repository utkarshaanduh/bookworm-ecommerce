package com.bookworm.controller;

import com.bookworm.config.AuthInterceptor;
import com.bookworm.dto.Dtos.*;
import com.bookworm.service.CartService;
import com.bookworm.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderService orders;
    private final CartService cart;

    public OrderController(OrderService orders, CartService cart) {
        this.orders = orders;
        this.cart = cart;
    }

    @GetMapping("/checkout/preview")
    public PricePreview preview(@RequestParam(required = false) String coupon,
                                @RequestParam(required = false) Integer points, HttpServletRequest req) {
        return orders.preview(AuthInterceptor.requireUser(req), coupon, points);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto checkout(@Valid @RequestBody CheckoutRequest r, HttpServletRequest req) {
        return orders.checkout(AuthInterceptor.requireUser(req), r);
    }

    @GetMapping("/orders")
    public List<OrderDto> history(HttpServletRequest req) {
        return orders.history(AuthInterceptor.requireUser(req));
    }

    @GetMapping("/orders/{id}")
    public OrderDto get(@PathVariable Long id, HttpServletRequest req) {
        return orders.get(AuthInterceptor.requireUser(req), id);
    }

    @PostMapping("/orders/{id}/cancel")
    public OrderDto cancel(@PathVariable Long id, HttpServletRequest req) {
        return orders.cancel(AuthInterceptor.requireUser(req), id);
    }

    /** Buy In Again: re-adds a past order's books to the cart and returns the cart. */
    @PostMapping("/orders/{id}/buy-again")
    public CartResponse buyAgain(@PathVariable Long id, HttpServletRequest req) {
        var user = AuthInterceptor.requireUser(req);
        orders.buyAgain(user, id);
        return cart.get(user);
    }

    @GetMapping("/addresses")
    public List<AddressDto> addresses(HttpServletRequest req) {
        return orders.addresses(AuthInterceptor.requireUser(req));
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressDto saveAddress(@Valid @RequestBody AddressRequest r, HttpServletRequest req) {
        return orders.saveAddress(AuthInterceptor.requireUser(req), r);
    }

    @DeleteMapping("/addresses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(@PathVariable Long id, HttpServletRequest req) {
        orders.deleteAddress(AuthInterceptor.requireUser(req), id);
    }
}
