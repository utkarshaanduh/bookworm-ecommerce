package com.bookworm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Book book;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private User user;
    private int rating;
    @Column(length = 100)
    private String comment;
    private LocalDateTime createdAt = LocalDateTime.now();
}
