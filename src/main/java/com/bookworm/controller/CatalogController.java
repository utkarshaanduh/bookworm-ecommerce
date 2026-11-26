package com.bookworm.controller;

import com.bookworm.config.AuthInterceptor;
import com.bookworm.dto.Dtos.*;
import com.bookworm.exception.ApiException;
import com.bookworm.model.Book;
import com.bookworm.model.Review;
import com.bookworm.model.User;
import com.bookworm.repository.BookRepository;
import com.bookworm.repository.ReviewRepository;
import com.bookworm.service.CatalogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final CatalogService catalog;
    private final BookRepository books;
    private final ReviewRepository reviews;

    public CatalogController(CatalogService catalog, BookRepository books, ReviewRepository reviews) {
        this.catalog = catalog;
        this.books = books;
        this.reviews = reviews;
    }

    @GetMapping("/home")
    public HomeResponse home(HttpServletRequest req) {
        return catalog.home(AuthInterceptor.optionalUser(req));
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return catalog.categoryNames();
    }

    @GetMapping("/brands")
    public List<String> brands() {
        return catalog.brands();
    }

    @GetMapping("/books")
    public List<BookDto> search(@RequestParam(required = false) String q,
                                @RequestParam(required = false) String category,
                                @RequestParam(required = false) String brand,
                                @RequestParam(required = false) String language,
                                @RequestParam(required = false) String format,
                                @RequestParam(required = false) BigDecimal minPrice,
                                @RequestParam(required = false) BigDecimal maxPrice,
                                @RequestParam(required = false) String sort) {
        return catalog.search(q, category, brand, language, format, minPrice, maxPrice, sort);
    }

    @GetMapping("/books/{id}")
    public BookDetailResponse detail(@PathVariable Long id, HttpServletRequest req) {
        return catalog.detail(id, AuthInterceptor.optionalUser(req));
    }

    @GetMapping("/recommendations")
    public List<BookDto> recommendations(HttpServletRequest req) {
        return catalog.recommendFor(AuthInterceptor.optionalUser(req), null, 6);
    }

    @PostMapping("/books/{id}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto review(@PathVariable Long id, @Valid @RequestBody ReviewRequest r, HttpServletRequest req) {
        User u = AuthInterceptor.requireUser(req);
        Book b = books.findById(id).orElseThrow(() -> ApiException.notFound("Book"));
        Review rv = new Review();
        rv.setBook(b);
        rv.setUser(u);
        rv.setRating(r.rating());
        rv.setComment(r.comment());
        Review saved = reviews.save(rv);
        double avg = reviews.findByBookIdOrderByCreatedAtDesc(id).stream().mapToInt(Review::getRating).average()
                .orElse(b.getRating());
        b.setRating(Math.round(avg * 10) / 10.0);
        books.save(b);
        return ReviewDto.of(saved);
    }

    // ---------- admin catalogue management ----------
    @PostMapping("/admin/books")
    @ResponseStatus(HttpStatus.CREATED)
    public BookDto create(@Valid @RequestBody BookRequest r, HttpServletRequest req) {
        requireAdmin(req);
        return catalog.create(r);
    }

    @PutMapping("/admin/books/{id}")
    public BookDto update(@PathVariable Long id, @Valid @RequestBody BookRequest r, HttpServletRequest req) {
        requireAdmin(req);
        return catalog.update(id, r);
    }

    @DeleteMapping("/admin/books/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, HttpServletRequest req) {
        requireAdmin(req);
        catalog.delete(id);
    }

    private void requireAdmin(HttpServletRequest req) {
        User u = AuthInterceptor.requireUser(req);
        if (!"ADMIN".equals(u.getRole())) throw new ApiException(HttpStatus.FORBIDDEN, "Admin role required");
    }
}
