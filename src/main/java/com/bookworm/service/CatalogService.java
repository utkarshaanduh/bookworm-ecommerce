package com.bookworm.service;

import com.bookworm.dto.Dtos.*;
import com.bookworm.exception.ApiException;
import com.bookworm.model.*;
import com.bookworm.repository.*;
import jakarta.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final BookRepository books;
    private final CategoryRepository categories;
    private final OrderRepository orders;
    private final ReviewRepository reviews;

    public CatalogService(BookRepository books, CategoryRepository categories, OrderRepository orders,
                          ReviewRepository reviews) {
        this.books = books;
        this.categories = categories;
        this.orders = orders;
        this.reviews = reviews;
    }

    public List<String> categoryNames() {
        return categories.findAll(Sort.by("name")).stream().map(Category::getName).toList();
    }

    public List<String> brands() {
        return books.findPublishers();
    }

    public List<BookDto> search(String q, String category, String brand, String language, String format,
                                java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice, String sort) {
        Specification<Book> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (hasText(q)) {
                String like = "%" + q.toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("title")), like), cb.like(cb.lower(root.get("author")), like)));
            }
            if (hasText(category)) p.add(cb.equal(cb.lower(root.get("category").get("name")), category.toLowerCase()));
            if (hasText(brand)) p.add(cb.equal(cb.lower(root.get("publisher")), brand.toLowerCase()));
            if (hasText(language)) p.add(cb.equal(cb.lower(root.get("language")), language.toLowerCase()));
            if (hasText(format)) p.add(cb.equal(cb.lower(root.get("format")), format.toLowerCase()));
            if (minPrice != null) p.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            if (maxPrice != null) p.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            return cb.and(p.toArray(new Predicate[0]));
        };
        Sort s = switch (sort == null ? "" : sort) {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "rating" -> Sort.by("rating").descending();
            default -> Sort.by("copiesSold").descending(); // relevance
        };
        return books.findAll(spec, s).stream().map(BookDto::of).toList();
    }

    /** Landing page. Recommendations are driven by the user's order history when logged in. */
    public HomeResponse home(User user) {
        List<BookDto> recommended = recommendFor(user, null, 6);
        List<BookDto> best = books.findBySectionOrderByCopiesSoldDesc("BESTSELLER").stream().map(BookDto::of).toList();
        List<BookDto> fresh = books.findBySectionOrderByCopiesSoldDesc("NEW").stream().map(BookDto::of).toList();
        return new HomeResponse(recommended, best, fresh, categoryNames());
    }

    public BookDetailResponse detail(Long id, User user) {
        Book b = books.findById(id).orElseThrow(() -> ApiException.notFound("Book"));
        List<BookDto> related = books.findTop4ByCategoryIdAndIdNotOrderByRatingDesc(b.getCategory().getId(), id)
                .stream().map(BookDto::of).toList();
        List<ReviewDto> rv = reviews.findByBookIdOrderByCreatedAtDesc(id).stream().map(ReviewDto::of).toList();
        return new BookDetailResponse(BookDto.of(b), related, rv);
    }

    /** Recommend books from categories the user has bought, excluding books already purchased. */
    public List<BookDto> recommendFor(User user, Long excludeBookId, int limit) {
        Set<Long> owned = new HashSet<>();
        Map<Long, Integer> categoryWeight = new HashMap<>();
        if (user != null) {
            for (Order o : orders.findByUserIdOrderByCreatedAtDesc(user.getId())) {
                if (o.getStatus() == Order.Status.CANCELLED) continue;
                for (OrderItem i : o.getItems()) {
                    owned.add(i.getBook().getId());
                    categoryWeight.merge(i.getBook().getCategory().getId(), i.getQuantity(), Integer::sum);
                }
            }
        }
        List<Book> pool;
        if (categoryWeight.isEmpty()) {
            pool = books.findTop6ByOrderByCopiesSoldDesc(); // cold start: popular
        } else {
            pool = books.findByCategoryIdIn(categoryWeight.keySet());
            pool.sort(Comparator
                    .comparingInt((Book b) -> -categoryWeight.getOrDefault(b.getCategory().getId(), 0))
                    .thenComparing(Comparator.comparingDouble(Book::getRating).reversed()));
        }
        return pool.stream()
                .filter(b -> !owned.contains(b.getId()))
                .filter(b -> excludeBookId == null || !b.getId().equals(excludeBookId))
                .limit(limit).map(BookDto::of).collect(Collectors.toList());
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    // ---------- admin ----------
    @Transactional
    public BookDto create(BookRequest r) {
        Book b = new Book();
        apply(b, r);
        return BookDto.of(books.save(b));
    }

    @Transactional
    public BookDto update(Long id, BookRequest r) {
        Book b = books.findById(id).orElseThrow(() -> ApiException.notFound("Book"));
        apply(b, r);
        return BookDto.of(books.save(b));
    }

    @Transactional
    public void delete(Long id) {
        if (!books.existsById(id)) throw ApiException.notFound("Book");
        try {
            books.deleteById(id);
            books.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw ApiException.conflict("Book is referenced by orders, carts or reviews and cannot be deleted");
        }
    }

    private void apply(Book b, BookRequest r) {
        b.setTitle(r.title());
        b.setAuthor(r.author());
        b.setDescription(r.description());
        b.setPublisher(r.publisher());
        b.setFormat(r.format());
        b.setLanguage(r.language());
        b.setPrice(r.price());
        b.setCoverUrl(r.coverUrl());
        if (r.section() != null) b.setSection(r.section());
        Category c = categories.findByNameIgnoreCase(r.category()).orElseGet(() -> categories.save(new Category(r.category())));
        b.setCategory(c);
    }
}
