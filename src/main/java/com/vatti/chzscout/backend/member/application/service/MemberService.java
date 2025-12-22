package com.vatti.chzscout.backend.member.application.service;

import com.vatti.chzscout.backend.member.application.usecase.MemberUseCase;
import com.vatti.chzscout.backend.member.domain.dto.MemberResponse;
import com.vatti.chzscout.backend.member.domain.entity.Member;
import org.springframework.stereotype.Service;

/** 멤버 조회 서비스 구현체. */
@Service
public class MemberService implements MemberUseCase {

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
}
