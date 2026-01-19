package com.vatti.chzscout.backend.stream.infrastructure.scheduler;

import com.vatti.chzscout.backend.ai.application.StreamEmbeddingSyncService;
import com.vatti.chzscout.backend.stream.application.StreamCacheService;
import com.vatti.chzscout.backend.stream.domain.AllFieldLiveDto;
import com.vatti.chzscout.backend.stream.domain.EnrichedStreamDto;
import com.vatti.chzscout.backend.stream.domain.event.StreamCacheRefreshedEvent;
import com.vatti.chzscout.backend.stream.domain.event.StreamNotificationTriggerEvent;
import com.vatti.chzscout.backend.stream.infrastructure.redis.StreamRedisStore;
import com.vatti.chzscout.backend.tag.application.usecase.TagUseCase;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 생방송 캐시를 주기적으로 갱신하는 스케줄러. */
@Component
@Profile("!test")
@Slf4j
@RequiredArgsConstructor
public class StreamCacheScheduler {

  private final StreamCacheService streamCacheService;
  private final TagUseCase tagUseCase;
  private final ApplicationEventPublisher eventPublisher;
  private final StreamRedisStore streamRedisStore;
  private final StreamEmbeddingSyncService streamEmbeddingSyncService;

  /** 애플리케이션 시작 시 즉시 캐시 초기화. 알림은 발송하지 않음. */
  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    log.info("Application ready, initializing live streams cache");
    refreshLiveStreamsCache(false);
  }

  /** 10분마다 치지직 API를 호출하여 생방송 목록 캐시 갱신. */
  @Scheduled(fixedRate = 600_000, initialDelay = 600_000)
  public void scheduledRefresh() {
    refreshLiveStreamsCache(true);
  }

  /**
   * 생방송 목록 캐시를 갱신합니다.
   *
   * @param sendNotification true면 알림 이벤트 발행, false면 캐시 갱신만 수행
   */
  private void refreshLiveStreamsCache(boolean sendNotification) {
    log.info("Starting scheduled live streams cache refresh");
    try {
      // 1. 치지직 API에서 생방송 목록 가져오기
      List<AllFieldLiveDto> streams = streamCacheService.fetchLiveStreams();
      log.info("Fetched {} live streams from API", streams.size());

      if (streams.isEmpty()) {
        log.warn("No streams fetched, skipping cache refresh");
        return;
      }

      // 2. 태그 DB 저장 (자동완성용)
      tagUseCase.extractAndSaveTag(streams);

      // 3. 변경 감지
      StreamRedisStore.StreamChangeResult streamChangeResult =
          streamRedisStore.detectChanges(streams);
      Set<String> changedIds = streamChangeResult.getAllChangedIds();
      Set<String> endedChannelIds = streamChangeResult.endedStreams();
      List<AllFieldLiveDto> changedStreams =
          streams.stream().filter(stream -> changedIds.contains(stream.channelId())).toList();
      streamEmbeddingSyncService.syncEmbeddings(changedStreams, changedIds, endedChannelIds);
      // [기존 AI 태그 추출 로직 - 벡터 임베딩으로 대체]
      // Map<String, EnrichedStreamDto> existingMap = getExistingEnrichedMap();
      // Map<String, EnrichedStreamDto> newEnrichedMap = extractAiTagsForChanged(streams,
      // changedIds);
      // List<EnrichedStreamDto> finalEnriched =
      //     buildFinalEnrichedList(streams, changedIds, existingMap, newEnrichedMap);

      // 4. Redis 저장 (AI 태그 없이 원본 데이터로 저장)
      List<EnrichedStreamDto> enrichedStreams =
          streams.stream().map(EnrichedStreamDto::fromWithoutAi).toList();
      streamRedisStore.saveEnrichedStreams(enrichedStreams);
      log.info("Redis에 {}개 방송 저장 완료", enrichedStreams.size());

      eventPublisher.publishEvent(new StreamCacheRefreshedEvent());

      if (sendNotification && !changedIds.isEmpty()) {
        eventPublisher.publishEvent(new StreamNotificationTriggerEvent(changedIds));
        log.info("알림 이벤트 발행 - {}개 변경된 방송", changedIds.size());
      }
    } catch (Exception e) {
      log.error("Failed to refresh live streams cache", e);
    }
  }
}
