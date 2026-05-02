package com.ecommerce.user.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.security.JwtProperties;
import com.ecommerce.common.security.JwtUtil;
import com.ecommerce.common.security.UserPrincipal;
import com.ecommerce.user.dto.*;
import com.ecommerce.user.entity.RefreshToken;
import com.ecommerce.user.entity.Role;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.RefreshTokenRepository;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered: " + request.email(), HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .roles(Set.of(Role.ROLE_USER))
                .enabled(true)
                .build();

        userRepository.save(user);
        return generateTokenPair(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        if (!user.isEnabled()) {
            throw new BusinessException("Account is disabled", HttpStatus.FORBIDDEN);
        }

        return generateTokenPair(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BusinessException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        if (stored.isRevoked() || stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new BusinessException("Refresh token expired or revoked", HttpStatus.UNAUTHORIZED);
        }

        // Token rotasyonu: eski sil, yeni üret
        refreshTokenRepository.delete(stored);
        return generateTokenPair(stored.getUser());
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.refreshToken())
                .ifPresent(refreshTokenRepository::delete);
    }

    private TokenResponse generateTokenPair(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Enum::name)
                .toList();

        UserPrincipal principal = new UserPrincipal(user.getId(), user.getEmail(), roles);
        String accessToken = jwtUtil.generateAccessToken(principal);
        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiresAt(Instant.now().plusMillis(jwtProperties.refreshTokenExpirationMs()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return TokenResponse.of(accessToken, refreshTokenValue, jwtProperties.accessTokenExpirationMs());
    }
}
