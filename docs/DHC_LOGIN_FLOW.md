# DHC (대양휴머니티칼리지) 로그인 플로우 상세 문서

한국어 | [English](DHC_LOGIN_FLOW.en.md)

## 📋 개요

**DHC (Daeyang Humanity College, 대양휴머니티칼리지)**는 세종대학교의 교양 교육 시스템으로, 학생들의 **고전독서 인증** 정보를 관리합니다.

| 항목 | 내용 |
|------|------|
| 도메인 | `classic.sejong.ac.kr` |
| 프로토콜 | HTTPS |
| 인증 방식 | 세종포털 SSO |
| 데이터 형식 | HTML |
| 제공 정보 | 학생 기본정보, 고전독서 인증 현황 |

---

## 🔄 인증 흐름 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         DHC 로그인 플로우                                 │
└─────────────────────────────────────────────────────────────────────────┘

  클라이언트                포털                    SSO                    DHC
      │                     │                      │                      │
      │  1. POST 로그인     │                      │                      │
      │ ─────────────────► │                      │                      │
      │   (id, password,   │                      │                      │
      │    rtUrl=classic)  │                      │                      │
      │                     │                      │                      │
      │  ◄───────────────── │                      │                      │
      │   Set-Cookie       │                      │                      │
      │   (세션쿠키)        │                      │                      │
      │                     │                      │                      │
      │  2. GET SSO        │                      │                      │
      │ ─────────────────────────────────────────►│                      │
      │   (쿠키 포함)       │                      │                      │
      │                     │                      │                      │
      │  ◄─────────────────────────────────────── │                      │
      │   SSO 토큰 발급     │                      │                      │
      │                     │                      │                      │
      │  3. GET 학생정보    │                      │                      │
      │ ─────────────────────────────────────────────────────────────────►│
      │   (쿠키 포함)       │                      │                      │
      │                     │                      │                      │
      │  ◄───────────────────────────────────────────────────────────────│
      │   HTML 응답         │                      │                      │
      │   (학생정보+고전독서)│                      │                      │
      │                     │                      │                      │
```

---

## 📡 상세 HTTP 요청/응답

### Step 1: 포털 로그인

세종대학교 포털에 로그인하고, `rtUrl` 파라미터로 DHC 사이트로 리다이렉트를 지정합니다.

**요청**
```http
POST https://portal.sejong.ac.kr/jsp/login/login_action.jsp HTTP/1.1
Host: portal.sejong.ac.kr
Referer: https://portal.sejong.ac.kr
Content-Type: application/x-www-form-urlencoded
Cookie: chknos=false

mainLogin=N&rtUrl=classic.sejong.ac.kr&id={학번}&password={비밀번호}
```

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `mainLogin` | O | 메인 로그인 여부 (항상 "N") |
| `rtUrl` | O | 로그인 후 리다이렉트 URL (classic.sejong.ac.kr) |
| `id` | O | 학번 |
| `password` | O | 비밀번호 |

**응답**
```http
HTTP/1.1 200 OK
Set-Cookie: JSESSIONID=...; Path=/; HttpOnly
Set-Cookie: ssotoken=...; Domain=.sejong.ac.kr; Path=/
```

**중요**: 이 단계에서 설정된 쿠키(`JSESSIONID`, `ssotoken`)가 이후 모든 요청에 자동으로 포함됩니다.

---

### Step 2: SSO 리다이렉트

포털에서 발급받은 쿠키로 DHC의 SSO 엔드포인트에 접근합니다.

**요청**
```http
GET https://classic.sejong.ac.kr/userCertified.do HTTP/1.1
Host: classic.sejong.ac.kr
Cookie: JSESSIONID=...; ssotoken=...
```

**응답**
```http
HTTP/1.1 200 OK
Set-Cookie: CLASSIC_JSESSIONID=...; Path=/
```

이 단계에서 DHC 사이트의 세션이 생성됩니다.

---

### Step 3: 고전독서인증현황 페이지 요청

인증된 세션으로 학생 정보가 포함된 페이지를 요청합니다.

**요청**
```http
GET https://classic.sejong.ac.kr/classical/certificationStatus.do HTTP/1.1
Host: classic.sejong.ac.kr
Cookie: CLASSIC_JSESSIONID=...; ssotoken=...
```

**응답 (성공 시)**
```http
HTTP/1.1 200 OK
Content-Type: text/html; charset=UTF-8

<!DOCTYPE html>
<html>
<head>...</head>
<body>
  <!-- 학생 정보 테이블 -->
  <div class="tbl">
    <table>
      <tr><th>학과</th><td>컴퓨터공학과</td></tr>
      <tr><th>학번</th><td>20171234</td></tr>
      <tr><th>성명</th><td>홍길동</td></tr>
      <tr><th>학년</th><td>4</td></tr>
      <tr><th>상태</th><td>재학</td></tr>
    </table>
  </div>

  <!-- 고전독서 인증 정보 -->
  <div class="certifications">...</div>
</body>
</html>
```

**응답 (인증 실패 시)**
```http
HTTP/1.1 401 Unauthorized
```

---

## 🔍 HTML 파싱 로직

### 학생 기본정보 파싱 (SejongStudentInfoParser)

HTML에서 학생 정보를 추출하는 로직입니다.

**대상 HTML 구조**
```html
<div class="tbl">
  <table>
    <tbody>
      <tr>
        <th>학과</th>
        <td>컴퓨터공학과</td>
        <th>학번</th>
        <td>20171234</td>
      </tr>
      <tr>
        <th>성명</th>
        <td>홍길동</td>
        <th>학년</th>
        <td>4</td>
      </tr>
      <tr>
        <th>재학상태</th>
        <td>재학</td>
      </tr>
    </tbody>
  </table>
