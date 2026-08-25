package com.auction.auction_play.repository;

import com.auction.auction_play.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
