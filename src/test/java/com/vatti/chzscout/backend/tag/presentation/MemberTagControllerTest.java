package com.vatti.chzscout.backend.tag.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;

import com.vatti.chzscout.backend.common.response.ApiResponse;
import com.vatti.chzscout.backend.tag.application.usecase.MemberTagUseCase;
import com.vatti.chzscout.backend.tag.domain.dto.MemberTagListResponse;
import com.vatti.chzscout.backend.tag.domain.dto.MemberTagRequest;
import com.vatti.chzscout.backend.tag.domain.dto.MemberTagResponse;
import com.vatti.chzscout.backend.tag.domain.entity.TagType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberTagControllerTest {

  @InjectMocks private MemberTagController memberTagController;

  @Mock private MemberTagUseCase memberTagUseCase;

  private final String memberUuid = "test-member-uuid";

  @Nested
  @DisplayName("getMemberTags 메서드")
  class GetMemberTags {

    @Test
    @DisplayName("멤버의 태그 목록을 성공적으로 조회한다")
    void getMemberTagsSuccess() {
      // given
      List<MemberTagResponse> customTags =
          List.of(
              MemberTagResponse.builder()
                  .memberUuid(memberUuid)
                  .tagName("롤 잘하는 방송")
                  .tagType(TagType.CUSTOM)
                  .build(),
              MemberTagResponse.builder()
                  .memberUuid(memberUuid)
                  .tagName("힐링 방송")
                  .tagType(TagType.CUSTOM)
                  .build());

      List<MemberTagResponse> categoryTags =
          List.of(
              MemberTagResponse.builder()
                  .memberUuid(memberUuid)
                  .tagName("League of Legends")
                  .tagType(TagType.CATEGORY)
                  .build());

      MemberTagListResponse expectedResponse = MemberTagListResponse.of(customTags, categoryTags);

      given(memberTagUseCase.getMemberTags(memberUuid)).willReturn(expectedResponse);

      // when
      ApiResponse<MemberTagListResponse> response = memberTagController.getMemberTags(memberUuid);

      // then
      assertThat(response.isSuccess()).isTrue();
      assertThat(response.getData().customTags()).hasSize(2);
      assertThat(response.getData().categoryTags()).hasSize(1);
      assertThat(response.getData().customTags())
          .extracting("tagName")
          .containsExactlyInAnyOrder("롤 잘하는 방송", "힐링 방송");

      then(memberTagUseCase).should().getMemberTags(memberUuid);
    }

    @Test
    @DisplayName("태그가 없는 멤버는 빈 리스트를 반환한다")
    void getMemberTagsEmpty() {
      // given
      MemberTagListResponse expectedResponse = MemberTagListResponse.of(List.of(), List.of());

      given(memberTagUseCase.getMemberTags(memberUuid)).willReturn(expectedResponse);

      // when
      ApiResponse<MemberTagListResponse> response = memberTagController.getMemberTags(memberUuid);

      // then
      assertThat(response.isSuccess()).isTrue();
      assertThat(response.getData().customTags()).isEmpty();
      assertThat(response.getData().categoryTags()).isEmpty();

      then(memberTagUseCase).should().getMemberTags(memberUuid);
    }
  }

  @Nested
  @DisplayName("setMemberTags 메서드")
  class SetMemberTags {

    @Test
    @DisplayName("커스텀 태그를 성공적으로 설정한다")
    void setCustomTagsSuccess() {
      // given
      List<String> tagNames = List.of("롤 잘하는 방송", "힐링 방송");
      MemberTagRequest request = MemberTagRequest.of(tagNames, TagType.CUSTOM);

      willDoNothing().given(memberTagUseCase).setMemberTags(memberUuid, request);

      // when
      ApiResponse<Void> response = memberTagController.setMemberTags(memberUuid, request);

      // then
      assertThat(response.isSuccess()).isTrue();
      assertThat(response.getData()).isNull();

      then(memberTagUseCase).should().setMemberTags(memberUuid, request);
    }

    @Test
    @DisplayName("카테고리 태그를 성공적으로 설정한다")
    void setCategoryTagsSuccess() {
      // given
      List<String> tagNames = List.of("League of Legends", "Just Chatting");
      MemberTagRequest request = MemberTagRequest.of(tagNames, TagType.CATEGORY);

      willDoNothing().given(memberTagUseCase).setMemberTags(memberUuid, request);

      // when
      ApiResponse<Void> response = memberTagController.setMemberTags(memberUuid, request);

      // then
      assertThat(response.isSuccess()).isTrue();

      then(memberTagUseCase).should().setMemberTags(memberUuid, request);
    }

    @Test
    @DisplayName("빈 태그 리스트로 설정하면 기존 태그를 삭제한다")
    void setEmptyTagsSuccess() {
      // given
      MemberTagRequest request = MemberTagRequest.of(List.of(), TagType.CUSTOM);

      willDoNothing().given(memberTagUseCase).setMemberTags(memberUuid, request);

      // when
      ApiResponse<Void> response = memberTagController.setMemberTags(memberUuid, request);

      // then
      assertThat(response.isSuccess()).isTrue();

      then(memberTagUseCase).should().setMemberTags(memberUuid, request);
    }
  }
}
