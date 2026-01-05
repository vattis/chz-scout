package com.vatti.chzscout.backend.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.vatti.chzscout.backend.member.domain.dto.MemberResponse;
import com.vatti.chzscout.backend.member.domain.entity.Member;
import com.vatti.chzscout.backend.member.infrastructure.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 단위 테스트")
class MemberServiceTest {

  @Mock private MemberRepository memberRepository;

  private MemberService memberService;

  @BeforeEach
  void setUp() {
    memberService = new MemberService(memberRepository);
  }

  @Nested
  @DisplayName("getCurrentMember 메서드 테스트")
  class GetCurrentMember {

    @Test
    @DisplayName("Member 엔티티를 MemberResponse DTO로 변환한다")
    void returnsMemberResponse() {
      // given
      Member member = Member.create("discord-123456", "테스트유저", "test@example.com");

      // when
      MemberResponse response = memberService.getCurrentMember(member);

      // then
      assertThat(response.uuid()).isEqualTo(member.getUuid());
      assertThat(response.nickname()).isEqualTo("테스트유저");
      assertThat(response.discordId()).isEqualTo("discord-123456");
    }

    @Test
    @DisplayName("이메일 없는 Member도 정상적으로 변환한다")
    void handlesNullEmail() {
      // given
      Member member = Member.create("discord-789", "이메일없는유저", null);

      // when
      MemberResponse response = memberService.getCurrentMember(member);

      // then
      assertThat(response.uuid()).isEqualTo(member.getUuid());
      assertThat(response.nickname()).isEqualTo("이메일없는유저");
      assertThat(response.discordId()).isEqualTo("discord-789");
    }
  }

  @Nested
  @DisplayName("updateNotificationEnabled 메서드 테스트")
  class UpdateNotificationEnabled {

    @Test
    @DisplayName("알림 설정을 true로 변경하면 true를 반환한다")
    void enablesNotification() {
      // given
      Member member = Member.create("discord-123", "테스트유저", "test@test.com");
      assertThat(member.isNotificationEnabled()).isFalse();

      // when
      boolean result = memberService.updateNotificationEnabled(member, true);

      // then
      assertThat(result).isTrue();
      assertThat(member.isNotificationEnabled()).isTrue();
      verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("알림 설정을 false로 변경하면 false를 반환한다")
    void disablesNotification() {
      // given
      Member member = Member.create("discord-123", "테스트유저", "test@test.com");
      member.updateNotificationEnabled(true);
      assertThat(member.isNotificationEnabled()).isTrue();

      // when
      boolean result = memberService.updateNotificationEnabled(member, false);

      // then
      assertThat(result).isFalse();
      assertThat(member.isNotificationEnabled()).isFalse();
      verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("같은 값으로 변경해도 정상적으로 처리된다")
    void handlesSameValue() {
      // given
      Member member = Member.create("discord-123", "테스트유저", "test@test.com");
      member.updateNotificationEnabled(true);

      // when
      boolean result = memberService.updateNotificationEnabled(member, true);

      // then
      assertThat(result).isTrue();
      verify(memberRepository).save(member);
    }
  }
}
