package com.vatti.chzscout.backend.example.application.usecase;

import com.vatti.chzscout.backend.example.domain.dto.ExampleResponse;
import java.util.List;

public interface GetExampleUseCase {
  ExampleResponse execute(Long id);

  List<ExampleResponse> executeAll();
}
