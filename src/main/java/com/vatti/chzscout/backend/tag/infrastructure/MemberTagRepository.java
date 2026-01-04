package com.vatti.chzscout.backend.tag.infrastructure;

import com.vatti.chzscout.backend.member.domain.entity.Member;
import com.vatti.chzscout.backend.tag.domain.entity.MemberTag;
import com.vatti.chzscout.backend.tag.domain.entity.TagType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberTagRepository extends JpaRepository<MemberTag, Long> {

  List<MemberTag> findByMember(Member member);

  void deleteByMember(Member member);

  @Modifying
  @Query("DELETE FROM MemberTag mt WHERE mt.member = :member AND mt.tag.tagType = :tagType")
  void deleteByMemberAndTagType(@Param("member") Member member, @Param("tagType") TagType tagType);

  @Query(
      "SELECT mt FROM MemberTag mt "
          + "JOIN FETCH mt.tag t "
          + "JOIN FETCH mt.member m "
          + "WHERE t.name IN :tagNames "
          + "AND m.notificationEnabled is true")
  List<MemberTag> findByTagNames(@Param("tagNames") Collection<String> tagNames);
}
