package com.vatti.chzscout.backend.ai.application;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.vatti.chzscout.backend.ai.domain.entity.StreamEmbedding;
import com.vatti.chzscout.backend.ai.infrastructure.StreamEmbeddingRepository;
import com.vatti.chzscout.backend.stream.domain.AllFieldLiveDto;
import com.vatti.chzscout.backend.stream.fixture.AllFieldLiveDtoFixture;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StreamEmbeddingSyncServiceTest {

  @Mock private EmbeddingService embeddingService;
  @Mock private StreamEmbeddingRepository streamEmbeddingRepository;

  @InjectMocks private StreamEmbeddingSyncService streamEmbeddingSyncService;

  @Nested
  @DisplayName("syncEmbeddings 메서드 테스트")
  class SyncEmbeddings {

    @Test
    @DisplayName("변경된 채널과 종료된 채널이 모두 없으면 스킵한다")
    void skipsWhenNoChangesOrEnded() {
      // given
      List<AllFieldLiveDto> streams = List.of(AllFieldLiveDtoFixture.create(1));
      Set<String> changedIds = Set.of();
      Set<String> endedIds = Set.of();

      // when
      streamEmbeddingSyncService.syncEmbeddings(streams, changedIds, endedIds);

      // then
      verify(streamEmbeddingRepository, never()).deleteByChannelIdIn(anyList());
      verify(embeddingService, never()).createEmbeddingsBatch(anyList());
      verify(streamEmbeddingRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("변경된 채널의 임베딩을 삭제 후 재생성한다")
    void deletesAndRecreatesEmbeddingsForChangedChannels() {
      // given
      List<AllFieldLiveDto> changedStreams =
          List.of(AllFieldLiveDtoFixture.create(1), AllFieldLiveDtoFixture.create(2));
      Set<String> changedIds = Set.of("channel_1", "channel_2");
      Set<String> endedIds = Set.of();

      List<StreamEmbedding> newEmbeddings =
          List.of(
              StreamEmbedding.create("channel_1", "text1", new float[1536]),
              StreamEmbedding.create("channel_2", "text2", new float[1536]));
      given(embeddingService.createEmbeddingsBatch(changedStreams)).willReturn(newEmbeddings);

      // when
      streamEmbeddingSyncService.syncEmbeddings(changedStreams, changedIds, endedIds);

      // then
      verify(streamEmbeddingRepository).deleteByChannelIdIn(anyList());
      verify(embeddingService).createEmbeddingsBatch(changedStreams);
      verify(streamEmbeddingRepository).saveAll(newEmbeddings);
    }

    @Test
    @DisplayName("종료된 채널의 임베딩만 삭제한다")
    void deletesEmbeddingsForEndedChannels() {
      // given
      List<AllFieldLiveDto> changedStreams = List.of();
      Set<String> changedIds = Set.of();
      Set<String> endedIds = Set.of("channel_ended_1", "channel_ended_2");

      given(embeddingService.createEmbeddingsBatch(changedStreams)).willReturn(List.of());

      // when
      streamEmbeddingSyncService.syncEmbeddings(changedStreams, changedIds, endedIds);

      // then
      verify(streamEmbeddingRepository).deleteByChannelIdIn(anyList());
      verify(streamEmbeddingRepository).saveAll(List.of());
    }

    @Test
    @DisplayName("변경 + 종료 채널을 한 번에 삭제한다")
    void deletesBothChangedAndEndedChannels() {
      // given
      List<AllFieldLiveDto> changedStreams = List.of(AllFieldLiveDtoFixture.create(1));
      Set<String> changedIds = Set.of("channel_1");
      Set<String> endedIds = Set.of("channel_ended");

      List<StreamEmbedding> newEmbeddings =
          List.of(StreamEmbedding.create("channel_1", "text1", new float[1536]));
      given(embeddingService.createEmbeddingsBatch(changedStreams)).willReturn(newEmbeddings);

      // when
      streamEmbeddingSyncService.syncEmbeddings(changedStreams, changedIds, endedIds);

      // then
      // 변경(channel_1) + 종료(channel_ended) 모두 삭제 대상
      verify(streamEmbeddingRepository).deleteByChannelIdIn(anyList());
      verify(streamEmbeddingRepository).saveAll(newEmbeddings);
    }

    @Test
    @DisplayName("새로 생성된 임베딩을 저장한다")
    void savesNewlyCreatedEmbeddings() {
      // given
      List<AllFieldLiveDto> changedStreams = List.of(AllFieldLiveDtoFixture.create(1));
      Set<String> changedIds = Set.of("channel_1");
      Set<String> endedIds = Set.of();

      List<StreamEmbedding> newEmbeddings =
          List.of(StreamEmbedding.create("channel_1", "새 텍스트", new float[1536]));
      given(embeddingService.createEmbeddingsBatch(changedStreams)).willReturn(newEmbeddings);

      // when
      streamEmbeddingSyncService.syncEmbeddings(changedStreams, changedIds, endedIds);

      // then
      verify(streamEmbeddingRepository).saveAll(newEmbeddings);
    }
  }
}
