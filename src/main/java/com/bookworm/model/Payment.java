package com.bookworm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class Payment {
    public enum Method { CREDIT_CARD, DEBIT_CARD, UPI, WALLET }
    public enum Status { SUCCESS, FAILED, REFUNDED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false)
    private Order order;
    @Enumerated(EnumType.STRING)
    private Method method;
    @Enumerated(EnumType.STRING)
    private Status status;
    private BigDecimal amount;
    private String transactionRef;
    private LocalDateTime createdAt = LocalDateTime.now();
}
