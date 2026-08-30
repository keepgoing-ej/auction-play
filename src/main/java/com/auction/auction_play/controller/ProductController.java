package com.auction.auction_play.controller;

import com.auction.auction_play.dto.request.ProductCreateRequest;
import com.auction.auction_play.dto.request.ProductUpdateRequest;
import com.auction.auction_play.dto.response.PageResponse;
import com.auction.auction_play.dto.response.ProductDetailResponse;
import com.auction.auction_play.dto.response.ProductSummaryResponse;
import com.auction.auction_play.dto.response.ProductViewResponse;
import com.auction.auction_play.service.ProductService;
import com.auction.auction_play.service.ProductViewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductViewService productViewService;

    // 등록
    @PostMapping
    public ResponseEntity<ProductDetailResponse> create(
            @Valid @RequestBody ProductCreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.create(request));
    }

    // 목록
    @GetMapping
    public ResponseEntity<PageResponse<ProductSummaryResponse>> getList(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(productService.getList(pageable));
    }

    // 상세
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getDetail(id));
    }

    // 수정
    @PatchMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {

        return ResponseEntity.ok(productService.update(id, request));
    }

    // 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // P-06. 조회 인정
    @PostMapping("/{id}/view")
    public ResponseEntity<ProductViewResponse> recordView(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {

        return ResponseEntity.ok(productViewService.recordView(id, userId));
    }
}