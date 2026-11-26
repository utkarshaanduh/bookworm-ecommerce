package com.bookworm.dto;

import com.bookworm.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    // ---------- requests ----------
    public record RegisterRequest(@NotBlank String name, @Email @NotBlank String email,
                                  @NotBlank @Size(min = 6) String password) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record CartAddRequest(@NotNull Long bookId, @Min(1) @Max(20) Integer quantity) {}

    public record CartUpdateRequest(@Min(0) @Max(20) int quantity) {}

    public record AddressRequest(@NotBlank String firstName, @NotBlank String lastName, @NotBlank String line1,
                                 @Email String email, @NotBlank String city, @NotBlank String pin,
                                 @NotBlank String state, String country, @NotBlank String phone) {}

    public record ReviewRequest(@Min(1) @Max(5) int rating, @Size(max = 100) String comment) {}

    public record CheckoutRequest(Long addressId, @Valid AddressRequest address, String couponCode,
                                  @Min(0) Integer pointsToRedeem, @NotNull Payment.Method paymentMethod) {}

    public record BookRequest(@NotBlank String title, @NotBlank String author, String description, String publisher,
                              String format, String language, @NotNull @Positive BigDecimal price, String coverUrl,
                              @NotBlank String category, String section) {}

    // ---------- responses ----------
    public record AuthResponse(String token, UserDto user) {}

    public record UserDto(Long id, String name, String email, String role, int giftPoints) {
        public static UserDto of(User u) {
            return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getGiftPoints());
        }
    }

    public record BookDto(Long id, String title, String author, String description, String publisher, String format,
                          String language, BigDecimal price, String coverUrl, double rating, int copiesSold,
                          String category, String section, LocalDate estimatedDelivery) {
        public static BookDto of(Book b) {
            return new BookDto(b.getId(), b.getTitle(), b.getAuthor(), b.getDescription(), b.getPublisher(),
                    b.getFormat(), b.getLanguage(), b.getPrice(), b.getCoverUrl(), b.getRating(), b.getCopiesSold(),
                    b.getCategory().getName(), b.getSection(), LocalDate.now().plusDays(b.getDeliveryDays()));
        }
    }

    public record HomeResponse(List<BookDto> recommended, List<BookDto> bestsellers, List<BookDto> newLaunches,
                               List<String> categories) {}

    public record BookDetailResponse(BookDto book, List<BookDto> relatedReads, List<ReviewDto> reviews) {}

    public record ReviewDto(Long id, String userName, int rating, String comment, LocalDateTime createdAt) {
        public static ReviewDto of(Review r) {
            return new ReviewDto(r.getId(), r.getUser().getName(), r.getRating(), r.getComment(), r.getCreatedAt());
        }
    }

    public record CartLine(Long itemId, BookDto book, int quantity, BigDecimal lineTotal) {}

    public record CartResponse(List<CartLine> items, int itemCount, BigDecimal subtotal, BigDecimal tax,
                               BigDecimal deliveryCharge, BigDecimal total) {}

    public record AddressDto(Long id, String firstName, String lastName, String line1, String email, String city,
                             String pin, String state, String country, String phone) {
        public static AddressDto of(Address a) {
            return new AddressDto(a.getId(), a.getFirstName(), a.getLastName(), a.getLine1(), a.getEmail(),
                    a.getCity(), a.getPin(), a.getState(), a.getCountry(), a.getPhone());
        }
    }

    public record OrderItemDto(BookDto book, int quantity, BigDecimal unitPrice) {}

    public record OrderDto(Long id, String status, List<OrderItemDto> items, BigDecimal subtotal, BigDecimal tax,
                           BigDecimal deliveryCharge, BigDecimal discount, int pointsRedeemed, BigDecimal total,
                           String couponCode, String shippingAddress, LocalDateTime createdAt,
                           LocalDate estimatedDelivery, String paymentMethod, String paymentStatus,
                           String transactionRef, boolean cancellable) {}

    public record PricePreview(BigDecimal subtotal, BigDecimal tax, BigDecimal deliveryCharge, BigDecimal discount,
                               int pointsRedeemed, BigDecimal total) {}
}
