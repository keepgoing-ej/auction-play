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
@Table(name = "auction_results",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_auction_results_auction",
                columnNames = "auction_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuctionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    private Long finalPrice;

    @Column(nullable = false)
    private Long estimatedValue;

    private Long profit;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private AuctionResult (Auction auction, User winner, Long finalPrice, Long estimatedValue, Long profit){
        this.auction = auction;
        this.winner = winner;
        this.finalPrice = finalPrice;
        this.estimatedValue = estimatedValue;
        this.profit = profit;
    }
}
