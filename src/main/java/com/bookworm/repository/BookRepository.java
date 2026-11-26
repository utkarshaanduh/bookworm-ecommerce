package com.bookworm.repository;

import com.bookworm.model.Book;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {
    List<Book> findBySectionOrderByCopiesSoldDesc(String section);

    List<Book> findTop6ByOrderByCopiesSoldDesc();

    List<Book> findByCategoryIdIn(Collection<Long> categoryIds);

    List<Book> findTop4ByCategoryIdAndIdNotOrderByRatingDesc(Long categoryId, Long id);

    @Query("select distinct b.publisher from Book b where b.publisher is not null order by b.publisher")
    List<String> findPublishers();
}
