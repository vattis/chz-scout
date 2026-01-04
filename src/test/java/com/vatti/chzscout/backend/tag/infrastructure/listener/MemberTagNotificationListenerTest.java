package com.vatti.chzscout.backend.tag.infrastructure.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.vatti.chzscout.backend.discord.application.DiscordDmService;
import com.vatti.chzscout.backend.member.domain.entity.Member;
import com.vatti.chzscout.backend.member.fixture.MemberFixture;
import com.vatti.chzscout.backend.stream.domain.EnrichedStreamDto;
import com.vatti.chzscout.backend.stream.domain.event.StreamNotificationTriggerEvent;
import com.vatti.chzscout.backend.stream.fixture.EnrichedStreamDtoFixture;
import com.vatti.chzscout.backend.stream.infrastructure.redis.StreamRedisStore;
import com.vatti.chzscout.backend.tag.domain.entity.MemberTag;
import com.vatti.chzscout.backend.tag.domain.entity.Tag;
import com.vatti.chzscout.backend.tag.fixture.TagFixture;
import com.vatti.chzscout.backend.tag.infrastructure.MemberTagRepository;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberTagNotificationListenerTest {

  @Mock private MemberTagRepository memberTagRepository;
  @Mock private StreamRedisStore streamRedisStore;
  @Mock private DiscordDmService discordDmService;

  @InjectMocks private MemberTagNotificationListener listener;

  @Nested
  @DisplayName("onNotificationTrigger 메서드 테스트")
  class OnNotificationTrigger {

    @Test
    @DisplayName("changedChannelIds가 비어있으면 후속 처리를 하지 않는다")
    void skipsWhenChangedIdsEmpty() {
      // given
      StreamNotificationTriggerEvent event = new StreamNotificationTriggerEvent(Set.of());

      // when
      listener.onNotificationTrigger(event);

      // then
      verify(streamRedisStore, never()).findEnrichedStreams();
      verify(memberTagRepository, never()).findByTagNames(any());
      verify(discordDmService, never()).sendNotification(any(), any());
    }

    @Test
    @DisplayName("캐시에 매칭되는 방송이 없으면 후속 처리를 하지 않는다")
    void skipsWhenNoMatchingStreamsInCache() {
      // given
      StreamNotificationTriggerEvent event =
          new StreamNotificationTriggerEvent(Set.of("nonexistent_channel"));
      given(streamRedisStore.findEnrichedStreams())
          .willReturn(List.of(EnrichedStreamDtoFixture.create(1)));

      // when
      listener.onNotificationTrigger(event);

      // then
      verify(memberTagRepository, never()).findByTagNames(any());
      verify(discordDmService, never()).sendNotification(any(), any());
    }

    @Test
    @DisplayName("매칭되는 MemberTag가 없으면 DM 발송을 하지 않는다")
    void skipsWhenNoMatchingMemberTags() {
      // given
      EnrichedStreamDto stream = EnrichedStreamDtoFixture.lolStream(1);
      StreamNotificationTriggerEvent event =
          new StreamNotificationTriggerEvent(Set.of("channel_1"));
      given(streamRedisStore.findEnrichedStreams()).willReturn(List.of(stream));
      given(memberTagRepository.findByTagNames(anySet())).willReturn(List.of());

      // when
      listener.onNotificationTrigger(event);

      // then
      verify(discordDmService, never()).sendNotification(any(), any());
    }

    @Test
    @DisplayName("매칭되는 멤버에게 DM 알림을 발송한다")
    void sendsDmToMatchingMembers() {
      // given
      EnrichedStreamDto stream =
          EnrichedStreamDtoFixture.withTags(1, List.of("롤"), List.of("롤", "게임"));
      Member member = MemberFixture.createWithNotificationEnabled("discord_123");
      Tag tag = TagFixture.createCustom("롤");
      MemberTag memberTag = MemberTag.create(member, tag);

      StreamNotificationTriggerEvent event =
          new StreamNotificationTriggerEvent(Set.of("channel_1"));
      given(streamRedisStore.findEnrichedStreams()).willReturn(List.of(stream));
      given(memberTagRepository.findByTagNames(Set.of("롤", "게임"))).willReturn(List.of(memberTag));
      given(discordDmService.sendNotification(any(), any()))
          .willReturn(CompletableFuture.completedFuture(true));

      // when
      listener.onNotificationTrigger(event);

      // then
      verify(discordDmService).sendNotification(eq("discord_123"), anySet());
    }

    @Test
    @DisplayName("여러 멤버가 매칭되면 각각에게 DM을 발송한다")
    void sendsDmToMultipleMembers() {
      // given
      EnrichedStreamDto stream = EnrichedStreamDtoFixture.withTags(1, List.of("롤"), List.of("롤"));
      Member member1 = MemberFixture.createWithNotificationEnabled("discord_1");
      Member member2 = MemberFixture.createWithNotificationEnabled("discord_2");
      Tag tag = TagFixture.createCustom("롤");
      MemberTag memberTag1 = MemberTag.create(member1, tag);
      MemberTag memberTag2 = MemberTag.create(member2, tag);

      StreamNotificationTriggerEvent event =
          new StreamNotificationTriggerEvent(Set.of("channel_1"));
      given(streamRedisStore.findEnrichedStreams()).willReturn(List.of(stream));
      given(memberTagRepository.findByTagNames(Set.of("롤")))
          .willReturn(List.of(memberTag1, memberTag2));
      given(discordDmService.sendNotification(any(), any()))
          .willReturn(CompletableFuture.completedFuture(true));

      // when
      listener.onNotificationTrigger(event);

      // then
      verify(discordDmService, times(2)).sendNotification(any(), anySet());
      verify(discordDmService).sendNotification(eq("discord_1"), anySet());
      verify(discordDmService).sendNotification(eq("discord_2"), anySet());
    }

    @Test
    @DisplayName("같은 방송이 여러 태그로 매칭되어도 유저당 한 번만 발송한다")
    void deduplicatesWhenSameStreamMatchesMultipleTags() {
      // given - 롤, 게임 두 태그 모두 같은 유저가 설정
      EnrichedStreamDto stream =
          EnrichedStreamDtoFixture.withTags(1, List.of("롤", "게임"), List.of("롤", "게임"));
      Member member = MemberFixture.createWithNotificationEnabled("discord_123");
      Tag rolTag = TagFixture.createCustom("롤");
      Tag gameTag = TagFixture.createCustom("게임");
      MemberTag memberTagRol = MemberTag.create(member, rolTag);
      MemberTag memberTagGame = MemberTag.create(member, gameTag);

      StreamNotificationTriggerEvent event =
          new StreamNotificationTriggerEvent(Set.of("channel_1"));
      given(streamRedisStore.findEnrichedStreams()).willReturn(List.of(stream));
      given(memberTagRepository.findByTagNames(Set.of("롤", "게임")))
          .willReturn(List.of(memberTagRol, memberTagGame));
      given(discordDmService.sendNotification(any(), any()))
          .willReturn(CompletableFuture.completedFuture(true));

      // when
      listener.onNotificationTrigger(event);

      // then - 한 번만 발송
      verify(discordDmService, times(1)).sendNotification(eq("discord_123"), anySet());
    }

    @Test
    @DisplayName("여러 방송이 매칭되면 해당 방송들을 모아서 발송한다")
    void aggregatesMultipleMatchingStreams() {
      // given
      EnrichedStreamDto stream1 = EnrichedStreamDtoFixture.withTags(1, List.of("롤"), List.of("롤"));
      EnrichedStreamDto stream2 = EnrichedStreamDtoFixture.withTags(2, List.of("롤"), List.of("롤"));
      Member member = MemberFixture.createWithNotificationEnabled("discord_123");
      Tag tag = TagFixture.createCustom("롤");
      MemberTag memberTag = MemberTag.create(member, tag);

      StreamNotificationTriggerEvent event =
          new StreamNotificationTriggerEvent(Set.of("channel_1", "channel_2"));
      given(streamRedisStore.findEnrichedStreams()).willReturn(List.of(stream1, stream2));
      given(memberTagRepository.findByTagNames(Set.of("롤"))).willReturn(List.of(memberTag));
      given(discordDmService.sendNotification(any(), any()))
          .willReturn(CompletableFuture.completedFuture(true));

      // when
      listener.onNotificationTrigger(event);

      // then - 한 번 발송에 여러 방송 포함
      verify(discordDmService, times(1)).sendNotification(eq("discord_123"), anySet());
    }
  }
}
