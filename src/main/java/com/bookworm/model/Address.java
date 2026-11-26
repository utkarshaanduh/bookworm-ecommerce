package com.bookworm.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class Address {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private User user;
    private String firstName;
    private String lastName;
    private String line1;
    private String email;
    private String city;
    private String pin;
    private String state;
    private String country = "India";
    private String phone;

    public String toDisplayString() {
        return firstName + " " + lastName + ", " + line1 + ", " + city + " " + pin + ", " + state + ", " + country
                + " | " + phone;
    }
}
