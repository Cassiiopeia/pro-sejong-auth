package kr.suhsaechan.sejongpeia.config;

import kr.suhsaechan.sejongpeia.client.SejongPortalClient;
import kr.suhsaechan.sejongpeia.client.SejongSisClient;
import kr.suhsaechan.sejongpeia.parser.SejongClassicReadingParser;
import kr.suhsaechan.sejongpeia.parser.SejongSisParser;
import kr.suhsaechan.sejongpeia.parser.SejongStudentInfoParser;
import kr.suhsaechan.sejongpeia.service.SuhSejongAuthEngine;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 세종대학교 인증 라이브러리 자동 설정
 * - Spring Boot Auto Configuration을 통해 Bean 자동 등록
 */
@AutoConfiguration
@EnableConfigurationProperties(SejongAuthProperties.class)
@ConditionalOnClass(SuhSejongAuthEngine.class)
public class SejongAuthAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public SejongPortalClient sejongPortalClient(SejongAuthProperties properties) {
    return new SejongPortalClient(properties);
  }

  @Bean
  @ConditionalOnMissingBean
  public SejongSisClient sejongSisClient(SejongAuthProperties properties) {
    return new SejongSisClient(properties);
  }

  @Bean
  @ConditionalOnMissingBean
  public SejongStudentInfoParser sejongStudentInfoParser() {
    return new SejongStudentInfoParser();
  }

  @Bean
  @ConditionalOnMissingBean
  public SejongClassicReadingParser sejongClassicReadingParser() {
    return new SejongClassicReadingParser();
  }

  @Bean
  @ConditionalOnMissingBean
  public SejongSisParser sejongSisParser() {
    return new SejongSisParser();
  }

  @Bean
  @ConditionalOnMissingBean
  public SuhSejongAuthEngine sejongAuthService(
      SejongPortalClient portalClient,
      SejongStudentInfoParser studentInfoParser,
      SejongClassicReadingParser classicReadingParser,
      SejongSisClient sisClient,
      SejongSisParser sisParser) {
    return new SuhSejongAuthEngine(portalClient, studentInfoParser, classicReadingParser, sisClient, sisParser);
  }
}
