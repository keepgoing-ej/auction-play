package com.auction.auction_play.scheduler;

import com.auction.auction_play.service.AuctionCloseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionCloseScheduler {

    private final AuctionCloseService auctionCloseService;

    @Scheduled(fixedDelay = 10000)          // 이전 작업이 끝난 뒤 10초
    public void run() {
        LocalDateTime now = LocalDateTime.now();

        // 1) 시작 처리
        List<Long> startTargets = auctionCloseService.findStartTargetIds(now);
        for (Long id : startTargets) {
            try {
                auctionCloseService.startOne(id);
            } catch (Exception e) {
                log.error("경매 시작 실패 — id={}", id, e);
            }
        }

        // 2) 종료 처리
        List<Long> closeTargets = auctionCloseService.findCloseTargetIds(now);
        for (Long id : closeTargets) {
            try {
                auctionCloseService.closeOne(id);
            } catch (Exception e) {
                log.error("경매 종료 실패 — id={}", id, e);
            }
        }
    }
}