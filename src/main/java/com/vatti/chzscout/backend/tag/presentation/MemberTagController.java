package com.vatti.chzscout.backend.tag.presentation;

import com.vatti.chzscout.backend.common.response.ApiResponse;
import com.vatti.chzscout.backend.tag.application.usecase.MemberTagUseCase;
import com.vatti.chzscout.backend.tag.domain.dto.MemberTagListResponse;
import com.vatti.chzscout.backend.tag.domain.dto.MemberTagRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 멤버 태그 설정 API 컨트롤러.
 *
 * <p>멤버별 태그 조회 및 설정 기능을 제공합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/members/{memberUuid}/tags")
public class MemberTagController {

  private final MemberTagUseCase memberTagUseCase;

  /**
   * 멤버가 설정한 태그 목록을 조회합니다.
   *
   * @param memberUuid 조회할 멤버의 UUID
   * @return CUSTOM 태그와 CATEGORY 태그가 분리된 응답
   */
  @GetMapping
  public ApiResponse<MemberTagListResponse> getMemberTags(@PathVariable String memberUuid) {
    return ApiResponse.success(memberTagUseCase.getMemberTags(memberUuid));
  }

  /**
   * 멤버의 태그를 설정합니다.
   *
   * <p>해당 타입의 기존 태그를 삭제하고 새로운 태그로 교체합니다 (부분 수정).
   *
   * @param memberUuid 설정할 멤버의 UUID
   * @param tagRequest 설정할 태그 요청 (태그 이름 목록 + 타입)
   * @return 성공 응답
   */
  @PatchMapping
  public ApiResponse<Void> setMemberTags(
      @PathVariable String memberUuid, @RequestBody MemberTagRequest tagRequest) {
    memberTagUseCase.setMemberTags(memberUuid, tagRequest);
    return ApiResponse.success(null);
  }
}
