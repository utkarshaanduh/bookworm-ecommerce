package com.bookworm.controller;

import com.bookworm.config.AuthInterceptor;
import com.bookworm.dto.Dtos.*;
import com.bookworm.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CartController {
    private final CartService cart;

    public CartController(CartService cart) {
        this.cart = cart;
    }

    @GetMapping("/cart")
    public CartResponse get(HttpServletRequest req) {
        return cart.get(AuthInterceptor.requireUser(req));
    }

    @PostMapping("/cart/items")
    public CartResponse add(@Valid @RequestBody CartAddRequest r, HttpServletRequest req) {
        return cart.add(AuthInterceptor.requireUser(req), r);
    }

    @PutMapping("/cart/items/{itemId}")
    public CartResponse update(@PathVariable Long itemId, @Valid @RequestBody CartUpdateRequest r,
                               HttpServletRequest req) {
        return cart.updateQuantity(AuthInterceptor.requireUser(req), itemId, r.quantity());
    }

    @DeleteMapping("/cart/items/{itemId}")
    public CartResponse remove(@PathVariable Long itemId, HttpServletRequest req) {
        return cart.remove(AuthInterceptor.requireUser(req), itemId);
    }

    @GetMapping("/wishlist")
    public List<BookDto> wishlist(HttpServletRequest req) {
        return cart.wishlist(AuthInterceptor.requireUser(req));
    }

    @PostMapping("/wishlist/{bookId}")
    public List<BookDto> addWish(@PathVariable Long bookId, HttpServletRequest req) {
        return cart.addToWishlist(AuthInterceptor.requireUser(req), bookId);
    }

    @DeleteMapping("/wishlist/{bookId}")
    public List<BookDto> removeWish(@PathVariable Long bookId, HttpServletRequest req) {
        return cart.removeFromWishlist(AuthInterceptor.requireUser(req), bookId);
    }
}
