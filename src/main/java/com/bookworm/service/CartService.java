package com.bookworm.service;

import com.bookworm.dto.Dtos.*;
import com.bookworm.exception.ApiException;
import com.bookworm.model.*;
import com.bookworm.repository.*;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CartService {
    private final CartItemRepository carts;
    private final BookRepository books;
    private final WishlistRepository wishlists;
    private final PricingService pricing;

    public CartService(CartItemRepository carts, BookRepository books, WishlistRepository wishlists,
                       PricingService pricing) {
        this.carts = carts;
        this.books = books;
        this.wishlists = wishlists;
        this.pricing = pricing;
    }

    @Transactional(readOnly = true)
    public CartResponse get(User user) {
        List<CartItem> items = carts.findByUserIdOrderByIdAsc(user.getId());
        PricePreview p = pricing.price(user, items, null, null);
        List<CartLine> lines = items.stream().map(i -> new CartLine(i.getId(), BookDto.of(i.getBook()), i.getQuantity(),
                i.getBook().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))).toList();
        int count = items.stream().mapToInt(CartItem::getQuantity).sum();
        return new CartResponse(lines, count, p.subtotal(), p.tax(), p.deliveryCharge(), p.total());
    }

    public CartResponse add(User user, CartAddRequest r) {
        Book b = books.findById(r.bookId()).orElseThrow(() -> ApiException.notFound("Book"));
        int qty = r.quantity() == null ? 1 : r.quantity();
        CartItem item = carts.findByUserIdAndBookId(user.getId(), b.getId()).orElseGet(() -> {
            CartItem c = new CartItem();
            c.setUser(user);
            c.setBook(b);
            c.setQuantity(0);
            return c;
        });
        item.setQuantity(Math.min(20, item.getQuantity() + qty));
        carts.save(item);
        return get(user);
    }

    public CartResponse updateQuantity(User user, Long itemId, int quantity) {
        CartItem item = owned(user, itemId);
        if (quantity == 0) carts.delete(item);
        else {
            item.setQuantity(quantity);
            carts.save(item);
        }
        return get(user);
    }

    public CartResponse remove(User user, Long itemId) {
        carts.delete(owned(user, itemId));
        return get(user);
    }

    public void clear(User user) {
        carts.deleteByUserId(user.getId());
    }

    private CartItem owned(User user, Long itemId) {
        CartItem item = carts.findById(itemId).orElseThrow(() -> ApiException.notFound("Cart item"));
        if (!item.getUser().getId().equals(user.getId())) throw ApiException.notFound("Cart item");
        return item;
    }

    // ---------- wishlist ----------
    @Transactional(readOnly = true)
    public List<BookDto> wishlist(User user) {
        return wishlists.findByUserIdOrderByIdDesc(user.getId()).stream().map(w -> BookDto.of(w.getBook())).toList();
    }

    public List<BookDto> addToWishlist(User user, Long bookId) {
        Book b = books.findById(bookId).orElseThrow(() -> ApiException.notFound("Book"));
        if (wishlists.findByUserIdAndBookId(user.getId(), bookId).isEmpty()) {
            WishlistItem w = new WishlistItem();
            w.setUser(user);
            w.setBook(b);
            wishlists.save(w);
        }
        return wishlist(user);
    }

    public List<BookDto> removeFromWishlist(User user, Long bookId) {
        wishlists.findByUserIdAndBookId(user.getId(), bookId).ifPresent(wishlists::delete);
        return wishlist(user);
    }
}
