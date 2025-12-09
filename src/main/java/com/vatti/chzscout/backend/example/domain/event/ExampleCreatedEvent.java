package com.vatti.chzscout.backend.example.domain.event;

import com.vatti.chzscout.backend.example.domain.entity.Example;
import java.time.LocalDateTime;

public record ExampleCreatedEvent(Long exampleId, String email, LocalDateTime createdAt) {
  public static ExampleCreatedEvent of(Example example) {
    return new ExampleCreatedEvent(example.getId(), example.getEmail(), LocalDateTime.now());
  }
}