</div>
```

**파싱 로직**
```java
// Jsoup을 사용한 파싱
Document doc = Jsoup.parse(html);

// 학생정보 테이블 선택
Element table = doc.selectFirst("div.tbl table");

// 각 행에서 데이터 추출
Elements rows = table.select("tr");
for (Element row : rows) {
    Elements headers = row.select("th");
    Elements values = row.select("td");

    for (int i = 0; i < headers.size(); i++) {
        String header = headers.get(i).text();
        String value = values.get(i).text();

        switch (header) {
            case "학과" -> major = value;
            case "학번" -> studentId = value;
            case "성명" -> name = value;
            case "학년" -> grade = value;
            case "재학상태" -> status = value;
        }
    }
}
```

**추출 결과**
```java
SejongStudentInfo {
    major: "컴퓨터공학과",
    studentId: "20171234",
    name: "홍길동",
    grade: "4",
    status: "재학"
}
```

---

### 고전독서 인증정보 파싱 (SejongClassicReadingParser)

**대상 HTML 구조**
```html
<!-- 영역별 인증 현황 -->
<div class="certArea">
  <div class="area1">
    <span class="title">서양의 역사와 사상</span>
    <ul class="books">
      <li>플라톤의 국가 (2023-05-15)</li>
      <li>아리스토텔레스의 정치학 (2023-06-20)</li>
    </ul>
  </div>
  <!-- ... 다른 영역들 -->
</div>

<!-- 시험 응시 이력 -->
<div class="examHistory">
  <table>
    <tr>
      <td>2023-05-15</td>
      <td>플라톤의 국가</td>
      <td>합격</td>
    </tr>
  </table>
</div>
```

**추출 결과**
```java
SejongClassicReading {
    certifications: [
        {area: "서양의 역사와 사상", books: ["플라톤의 국가", "아리스토텔레스의 정치학"]},
        {area: "동양의 역사와 사상", books: [...]},
        // ...
    ],
    examRecords: [
        {date: "2023-05-15", book: "플라톤의 국가", result: "합격"},
        // ...
    ],
    contestRecords: [...]
}
```

---

## ⚠️ 에러 처리

### 인증 실패 감지

| HTTP 응답 코드 | 의미 | 처리 |
|---------------|------|------|
| 401 Unauthorized | 학번/비밀번호 불일치 | `AUTHENTICATION_FAILED` 예외 발생 |
| 200 (빈 HTML) | 세션 만료 | `SESSION_ERROR` 예외 발생 |
| 500 | 서버 오류 | `CONNECTION_FAILED` 예외 발생 |
| Timeout | 연결 시간 초과 | `CONNECTION_TIMEOUT` 예외 발생 |

### 파싱 실패 감지

```java
// 필수 요소가 없는 경우
if (table == null || table.select("tr").isEmpty()) {
    throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR,
        "학생 정보 테이블을 찾을 수 없습니다.");
}
```

---

## 🛠️ 코드 레퍼런스

### 관련 클래스 매핑

| 클래스 | 역할 | 위치 |
|--------|------|------|
| `SejongPortalClient` | HTTP 요청 처리 | `client/SejongPortalClient.java` |
| `SejongStudentInfoParser` | 학생정보 HTML 파싱 | `parser/SejongStudentInfoParser.java` |
| `SejongClassicReadingParser` | 고전독서 HTML 파싱 | `parser/SejongClassicReadingParser.java` |
| `SejongDhcAuthResult` | DHC 인증 결과 모델 | `model/SejongDhcAuthResult.java` |
| `SejongStudentInfo` | 학생정보 모델 | `model/SejongStudentInfo.java` |
| `SejongClassicReading` | 고전독서 정보 모델 | `model/SejongClassicReading.java` |

### 설정 프로퍼티

```yaml
sejong:
  auth:
    portal-login-url: https://portal.sejong.ac.kr/jsp/login/login_action.jsp
    sso-redirect-url: https://classic.sejong.ac.kr/userCertified.do
    classic-status-url: https://classic.sejong.ac.kr/classical/certificationStatus.do
```

---

## 🔒 보안 고려사항

### SSL 인증서

세종대학교 포털의 SSL 인증서는 간헐적으로 문제가 발생할 수 있습니다. 프로덕션 환경에서는 `ssl-verification: false` 설정을 사용할 수 있지만, 가능하면 신뢰할 수 있는 인증서를 사용하는 것이 권장됩니다.

### 비밀번호 보안

- 비밀번호는 메모리에서 즉시 처리 후 삭제
- 로그에 비밀번호 노출 금지
- HTTPS 통신만 사용

### 세션 관리

- 쿠키는 요청별로 생성되는 `CookieManager`에서 관리
- 요청 완료 후 쿠키 자동 폐기
- 세션 재사용 없음

---

## 📊 성능 특성

| 항목 | 값 |
|------|-----|
| 평균 응답 시간 | 2-4초 |
| 타임아웃 기본값 | 10초 |
| 재시도 횟수 | 3회 |
| HTTP 연결 | Keep-Alive 미사용 (요청별 새 연결) |

---

## 🔗 관련 문서

- [SIS 로그인 플로우](SIS_LOGIN_FLOW.md) - 학사정보시스템 인증 상세
- [README](../README.md) - 라이브러리 사용 가이드
