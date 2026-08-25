package com.auction.auction_play.domain;

public enum AuctionStatus {

    SCHEDULED,    // 시작 전
    RUNNING,      // 진행 중
    CLOSED,       // 종료(낙찰)
    CANCELLED     // 취소(유찰)

}
