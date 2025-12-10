package com.vatti.chzscout.backend.member.domain.entity;

import com.vatti.chzscout.backend.common.domain.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

  @Column(name = "discord_id", nullable = false, unique = true)
  private String discordId;

  @Column(name = "discord_username", nullable = false)
  private String discordUsername;

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "email_hash", nullable = false, unique = true)
  private String emailHash;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "name_hash", nullable = false)
  private String nameHash;

  private Member(String discordId, String discordUsername) {
    this.discordId = discordId;
    this.discordUsername = discordUsername;
  }

  public static Member create(String discordId, String discordUsername) {
    return new Member(discordId, discordUsername);
  }

  // TODO(human): 프로필 업데이트 메서드 구현
  // public void updateProfile(...) { ... }
}
