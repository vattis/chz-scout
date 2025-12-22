package com.vatti.chzscout.backend.member.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vatti.chzscout.backend.auth.domain.CustomUserDetails;
import com.vatti.chzscout.backend.common.response.ApiResponse;
import com.vatti.chzscout.backend.member.domain.dto.MemberResponse;
import com.vatti.chzscout.backend.member.domain.entity.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

  @InjectMocks private MemberController memberController;

  private Member testMember;
  private CustomUserDetails userDetails;

  @BeforeEach
  void setUp() {
    testMember = Member.create("discord-123456", "테스트유저", "test@example.com");
    userDetails = new CustomUserDetails(testMember, "USER");
  }

  @Nested
  @DisplayName("getMe 메서드")
  class GetMe {

    @Test
    @DisplayName("인증된 사용자 정보를 성공적으로 반환한다")
    void getMeSuccess() {
      // when
      ApiResponse<MemberResponse> response = memberController.getMe(userDetails);

      // then
      assertThat(response.isSuccess()).isTrue();
      assertThat(response.getData()).isNotNull();
      assertThat(response.getData().uuid()).isEqualTo(testMember.getUuid());
      assertThat(response.getData().nickname()).isEqualTo("테스트유저");
      assertThat(response.getData().discordId()).isEqualTo("discord-123456");
    }

    @Test
    @DisplayName("이메일 없이 가입한 사용자도 정보를 반환한다")
    void getMeWithoutEmail() {
      // given
      Member memberWithoutEmail = Member.create("discord-789", "이메일없는유저", null);
      CustomUserDetails detailsWithoutEmail = new CustomUserDetails(memberWithoutEmail, "USER");

      // when
      ApiResponse<MemberResponse> response = memberController.getMe(detailsWithoutEmail);

      // then
      assertThat(response.isSuccess()).isTrue();
      assertThat(response.getData().nickname()).isEqualTo("이메일없는유저");
      assertThat(response.getData().discordId()).isEqualTo("discord-789");
    }
  }
}
