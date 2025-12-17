package com.vatti.chzscout.backend.common.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

/** 테스트용 Embedded Redis 서버 설정. */
@Configuration
@Profile("test")
public class EmbeddedRedisConfig {

  @Value("${spring.data.redis.port:6379}")
  private int redisPort;

  private RedisServer redisServer;

  @PostConstruct
  public void startRedis() {
    try {
      redisServer = new RedisServer(redisPort);
      redisServer.start();
    } catch (Exception e) {
      // 이미 Redis가 실행 중인 경우 무시 (로컬 개발 환경)
    }
  }

  @PreDestroy
  public void stopRedis() throws IOException {
    if (redisServer != null && redisServer.isActive()) {
      redisServer.stop();
    }
  }
}
