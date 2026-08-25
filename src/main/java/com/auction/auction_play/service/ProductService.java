package com.auction.auction_play.service;

import com.auction.auction_play.domain.Product;
import com.auction.auction_play.dto.request.ProductCreateRequest;
import com.auction.auction_play.dto.request.ProductUpdateRequest;
import com.auction.auction_play.dto.response.PageResponse;
import com.auction.auction_play.dto.response.ProductDetailResponse;
import com.auction.auction_play.dto.response.ProductSummaryResponse;
import com.auction.auction_play.exception.BusinessException;
import com.auction.auction_play.exception.ErrorCode;
import com.auction.auction_play.repository.AuctionRepository;
import com.auction.auction_play.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final AuctionRepository auctionRepository;

    // P-01. 등록
    @Transactional
    public ProductDetailResponse create(ProductCreateRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .itemCondition(request.getItemCondition())
                .rarity(request.getRarity())
                .estimatedValue(request.getEstimatedValue())
                .build();

        return toDetailResponse(productRepository.save(product));
    }

    // P-02. 목록
    public PageResponse<ProductSummaryResponse> getList(Pageable pageable) {
        Page<ProductSummaryResponse> page = productRepository.findAll(pageable)
                .map(this::toSummaryResponse);
        return PageResponse.from(page);
    }

    // P-03. 상세
    public ProductDetailResponse getDetail(Long id) {
        return toDetailResponse(findProductById(id));
    }

    // P-04. 수정
    @Transactional
    public ProductDetailResponse update(Long id, ProductUpdateRequest request) {
        Product product = findProductById(id);

        product.update(
                request.getName(),
                request.getDescription(),
                request.getImageUrl(),
                request.getItemCondition(),
                request.getRarity(),
                request.getEstimatedValue()
        );

        return toDetailResponse(product);
    }

    // P-05. 삭제
    @Transactional
    public void delete(Long id) {
        Product product = findProductById(id);

        if (auctionRepository.existsByProductId(id)) {
            throw new BusinessException(ErrorCode.PRODUCT_IN_AUCTION);
        }

        productRepository.delete(product);
    }

    // ===== 내부 공통 =====

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private ProductDetailResponse toDetailResponse(Product product) {
        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .itemCondition(product.getItemCondition())
                .rarity(product.getRarity())
                .estimatedValue(product.getEstimatedValue())
                .createdAt(product.getCreatedAt())
                .build();
    }

    private ProductSummaryResponse toSummaryResponse(Product product) {
        return ProductSummaryResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .imageUrl(product.getImageUrl())
                .itemCondition(product.getItemCondition())
                .rarity(product.getRarity())
                .estimatedValue(product.getEstimatedValue())
                .build();
    }
    // 8/23 추가
    private final ProductViewService productViewService;
}