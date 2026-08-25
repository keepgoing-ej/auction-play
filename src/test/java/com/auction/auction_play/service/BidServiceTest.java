package com.auction.auction_play.service;

import com.auction.auction_play.domain.*;
import com.auction.auction_play.dto.request.BidCreateRequest;
import com.auction.auction_play.exception.BusinessException;
import com.auction.auction_play.exception.ErrorCode;
import com.auction.auction_play.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class BidServiceTest {

    @Autowired BidService bidService;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired AuctionRepository auctionRepository;
    @Autowired ProductViewRepository productViewRepository;
    @Autowired BidRepository bidRepository;

    Product product;

    @BeforeEach
    void setUp() {
        product = 상품을_만든다();
    }

    // ===================================================
    // 헬퍼
    // ===================================================

    private Product 상품을_만든다() {
        return productRepository.save(Product.builder()
                .name("테스트 상품")
                .itemCondition("GOOD")
                .rarity("RARE")
                .estimatedValue(100000L)
                .build());
    }

    private User 사용자를_만든다(long point) {
        return userRepository.save(User.builder()
                .email(UUID.randomUUID() + "@test.com")
                .password("TEMP")
                .nickname("tester")
                .point(point)
                .build());
    }

    private Auction 진행중인_경매를_만든다(long currentPrice) {
        return auctionRepository.save(Auction.builder()
                .product(product)
                .startPrice(currentPrice)
                .currentPrice(currentPrice)
                .startAt(LocalDateTime.now().minusHours(1))
                .endAt(LocalDateTime.now().plusHours(1))
                .status(AuctionStatus.RUNNING)
                .build());
    }

    private void 상품을_조회한다(User user) {
        productViewRepository.save(ProductView.builder()
                .user(user)
                .product(product)
                .build());
    }

    private BidCreateRequest 입찰요청(Long userId, Long amount) {
        try {
            BidCreateRequest request = new BidCreateRequest();
            Field f1 = BidCreateRequest.class.getDeclaredField("userId");
            f1.setAccessible(true);
            f1.set(request, userId);

            Field f2 = BidCreateRequest.class.getDeclaredField("amount");
            f2.setAccessible(true);
            f2.set(request, amount);

            return request;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ===================================================
    // 테스트
    // ===================================================

    @Test
    @DisplayName("정상 입찰 시 포인트가 보류되고 현재가가 갱신된다")
    void 정상_입찰() {
        // given
        User user = 사용자를_만든다(100000L);
        상품을_조회한다(user);
        Auction auction = 진행중인_경매를_만든다(10000L);

        // when
        bidService.bid(auction.getId(), 입찰요청(user.getId(), 11000L));

        // then 수정
        User 갱신된_사용자 = userRepository.findById(user.getId()).orElseThrow();
        Auction 갱신된_경매 = auctionRepository.findById(auction.getId()).orElseThrow();

        assertThat(갱신된_사용자.getPoint()).isEqualTo(89000L);
        assertThat(갱신된_경매.getCurrentPrice()).isEqualTo(11000L);
        assertThat(bidRepository.countByAuctionId(auction.getId())).isEqualTo(1);
    }


    @Test
    @DisplayName("존재하지 않는 경매면 실패한다")
    void 없는_경매() {
        User user = 사용자를_만든다(100000L);

        assertThatThrownBy(() -> bidService.bid(999999L, 입찰요청(user.getId(), 11000L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUCTION_NOT_FOUND);
    }

    @Test
    @DisplayName("상품을 조회하지 않았으면 입찰할 수 없다")
    void 상품_미조회() {
        User user = 사용자를_만든다(100000L);
        Auction auction = 진행중인_경매를_만든다(10000L);
        // 상품을_조회한다(user) 호출 안 함

        assertThatThrownBy(() -> bidService.bid(auction.getId(), 입찰요청(user.getId(), 11000L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_NOT_VIEWED);
    }

    @Test
    @DisplayName("포인트가 부족하면 입찰할 수 없다")
    void 포인트_부족() {
        User user = 사용자를_만든다(5000L);
        상품을_조회한다(user);
        Auction auction = 진행중인_경매를_만든다(10000L);

        assertThatThrownBy(() -> bidService.bid(auction.getId(), 입찰요청(user.getId(), 11000L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_POINT);
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 실패한다")
    void 없는_사용자() {
        Auction auction = 진행중인_경매를_만든다(10000L);

        assertThatThrownBy(() -> bidService.bid(auction.getId(), 입찰요청(999999L, 11000L)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("현재가보다 낮으면 실패한다")
    void 금액_미달() {
        User user = 사용자를_만든다(100000L);
        상품을_조회한다(user);
        Auction auction = 진행중인_경매를_만든다(10000L);

        assertThatThrownBy(() -> bidService.bid(auction.getId(), 입찰요청(user.getId(), 9000L)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_BID_AMOUNT);
    }

    @Test
    @DisplayName("최소 증가액을 못 채우면 실패한다")
    void 증가액_부족() {
        User user = 사용자를_만든다(100000L);
        상품을_조회한다(user);
        Auction auction = 진행중인_경매를_만든다(10000L);

        assertThatThrownBy(() -> bidService.bid(auction.getId(), 입찰요청(user.getId(), 10500L)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_BID_AMOUNT);
    }

    @Test
    @DisplayName("취소된 경매면 실패한다")
    void 취소된_경매() {
        User user = 사용자를_만든다(100000L);
        상품을_조회한다(user);
        Auction auction = 진행중인_경매를_만든다(10000L);
        auction.cancel();

        assertThatThrownBy(() -> bidService.bid(auction.getId(), 입찰요청(user.getId(), 11000L)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUCTION_CANCELLED);
    }

    @Test
    @DisplayName("시작 전 경매면 실패한다")
    void 시작_전_경매() {
        User user = 사용자를_만든다(100000L);
        상품을_조회한다(user);

        Auction auction = auctionRepository.save(Auction.builder()
                .product(product)
                .startPrice(10000L)
                .currentPrice(10000L)
                .startAt(LocalDateTime.now().plusHours(1))
                .endAt(LocalDateTime.now().plusHours(2))
                .status(AuctionStatus.SCHEDULED)
                .build());

        assertThatThrownBy(() -> bidService.bid(auction.getId(), 입찰요청(user.getId(), 11000L)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUCTION_NOT_STARTED);
    }

    @Test
    @DisplayName("종료된 경매면 실패한다")
    void 종료된_경매() {
        User user = 사용자를_만든다(100000L);
        상품을_조회한다(user);

        Auction auction = auctionRepository.save(Auction.builder()
                .product(product)
                .startPrice(10000L)
                .currentPrice(10000L)
                .startAt(LocalDateTime.now().minusHours(2))
                .endAt(LocalDateTime.now().minusHours(1))
                .status(AuctionStatus.RUNNING)
                .build());

        assertThatThrownBy(() -> bidService.bid(auction.getId(), 입찰요청(user.getId(), 11000L)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUCTION_CLOSED);
    }


    @Test
    @DisplayName("밀리면 이전 최고 입찰자에게 환급된다")
    void 환급() {
        // given
        User a = 사용자를_만든다(100000L);
        User b = 사용자를_만든다(100000L);
        상품을_조회한다(a);
        상품을_조회한다(b);
        Auction auction = 진행중인_경매를_만든다(10000L);

        // when
        bidService.bid(auction.getId(), 입찰요청(a.getId(), 11000L));   // A 입찰
        bidService.bid(auction.getId(), 입찰요청(b.getId(), 12000L));   // B가 밀어냄

        // then
        User 갱신된A = userRepository.findById(a.getId()).orElseThrow();
        User 갱신된B = userRepository.findById(b.getId()).orElseThrow();

        assertThat(갱신된A.getPoint()).isEqualTo(100000L);   // 환급됨
        assertThat(갱신된B.getPoint()).isEqualTo(88000L);    // 보류 중
    }

    @Test
    @DisplayName("입찰에 실패하면 포인트가 변하지 않는다")
    void 실패시_롤백() {
        // given
        User user = 사용자를_만든다(100000L);
        상품을_조회한다(user);
        Auction auction = 진행중인_경매를_만든다(10000L);

        // when — 포인트보다 큰 금액으로 실패
        assertThatThrownBy(() -> bidService.bid(auction.getId(), 입찰요청(user.getId(), 500000L)))
                .isInstanceOf(BusinessException.class);

        // then
        User 갱신된_사용자 = userRepository.findById(user.getId()).orElseThrow();
        Auction 갱신된_경매 = auctionRepository.findById(auction.getId()).orElseThrow();

        assertThat(갱신된_사용자.getPoint()).isEqualTo(100000L);      // 그대로
        assertThat(갱신된_경매.getCurrentPrice()).isEqualTo(10000L);  // 그대로
        assertThat(bidRepository.countByAuctionId(auction.getId())).isEqualTo(0);
    }

    @Test
    @DisplayName("포인트 총액은 항상 보존된다")
    void 정합성() {
        // given
        User a = 사용자를_만든다(100000L);
        User b = 사용자를_만든다(100000L);
        상품을_조회한다(a);
        상품을_조회한다(b);
        Auction auction = 진행중인_경매를_만든다(10000L);

        // when
        bidService.bid(auction.getId(), 입찰요청(a.getId(), 11000L));
        bidService.bid(auction.getId(), 입찰요청(b.getId(), 12000L));
        bidService.bid(auction.getId(), 입찰요청(a.getId(), 15000L));

        // then — 보유 포인트 합 + 보류 금액 = 초기 총액
        long 보유합 = userRepository.findById(a.getId()).orElseThrow().getPoint()
                + userRepository.findById(b.getId()).orElseThrow().getPoint();
        long 보류 = auctionRepository.findById(auction.getId()).orElseThrow().getCurrentPrice();

        assertThat(보유합 + 보류).isEqualTo(200000L);
    }
}