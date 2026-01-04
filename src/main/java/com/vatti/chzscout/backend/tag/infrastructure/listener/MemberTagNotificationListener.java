package com.vatti.chzscout.backend.tag.infrastructure.listener;

import com.vatti.chzscout.backend.discord.application.DiscordDmService;
import com.vatti.chzscout.backend.member.domain.entity.Member;
import com.vatti.chzscout.backend.stream.domain.EnrichedStreamDto;
import com.vatti.chzscout.backend.stream.domain.event.StreamNotificationTriggerEvent;
import com.vatti.chzscout.backend.stream.infrastructure.redis.StreamRedisStore;
import com.vatti.chzscout.backend.tag.domain.entity.MemberTag;
import com.vatti.chzscout.backend.tag.infrastructure.MemberTagRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * MemberTag 기반 방송 알림 리스너.
 *
 * <p>StreamNotificationTriggerEvent를 수신하여 유저가 설정한 태그와 현재 방송 태그를 대조하고, 매칭되는 유저에게 Discord DM으로 알림을
 * 발송합니다.
 */
@Component
@Profile("!test")
@Slf4j
@RequiredArgsConstructor
public class MemberTagNotificationListener {

  private final MemberTagRepository memberTagRepository;
  private final StreamRedisStore streamRedisStore;
  private final DiscordDmService discordDmService;

  /**
   * 스트림 알림 트리거 이벤트를 수신하여 태그 매칭 및 알림을 발송합니다.
   *
   * <p>@Async로 비동기 실행되어 스케줄러 스레드를 블로킹하지 않습니다.
   */
  @Async
  @EventListener
  public void onNotificationTrigger(StreamNotificationTriggerEvent event) {
    Set<String> changedChannelIds = event.changedChannelIds();
    log.info(
        "Received StreamNotificationTriggerEvent, {} changed streams", changedChannelIds.size());

    if (changedChannelIds.isEmpty()) {
      log.info("No changed streams, skipping notification");
      return;
    }

    // 1. 변경된 방송만 필터링
    List<EnrichedStreamDto> changedStreams =
        streamRedisStore.findEnrichedStreams().stream()
            .filter(stream -> changedChannelIds.contains(stream.channelId()))
            .toList();

    if (changedStreams.isEmpty()) {
      log.info("No matching streams found in cache, skipping notification");
      return;
    }

    // 2. 변경된 방송의 모든 태그 수집
    Set<String> allEnrichedTags =
        changedStreams.stream()
            .flatMap(stream -> stream.enrichedTags().stream())
            .collect(Collectors.toSet());

    // 3. 해당 태그를 설정한 MemberTag만 DB에서 조회(알림 허용 유저만) (1차 필터링)
    List<MemberTag> matchedMemberTags = memberTagRepository.findByTagNames(allEnrichedTags);

    if (matchedMemberTags.isEmpty()) {
      log.info("No matching member tags found, skipping notification");
      return;
    }

    log.info(
        "Found {} member tags matching {} enriched tags",
        matchedMemberTags.size(),
        allEnrichedTags.size());

    // 4. 태그명 → Member 리스트 역인덱스 생성
    Map<String, List<Member>> tagToMembers = buildTagToMembersMap(matchedMemberTags);

    // 5. 2차 매칭: Member → 매칭된 방송들 (Set으로 중복 제거)
    Map<Member, Set<EnrichedStreamDto>> memberToStreams =
        findMemberToStreamsMatches(changedStreams, tagToMembers);

    log.info("Matched {} members to streams", memberToStreams.size());
    for (Map.Entry<Member, Set<EnrichedStreamDto>> entry : memberToStreams.entrySet()) {
      discordDmService.sendNotification(entry.getKey().getDiscordId(), entry.getValue());
    }

    log.info("Tag matching completed");
  }

  /**
   * 태그명 → Member 리스트 역인덱스를 생성합니다.
   *
   * @param memberTags MemberTag 목록
   * @return 태그명을 키로, 해당 태그를 설정한 Member 리스트를 값으로 하는 Map
   */
  private Map<String, List<Member>> buildTagToMembersMap(List<MemberTag> memberTags) {
    Map<String, List<Member>> tagToMembers = new HashMap<>();

    for (MemberTag mt : memberTags) {
      tagToMembers
          .computeIfAbsent(mt.getTag().getName(), k -> new ArrayList<>())
          .add(mt.getMember());
    }

    return tagToMembers;
  }

  /**
   * Member별로 매칭되는 방송을 찾습니다.
   *
   * @param streams 변경된 방송 목록
   * @param tagToMembers 태그명 → Member 리스트 Map
   * @return Member → 매칭된 방송 Set (중복 제거됨)
   */
  private Map<Member, Set<EnrichedStreamDto>> findMemberToStreamsMatches(
      List<EnrichedStreamDto> streams, Map<String, List<Member>> tagToMembers) {

    Map<Member, Set<EnrichedStreamDto>> memberToStreams = new HashMap<>();

    for (EnrichedStreamDto stream : streams) {
      for (String tag : stream.enrichedTags()) {
        List<Member> members = tagToMembers.get(tag);
        if (members != null) {
          for (Member member : members) {
            memberToStreams.computeIfAbsent(member, k -> new HashSet<>()).add(stream);
          }
        }
      }
    }

    return memberToStreams;
  }
}
