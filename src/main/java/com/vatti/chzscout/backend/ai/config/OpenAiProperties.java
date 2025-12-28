package com.vatti.chzscout.backend.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OpenAI API 설정 프로퍼티.
 *
 * <p>application.yml의 openai.api 설정을 바인딩합니다.
 */
@Component
@ConfigurationProperties(prefix = "openai.api")
@Getter
@Setter
public class OpenAiProperties {

  /** OpenAI API 키. */
  private String key;

  /** 사용할 모델명 (예: gpt-5-nano). */
  private String model;

  /** API 기본 URL. */
  private String baseUrl;
}
