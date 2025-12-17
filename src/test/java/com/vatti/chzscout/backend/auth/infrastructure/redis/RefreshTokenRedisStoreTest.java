package com.vatti.chzscout.backend.auth.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.vatti.chzscout.backend.common.config.EmbeddedRedisConfig;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(EmbeddedRedisConfig.class)
class RefreshTokenRedisStoreTest {

  @Autowired private RefreshTokenRedisStore refreshTokenRedisStore;

  @Autowired private StringRedisTemplate stringRedisTemplate;

  String jti = "test-jti-123";

  String refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

  Duration ttl = Duration.ofMinutes(30);

  @BeforeEach
  void setUp() {
    // 테스트 전 Redis 데이터 초기화
    stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
  }

  @Nested
  @DisplayName("save 메서드")
  class Save {

    @Test
    @DisplayName("Refresh Token을 저장하면 Redis에 TTL과 함께 저장된다")
    void success() {
      // given

      // when
      refreshTokenRedisStore.save(jti, refreshToken, ttl);

      // then
      String savedToken = stringRedisTemplate.opsForValue().get("auth:refresh:" + jti);
      assertThat(savedToken).isEqualTo(refreshToken);

      Long expireSeconds = stringRedisTemplate.getExpire("auth:refresh:" + jti);
      assertThat(expireSeconds).isGreaterThan(0).isLessThanOrEqualTo(ttl.toSeconds());
    }
  }

  @Nested
  @DisplayName("findByJti 메서드")
  class FindByJti {

    @Test
    @DisplayName("존재하는 jti로 조회시, 저장된 토큰을 반환한다")
    void findByJti() {
      // given
      refreshTokenRedisStore.save(jti, refreshToken, ttl);

      // when
      String refreshResult = refreshTokenRedisStore.findByJti(jti);

      // then
      assertThat(refreshResult).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("존재하지 않는 jti를 조회시, null을 반환한다")
    void findByJtiNull() {
      // given

      // when
      String refreshResult = refreshTokenRedisStore.findByJti(jti);

      // then
      assertThat(refreshResult).isNull();
    }
  }

  @Nested
  @DisplayName("deleteByJti 메서드")
  class DeleteByJti {

    @Test
    @DisplayName("저장된 Refresh Token을 삭제한다")
    void success() {
      // given
      String jti = "delete-test-jti";
      refreshTokenRedisStore.save(jti, "token-to-delete", Duration.ofHours(1));

      // when
      refreshTokenRedisStore.deleteByJti(jti);

      // then
      String result = refreshTokenRedisStore.findByJti(jti);
      assertThat(result).isNull();
    }
  }
}
