package com.authutil.authmaster.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String hashToken;

    private Instant expiryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private boolean revoked;

    // getters & setters
}
