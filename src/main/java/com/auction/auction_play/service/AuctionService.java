package com.auction.auction_play.service;

import com.auction.auction_play.domain.Auction;
import com.auction.auction_play.domain.AuctionStatus;
import com.auction.auction_play.domain.Product;
import com.auction.auction_play.dto.request.AuctionCreateRequest;
import com.auction.auction_play.dto.response.AuctionDetailResponse;
import com.auction.auction_play.dto.response.AuctionSummaryResponse;
import com.auction.auction_play.dto.response.PageResponse;
import com.auction.auction_play.dto.response.ProductSummaryResponse;
import com.auction.auction_play.exception.BusinessException;
import com.auction.auction_play.exception.ErrorCode;
import com.auction.auction_play.repository.AuctionRepository;
import com.auction.auction_play.repository.BidRepository;
import com.auction.auction_play.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionService {

    private static final long MIN_BID_INCREMENT = 1000L;

    private final AuctionRepository auctionRepository;
    private final ProductRepository productRepository;
    private final BidRepository bidRepository;

    // A-01. 경매 등록
    @Transactional
    public AuctionDetailResponse create(AuctionCreateRequest request) {

        // 1) 상품이 실제로 있는지
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 2) 시간 교차 검증 — 어노테이션으로 못 하는 부분
        if (!request.getEndAt().isAfter(request.getStartAt())) {
            throw new BusinessException(ErrorCode.INVALID_AUCTION_TIME);
        }

        // 3) 파생값은 서버가 정한다
        Auction auction = Auction.builder()
                .product(product)
                .startPrice(request.getStartPrice())
                .currentPrice(request.getStartPrice())   // 현재가 = 시작가
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .status(AuctionStatus.SCHEDULED)         // 상태는 항상 SCHEDULED로 시작
                .build();

        return toDetailResponse(auctionRepository.save(auction), 0L);    }

    public PageResponse<AuctionSummaryResponse> getList(AuctionStatus status, Pageable pageable) {

        Page<Auction> auctions = (status == null)
                ? auctionRepository.findAllWithProduct(pageable)
                : auctionRepository.findByStatusWithProduct(status, pageable);

        // 이 페이지의 경매 ID를 모아서 입찰 수를 한 번에 조회
        List<Long> auctionIds = auctions.getContent().stream()
                .map(Auction::getId)
                .toList();

        Map<Long, Long> bidCountMap = getBidCountMap(auctionIds);

        return PageResponse.from(
                auctions.map(a -> toSummaryResponse(a, bidCountMap.getOrDefault(a.getId(), 0L)))
        );
    }

    // A-03. 경매 상세
    public AuctionDetailResponse getDetail(Long id) {
        Auction auction = auctionRepository.findByIdWithProduct(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        return toDetailResponse(auction, bidRepository.countByAuctionId(id));
    }

    // ===== 내부 공통 =====

    private Map<Long, Long> getBidCountMap(List<Long> auctionIds) {
        if (auctionIds.isEmpty()) {
            return Map.of();
        }
        return bidRepository.countByAuctionIds(auctionIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    private AuctionDetailResponse toDetailResponse(Auction auction) {
        return AuctionDetailResponse.builder()
                .id(auction.getId())
                .status(auction.getStatus())
                .startPrice(auction.getStartPrice())
                .currentPrice(auction.getCurrentPrice())
                .minBidAmount(auction.getCurrentPrice() + MIN_BID_INCREMENT)
                .startAt(auction.getStartAt())
                .endAt(auction.getEndAt())
                .createdAt(auction.getCreatedAt())
                .bidCount(bidRepository.countByAuctionId(auction.getId()))
                .product(toProductSummary(auction.getProduct()))
                .build();
    }

    private AuctionDetailResponse toDetailResponse(Auction auction, long bidCount) {
        return AuctionDetailResponse.builder()
                .id(auction.getId())
                .status(auction.getStatus())
                .startPrice(auction.getStartPrice())
                .currentPrice(auction.getCurrentPrice())
                .minBidAmount(auction.getCurrentPrice() + MIN_BID_INCREMENT)
                .startAt(auction.getStartAt())
                .endAt(auction.getEndAt())
                .createdAt(auction.getCreatedAt())
                .bidCount(bidCount)
                .product(toProductSummary(auction.getProduct()))
                .build();
    }

    private AuctionSummaryResponse toSummaryResponse(Auction auction, long bidCount) {
        return AuctionSummaryResponse.builder()
                .id(auction.getId())
                .status(auction.getStatus())
                .currentPrice(auction.getCurrentPrice())
                .endAt(auction.getEndAt())
                .bidCount(bidCount)
                .product(toProductSummary(auction.getProduct()))
                .build();
    }
    // 이게 날라간거였음
    private ProductSummaryResponse toProductSummary(Product product) {
        return ProductSummaryResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .imageUrl(product.getImageUrl())
                .itemCondition(product.getItemCondition())
                .rarity(product.getRarity())
                .estimatedValue(product.getEstimatedValue())
                .build();
    }
}