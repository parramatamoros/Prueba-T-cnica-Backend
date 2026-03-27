package com.diary.security;

import com.diary.config.JwtProperties;
import com.diary.entity.User;
import com.diary.entity.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService")
class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    private static final String TEST_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 3_600_000L; // 1 hora

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(TEST_SECRET);
        properties.setExpirationMs(EXPIRATION_MS);

        jwtService = new JwtService(properties);

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .role(Role.USER)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("generateToken() debe retornar un token no vacío")
    void shouldGenerateNonEmptyToken() {
        String token = jwtService.generateToken(testUser);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("extractUsername() debe retornar el username del subject")
    void shouldExtractCorrectUsername() {
        String token = jwtService.generateToken(testUser);
        String extracted = jwtService.extractUsername(token);
        assertThat(extracted).isEqualTo("testuser");
    }

    @Test
    @DisplayName("extractRole() debe retornar el rol del usuario")
    void shouldExtractCorrectRole() {
        String token = jwtService.generateToken(testUser);
        String role = jwtService.extractRole(token);
        assertThat(role).isEqualTo("USER");
    }

    @Test
    @DisplayName("isTokenValid() debe retornar true para token recién generado")
    void shouldReturnTrueForValidToken() {
        String token = jwtService.generateToken(testUser);
        assertThat(jwtService.isTokenValid(token, testUser)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid() debe retornar false si el username no coincide")
    void shouldReturnFalseForDifferentUser() {
        String token = jwtService.generateToken(testUser);

        User anotherUser = User.builder()
                .id(2L)
                .username("otrousuario")
                .role(Role.USER)
                .enabled(true)
                .build();

        assertThat(jwtService.isTokenValid(token, anotherUser)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid() debe retornar false para token con firma alterada")
    void shouldReturnFalseForTamperedToken() {
        String token = jwtService.generateToken(testUser);

        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtService.isTokenValid(tamperedToken, testUser)).isFalse();
    }

    @Test
    @DisplayName("Token expirado debe ser inválido")
    void shouldReturnFalseForExpiredToken() {

        JwtProperties expiredProps = new JwtProperties();
        expiredProps.setSecret(TEST_SECRET);
        expiredProps.setExpirationMs(-1L);
        JwtService expiredJwtService = new JwtService(expiredProps);

        String expiredToken = expiredJwtService.generateToken(testUser);
        assertThat(expiredJwtService.isTokenValid(expiredToken, testUser)).isFalse();
    }

    @Test
    @DisplayName("getExpirationMs() debe retornar el valor configurado")
    void shouldReturnConfiguredExpiration() {
        assertThat(jwtService.getExpirationMs()).isEqualTo(EXPIRATION_MS);
    }

    @Test
    @DisplayName("Token de ADMIN debe contener role=ADMIN en los claims")
    void shouldIncludeAdminRoleInToken() {
        User adminUser = User.builder()
                .id(99L)
                .username("admin")
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        String token = jwtService.generateToken(adminUser);
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }
}
