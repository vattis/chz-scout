package com.vatti.chzscout.backend.common.config;

import org.jasypt.encryption.StringEncryptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * @DataJpaTest용 공통 설정.
 *
 * <p>JPA 슬라이스 테스트에서 필요한 Mock Bean들과 설정을 제공합니다.
 */
@TestConfiguration
@Import(JpaConfig.class)
public class DataJpaTestConfig {

  /** StringEncryptConverter가 필요로 하는 Mock Bean */
  @Bean(name = "jasyptEncryptorAES")
  public StringEncryptor jasyptEncryptorAES() {
    return new StringEncryptor() {
      @Override
      public String encrypt(String message) {
        return message; // 테스트에서는 암호화 안 함
      }

      @Override
      public String decrypt(String encryptedMessage) {
        return encryptedMessage; // 테스트에서는 복호화 안 함
      }
    };
  }
}
