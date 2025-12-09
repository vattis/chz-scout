package com.vatti.chzscout.backend.example.presentation;

import com.vatti.chzscout.backend.common.response.ApiResponse;
import com.vatti.chzscout.backend.example.application.usecase.CreateExampleUseCase;
import com.vatti.chzscout.backend.example.application.usecase.GetExampleUseCase;
import com.vatti.chzscout.backend.example.domain.dto.CreateExampleRequest;
import com.vatti.chzscout.backend.example.domain.dto.ExampleResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/examples")
public class ExampleController {

  private final CreateExampleUseCase createExampleUseCase;
  private final GetExampleUseCase getExampleUseCase;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<ExampleResponse> create(@RequestBody CreateExampleRequest request) {
    return ApiResponse.success(createExampleUseCase.execute(request));
  }

  @GetMapping("/{id}")
  public ApiResponse<ExampleResponse> get(@PathVariable Long id) {
    return ApiResponse.success(getExampleUseCase.execute(id));
  }

  @GetMapping
  public ApiResponse<List<ExampleResponse>> getAll() {
    return ApiResponse.success(getExampleUseCase.executeAll());
  }
}
