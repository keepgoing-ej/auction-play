package com.auction.auction_play.domain;

public enum PointTransactionType {

    CHARGE,       // 포인트 충전
    BID_HOLD,     // 입찰 시 임시 차감(보류)
    REFUND,       // 입찰 실패 시 환불
    PURCHASE,     // 낙찰 시 최종 차감
    REWARD        // 보상 지급

}
