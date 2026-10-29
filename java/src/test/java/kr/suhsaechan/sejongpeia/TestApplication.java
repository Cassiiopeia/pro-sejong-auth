package kr.suhsaechan.sejongpeia;

import kr.suhsaechan.sejongpeia.support.SejongTestAccountProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 테스트용 Spring Boot Application 클래스
 * - 라이브러리 프로젝트이므로 테스트 시에만 사용
 */
@SpringBootApplication
@EnableConfigurationProperties(SejongTestAccountProperties.class)
public class TestApplication {
}
