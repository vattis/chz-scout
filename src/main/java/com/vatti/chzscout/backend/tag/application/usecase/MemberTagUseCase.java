package com.vatti.chzscout.backend.tag.application.usecase;

import com.vatti.chzscout.backend.tag.domain.dto.MemberTagListResponse;
import com.vatti.chzscout.backend.tag.domain.dto.MemberTagRequest;

public interface MemberTagUseCase {

  /**
   * 멤버가 설정한 태그 목록을 조회합니다.
   *
   * @param memberUuid 멤버 UUID
   * @return 멤버의 태그 목록 (CUSTOM, CATEGORY 분리)
   */
  MemberTagListResponse getMemberTags(String memberUuid);

  /**
   * 멤버의 태그 설정을 저장합니다.
   *
   * <p>해당 타입의 기존 태그를 삭제하고 새로운 태그 목록으로 교체합니다.
   *
   * @param memberUuid 멤버 UUID
   * @param tagRequest 설정할 태그 요청 (태그 이름 목록 + 타입)
   */
  void setMemberTags(String memberUuid, MemberTagRequest tagRequest);
}
