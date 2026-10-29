package kr.suhsaechan.sejong.auth.parser;

import kr.suhsaechan.sejong.auth.exception.SejongAuthException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SejongSisParser 단위 테스트
 * - initUserInfo.do API 응답 JSON 파싱 테스트
 */
@Slf4j
class SejongSisParserTest {

  private SejongSisParser parser;

  @BeforeEach
  void setUp() {
    parser = new SejongSisParser();
  }

  @Test
  void 학생정보_파싱_성공() {
    log.info("============ 학생정보 파싱 성공 테스트 ============");

    // Given
    String json = """
        {
          "dm_UserInfo": {
            "INTG_USR_NO": "20171234",
            "INTG_USR_NM": "홍길동",
            "INTG_ENG_NM": "Hong Gildong"
          },
          "dm_UserInfoGam": {
            "DEPT_NM": "컴퓨터공학과",
            "USER_EMAIL": "test@example.com"
          },
          "dm_UserInfoSch": {
            "NM_ENG": "Hong Gildong"
          }
        }
        """;

    // When
    String studentId = parser.parseStudentId(json);
    String name = parser.parseName(json);
    String major = parser.parseMajor(json);

    // Then
    assertEquals("20171234", studentId);
    assertEquals("홍길동", name);
    assertEquals("컴퓨터공학과", major);

    log.info("파싱 결과: studentId={}, name={}, major={}", studentId, name, major);
  }

  @Test
  void 연락처정보_파싱_성공() {
    log.info("============ 연락처정보 파싱 성공 테스트 ============");

    // Given
    String json = """
        {
          "dm_UserInfo": {
            "INTG_USR_NO": "20171234",
            "INTG_USR_NM": "홍길동",
            "INTG_ENG_NM": "Hong Gildong"
          },
          "dm_UserInfoGam": {
            "DEPT_NM": "컴퓨터공학과",
            "USER_EMAIL": "test@example.com",
            "USER_PHONE_NO1": "010",
            "USER_PHONE_NO2": "1234",
            "USER_PHONE_NO3": "5678"
          },
          "dm_UserInfoSch": {
            "NM_ENG": "Hong Gildong"
          }
        }
        """;

    // When
    String email = parser.parseEmail(json);
    String phoneNumber = parser.parsePhoneNumber(json);
    String englishName = parser.parseEnglishName(json);

    // Then
    assertEquals("Hong Gildong", englishName);
    assertEquals("test@example.com", email);
    assertEquals("010-1234-5678", phoneNumber);

    log.info("파싱 결과: email={}, phoneNumber={}, englishName={}", email, phoneNumber, englishName);
  }

  @Test
  void 학번_파싱_dm_UserInfo_없음() {
    log.info("============ 학번 파싱 실패 - dm_UserInfo 없음 ============");

    // Given
    String json = """
        {
          "dm_UserInfoGam": {
            "DEPT_NM": "컴퓨터공학과"
          }
        }
        """;

    // When & Then
    SejongAuthException exception = assertThrows(SejongAuthException.class, () -> {
      parser.parseStudentId(json);
    });

    log.info("예외 발생 확인: {}", exception.getMessage());
  }

  @Test
  void 학번_파싱_빈_JSON() {
    log.info("============ 학번 파싱 실패 - 빈 JSON ============");

    // Given
    String json = "{}";

    // When & Then
    SejongAuthException exception = assertThrows(SejongAuthException.class, () -> {
      parser.parseStudentId(json);
    });

    log.info("예외 발생 확인: {}", exception.getMessage());
  }

  @Test
  void 학생정보_파싱_유효하지않은_JSON() {
    log.info("============ 학생정보 파싱 실패 - 유효하지 않은 JSON ============");

    // Given
    String invalidJson = "{ invalid json }";

    // When & Then
    SejongAuthException exception = assertThrows(SejongAuthException.class, () -> {
      parser.parseStudentId(invalidJson);
    });

    log.info("예외 발생 확인: {}", exception.getMessage());
  }

