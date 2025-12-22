package com.vatti.chzscout.backend.tag.domain.dto;

import com.vatti.chzscout.backend.tag.domain.entity.MemberTag;
import com.vatti.chzscout.backend.tag.domain.entity.TagType;
import lombok.Builder;

/**
 * 멤버 태그 응답 DTO.
 *
 * @param memberUuid 멤버 UUID
 * @param tagName 태그 이름
 * @param tagType 태그 타입 (CUSTOM 또는 CATEGORY)
 */
@Builder
public record MemberTagResponse(String memberUuid, String tagName, TagType tagType) {

  /**
   * MemberTag 엔티티로부터 응답 DTO를 생성합니다.
   *
   * @param memberTag 변환할 MemberTag 엔티티
   * @return MemberTagResponse DTO
   */
  public static MemberTagResponse from(MemberTag memberTag) {
    return MemberTagResponse.builder()
        .memberUuid(memberTag.getMember().getUuid())
        .tagName(memberTag.getTag().getName())
        .tagType(memberTag.getTag().getTagType())
        .build();
  }
}
