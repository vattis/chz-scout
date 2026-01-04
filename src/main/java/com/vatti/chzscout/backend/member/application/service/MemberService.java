package com.vatti.chzscout.backend.member.application.service;

import com.vatti.chzscout.backend.member.application.usecase.MemberUseCase;
import com.vatti.chzscout.backend.member.domain.dto.MemberResponse;
import com.vatti.chzscout.backend.member.domain.entity.Member;
import com.vatti.chzscout.backend.member.infrastructure.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 멤버 서비스 구현체. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService implements MemberUseCase {

  private final MemberRepository memberRepository;

  /**
   * 현재 로그인한 사용자 정보를 조회합니다.
   *
   * <p>Spring Security의 @AuthenticationPrincipal로 주입받은 Member를 DTO로 변환합니다.
   *
   * @param member 인증된 멤버 엔티티
   * @return 멤버 응답 DTO
   */
  @Override
  public MemberResponse getCurrentMember(Member member) {
    return MemberResponse.from(member);
  }

  /**
   * 알림 수신 설정을 변경합니다.
   *
   * @param member 인증된 멤버 엔티티
   * @param enabled 알림 수신 여부
   * @return 변경된 알림 수신 여부
   */
  @Override
  @Transactional
  public boolean updateNotificationEnabled(Member member, boolean enabled) {
    member.updateNotificationEnabled(enabled);
    memberRepository.save(member);
    return member.isNotificationEnabled();
  }
}