  @Test
  void 연락처정보_파싱_전화번호_부분누락() {
    log.info("============ 연락처정보 파싱 - 전화번호 부분 누락 ============");

    // Given
    String json = """
        {
          "dm_UserInfo": {
            "INTG_ENG_NM": "Hong Gildong"
          },
          "dm_UserInfoGam": {
            "USER_EMAIL": "test@example.com",
            "USER_PHONE_NO1": "010"
          }
        }
        """;

    // When
    String email = parser.parseEmail(json);
    String phoneNumber = parser.parsePhoneNumber(json);
    String englishName = parser.parseEnglishName(json);

    // Then
    assertEquals("Hong Gildong", englishName);
    assertEquals("test@example.com", email);
    assertEquals("010", phoneNumber);

    log.info("파싱 결과: email={}, phoneNumber={}, englishName={}", email, phoneNumber, englishName);
  }

  @Test
  void 연락처정보_파싱_영어이름_dm_UserInfoSch에서_가져오기() {
    log.info("============ 연락처정보 파싱 - 영어이름 백업 소스 ============");

    // Given - dm_UserInfo에 INTG_ENG_NM 없음, dm_UserInfoSch.NM_ENG 사용
    String json = """
        {
          "dm_UserInfo": {
            "INTG_USR_NO": "20171234"
          },
          "dm_UserInfoGam": {
            "USER_EMAIL": "test@example.com"
          },
          "dm_UserInfoSch": {
            "NM_ENG": "Hong Gildong Backup"
          }
        }
        """;

    // When
    String englishName = parser.parseEnglishName(json);

    // Then
    assertEquals("Hong Gildong Backup", englishName);

    log.info("파싱 결과: englishName={}", englishName);
  }

  @Test
  void 학과_파싱_dm_UserInfoSch에서_가져오기() {
    log.info("============ 학과 파싱 - 학과명 백업 소스 ============");

    // Given - dm_UserInfoGam에 DEPT_NM 없음, dm_UserInfoSch.DEPT_NM 사용
    String json = """
        {
          "dm_UserInfo": {
            "INTG_USR_NO": "20171234",
            "INTG_USR_NM": "홍길동"
          },
          "dm_UserInfoGam": {
            "USER_EMAIL": "test@example.com"
          },
          "dm_UserInfoSch": {
            "DEPT_NM": "소프트웨어학과"
          }
        }
        """;

    // When
    String major = parser.parseMajor(json);

    // Then
    assertEquals("소프트웨어학과", major);

    log.info("파싱 결과: major={}", major);
  }

  @Test
  void 연락처정보_파싱_모든필드_빈값() {
    log.info("============ 연락처정보 파싱 - 모든 필드 빈값 ============");

    // Given
    String json = """
        {
          "dm_UserInfo": {},
          "dm_UserInfoGam": {},
          "dm_UserInfoSch": {}
        }
        """;

    // When
    String email = parser.parseEmail(json);
    String phoneNumber = parser.parsePhoneNumber(json);
    String englishName = parser.parseEnglishName(json);

    // Then
    assertEquals("", englishName);
    assertEquals("", email);
    assertEquals("", phoneNumber);

    log.info("파싱 결과: email={}, phoneNumber={}, englishName={}", email, phoneNumber, englishName);
  }

  @Test
  void 사용자구분_파싱_성공() {
    String json = """
        {
          "dm_UserInfo": {},
          "dm_UserInfoSch": {
            "USER_CLSF_CD_NM": "학부생"
          }
        }
        """;

    assertEquals("학부생", parser.parseUserCategory(json));
  }

  @Test
  void 사용자구분_파싱_값없음은_빈문자열() {
    String json = """
        {
          "dm_UserInfo": {},
          "dm_UserInfoSch": {}
        }
        """;

    assertEquals("", parser.parseUserCategory(json));
  }
}
