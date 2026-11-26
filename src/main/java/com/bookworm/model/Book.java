package com.bookworm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class Book {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private String author;
    @Column(length = 2000)
    private String description;
    /** Publisher acts as the brand in the catalogue. */
    private String publisher;
    /** Paperback, Hardcover or eBook. */
    private String format;
    private String language;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    private String coverUrl;
    private double rating;
    private int copiesSold;
    private int deliveryDays = 3;
    /** BESTSELLER, NEW or REGULAR - drives the landing page sections. */
    private String section = "REGULAR";
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Category category;
}
