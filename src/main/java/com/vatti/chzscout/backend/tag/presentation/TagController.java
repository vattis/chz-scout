package com.vatti.chzscout.backend.tag.presentation;

import com.vatti.chzscout.backend.common.response.ApiResponse;
import com.vatti.chzscout.backend.tag.application.usecase.TagUseCase;
import com.vatti.chzscout.backend.tag.domain.dto.TagAutocompleteResponse;
import com.vatti.chzscout.backend.tag.domain.entity.TagType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/tags")
public class TagController {

  private static final int DEFAULT_LIMIT = 10;

  private final TagUseCase tagUseCase;

  @GetMapping("/suggestions")
  public ApiResponse<List<TagAutocompleteResponse>> getSuggestions(
      @RequestParam String prefix,
      @RequestParam TagType tagType,
      @RequestParam(defaultValue = "10") int limit) {
    return ApiResponse.success(tagUseCase.searchAutocomplete(prefix, tagType, limit));
  }
}
