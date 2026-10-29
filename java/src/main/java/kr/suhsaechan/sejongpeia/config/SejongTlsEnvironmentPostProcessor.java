package kr.suhsaechan.sejongpeia.config;

import kr.suhsaechan.sejongpeia.util.SejongTlsSupport;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Spring Boot 시작 직후(빈 생성·앱 HTTPS 이전) 포털 cipher 재허용 (META-INF/spring.factories 등록)
 */
public class SejongTlsEnvironmentPostProcessor implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    SejongTlsSupport.ensureLegacyCipherEnabled();
  }
}
