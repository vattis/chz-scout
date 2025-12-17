package com.vatti.chzscout.backend.stream.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.vatti.chzscout.backend.common.config.EmbeddedRedisConfig;
import com.vatti.chzscout.backend.stream.domain.AllFieldLiveDto;
import com.vatti.chzscout.backend.stream.fixture.AllFieldLiveDtoFixture;
import java.time.Duration;
import java.util.List;
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
class StreamRedisStoreTest {

  @Autowired private StreamRedisStore streamRedisStore;

  @Autowired private StringRedisTemplate stringRedisTemplate;

  private List<AllFieldLiveDto> testStreams;

  @BeforeEach
  void setUp() {
    stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    testStreams = List.of(AllFieldLiveDtoFixture.create(1), AllFieldLiveDtoFixture.create(2));
  }

  @Nested
  @DisplayName("saveLiveStreams 메서드")
  class SaveLiveStreams {

    @Test
    @DisplayName("생방송 목록을 저장하면 TTL 5분과 함께 Redis에 저장된다")
    void success() {
      // when
      streamRedisStore.saveLiveStreams(testStreams);

      // then
      List<AllFieldLiveDto> result = streamRedisStore.findLiveStreams();
      assertThat(result).hasSize(testStreams.size());

      Long expireSeconds = stringRedisTemplate.getExpire("stream:lives");
      assertThat(expireSeconds)
          .isGreaterThan(0)
          .isLessThanOrEqualTo(Duration.ofMinutes(5).toSeconds());
    }
  }

  @Nested
  @DisplayName("findLiveStreams 메서드")
  class FindLiveStreams {

    @Test
    @DisplayName("저장된 데이터가 있으면 목록을 반환한다")
    void whenExists_returnsList() {
      // given
      streamRedisStore.saveLiveStreams(testStreams);

      // when
      List<AllFieldLiveDto> result = streamRedisStore.findLiveStreams();

      // then
      assertThat(result).isNotNull().hasSize(2);
      assertThat(result.get(0).liveId()).isEqualTo(testStreams.get(0).liveId());
    }

    @Test
    @DisplayName("저장된 데이터가 없으면 null을 반환한다")
    void whenNotExists_returnsNull() {
      // given - @BeforeEach에서 flushAll 완료

      // when
      List<AllFieldLiveDto> result = streamRedisStore.findLiveStreams();

      // then
      assertThat(result).isNull();
    }
  }
}
