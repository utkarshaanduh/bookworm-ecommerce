package com.bookworm.config;

import com.bookworm.model.*;
import com.bookworm.repository.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** Loads demo categories, books, coupons and users on first start (empty database only). */
@Component
public class DataSeeder implements CommandLineRunner {
    private final CategoryRepository categories;
    private final BookRepository books;
    private final CouponRepository coupons;
    private final UserRepository users;
    private final boolean enabled;

    public DataSeeder(CategoryRepository categories, BookRepository books, CouponRepository coupons,
                      UserRepository users, @Value("${bookworm.seed-data}") boolean enabled) {
        this.categories = categories;
        this.books = books;
        this.coupons = coupons;
        this.users = users;
        this.enabled = enabled;
    }

    @Override
    public void run(String... args) {
        if (!enabled || books.count() > 0) return;

        Map<String, Category> cat = new HashMap<>();
        for (String n : new String[]{"Romance", "Mystery", "Science Fiction", "Fantasy", "Historical", "Biography",
                "Self-help", "Memoir", "Travel", "Cooking", "Children's", "Young Adult", "Comics & Graphic Novels",
                "Poetry", "Drama", "Science", "Philosophy", "Religion", "Language Learning", "Non-Fiction", "Fiction"}) {
            cat.put(n, categories.save(new Category(n)));
        }

        // title, author, publisher, format, price, category, section, rating, sold, description
        Object[][] data = {
                {"The Art of Focus", "Arjun Patel", "ABC Publishers", "Paperback", 399, "Self-help", "REGULAR", 4.5, 210,
                        "Practical guide to mastering focus and boosting productivity every day."},
                {"The Art of Learning", "Raj Patel", "ABC Publishers", "Paperback", 259, "Self-help", "REGULAR", 4.2, 150,
                        "Master the mindset and methods for effective lifelong learning."},
                {"The Path to Success", "James Wright", "Summit Press", "Paperback", 359, "Self-help", "REGULAR", 4.0, 180,
                        "A practical guide to achieving goals with clarity and confidence."},
                {"The Midnight Hour", "James Adams", "Nightfall Books", "Paperback", 299, "Mystery", "BESTSELLER", 4.6, 900,
                        "Haunting tale of a man's journey and the shadows of a forgotten past."},
                {"Beneath the Stars", "Jessica Martin", "Heartline", "Hardcover", 499, "Romance", "BESTSELLER", 4.4, 820,
                        "A heartwarming tale, where two souls discover who you need."},
                {"The Final Frontier", "Laura Mitchell", "Orbit House", "Paperback", 359, "Science Fiction", "BESTSELLER", 4.7, 760,
                        "A mission to space secrets to change humanity forever."},
                {"Joy of Minimalism", "Daniel Reed", "ABC Publishers", "Paperback", 149, "Self-help", "NEW", 4.8, 145,
                        "Declutter your life to uncover peace, clarity, and joy."},
                {"The Vanishing House", "Clara Nelson", "Nightfall Books", "eBook", 99, "Mystery", "NEW", 4.1, 60,
                        "A chilling mystery unfolds within a house that disappears."},
                {"The Lost Kitten", "Emily Parker", "Little Leaf", "Hardcover", 339, "Children's", "NEW", 4.3, 75,
                        "A heartwarming tale of courage, friendship, and feline adventure."},
                {"Dragons of the Ember Coast", "Maya Sorensen", "Orbit House", "Paperback", 449, "Fantasy", "REGULAR", 4.5, 300,
                        "A smuggler and a dragon rider fight to keep a coastal kingdom alive."},
                {"A Brief History of Tomorrow", "Dr. Anil Rao", "Summit Press", "Hardcover", 599, "Science", "REGULAR", 4.6, 410,
                        "How emerging science will reshape the next hundred years."},
                {"Letters from Lisbon", "Sofia Alves", "Heartline", "Paperback", 279, "Historical", "REGULAR", 4.0, 130,
                        "Love and espionage in 1940s Portugal."},
                {"Spice Route Kitchen", "Meera Iyer", "Little Leaf", "Hardcover", 649, "Cooking", "REGULAR", 4.7, 220,
                        "Home recipes gathered along the old spice routes."},
                {"Walking the Himalaya", "Karan Bisht", "Summit Press", "Paperback", 329, "Travel", "REGULAR", 4.2, 110,
                        "A trekker's diary across 2,000 km of mountains."},
        };
        for (Object[] d : data) {
            Book b = new Book();
            b.setTitle((String) d[0]);
            b.setAuthor((String) d[1]);
            b.setPublisher((String) d[2]);
            b.setFormat((String) d[3]);
            b.setPrice(BigDecimal.valueOf((int) d[4]));
            b.setCategory(cat.get((String) d[5]));
            b.setSection((String) d[6]);
            b.setRating((double) d[7]);
            b.setCopiesSold((int) d[8]);
            b.setDescription((String) d[9]);
            b.setLanguage("English");
            b.setDeliveryDays("eBook".equals(d[3]) ? 0 : 3);
            books.save(b);
        }

        coupons.save(new Coupon("WELCOME100", BigDecimal.valueOf(100)));
        coupons.save(new Coupon("BOOKS50", BigDecimal.valueOf(50)));

        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        User admin = new User();
        admin.setName("Admin");
        admin.setEmail("admin@bookworm.com");
        admin.setPasswordHash(enc.encode("admin123"));
        admin.setRole("ADMIN");
        users.save(admin);

        User demo = new User();
        demo.setName("Demo Reader");
        demo.setEmail("demo@bookworm.com");
        demo.setPasswordHash(enc.encode("demo1234"));
        demo.setGiftPoints(250);
        users.save(demo);
    }
}
