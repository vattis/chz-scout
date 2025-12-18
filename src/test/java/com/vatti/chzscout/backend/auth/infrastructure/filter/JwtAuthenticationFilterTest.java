package com.vatti.chzscout.backend.auth.infrastructure.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.vatti.chzscout.backend.auth.application.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock private JwtTokenProvider jwtTokenProvider;
  @Mock private UserDetailsService userDetailsService;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;

  @InjectMocks private JwtAuthenticationFilter jwtAuthenticationFilter;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Nested
  @DisplayName("doFilterInternal 메서드 테스트")
  class DoFilterInternal {

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증 없이 다음 필터로 진행한다")
    void noAuthorizationHeader() throws Exception {
      // given
      given(request.getHeader("Authorization")).willReturn(null);

      // when
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // then
      verify(filterChain).doFilter(request, response);
      verify(jwtTokenProvider, never()).validateToken(any());
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Authorization 헤더가 Bearer로 시작하지 않으면 인증 없이 다음 필터로 진행한다")
    void nonBearerAuthorizationHeader() throws Exception {
      // given
      given(request.getHeader("Authorization")).willReturn("Basic some-credentials");

      // when
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // then
      verify(filterChain).doFilter(request, response);
      verify(jwtTokenProvider, never()).validateToken(any());
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("토큰이 유효하지 않으면 인증 없이 다음 필터로 진행한다")
    void invalidToken() throws Exception {
      // given
      String invalidToken = "invalid.jwt.token";
      given(request.getHeader("Authorization")).willReturn("Bearer " + invalidToken);
      given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false);

      // when
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // then
      verify(filterChain).doFilter(request, response);
      verify(jwtTokenProvider).validateToken(invalidToken);
      verify(jwtTokenProvider, never()).getUuid(any());
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("유효한 토큰이면 SecurityContext에 인증 정보를 설정하고 다음 필터로 진행한다")
    void validToken() throws Exception {
      // given
      String validToken = "valid.jwt.token";
      String uuid = "user-uuid-123";
      UserDetails userDetails = new User(uuid, "", Collections.singletonList(() -> "ROLE_USER"));

      given(request.getHeader("Authorization")).willReturn("Bearer " + validToken);
      given(jwtTokenProvider.validateToken(validToken)).willReturn(true);
      given(jwtTokenProvider.getUuid(validToken)).willReturn(uuid);
      given(userDetailsService.loadUserByUsername(uuid)).willReturn(userDetails);

      // when
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

      // then
      verify(filterChain).doFilter(request, response);
      verify(jwtTokenProvider).validateToken(validToken);
      verify(jwtTokenProvider).getUuid(validToken);
      verify(userDetailsService).loadUserByUsername(uuid);

      // SecurityContext에 인증 정보가 설정되었는지 확인
      var authentication = SecurityContextHolder.getContext().getAuthentication();
      assertThat(authentication).isNotNull();
      assertThat(authentication.getPrincipal()).isEqualTo(userDetails);
      assertThat(authentication.getAuthorities()).hasSize(1);
    }
  }
}
