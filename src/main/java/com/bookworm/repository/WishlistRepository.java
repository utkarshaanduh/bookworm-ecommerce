package com.bookworm.repository;

import com.bookworm.model.WishlistItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByUserIdOrderByIdDesc(Long userId);

    Optional<WishlistItem> findByUserIdAndBookId(Long userId, Long bookId);
}
