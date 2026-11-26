package com.bookworm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class Coupon {
    @Id
    private String code;
    @Column(nullable = false)
    private BigDecimal discountAmount;
    private boolean active = true;

    public Coupon(String code, BigDecimal discountAmount) {
        this.code = code;
        this.discountAmount = discountAmount;
    }
}
