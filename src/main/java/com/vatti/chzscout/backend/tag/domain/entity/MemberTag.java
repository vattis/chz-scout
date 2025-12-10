package com.vatti.chzscout.backend.tag.domain.entity;

import com.vatti.chzscout.backend.common.domain.entity.BaseEntity;
import com.vatti.chzscout.backend.member.domain.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
    name = "member_tag",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_member_tag_member_id_tag_id",
          columnNames = {"member_id", "tag_id"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public class MemberTag extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tag_id", nullable = false)
  private Tag tag;
}
