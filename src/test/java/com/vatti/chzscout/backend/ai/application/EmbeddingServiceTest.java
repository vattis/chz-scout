package com.vatti.chzscout.backend.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.vatti.chzscout.backend.ai.domain.entity.StreamEmbedding;
import com.vatti.chzscout.backend.ai.infrastructure.EmbeddingClient;
import com.vatti.chzscout.backend.stream.domain.AllFieldLiveDto;
import com.vatti.chzscout.backend.stream.fixture.AllFieldLiveDtoFixture;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

  @Mock private EmbeddingClient embeddingClient;
  @Spy private ExecutorService aiExecutor = Executors.newVirtualThreadPerTaskExecutor();

  @InjectMocks private EmbeddingService embeddingService;

  private float[] createTestEmbedding() {
    float[] embedding = new float[1536];
    for (int i = 0; i < 1536; i++) {
      embedding[i] = 0.1f + i * 0.0001f;
    }
    return embedding;
  }

  @Nested
  @DisplayName("createEmbedding 메서드 테스트")
  class CreateEmbedding {

    @Test
    @DisplayName("단일 방송의 임베딩을 생성한다")
    void createsEmbeddingForSingleStream() {
      // given
      AllFieldLiveDto stream = AllFieldLiveDtoFixture.create(1);
      float[] embedding = createTestEmbedding();
      given(embeddingClient.embed(anyString())).willReturn(embedding);

      // when
      StreamEmbedding result = embeddingService.createEmbedding(stream);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getChannelId()).isEqualTo("channel_1");
      assertThat(result.getEmbedding()).isEqualTo(embedding);
      assertThat(result.getEmbeddingText()).contains("제목:").contains("스트리머:").contains("카테고리:");
    }

    @Test
    @DisplayName("임베딩 텍스트에 제목, 스트리머, 카테고리, 태그가 포함된다")
    void embeddingTextContainsAllFields() {
      // given
      AllFieldLiveDto stream = AllFieldLiveDtoFixture.create(1, List.of("롤", "다이아"));
      float[] embedding = createTestEmbedding();
      given(embeddingClient.embed(anyString())).willReturn(embedding);

      // when
      StreamEmbedding result = embeddingService.createEmbedding(stream);

      // then
      assertThat(result.getEmbeddingText())
          .contains("테스트 방송 제목 1") // 제목
          .contains("스트리머1") // 스트리머
          .contains("리그 오브 레전드") // 카테고리
          .contains("롤")
          .contains("다이아"); // 태그
    }
  }

  @Nested
  @DisplayName("createEmbeddingsBatch 메서드 테스트")
  class CreateEmbeddingsBatch {

    @Test
    @DisplayName("빈 리스트면 빈 리스트를 반환한다")
    void returnsEmptyListForEmptyInput() {
      // when
      List<StreamEmbedding> result = embeddingService.createEmbeddingsBatch(List.of());

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("여러 방송의 임베딩을 배치로 생성한다")
    void createsEmbeddingsForMultipleStreams() {
      // given
      List<AllFieldLiveDto> streams =
          List.of(
              AllFieldLiveDtoFixture.create(1),
              AllFieldLiveDtoFixture.create(2),
              AllFieldLiveDtoFixture.create(3));

      List<float[]> embeddings =
          List.of(createTestEmbedding(), createTestEmbedding(), createTestEmbedding());
      given(embeddingClient.embedBatch(anyList())).willReturn(embeddings);

      // when
      List<StreamEmbedding> result = embeddingService.createEmbeddingsBatch(streams);

      // then
      assertThat(result).hasSize(3);
      assertThat(result.get(0).getChannelId()).isEqualTo("channel_1");
      assertThat(result.get(1).getChannelId()).isEqualTo("channel_2");
      assertThat(result.get(2).getChannelId()).isEqualTo("channel_3");
    }

    @Test
    @DisplayName("청크 처리 중 예외 발생 시 빈 리스트를 반환한다")
    void returnsEmptyListWhenChunkProcessingFails() {
      // given
      List<AllFieldLiveDto> streams = List.of(AllFieldLiveDtoFixture.create(1));
      given(embeddingClient.embedBatch(anyList())).willThrow(new RuntimeException("API 오류"));

      // when
      List<StreamEmbedding> result = embeddingService.createEmbeddingsBatch(streams);

      // then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("createQueryEmbedding 메서드 테스트")
  class CreateQueryEmbedding {

    @Test
    @DisplayName("쿼리의 임베딩을 생성한다")
    void createsEmbeddingForQuery() {
      // given
      float[] expectedEmbedding = createTestEmbedding();
      given(embeddingClient.embed("롤 방송 추천")).willReturn(expectedEmbedding);

      // when
      float[] result = embeddingService.createQueryEmbedding("롤 방송 추천");

      // then
      assertThat(result).isEqualTo(expectedEmbedding);
      verify(embeddingClient).embed("롤 방송 추천");
    }
  }
}
