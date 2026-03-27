package com.diary.service;

import com.diary.dto.request.LoginRequest;
import com.diary.dto.request.RegisterRequest;
import com.diary.dto.response.AuthResponse;
import com.diary.entity.User;
import com.diary.entity.enums.Role;
import com.diary.exception.UserAlreadyExistsException;
import com.diary.repository.UserRepository;
import com.diary.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;


    private RegisterRequest validRegisterRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setUsername("juanito");
        validRegisterRequest.setEmail("juanito@mail.com");
        validRegisterRequest.setPassword("Password1");

        savedUser = User.builder()
                .id(1L)
                .username("juanito")
                .email("juanito@mail.com")
                .password("$2a$12$hashed")
                .role(Role.USER)
                .build();
    }

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("Debe registrar un usuario nuevo y retornar token JWT")
        void shouldRegisterNewUser() {

            given(userRepository.existsByUsername("juanito")).willReturn(false);
            given(userRepository.existsByEmail("juanito@mail.com")).willReturn(false);
            given(passwordEncoder.encode("Password1")).willReturn("$2a$12$hashed");
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(jwtService.generateToken(savedUser)).willReturn("token.jwt.value");
            given(jwtService.getExpirationMs()).willReturn(86400000L);

            AuthResponse response = authService.register(validRegisterRequest);

            assertThat(response.getToken()).isEqualTo("token.jwt.value");
            assertThat(response.getUsername()).isEqualTo("juanito");
            assertThat(response.getRole()).isEqualTo("USER");
            assertThat(response.getTokenType()).isEqualTo("Bearer");

            then(passwordEncoder).should().encode("Password1");
            then(userRepository).should().save(any(User.class));
        }

        @Test
        @DisplayName("Debe lanzar UserAlreadyExistsException si el username ya existe")
        void shouldThrowWhenUsernameExists() {

            given(userRepository.existsByUsername("juanito")).willReturn(true);

            assertThatThrownBy(() -> authService.register(validRegisterRequest))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("juanito");

            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar UserAlreadyExistsException si el email ya existe")
        void shouldThrowWhenEmailExists() {

            given(userRepository.existsByUsername("juanito")).willReturn(false);
            given(userRepository.existsByEmail("juanito@mail.com")).willReturn(true);

            assertThatThrownBy(() -> authService.register(validRegisterRequest))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("juanito@mail.com");

            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("El usuario registrado siempre debe tener rol USER")
        void shouldAlwaysAssignUserRole() {

            given(userRepository.existsByUsername(any())).willReturn(false);
            given(userRepository.existsByEmail(any())).willReturn(false);
            given(passwordEncoder.encode(any())).willReturn("hashed");
            given(userRepository.save(any(User.class))).willAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(1L);
                return u;
            });
            given(jwtService.generateToken(any())).willReturn("tok");
            given(jwtService.getExpirationMs()).willReturn(3600000L);

            AuthResponse response = authService.register(validRegisterRequest);

            assertThat(response.getRole()).isEqualTo("USER");
        }
    }

    @Nested
    @DisplayName("login()")
    class LoginTests {

        private LoginRequest loginRequest;

        @BeforeEach
        void setUp() {
            loginRequest = new LoginRequest();
            loginRequest.setUsername("juanito");
            loginRequest.setPassword("Password1");
        }

        @Test
        @DisplayName("Debe retornar token JWT con credenciales correctas")
        void shouldReturnTokenOnValidCredentials() {

            given(authenticationManager.authenticate(any())).willReturn(
                    new UsernamePasswordAuthenticationToken("juanito", "Password1"));
            given(userRepository.findByUsername("juanito")).willReturn(Optional.of(savedUser));
            given(jwtService.generateToken(savedUser)).willReturn("token.jwt.value");
            given(jwtService.getExpirationMs()).willReturn(86400000L);

            AuthResponse response = authService.login(loginRequest);

            assertThat(response.getToken()).isEqualTo("token.jwt.value");
            assertThat(response.getUsername()).isEqualTo("juanito");
        }

        @Test
        @DisplayName("Debe lanzar BadCredentialsException con credenciales incorrectas")
        void shouldThrowOnInvalidCredentials() {

            given(authenticationManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("Credenciales inválidas"));

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class);

            then(userRepository).should(never()).findByUsername(any());
            then(jwtService).should(never()).generateToken(any());
        }
    }
}
