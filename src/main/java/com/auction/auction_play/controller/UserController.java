package com.auction.auction_play.controller;

import com.auction.auction_play.dto.request.UserCreateRequest;
import com.auction.auction_play.dto.response.UserDetailResponse;
import com.auction.auction_play.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 임시 회원 등록 — 인증 단계에서 /api/auth/signup으로 교체 예정
    @PostMapping
    public ResponseEntity<UserDetailResponse> create(
            @Valid @RequestBody UserCreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDetailResponse> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getDetail(id));
    }
}