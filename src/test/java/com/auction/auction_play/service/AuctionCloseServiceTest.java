package com.auction.auction_play.service;

import com.auction.auction_play.domain.*;
import com.auction.auction_play.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AuctionCloseServiceTest {

    @Autowired AuctionCloseService auctionCloseService;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired AuctionRepository auctionRepository;
    @Autowired BidRepository bidRepository;
    @Autowired AuctionResultRepository auctionResultRepository;

    Product product;

    @BeforeEach
    void setUp() {
        product = productRepository.save(Product.builder()
                .name("테스트 상품")
                .itemCondition("GOOD")
                .rarity("RARE")
                .estimatedValue(100000L)
                .build());
    }

    // ===================================================
    // 헬퍼
    // ===================================================

    private User 사용자를_만든다(long point) {
        return userRepository.save(User.builder()
                .email(UUID.randomUUID() + "@test.com")
                .password("TEMP")
                .nickname("tester")
                .point(point)
                .build());
    }

    private Auction 경매를_만든다(AuctionStatus status, LocalDateTime startAt, LocalDateTime endAt) {
        return auctionRepository.save(Auction.builder()
                .product(product)
                .startPrice(10000L)
                .currentPrice(10000L)
                .startAt(startAt)
                .endAt(endAt)
                .status(status)
                .build());
    }

    private void 입찰한다(Auction auction, User user, long amount) {
        bidRepository.save(Bid.builder()
                .auction(auction)
                .user(user)
                .amount(amount)
                .build());
        auction.updateCurrentPrice(amount);
    }

    // ===================================================
    // 테스트
    // ===================================================

    @Test
    @DisplayName("시작 시각이 지난 경매는 RUNNING으로 바뀐다")
    void 시작_전이() {
        Auction auction = 경매를_만든다(
                AuctionStatus.SCHEDULED,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1));

        auctionCloseService.startOne(auction.getId());

        Auction 갱신됨 = auctionRepository.findById(auction.getId()).orElseThrow();
        assertThat(갱신됨.getStatus()).isEqualTo(AuctionStatus.RUNNING);
    }

    @Test
    @DisplayName("입찰이 있으면 낙찰 처리된다")
    void 낙찰() {
        // given
        User winner = 사용자를_만든다(100000L);
        Auction auction = 경매를_만든다(
                AuctionStatus.RUNNING,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1));
        입찰한다(auction, winner, 30000L);

        // when
        auctionCloseService.closeOne(auction.getId());

        // then
        Auction 갱신된_경매 = auctionRepository.findById(auction.getId()).orElseThrow();
        assertThat(갱신된_경매.getStatus()).isEqualTo(AuctionStatus.CLOSED);

        AuctionResult result = auctionResultRepository.findAll().stream()
                .filter(r -> r.getAuction().getId().equals(auction.getId()))
                .findFirst().orElseThrow();

        assertThat(result.getWinner().getId()).isEqualTo(winner.getId());
        assertThat(result.getFinalPrice()).isEqualTo(30000L);
        assertThat(result.getEstimatedValue()).isEqualTo(100000L);
        assertThat(result.getProfit()).isEqualTo(70000L);
    }

    @Test
    @DisplayName("낙찰 시 포인트가 추가로 차감되지 않는다")
    void 낙찰_포인트_불변() {
        User winner = 사용자를_만든다(70000L);   // 이미 30,000 보류된 상태
        Auction auction = 경매를_만든다(
                AuctionStatus.RUNNING,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1));
        입찰한다(auction, winner, 30000L);

        auctionCloseService.closeOne(auction.getId());

        User 갱신된_사용자 = userRepository.findById(winner.getId()).orElseThrow();
        assertThat(갱신된_사용자.getPoint()).isEqualTo(70000L);
    }

    @Test
    @DisplayName("입찰이 없으면 유찰 처리된다")
    void 유찰() {
        Auction auction = 경매를_만든다(
                AuctionStatus.RUNNING,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1));

        auctionCloseService.closeOne(auction.getId());

        Auction 갱신된_경매 = auctionRepository.findById(auction.getId()).orElseThrow();
        assertThat(갱신된_경매.getStatus()).isEqualTo(AuctionStatus.CANCELLED);

        AuctionResult result = auctionResultRepository.findAll().stream()
                .filter(r -> r.getAuction().getId().equals(auction.getId()))
                .findFirst().orElseThrow();

        assertThat(result.getWinner()).isNull();
        assertThat(result.getFinalPrice()).isNull();
    }

    @Test
    @DisplayName("이미 종료된 경매는 다시 처리되지 않는다")
    void 중복_처리_방지() {
        User winner = 사용자를_만든다(100000L);
        Auction auction = 경매를_만든다(
                AuctionStatus.RUNNING,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1));
        입찰한다(auction, winner, 30000L);

        auctionCloseService.closeOne(auction.getId());
        auctionCloseService.closeOne(auction.getId());   // 두 번째 호출

        long count = auctionResultRepository.findAll().stream()
                .filter(r -> r.getAuction().getId().equals(auction.getId()))
                .count();

        assertThat(count).isEqualTo(1);
    }
}