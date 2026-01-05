package com.vatti.chzscout.backend.tag.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.vatti.chzscout.backend.common.config.DataJpaTestConfig;
import com.vatti.chzscout.backend.member.domain.entity.Member;
import com.vatti.chzscout.backend.member.fixture.MemberFixture;
import com.vatti.chzscout.backend.member.infrastructure.MemberRepository;
import com.vatti.chzscout.backend.tag.domain.entity.MemberTag;
import com.vatti.chzscout.backend.tag.domain.entity.Tag;
import com.vatti.chzscout.backend.tag.fixture.TagFixture;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(DataJpaTestConfig.class)
class MemberTagRepositoryTest {

  @Autowired MemberTagRepository memberTagRepository;
  @Autowired MemberRepository memberRepository;
  @Autowired TagRepository tagRepository;

  Member enabledMember1;
  Member enabledMember2;
  Member disabledMember;
  Tag tag1;
  Tag tag2;
  Tag tag3;

  @BeforeEach
  void setup() {
    // 알림 허용 멤버 2명
    enabledMember1 = MemberFixture.createWithNotificationEnabled("enabled_1");
    enabledMember2 = MemberFixture.createWithNotificationEnabled("enabled_2");
    // 알림 비허용 멤버 1명
    disabledMember = MemberFixture.createWithNotificationDisabled("disabled_1");

    memberRepository.saveAll(List.of(enabledMember1, enabledMember2, disabledMember));

    // 태그 3개 생성
    tag1 = TagFixture.createCustom("롤");
    tag2 = TagFixture.createCustom("FPS");
    tag3 = TagFixture.createCustom("음악");

    tagRepository.saveAll(List.of(tag1, tag2, tag3));

    // MemberTag 연결
    // enabledMember1: 롤, FPS
    // enabledMember2: 롤, 음악
    // disabledMember: 롤, FPS (알림 비허용)
    memberTagRepository.saveAll(
        List.of(
            MemberTag.create(enabledMember1, tag1),
            MemberTag.create(enabledMember1, tag2),
            MemberTag.create(enabledMember2, tag1),
            MemberTag.create(enabledMember2, tag3),
            MemberTag.create(disabledMember, tag1),
            MemberTag.create(disabledMember, tag2)));
  }

  @Nested
  @DisplayName("findByTagNames 메서드 테스트")
  class FindByTagNames {

    @Test
    @DisplayName("알림 허용된 멤버의 MemberTag만 조회한다")
    void findsOnlyNotificationEnabledMembers() {
      // given
      Set<String> tagNames = Set.of("롤");

      // when
      List<MemberTag> result = memberTagRepository.findByTagNames(tagNames);

      // then - 롤 태그: enabled1, enabled2만 (disabled 제외)
      assertThat(result).hasSize(2);
      assertThat(result)
          .extracting(mt -> mt.getMember().getDiscordId())
          .containsExactlyInAnyOrder("enabled_1", "enabled_2");
    }

    @Test
    @DisplayName("알림 비허용 멤버는 결과에서 제외된다")
    void excludesNotificationDisabledMembers() {
      // given
      Set<String> tagNames = Set.of("FPS");

      // when
      List<MemberTag> result = memberTagRepository.findByTagNames(tagNames);

      // then - FPS 태그: enabled1만 (disabled 제외)
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getMember().getDiscordId()).isEqualTo("enabled_1");
    }

    @Test
    @DisplayName("여러 태그로 조회 시 알림 허용 멤버만 반환한다")
    void findsMultipleTagsWithOnlyEnabledMembers() {
      // given
      Set<String> tagNames = Set.of("롤", "FPS", "음악");

      // when
      List<MemberTag> result = memberTagRepository.findByTagNames(tagNames);

      // then
      // 롤: enabled1, enabled2 (2개)
      // FPS: enabled1 (1개)
      // 음악: enabled2 (1개)
      // 총 4개 (disabled 멤버의 롤, FPS 제외)
      assertThat(result).hasSize(4);
      assertThat(result)
          .extracting(mt -> mt.getMember().getDiscordId())
          .containsOnly("enabled_1", "enabled_2");
    }

    @Test
    @DisplayName("존재하지 않는 태그로 조회 시 빈 리스트를 반환한다")
    void returnsEmptyForNonExistentTags() {
      // given
      Set<String> tagNames = Set.of("존재하지않는태그");

      // when
      List<MemberTag> result = memberTagRepository.findByTagNames(tagNames);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("알림 허용 멤버가 없는 태그는 빈 리스트를 반환한다")
    void returnsEmptyWhenAllMembersDisabled() {
      // given - 모든 멤버 알림 비허용으로 변경
      enabledMember1.updateNotificationEnabled(false);
      enabledMember2.updateNotificationEnabled(false);
      memberRepository.saveAll(List.of(enabledMember1, enabledMember2));

      Set<String> tagNames = Set.of("롤");

      // when
      List<MemberTag> result = memberTagRepository.findByTagNames(tagNames);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Member와 Tag가 함께 페치 조인되어 조회된다")
    void fetchJoinsMemberAndTag() {
      // given
      Set<String> tagNames = Set.of("롤");

      // when
      List<MemberTag> result = memberTagRepository.findByTagNames(tagNames);

      // then - N+1 없이 바로 접근 가능 (LazyInitializationException 발생 안 함)
      assertThat(result)
          .allSatisfy(
              mt -> {
                assertThat(mt.getMember().getNickname()).isNotNull();
                assertThat(mt.getTag().getName()).isEqualTo("롤");
              });
    }
  }
}
