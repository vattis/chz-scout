package com.vatti.chzscout.backend.tag.application.service;

import com.vatti.chzscout.backend.common.exception.BusinessException;
import com.vatti.chzscout.backend.member.domain.entity.Member;
import com.vatti.chzscout.backend.member.exception.MemberErrorCode;
import com.vatti.chzscout.backend.member.infrastructure.MemberRepository;
import com.vatti.chzscout.backend.tag.application.usecase.MemberTagUseCase;
import com.vatti.chzscout.backend.tag.domain.dto.MemberTagListResponse;
import com.vatti.chzscout.backend.tag.domain.dto.MemberTagRequest;
import com.vatti.chzscout.backend.tag.domain.dto.MemberTagResponse;
import com.vatti.chzscout.backend.tag.domain.entity.MemberTag;
import com.vatti.chzscout.backend.tag.domain.entity.Tag;
import com.vatti.chzscout.backend.tag.domain.entity.TagType;
import com.vatti.chzscout.backend.tag.infrastructure.MemberTagRepository;
import com.vatti.chzscout.backend.tag.infrastructure.TagRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberTagService implements MemberTagUseCase {

  private final MemberRepository memberRepository;
  private final MemberTagRepository memberTagRepository;
  private final TagRepository tagRepository;

  /**
   * 멤버가 설정한 태그 목록을 조회합니다.
   *
   * <p>1회 쿼리로 전체 태그를 조회한 후, 메모리에서 CUSTOM/CATEGORY로 분리합니다.
   *
   * @param memberUuid 조회할 멤버의 UUID
   * @return CUSTOM 태그와 CATEGORY 태그가 분리된 응답
   */
  @Override
  public MemberTagListResponse getMemberTags(String memberUuid) {
    Member member = findMemberByUuid(memberUuid);

    // 1회 쿼리로 전체 조회 (타입별 2회 조회보다 효율적)
    List<MemberTag> memberTags = memberTagRepository.findByMember(member);

    // 메모리에서 타입별 분리
    List<MemberTagResponse> customMemberTags = new ArrayList<>();
    List<MemberTagResponse> categoryMemberTags = new ArrayList<>();

    for (MemberTag memberTag : memberTags) {
      if (memberTag.getTag().getTagType().equals(TagType.CUSTOM)) {
        customMemberTags.add(MemberTagResponse.from(memberTag));
      } else if (memberTag.getTag().getTagType().equals(TagType.CATEGORY)) {
        categoryMemberTags.add(MemberTagResponse.from(memberTag));
      }
    }
    return MemberTagListResponse.of(customMemberTags, categoryMemberTags);
  }

  /**
   * 멤버의 태그 설정을 저장합니다.
   *
   * <p>해당 타입의 기존 태그를 모두 삭제하고, 새로운 태그 목록으로 교체합니다. IN 쿼리를 사용하여 N+1 문제를 방지합니다.
   *
   * @param memberUuid 설정할 멤버의 UUID
   * @param tagRequest 설정할 태그 요청 (태그 이름 목록 + 타입)
   */
  @Override
  @Transactional
  public void setMemberTags(String memberUuid, MemberTagRequest tagRequest) {
    Member member = findMemberByUuid(memberUuid);

    // 해당 타입의 기존 태그만 삭제 (다른 타입은 유지)
    memberTagRepository.deleteByMemberAndTagType(member, tagRequest.tagType());

    if (tagRequest.names().isEmpty()) {
      return;
    }

    // IN 쿼리로 한 번에 조회 (N+1 방지)
    List<Tag> tags =
        tagRepository.findByNameInAndTagType(
            new HashSet<>(tagRequest.names()), tagRequest.tagType());

    List<MemberTag> memberTags = tags.stream().map(tag -> MemberTag.create(member, tag)).toList();
    memberTagRepository.saveAll(memberTags);
  }

  private Member findMemberByUuid(String memberUuid) {
    return memberRepository
        .findByUuid(memberUuid)
        .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
  }
}
