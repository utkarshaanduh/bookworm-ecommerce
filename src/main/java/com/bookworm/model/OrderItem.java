package com.bookworm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Order order;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Book book;
    private int quantity;
    private BigDecimal unitPrice;
}
