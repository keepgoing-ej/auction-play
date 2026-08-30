package com.auction.auction_play.concurrency;

import com.auction.auction_play.domain.*;
import com.auction.auction_play.dto.request.BidCreateRequest;
import com.auction.auction_play.repository.*;
import com.auction.auction_play.service.BidService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
class BidConcurrencyTest {

    @Autowired BidService bidService;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired AuctionRepository auctionRepository;
    @Autowired ProductViewRepository productViewRepository;
    @Autowired BidRepository bidRepository;
    @Autowired PointTransactionRepository pointTransactionRepository;
    @Autowired AuctionResultRepository auctionResultRepository;

    private static final int THREAD_COUNT = 10;

    Long auctionId;
    List<Long> userIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리 — 자식 → 부모 순서로 삭제 (FK 제약)
        auctionResultRepository.deleteAll();
        bidRepository.deleteAll();
        pointTransactionRepository.deleteAll();
        productViewRepository.deleteAll();
        auctionRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        // 상품
        Product product = productRepository.save(Product.builder()
                .name("동시성 테스트 상품")
                .itemCondition("GOOD")
                .rarity("RARE")
                .estimatedValue(100000L)
                .build());

        // 경매 (지금 진행 중)
        Auction auction = auctionRepository.save(Auction.builder()
                .product(product)
                .startPrice(10000L)
                .currentPrice(10000L)
                .startAt(LocalDateTime.now().minusHours(1))
                .endAt(LocalDateTime.now().plusHours(1))
                .status(AuctionStatus.RUNNING)
                .build());
        auctionId = auction.getId();

        // 사용자 10명 + 조회 기록
        for (int i = 0; i < THREAD_COUNT; i++) {
            User user = userRepository.save(User.builder()
                    .email("user" + i + "@test.com")
                    .password("TEMP")
                    .nickname("user" + i)
                    .point(1000000L)
                    .build());
            userIds.add(user.getId());

            productViewRepository.save(ProductView.builder()
                    .user(user)
                    .product(product)
                    .build());
        }
    }

    @Test
    @DisplayName("10명이 동시에 서로 다른 금액으로 입찰하면?")
    void 동시_입찰_문제_재현() throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < THREAD_COUNT; i++) {
            Long userId = userIds.get(i);
            final int index = i;

            executor.submit(() -> {
                try {
                    BidCreateRequest request = createRequest(11000L + (index * 1000L));
                    bidService.bid(auctionId, userId, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // ===== 결과 확인 =====
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        long bidCount = bidRepository.count();
        long totalPoint = userRepository.findAll().stream()
                .mapToLong(User::getPoint).sum();

        System.out.println("포인트 총합 : " + totalPoint + " (기대: 10000000 - 최종 현재가)");
        System.out.println("성공한 입찰 : " + successCount.get() + "건");
        System.out.println("실패한 입찰 : " + failCount.get() + "건");
        System.out.println("저장된 입찰 : " + bidCount + "건");
        System.out.println("최종 현재가 : " + auction.getCurrentPrice());
        System.out.println("========================================");
        System.out.println("판정 → 포인트 총합 == 10000000 - 최종 현재가 이면 정합성 OK");
        System.out.println("========================================");
    }

    // amount만 리플렉션으로 주입 (userId는 bid 인자로 직접 전달)
    private BidCreateRequest createRequest(Long amount) {
        try {
            BidCreateRequest request = new BidCreateRequest();
            Field amountField = BidCreateRequest.class.getDeclaredField("amount");
            amountField.setAccessible(true);
            amountField.set(request, amount);
            return request;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}