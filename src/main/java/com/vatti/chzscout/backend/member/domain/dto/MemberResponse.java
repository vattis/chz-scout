package com.vatti.chzscout.backend.member.domain.dto;

import com.vatti.chzscout.backend.member.domain.entity.Member;

/**
 * 멤버 정보 응답 DTO.
 *
 * <p>토큰 검증 및 로그인 상태 유지에 사용됩니다.
 *
 * @param uuid 멤버 UUID (외부 노출용)
 * @param nickname 닉네임 (Discord 사용자명)
 * @param discordId Discord 사용자 ID
 */
public record MemberResponse(String uuid, String nickname, String discordId) {

  public static MemberResponse from(Member member) {
    return new MemberResponse(member.getUuid(), member.getNickname(), member.getDiscordId());
  }
}
