package com.vatti.chzscout.backend.example.application.usecase;

import com.vatti.chzscout.backend.example.domain.dto.CreateExampleRequest;
import com.vatti.chzscout.backend.example.domain.dto.ExampleResponse;

public interface CreateExampleUseCase {
  ExampleResponse execute(CreateExampleRequest request);
}
