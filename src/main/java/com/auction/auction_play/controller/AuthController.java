package com.auction.auction_play.controller;

import com.auction.auction_play.dto.request.LoginRequest;
import com.auction.auction_play.dto.request.SignupRequest;
import com.auction.auction_play.dto.response.LoginResponse;
import com.auction.auction_play.dto.response.UserDetailResponse;
import com.auction.auction_play.service.AuthService;
import com.auction.auction_play.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    // U-01. 회원가입
    @PostMapping("/signup")
    public ResponseEntity<UserDetailResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.signup(request));
    }

    // U-02. 로그인
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}