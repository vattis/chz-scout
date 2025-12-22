package com.vatti.chzscout.backend.tag.domain.dto;

import java.util.List;

/**
 * 멤버 태그 목록 응답 DTO.
 *
 * <p>CUSTOM 태그와 CATEGORY 태그를 분리하여 제공합니다.
 *
 * @param customTags 커스텀 태그 목록
 * @param categoryTags 카테고리 태그 목록
 */
public record MemberTagListResponse(
    List<MemberTagResponse> customTags, List<MemberTagResponse> categoryTags) {

  public static MemberTagListResponse of(
      List<MemberTagResponse> customTags, List<MemberTagResponse> categoryTags) {
    return new MemberTagListResponse(customTags, categoryTags);
  }
}
