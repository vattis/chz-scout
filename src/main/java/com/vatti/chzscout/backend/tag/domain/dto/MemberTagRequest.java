package com.vatti.chzscout.backend.tag.domain.dto;

import com.vatti.chzscout.backend.tag.domain.entity.TagType;
import java.util.List;

/**
 * 멤버 태그 설정 요청 DTO.
 *
 * @param names 태그 이름 목록
 * @param tagType 태그 타입 (CATEGORY 또는 CUSTOM)
 */
public record MemberTagRequest(List<String> names, TagType tagType) {

  public static MemberTagRequest of(List<String> names, TagType tagType) {
    return new MemberTagRequest(names, tagType);
  }
}
