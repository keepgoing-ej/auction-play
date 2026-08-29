package com.auction.auction_play.service;

import com.auction.auction_play.domain.User;
import com.auction.auction_play.dto.request.UserCreateRequest;
import com.auction.auction_play.dto.response.UserDetailResponse;
import com.auction.auction_play.exception.BusinessException;
import com.auction.auction_play.exception.ErrorCode;
import com.auction.auction_play.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final long INITIAL_POINT = 100000L;

    private final UserRepository userRepository;
    // 비밀번호 추가
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Transactional
    public UserDetailResponse create(UserCreateRequest request) {
        User user = User.builder()
                .email(request.getEmail())
                .password("TEMP")          // 인증 단계에서 BCrypt 해시로 교체
                .nickname(request.getNickname())
                .point(INITIAL_POINT)
                .build();

        return toDetailResponse(userRepository.save(user));
    }
    // 로그인 추가
    @Transactional
    public UserDetailResponse signup(com.auction.auction_play.dto.request.SignupRequest request) {

        // 1. 이메일 중복 검사
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 2. 비밀번호는 BCrypt로 해싱해서 저장 (평문 저장 금지)
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .point(INITIAL_POINT)
                .build();

        return toDetailResponse(userRepository.save(user));
    }

    public UserDetailResponse getDetail(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return toDetailResponse(user);
    }

    private UserDetailResponse toDetailResponse(User user) {
        return UserDetailResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .point(user.getPoint())
                .createdAt(user.getCreatedAt())
                .build();
    }
}