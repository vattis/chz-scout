package com.vatti.chzscout.backend.tag.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.vatti.chzscout.backend.common.exception.BusinessException;
import com.vatti.chzscout.backend.member.domain.entity.Member;
import com.vatti.chzscout.backend.member.exception.MemberErrorCode;
import com.vatti.chzscout.backend.member.infrastructure.MemberRepository;
import com.vatti.chzscout.backend.tag.domain.dto.MemberTagListResponse;
import com.vatti.chzscout.backend.tag.domain.dto.MemberTagRequest;
import com.vatti.chzscout.backend.tag.domain.entity.MemberTag;
import com.vatti.chzscout.backend.tag.domain.entity.Tag;
import com.vatti.chzscout.backend.tag.domain.entity.TagType;
import com.vatti.chzscout.backend.tag.infrastructure.MemberTagRepository;
import com.vatti.chzscout.backend.tag.infrastructure.TagRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberTagServiceTest {
  @InjectMocks private MemberTagService memberTagService;
  @Mock private MemberTagRepository memberTagRepository;
  @Mock private MemberRepository memberRepository;
  @Mock private TagRepository tagRepository;

  private final String memberUuid = "member-uuid";

  // 테스트용 Member
  private Member testMember;

  // 태그 픽스처
  private Tag customTag1;
  private Tag customTag2;
  private Tag categoryTag1;
  private Tag categoryTag2;

  // MemberTag 리스트 픽스처
  private List<MemberTag> customOnlyMemberTags;
  private List<MemberTag> categoryOnlyMemberTags;
  private List<MemberTag> mixedMemberTags;

  @BeforeEach
  void setUp() {
    // Member 생성
    testMember = Member.create("discord-123", "테스트유저", "test@example.com");

    // Custom 태그 생성
    customTag1 = Tag.createCustom("롤 잘하는 방송");
    customTag2 = Tag.createCustom("힐링 방송");

    // Category 태그 생성
    categoryTag1 = Tag.createCategory("League of Legends", 1500L);
    categoryTag2 = Tag.createCategory("Just Chatting", 2000L);

    // Case 1: Custom 태그만 있는 리스트
    customOnlyMemberTags =
        List.of(MemberTag.create(testMember, customTag1), MemberTag.create(testMember, customTag2));

    // Case 2: Category 태그만 있는 리스트
    categoryOnlyMemberTags =
        List.of(
            MemberTag.create(testMember, categoryTag1), MemberTag.create(testMember, categoryTag2));

    // Case 3: Custom + Category 섞인 리스트
    mixedMemberTags =
        List.of(
            MemberTag.create(testMember, customTag1),
            MemberTag.create(testMember, categoryTag1),
            MemberTag.create(testMember, customTag2),
            MemberTag.create(testMember, categoryTag2));
  }

  @Nested
  @DisplayName("getMemberTags 메서드")
  class GetMemberTags {
    @Test
    @DisplayName("커스텀 태그와 카테고리 태그를 갖고 있는 member의 태그를 분리해서 dto로 반환한다")
    void getMemberTagsSuccess() {
      // given
      given(memberRepository.findByUuid(memberUuid)).willReturn(Optional.of(testMember));
      given(memberTagRepository.findByMember(testMember)).willReturn(mixedMemberTags);

      // when
      MemberTagListResponse response = memberTagService.getMemberTags(memberUuid);

      // then
      assertThat(response.customTags()).hasSize(2);
      assertThat(response.customTags())
          .extracting("tagName")
          .containsExactlyInAnyOrder("롤 잘하는 방송", "힐링 방송");

      assertThat(response.categoryTags()).hasSize(2);
      assertThat(response.categoryTags())
          .extracting("tagName")
          .containsExactlyInAnyOrder("League of Legends", "Just Chatting");
    }

    @Test
    @DisplayName("커스텀 태그만 있는 경우 카테고리 태그는 빈 리스트로 반환한다")
    void getCustomOnlyMemberTags() {
      // given
      given(memberRepository.findByUuid(memberUuid)).willReturn(Optional.of(testMember));
      given(memberTagRepository.findByMember(testMember)).willReturn(customOnlyMemberTags);

      // when
      MemberTagListResponse response = memberTagService.getMemberTags(memberUuid);

      // then
      assertThat(response.customTags()).hasSize(2);
      assertThat(response.customTags())
          .extracting("tagName")
          .containsExactlyInAnyOrder("롤 잘하는 방송", "힐링 방송");

      assertThat(response.categoryTags()).isEmpty();
    }

    @Test
    @DisplayName("카테고리 태그만 있는 경우 커스텀 태그는 빈 리스트로 반환한다")
    void getCategoryOnlyMemberTags() {
      // given
      given(memberRepository.findByUuid(memberUuid)).willReturn(Optional.of(testMember));
      given(memberTagRepository.findByMember(testMember)).willReturn(categoryOnlyMemberTags);

      // when
      MemberTagListResponse response = memberTagService.getMemberTags(memberUuid);

      // then
      assertThat(response.customTags()).isEmpty();

      assertThat(response.categoryTags()).hasSize(2);
      assertThat(response.categoryTags())
          .extracting("tagName")
          .containsExactlyInAnyOrder("League of Legends", "Just Chatting");
    }

    @Test
    @DisplayName("멤버 태그가 아무것도 없더라도 빈 리스트를 포함한 dto를 반환한다")
    void getEmptyMemberTags() {
      // given
      given(memberRepository.findByUuid(memberUuid)).willReturn(Optional.of(testMember));
      given(memberTagRepository.findByMember(testMember)).willReturn(List.of());

      // when
      MemberTagListResponse response = memberTagService.getMemberTags(memberUuid);

      // then
      assertThat(response.categoryTags().isEmpty()).isTrue();
      assertThat(response.customTags()).isEmpty();
    }

    @Test
    @DisplayName("멤버를 찾지 못하면 MEMBER_NOT_FOUND 에러 코드를 보낸다")
    void getMemberNotFound() {
      // given
      given(memberRepository.findByUuid(memberUuid)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> memberTagService.getMemberTags(memberUuid))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("setMemberTags 메서드")
  class SetMemberTags {

    @Test
    @DisplayName("커스텀 태그를 설정하면 기존 커스텀 태그를 삭제하고 새 태그를 저장한다")
    void setCustomTagsSuccess() {
      // given
      List<String> tagNames = List.of("롤 잘하는 방송", "힐링 방송");
      MemberTagRequest request = MemberTagRequest.of(tagNames, TagType.CUSTOM);

      given(memberRepository.findByUuid(memberUuid)).willReturn(Optional.of(testMember));
      given(tagRepository.findByNameInAndTagType(new HashSet<>(tagNames), TagType.CUSTOM))
          .willReturn(List.of(customTag1, customTag2));

      // when
      memberTagService.setMemberTags(memberUuid, request);

      // then
      then(memberTagRepository).should().deleteByMemberAndTagType(testMember, TagType.CUSTOM);
      then(tagRepository).should().findByNameInAndTagType(new HashSet<>(tagNames), TagType.CUSTOM);
      then(memberTagRepository).should().saveAll(anyList());
    }

    @Test
    @DisplayName("카테고리 태그를 설정하면 기존 카테고리 태그를 삭제하고 새 태그를 저장한다")
    void setCategoryTagsSuccess() {
      // given
      List<String> tagNames = List.of("League of Legends", "Just Chatting");
      MemberTagRequest request = MemberTagRequest.of(tagNames, TagType.CATEGORY);

      given(memberRepository.findByUuid(memberUuid)).willReturn(Optional.of(testMember));
      given(tagRepository.findByNameInAndTagType(new HashSet<>(tagNames), TagType.CATEGORY))
          .willReturn(List.of(categoryTag1, categoryTag2));

      // when
      memberTagService.setMemberTags(memberUuid, request);

      // then
      then(memberTagRepository).should().deleteByMemberAndTagType(testMember, TagType.CATEGORY);
      then(tagRepository)
          .should()
          .findByNameInAndTagType(new HashSet<>(tagNames), TagType.CATEGORY);
      then(memberTagRepository).should().saveAll(anyList());
    }

    @Test
    @DisplayName("빈 태그 리스트를 설정하면 기존 태그만 삭제하고 저장하지 않는다")
    void setEmptyTagsOnlyDeletes() {
      // given
      MemberTagRequest request = MemberTagRequest.of(List.of(), TagType.CUSTOM);

      given(memberRepository.findByUuid(memberUuid)).willReturn(Optional.of(testMember));

      // when
      memberTagService.setMemberTags(memberUuid, request);

      // then
      then(memberTagRepository).should().deleteByMemberAndTagType(testMember, TagType.CUSTOM);
      then(tagRepository).should(never()).findByNameInAndTagType(any(), any());
      then(memberTagRepository).should(never()).saveAll(anyList());
    }

    @Test
    @DisplayName("멤버를 찾지 못하면 MEMBER_NOT_FOUND 에러 코드를 보낸다")
    void setMemberNotFound() {
      // given
      MemberTagRequest request = MemberTagRequest.of(List.of("태그"), TagType.CUSTOM);
      given(memberRepository.findByUuid(memberUuid)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> memberTagService.setMemberTags(memberUuid, request))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
    }
  }
}
