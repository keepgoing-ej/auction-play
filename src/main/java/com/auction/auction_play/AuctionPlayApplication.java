package com.auction.auction_play;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling // 8/24추가
@SpringBootApplication
public class AuctionPlayApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuctionPlayApplication.class, args);
	}

}
