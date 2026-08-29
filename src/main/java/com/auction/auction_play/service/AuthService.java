package com.auction.auction_play.service;

import com.auction.auction_play.domain.User;
import com.auction.auction_play.dto.request.LoginRequest;
import com.auction.auction_play.dto.response.LoginResponse;
import com.auction.auction_play.exception.BusinessException;
import com.auction.auction_play.exception.ErrorCode;
import com.auction.auction_play.jwt.JwtProvider;
import com.auction.auction_play.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public LoginResponse login(LoginRequest request) {

        // 1. 이메일로 사용자 조회 (없으면 = 비번 틀림과 같은 에러로 처리)
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // 2. 비밀번호 검증 — 평문(입력)과 해시(DB)를 비교
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 3. Access Token 발급 (userId를 담는다)
        String accessToken = jwtProvider.createAccessToken(user.getId());

        return new LoginResponse(accessToken);
    }
}