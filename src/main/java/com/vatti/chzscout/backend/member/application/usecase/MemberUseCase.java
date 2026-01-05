package com.vatti.chzscout.backend.member.application.usecase;

import com.vatti.chzscout.backend.member.domain.dto.MemberResponse;
import com.vatti.chzscout.backend.member.domain.entity.Member;

/** 멤버 유즈케이스 인터페이스. */
public interface MemberUseCase {

  /**
   * 현재 로그인한 사용자 정보를 조회합니다.
   *
   * @param member 인증된 멤버 엔티티
   * @return 멤버 응답 DTO
   */
  MemberResponse getCurrentMember(Member member);

  /**
   * 알림 수신 설정을 변경합니다.
   *
   * @param member 인증된 멤버 엔티티
   * @param enabled 알림 수신 여부
   * @return 변경된 알림 수신 여부
   */
  boolean updateNotificationEnabled(Member member, boolean enabled);
}
