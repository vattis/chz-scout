package com.vatti.chzscout.backend.tag.domain.entity;

import com.vatti.chzscout.backend.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tag")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag extends BaseEntity {

  @Column(nullable = false, unique = true)
  private String name;

  @Column private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TagType tagType;

  @Column(nullable = false)
  private int usageCount = 0;

  private Tag(String name, String description, TagType tagType) {
    this.name = name;
    this.description = description;
    this.tagType = tagType;
  }

  public static Tag createCategory(String name, String description) {
    return new Tag(name, description, TagType.CATEGORY);
  }

  public static Tag createCustom(String name, String description) {
    return new Tag(name, description, TagType.CUSTOM);
  }

  public static Tag createCustom(String name) {
    return new Tag(name, null, TagType.CUSTOM);
  }

  public void increaseUsageCount() {
    this.usageCount++;
  }
}
