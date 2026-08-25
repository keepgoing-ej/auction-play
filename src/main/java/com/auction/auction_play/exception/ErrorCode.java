package com.auction.auction_play.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    //공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // 상품
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    PRODUCT_IN_AUCTION(HttpStatus.CONFLICT, "경매 이력이 있는 상품은 삭제할 수 없습니다."),

    // 경매
    AUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다."),
    AUCTION_NOT_RUNNING(HttpStatus.CONFLICT, "진행 중인 경매가 아닙니다."),
    // 추가
    INVALID_AUCTION_TIME(HttpStatus.BAD_REQUEST, "종료 시각은 시작 시각보다 뒤여야 합니다."),
    AUCTION_CLOSED(HttpStatus.CONFLICT, "종료된 경매입니다."),
    // 8/21 추가
    AUCTION_CANCELLED(HttpStatus.CONFLICT, "취소된 경매입니다."),
    AUCTION_NOT_STARTED(HttpStatus.CONFLICT, "아직 시작되지 않은 경매입니다."),

    //입찰
    INVALID_BID_AMOUNT(HttpStatus.BAD_REQUEST, "입찰 금액이 올바르지 않습니다."),
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "포인트가 부족합니다."),
    PRODUCT_NOT_VIEWED(HttpStatus.FORBIDDEN, "상품을 먼저 확인해야 입찰할 수 있습니다."),

    //회원
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
