package com.auction.auction_play.repository;

import com.auction.auction_play.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
