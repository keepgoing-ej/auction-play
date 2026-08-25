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
@Table(name = "auctions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Long startPrice;

    @Column(nullable = false)
    private Long currentPrice;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionStatus status;

    @Column(nullable = false)
    private Long version = 0L;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Auction (Product product, Long startPrice, Long currentPrice, LocalDateTime startAt, LocalDateTime endAt,
                     AuctionStatus status){
        this.product = product;
        this.startPrice = startPrice;
        this.currentPrice = currentPrice;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
    }
    // 8/21 추가
    public void updateCurrentPrice(Long amount) {
        this.currentPrice = amount;
    }

    // 8/24 추가
    public void start() {
        this.status = AuctionStatus.RUNNING;
    }

    public void close() {
        this.status = AuctionStatus.CLOSED;
    }

    public void cancel() {
        this.status = AuctionStatus.CANCELLED;
    }
}
