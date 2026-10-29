package kr.suhsaechan.sejongpeia.support;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 실제 포털 연동 테스트 계정 (application-dev.yml, gitignore 대상)
 * sejongpeia.test.student-id / sejongpeia.test.password
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "sejongpeia.test")
public class SejongTestAccountProperties {

  /** 테스트용 학번 */
  private String studentId;

  /** 테스트용 비밀번호 */
  private String password;
}
