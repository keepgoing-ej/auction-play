package com.auction.auction_play.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 500)
    private String imageUrl;

    @Column(name = "item_condition", nullable = false, length = 20)
    private String itemCondition;

    @Column(nullable = false, length = 20)
    private String rarity;

    @Column(nullable = false)
    private Long estimatedValue;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Product (String name, String description, String imageUrl, String itemCondition, String rarity, Long estimatedValue) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.itemCondition = itemCondition;
        this.rarity = rarity;
        this.estimatedValue = estimatedValue;
    }

    public void update(String name, String description, String imageUrl, String itemCondition,
                       String rarity, Long estimatedValue) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (imageUrl != null) this.imageUrl = imageUrl;
        if (itemCondition != null) this.itemCondition = itemCondition;
        if (rarity != null) this.rarity = rarity;
        if (estimatedValue != null) this.estimatedValue =estimatedValue;
    }

}
