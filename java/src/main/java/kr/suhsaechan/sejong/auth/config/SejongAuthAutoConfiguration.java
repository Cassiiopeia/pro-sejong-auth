package kr.suhsaechan.sejong.auth.config;

import kr.suhsaechan.sejong.auth.client.SejongPortalClient;
import kr.suhsaechan.sejong.auth.client.SejongSisClient;
import kr.suhsaechan.sejong.auth.parser.SejongClassicReadingParser;
import kr.suhsaechan.sejong.auth.parser.SejongPortalMainParser;
import kr.suhsaechan.sejong.auth.parser.SejongSisParser;
import kr.suhsaechan.sejong.auth.parser.SejongStudentInfoParser;
import kr.suhsaechan.sejong.auth.service.SuhSejongAuthEngine;
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
  public SejongPortalMainParser sejongPortalMainParser() {
    return new SejongPortalMainParser();
  }

  @Bean
  @ConditionalOnMissingBean
  public SuhSejongAuthEngine sejongAuthService(
      SejongPortalClient portalClient,
      SejongStudentInfoParser studentInfoParser,
      SejongClassicReadingParser classicReadingParser,
      SejongSisClient sisClient,
      SejongSisParser sisParser,
      SejongPortalMainParser portalMainParser) {
    return new SuhSejongAuthEngine(portalClient, studentInfoParser, classicReadingParser, sisClient, sisParser, portalMainParser);
  }
}
