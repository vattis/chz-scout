package com.vatti.chzscout.backend.member.presentation;

import com.vatti.chzscout.backend.auth.domain.CustomUserDetails;
import com.vatti.chzscout.backend.common.response.ApiResponse;
import com.vatti.chzscout.backend.member.domain.dto.MemberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 멤버 API 컨트롤러.
 *
 * <p>현재 로그인한 사용자 정보 조회 기능을 제공합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/members")
public class MemberController {

  /**
   * 현재 로그인한 사용자 정보를 조회합니다.
   *
   * <p>토큰이 유효하면 사용자 정보를 반환하고, 무효하면 401 Unauthorized가 반환됩니다.
   *
   * @param userDetails 인증된 사용자 정보 (JWT 토큰에서 추출)
   * @return 현재 로그인한 멤버 정보
   */
  @GetMapping("/me")
  public ApiResponse<MemberResponse> getMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
    return ApiResponse.success(MemberResponse.from(userDetails.getMember()));
  }
}
