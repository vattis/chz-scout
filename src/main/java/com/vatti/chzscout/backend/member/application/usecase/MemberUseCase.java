package com.vatti.chzscout.backend.member.application.usecase;

import com.vatti.chzscout.backend.member.domain.dto.MemberResponse;
import com.vatti.chzscout.backend.member.domain.entity.Member;

/** 멤버 조회 유즈케이스 인터페이스. */
public interface MemberUseCase {

  /**
   * 현재 로그인한 사용자 정보를 조회합니다.
   *
   * @param member 인증된 멤버 엔티티
   * @return 멤버 응답 DTO
   */
  MemberResponse getCurrentMember(Member member);
}
