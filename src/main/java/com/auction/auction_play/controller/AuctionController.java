package com.auction.auction_play.controller;

import com.auction.auction_play.domain.AuctionStatus;
import com.auction.auction_play.dto.request.AuctionCreateRequest;
import com.auction.auction_play.dto.response.AuctionDetailResponse;
import com.auction.auction_play.dto.response.AuctionSummaryResponse;
import com.auction.auction_play.dto.response.PageResponse;
import com.auction.auction_play.service.AuctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    // A-01. 등록
    @PostMapping
    public ResponseEntity<AuctionDetailResponse> create(
            @Valid @RequestBody AuctionCreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(auctionService.create(request));
    }

    // A-02. 목록
    @GetMapping
    public ResponseEntity<PageResponse<AuctionSummaryResponse>> getList(
            @RequestParam(required = false) AuctionStatus status,
            @PageableDefault(size = 20, sort = "endAt", direction = Sort.Direction.ASC)
            Pageable pageable) {

        return ResponseEntity.ok(auctionService.getList(status, pageable));
    }

    // A-03. 상세
    @GetMapping("/{id}")
    public ResponseEntity<AuctionDetailResponse> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.getDetail(id));
    }
}