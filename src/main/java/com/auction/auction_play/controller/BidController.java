package com.auction.auction_play.controller;

import com.auction.auction_play.dto.request.BidCreateRequest;
import com.auction.auction_play.dto.response.BidCreateResponse;
import com.auction.auction_play.dto.response.BidSummaryResponse;
import com.auction.auction_play.dto.response.PageResponse;
import com.auction.auction_play.service.BidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auctions/{auctionId}/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    // A-04. 입찰
    @PostMapping
    public ResponseEntity<BidCreateResponse> bid(
            @PathVariable Long auctionId,
            @Valid @RequestBody BidCreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bidService.bid(auctionId, request));
    }

    // A-05. 입찰 이력
    @GetMapping
    public ResponseEntity<PageResponse<BidSummaryResponse>> getBids(
            @PathVariable Long auctionId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(bidService.getBids(auctionId, pageable));
    }
}