package kr.suhsaechan.sejongpeia.auth.model;

/**
 * SIS 요청 실행 파라미터 원천값: 세션 식별자 포함이라 출력·로그·결과에 넣지 않음
 * 사용자 번호(INTG_USR_NO), 로그인 일시(LOGIN_DT, 화면 열기 전), 세션 키(RUNNING_SEJONG)
 */
public record SisRunValues(String userNumber, String loginDate, String sessionKey) {

  @Override
  public String toString() {
    return "SisRunValues{hidden}";
  }
}
