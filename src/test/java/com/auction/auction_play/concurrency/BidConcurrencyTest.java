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

    private static final int THREAD_COUNT = 10;

    Long auctionId;
    List<Long> userIds = new ArrayList<>();
    @Autowired
    private AuctionResultRepository auctionResultRepository;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        auctionResultRepository.deleteAll();   // ← 추가 자식 → 부모 순서로 삭제 (FK 제약)
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
    @DisplayName("10명이 동시에 같은 금액으로 입찰하면?")
    void 동시_입찰_문제_재현() throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT); // 10으로 시작
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        AtomicInteger successCount = new AtomicInteger(); // 여러일꾼 같이 세는 계수기
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < THREAD_COUNT; i++) {
            Long userId = userIds.get(i);
            final int index = i; // 인덱스 추가

            executor.submit(() -> {   // ← 여기. 10번 반복해서 일 던짐
                try { // 인덱스 추가
                    BidCreateRequest request = createRequest(userId, 11000L + (index * 1000L));
                    bidService.bid(auctionId, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown(); // ← 일 하나 끝날 때마다 10→9→8...
                }
            });
        }

        latch.await();  // ← 0 될 때까지 여기서 멈춰 있음
        executor.shutdown();

        // ===== 결과 확인 =====
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        long bidCount = bidRepository.count();
        long totalPoint = userRepository.findAll().stream()
                .mapToLong(User::getPoint).sum();
        System.out.println("포인트 총합 : " + totalPoint + " (기대: 10000000 - 최고입찰액)");  // 추가
        System.out.println("성공한 입찰 : " + successCount.get() + "건");
        System.out.println("실패한 입찰 : " + failCount.get() + "건");
        System.out.println("저장된 입찰 : " + bidCount + "건");
        System.out.println("최종 현재가 : " + auction.getCurrentPrice());
        System.out.println("========================================");
        System.out.println("기대값 → 성공 1건 / 실패 9건 / 현재가 11,000");
        System.out.println("========================================");
    }

    // DTO에 Setter가 없으므로 리플렉션으로 값 주입 (테스트 전용)
    private BidCreateRequest createRequest(Long userId, Long amount) {
        try {
            BidCreateRequest request = new BidCreateRequest();
            Field userIdField = BidCreateRequest.class.getDeclaredField("userId");
            userIdField.setAccessible(true);
            userIdField.set(request, userId);

            Field amountField = BidCreateRequest.class.getDeclaredField("amount");
            amountField.setAccessible(true);
            amountField.set(request, amount);

            return request;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}