package com.vatti.chzscout.backend.example.domain.dto;

import com.vatti.chzscout.backend.example.domain.entity.Example;
import java.time.LocalDateTime;

public record ExampleResponse(Long id, String name, String email, LocalDateTime createdAt) {
  public static ExampleResponse from(Example example) {
    return new ExampleResponse(
        example.getId(), example.getName(), example.getEmail(), example.getCreatedAt());
  }
}
