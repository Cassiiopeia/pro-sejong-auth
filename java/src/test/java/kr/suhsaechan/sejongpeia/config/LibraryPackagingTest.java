package kr.suhsaechan.sejongpeia.config;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * 라이브러리 jar 규칙: 사용하는 앱의 설정을 덮는 application 설정 파일을 싣지 않음
 * (예전 번들 application.yml 이 spring.application.name, ssl-verification=false 를 주입할 수 있었음)
 */
class LibraryPackagingTest {

  @Test
  void application_설정파일을_라이브러리에_싣지_않는다() {
    ClassLoader classLoader = getClass().getClassLoader();

    assertNull(classLoader.getResource("application.yml"));
    assertNull(classLoader.getResource("application.yaml"));
    assertNull(classLoader.getResource("application.properties"));
  }
}
