package com.vatti.chzscout.backend.stream.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.vatti.chzscout.backend.ai.application.StreamEmbeddingSyncService;
import com.vatti.chzscout.backend.stream.application.StreamCacheService;
import com.vatti.chzscout.backend.stream.domain.AllFieldLiveDto;
import com.vatti.chzscout.backend.stream.domain.EnrichedStreamDto;
import com.vatti.chzscout.backend.stream.domain.event.StreamCacheRefreshedEvent;
import com.vatti.chzscout.backend.stream.domain.event.StreamNotificationTriggerEvent;
import com.vatti.chzscout.backend.stream.fixture.AllFieldLiveDtoFixture;
import com.vatti.chzscout.backend.stream.infrastructure.redis.StreamRedisStore;
import com.vatti.chzscout.backend.tag.application.usecase.TagUseCase;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class StreamCacheSchedulerTest {
  @Mock private StreamCacheService streamCacheService;
  @Mock private TagUseCase tagUseCase;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private StreamRedisStore streamRedisStore;
  @Mock private StreamEmbeddingSyncService streamEmbeddingSyncService;

  @InjectMocks private StreamCacheScheduler streamCacheScheduler;

  @Nested
  @DisplayName("onApplicationReady 메서드 테스트")
  class OnApplicationReady {

    @Test
    @DisplayName("애플리케이션 시작 시 캐시를 초기화하지만 알림 이벤트는 발행하지 않는다")
    void initializesCacheWithoutNotification() {
      // given
      List<AllFieldLiveDto> streams = List.of(AllFieldLiveDtoFixture.create(1));
      given(streamCacheService.fetchLiveStreams()).willReturn(streams);
      given(streamRedisStore.detectChanges(streams))
          .willReturn(
              new StreamRedisStore.StreamChangeResult(
                  Set.of("channel_1"), // 신규 방송
                  Set.of(),
                  Set.of()));

      // when
      streamCacheScheduler.onApplicationReady();

      // then
      verify(streamCacheService).fetchLiveStreams();
      verify(streamRedisStore).saveEnrichedStreams(anyList());
      verify(eventPublisher).publishEvent(any(StreamCacheRefreshedEvent.class));
      // 알림 이벤트는 발행되지 않음 (sendNotification=false)
      verify(eventPublisher, never()).publishEvent(any(StreamNotificationTriggerEvent.class));
    }
  }

  @Nested
  @DisplayName("refreshLiveStreamsCache 메서드 테스트")
  class RefreshLiveStreamsCache {
    @Test
    @DisplayName("정상적으로 api를 호출하고 임베딩 동기화 후 이벤트를 발행한다")
    void refreshLiveStreamsCache() {
      // given
      List<AllFieldLiveDto> streams = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        streams.add(AllFieldLiveDtoFixture.create(i));
      }
      List<String> streamChannelIds = streams.stream().map(AllFieldLiveDto::channelId).toList();
      given(streamCacheService.fetchLiveStreams()).willReturn(streams);
      given(streamRedisStore.detectChanges(streams))
          .willReturn(
              new StreamRedisStore.StreamChangeResult(
                  Set.copyOf(streamChannelIds.subList(0, 3)), // newStreams: 0,1,2
                  Set.copyOf(streamChannelIds.subList(3, 5)), // changedStreams: 3,4
                  Set.of("channel_10", "channel_11") // endedStreams: 10,11
                  ));

      // when
      streamCacheScheduler.scheduledRefresh();

      // then
      // 1. API 호출 검증
      verify(streamCacheService).fetchLiveStreams();

      // 2. 태그 DB 저장 검증
      verify(tagUseCase).extractAndSaveTag(streams);

      // 3. 임베딩 동기화 호출 검증
      verify(streamEmbeddingSyncService).syncEmbeddings(anyList(), anySet(), anySet());

      // 4. Redis 저장 검증 - 10개 방송 저장
      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<EnrichedStreamDto>> enrichedCaptor = ArgumentCaptor.forClass(List.class);
      verify(streamRedisStore).saveEnrichedStreams(enrichedCaptor.capture());

      List<EnrichedStreamDto> savedStreams = enrichedCaptor.getValue();
      assertThat(savedStreams).hasSize(10);

      // channelId 순서 검증 (0~9)
      List<String> savedChannelIds =
          savedStreams.stream().map(EnrichedStreamDto::channelId).toList();
      assertThat(savedChannelIds)
          .containsExactly(
              "channel_0",
              "channel_1",
              "channel_2",
              "channel_3",
              "channel_4",
              "channel_5",
              "channel_6",
              "channel_7",
              "channel_8",
              "channel_9");

      // 5. 이벤트 발행 검증
      verify(eventPublisher).publishEvent(any(StreamCacheRefreshedEvent.class));
    }

    @Test
    @DisplayName("API 호출 중 예외 발생 시 예외를 삼키고 후속 로직을 실행하지 않는다")
    void swallowsExceptionWhenApiCallFails() {
      // given
      given(streamCacheService.fetchLiveStreams()).willThrow(new RuntimeException("API 호출 실패"));

      // when & then - 예외가 전파되지 않음
      assertThatCode(() -> streamCacheScheduler.scheduledRefresh()).doesNotThrowAnyException();

      // tagService는 호출되지 않음
      verify(tagUseCase, never()).extractAndSaveTag(any());

      // 이벤트도 발행되지 않음
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("태그 추출 중 예외 발생 시 예외를 삼키고 이벤트를 발행하지 않는다")
    void swallowsExceptionWhenTagExtractionFails() {
      // given
      List<AllFieldLiveDto> streams = List.of(AllFieldLiveDtoFixture.create(1));
      given(streamCacheService.fetchLiveStreams()).willReturn(streams);
      willThrow(new RuntimeException("태그 추출 실패")).given(tagUseCase).extractAndSaveTag(any());

      // when & then - 예외가 전파되지 않음
      assertThatCode(() -> streamCacheScheduler.scheduledRefresh()).doesNotThrowAnyException();

      // 두 메서드 모두 호출됨
      verify(streamCacheService).fetchLiveStreams();
      verify(tagUseCase).extractAndSaveTag(streams);

      // 이벤트는 발행되지 않음
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("스트림이 비어있으면 조기 반환하고 후속 로직을 실행하지 않는다")
    void skipsProcessingWhenStreamsEmpty() {
      // given
      given(streamCacheService.fetchLiveStreams()).willReturn(List.of());

      // when
      streamCacheScheduler.scheduledRefresh();

      // then
      verify(streamCacheService).fetchLiveStreams();

      // 후속 로직 모두 스킵
      verify(tagUseCase, never()).extractAndSaveTag(any());
      verify(streamRedisStore, never()).detectChanges(any());
      verify(streamEmbeddingSyncService, never()).syncEmbeddings(anyList(), anySet(), anySet());
      verify(streamRedisStore, never()).saveEnrichedStreams(any());
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("변경된 방송이 없어도 Redis에 저장하고 이벤트를 발행한다")
    void savesToRedisEvenWhenNoChanges() {
      // given
      List<AllFieldLiveDto> streams = new ArrayList<>();
      for (int i = 0; i < 5; i++) {
        streams.add(AllFieldLiveDtoFixture.create(i));
      }
      given(streamCacheService.fetchLiveStreams()).willReturn(streams);

      // 변경 없음 - 모든 방송이 이전과 동일
      given(streamRedisStore.detectChanges(streams))
          .willReturn(
              new StreamRedisStore.StreamChangeResult(
                  Set.of(), // newStreams: 없음
                  Set.of(), // changedStreams: 없음
                  Set.of() // endedStreams: 없음
                  ));

      // when
      streamCacheScheduler.scheduledRefresh();

      // then
      // 임베딩 동기화는 빈 changedIds로 호출됨
      verify(streamEmbeddingSyncService).syncEmbeddings(anyList(), anySet(), anySet());

      // Redis 저장 검증
      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<EnrichedStreamDto>> enrichedCaptor = ArgumentCaptor.forClass(List.class);
      verify(streamRedisStore).saveEnrichedStreams(enrichedCaptor.capture());

      List<EnrichedStreamDto> savedStreams = enrichedCaptor.getValue();
      assertThat(savedStreams).hasSize(5);

      // 이벤트 발행됨
      verify(eventPublisher).publishEvent(any(StreamCacheRefreshedEvent.class));
    }

    @Test
    @DisplayName("임베딩 동기화 예외 발생 시 예외를 삼키고 이벤트를 발행하지 않는다")
    void swallowsExceptionWhenEmbeddingSyncFails() {
      // given
      List<AllFieldLiveDto> streams = List.of(AllFieldLiveDtoFixture.create(1));
      given(streamCacheService.fetchLiveStreams()).willReturn(streams);
      given(streamRedisStore.detectChanges(streams))
          .willReturn(
              new StreamRedisStore.StreamChangeResult(
                  Set.of("channel_1"), // 신규 방송
                  Set.of(),
                  Set.of()));
      willThrow(new RuntimeException("임베딩 동기화 오류"))
          .given(streamEmbeddingSyncService)
          .syncEmbeddings(anyList(), anySet(), anySet());

      // when & then - 예외가 전파되지 않음
      assertThatCode(() -> streamCacheScheduler.scheduledRefresh()).doesNotThrowAnyException();

      // 임베딩 동기화 호출 시도됨
      verify(streamEmbeddingSyncService).syncEmbeddings(anyList(), anySet(), anySet());

      // 이벤트 발행되지 않음
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Redis 저장 예외 발생 시 예외를 삼키고 이벤트를 발행하지 않는다")
    void swallowsExceptionWhenRedisSaveFails() {
      // given
      List<AllFieldLiveDto> streams = List.of(AllFieldLiveDtoFixture.create(1));
      given(streamCacheService.fetchLiveStreams()).willReturn(streams);
      given(streamRedisStore.detectChanges(streams))
          .willReturn(
              new StreamRedisStore.StreamChangeResult(Set.of("channel_1"), Set.of(), Set.of()));
      willThrow(new RuntimeException("Redis 저장 오류"))
          .given(streamRedisStore)
          .saveEnrichedStreams(any());

      // when & then - 예외가 전파되지 않음
      assertThatCode(() -> streamCacheScheduler.scheduledRefresh()).doesNotThrowAnyException();

      // Redis 저장 시도됨
      verify(streamRedisStore).saveEnrichedStreams(any());

      // 이벤트 발행되지 않음
      verify(eventPublisher, never()).publishEvent(any());
    }
  }
}
