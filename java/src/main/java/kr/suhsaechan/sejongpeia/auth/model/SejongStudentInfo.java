package kr.suhsaechan.sejongpeia.auth.model;

/**
 * DHC 사용자 정보 표 1회 파싱 결과: 학과, 학번, 이름, 학년, 학적 상태
 * 값이 없으면 빈 문자열
 */
public record SejongStudentInfo(
    String major,
    String studentId,
    String name,
    String grade,
    String status
) {
}
