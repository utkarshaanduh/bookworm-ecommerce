package com.bookworm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor
public class Order {
    public enum Status { PLACED, CANCELLED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private User user;
    @Enumerated(EnumType.STRING)
    private Status status = Status.PLACED;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal deliveryCharge;
    private BigDecimal discount = BigDecimal.ZERO;
    private int pointsRedeemed;
    private BigDecimal total;
    private String couponCode;
    @Column(length = 500)
    private String shippingAddress;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDate estimatedDelivery;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Payment payment;
}
