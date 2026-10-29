package kr.suhsaechan.sejongpeia.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.Collections;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.EnvironmentPostProcessor;

/**
 * TLS 재허용이 Spring Boot 시작 초기에 실행되도록 spring.factories 에 등록됐는지 (늦으면 handshake_failure)
 */
class SejongTlsEnvironmentPostProcessorTest {

  @Test
  void spring_factories에_TLS_초기화가_등록되어_있다() throws Exception {
    boolean isRegistered = false;
    for (URL factoriesUrl : Collections.list(getClass().getClassLoader().getResources("META-INF/spring.factories"))) {
      Properties factories = new Properties();
      try (var inputStream = factoriesUrl.openStream()) {
        factories.load(inputStream);
      }
      String postProcessors = factories.getProperty(EnvironmentPostProcessor.class.getName(), "");
      isRegistered |= postProcessors.contains(SejongTlsEnvironmentPostProcessor.class.getName());
    }

    assertTrue(isRegistered);
  }
}
