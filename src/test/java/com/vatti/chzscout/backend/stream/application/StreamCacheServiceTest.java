package com.vatti.chzscout.backend.stream.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.vatti.chzscout.backend.stream.domain.ChzzkLiveResponse;
import com.vatti.chzscout.backend.stream.fixture.ChzzkLiveResponseFixture;
import com.vatti.chzscout.backend.stream.infrastructure.redis.StreamRedisStore;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StreamCacheServiceTest {

  @InjectMocks StreamCacheService streamCacheService;

  @Mock ChzzkApiClient chzzkApiClient;
  @Mock StreamRedisStore streamRedisStore;

  @Captor ArgumentCaptor<List> streamsCaptor;

  @Nested
  @DisplayName("refreshLiveStreams 메서드 테스트")
  class RefreshLiveStreamsTest {

    @Test
    @DisplayName("정상적으로 10페이지의 라이브 데이터를 받아 redis에 저장한다")
    void refresh200LiveStreamsTest() {
      // given
      List<ChzzkLiveResponse> pages = ChzzkLiveResponseFixture.paginatedResponses(10, 20);

      given(chzzkApiClient.getChzzkLive(null)).willReturn(pages.get(0));
      for (int i = 1; i < 10; i++) {
        String cursor = pages.get(i - 1).page().next();
        given(chzzkApiClient.getChzzkLive(cursor)).willReturn(pages.get(i));
      }

      // when
      streamCacheService.refreshLiveStreams();

      // then
      then(streamRedisStore).should().saveLiveStreams(streamsCaptor.capture());
      assertThat(streamsCaptor.getValue()).hasSize(200);
    }

    @Test
    @DisplayName("API 응답이 비어있으면 Redis에 저장하지 않는다")
    void doNotSaveWhenResponseIsEmpty() {
      // given
      given(chzzkApiClient.getChzzkLive(null)).willReturn(ChzzkLiveResponseFixture.empty());

      // when
      streamCacheService.refreshLiveStreams();

      // then
      then(streamRedisStore).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("3페이지에서 다음 커서가 없으면 3페이지까지만 저장한다")
    void stopAtPageWithoutNextCursor() {
      // given
      given(chzzkApiClient.getChzzkLive(null))
          .willReturn(ChzzkLiveResponseFixture.withNextPage(20, "cursor_1"));
      given(chzzkApiClient.getChzzkLive("cursor_1"))
          .willReturn(ChzzkLiveResponseFixture.withNextPage(20, "cursor_2"));
      given(chzzkApiClient.getChzzkLive("cursor_2"))
          .willReturn(ChzzkLiveResponseFixture.lastPage(20));

      // when
      streamCacheService.refreshLiveStreams();

      // then
      then(streamRedisStore).should().saveLiveStreams(streamsCaptor.capture());
      assertThat(streamsCaptor.getValue()).hasSize(60);
    }
  }
}
