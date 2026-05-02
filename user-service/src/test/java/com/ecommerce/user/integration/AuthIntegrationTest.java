package com.ecommerce.user.integration;

import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.RefreshTokenRequest;
import com.ecommerce.user.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void givenValidCredentials_whenRegisterAndLogin_thenReturnTokenPair() {
        RegisterRequest registerRequest = new RegisterRequest(
                "test@example.com", "password123", "John", "Doe", null);

        ResponseEntity<Map> registerResponse =
                restTemplate.postForEntity("/api/auth/register", registerRequest, Map.class);

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");

        ResponseEntity<Map> loginResponse =
                restTemplate.postForEntity("/api/auth/login", loginRequest, Map.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) loginResponse.getBody().get("data");
        assertThat(data).containsKeys("accessToken", "refreshToken");
        assertThat((String) data.get("accessToken")).isNotBlank();
        assertThat((String) data.get("refreshToken")).isNotBlank();
    }

    @Test
    void givenDuplicateEmail_whenRegister_thenReturnBadRequest() {
        RegisterRequest request = new RegisterRequest(
                "duplicate@example.com", "password123", "Jane", "Doe", null);

        restTemplate.postForEntity("/api/auth/register", request, Map.class);

        ResponseEntity<Map> secondResponse =
                restTemplate.postForEntity("/api/auth/register", request, Map.class);

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void givenWrongPassword_whenLogin_thenReturnUnauthorized() {
        RegisterRequest registerRequest = new RegisterRequest(
                "wrongpass@example.com", "correctpass", "Alice", "Smith", null);
        restTemplate.postForEntity("/api/auth/register", registerRequest, Map.class);

        LoginRequest loginRequest = new LoginRequest("wrongpass@example.com", "wrongpass");
        ResponseEntity<Map> loginResponse =
                restTemplate.postForEntity("/api/auth/login", loginRequest, Map.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void givenValidRefreshToken_whenRefresh_thenReturnNewTokenPair() {
        RegisterRequest registerRequest = new RegisterRequest(
                "refresh@example.com", "password123", "Bob", "Brown", null);
        restTemplate.postForEntity("/api/auth/register", registerRequest, Map.class);

        LoginRequest loginRequest = new LoginRequest("refresh@example.com", "password123");
        ResponseEntity<Map> loginResponse =
                restTemplate.postForEntity("/api/auth/login", loginRequest, Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> loginData = (Map<String, Object>) loginResponse.getBody().get("data");
        String refreshToken = (String) loginData.get("refreshToken");

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);
        ResponseEntity<Map> refreshResponse =
                restTemplate.postForEntity("/api/auth/refresh", refreshRequest, Map.class);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> refreshData = (Map<String, Object>) refreshResponse.getBody().get("data");
        assertThat(refreshData).containsKeys("accessToken", "refreshToken");
        assertThat((String) refreshData.get("refreshToken")).isNotEqualTo(refreshToken);
    }

    @Test
    void givenInvalidRefreshToken_whenRefresh_thenReturnBadRequest() {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token-uuid");
        ResponseEntity<Map> response =
                restTemplate.postForEntity("/api/auth/refresh", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
