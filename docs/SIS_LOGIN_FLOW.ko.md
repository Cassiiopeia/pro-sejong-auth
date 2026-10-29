# SIS (학사정보시스템) 로그인 플로우 상세 문서

[English](SIS_LOGIN_FLOW.md) | 한국어

## 📋 개요

**SIS (Student Information System, 학사정보시스템)**는 세종대학교의 학사 관리 시스템으로, 학생의 **연락처 정보**(이메일, 전화번호, 영어이름)를 제공합니다.

| 항목 | 내용 |
|------|------|
| 도메인 | `sjpt.sejong.ac.kr` |
| 프로토콜 | HTTPS |
| 인증 방식 | 세종포털 SSO |
| 데이터 형식 | JSON |
| API 엔드포인트 | `/main/sys/UserInfo/initUserInfo.do` |
| 제공 정보 | 학생 기본정보, 연락처 정보 |

---

## 🔄 인증 흐름 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         SIS 로그인 플로우                                 │
└─────────────────────────────────────────────────────────────────────────┘

  클라이언트                포털                    SSO                    SIS
      │                     │                      │                      │
      │  1. POST 로그인     │                      │                      │
      │ ─────────────────► │                      │                      │
      │   (id, password,   │                      │                      │
      │    rtUrl=sjpt)     │                      │                      │
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
      │  3. POST initUserInfo.do                  │                      │
      │ ─────────────────────────────────────────────────────────────────►│
      │   (쿠키 + JSON Body)│                      │                      │
      │                     │                      │                      │
      │  ◄───────────────────────────────────────────────────────────────│
      │   JSON 응답         │                      │                      │
      │   (학생정보+연락처) │                      │                      │
      │                     │                      │                      │
```

---

## 📡 상세 HTTP 요청/응답

### Step 1: 포털 로그인

세종대학교 포털에 로그인하고, `rtUrl` 파라미터로 SIS SSO 페이지로 리다이렉트를 지정합니다.

**요청**
```http
POST https://portal.sejong.ac.kr/jsp/login/login_action.jsp HTTP/1.1
Host: portal.sejong.ac.kr
Referer: https://portal.sejong.ac.kr
Content-Type: application/x-www-form-urlencoded
Cookie: chknos=false

mainLogin=N&rtUrl=sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p=&id={학번}&password={비밀번호}
```

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `mainLogin` | O | 메인 로그인 여부 (항상 "N") |
| `rtUrl` | O | 로그인 후 리다이렉트 URL (sjpt SSO 경로) |
| `id` | O | 학번 |
| `password` | O | 비밀번호 |

**응답**
```http
HTTP/1.1 200 OK
Set-Cookie: JSESSIONID=...; Path=/; HttpOnly
Set-Cookie: ssotoken=...; Domain=.sejong.ac.kr; Path=/
```

---

### Step 2: SSO 페이지 접근

포털에서 발급받은 쿠키로 SIS의 SSO 엔드포인트에 접근합니다.

**요청**
```http
GET https://sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p= HTTP/1.1
Host: sjpt.sejong.ac.kr
Referer: https://portal.sejong.ac.kr
Cookie: JSESSIONID=...; ssotoken=...
```

**응답**
```http
HTTP/1.1 200 OK
Set-Cookie: SJPT_JSESSIONID=...; Path=/
```

이 단계에서 SIS 사이트의 세션이 생성됩니다.

---

### Step 3: initUserInfo.do API 호출

인증된 세션으로 사용자 정보 JSON API를 호출합니다.

**addParam 생성**

`addParam`은 빈 파라미터를 Base64로 인코딩한 값입니다.

```java
// 원본 JSON
String json = "{\"_runIntgUsrNo\":\"\",\"_runPgLoginDt\":\"\",\"_runningSejong\":\"\"}";

// URL 인코딩
String urlEncoded = URLEncoder.encode(json, StandardCharsets.UTF_8);
// 결과: %7B%22_runIntgUsrNo%22%3A%22%22%2C%22_runPgLoginDt%22%3A%22%22%2C%22_runningSejong%22%3A%22%22%7D

// Base64 인코딩
String addParam = Base64.getEncoder().encodeToString(urlEncoded.getBytes(StandardCharsets.UTF_8));
// 결과: JTdCJTIyX3J1bkludGdVc3JObyUyMiUzQSUyMiUyMiUyQyUyMl9ydW5QZ0xvZ2luRHQlMjIlM0ElMjIlMjIlMkMlMjJfcnVubmluZ1Nlam9uZyUyMiUzQSUyMiUyMiU3RA==
```

**요청**
```http
POST https://sjpt.sejong.ac.kr/main/sys/UserInfo/initUserInfo.do?addParam={addParam} HTTP/1.1
Host: sjpt.sejong.ac.kr
Accept: application/json
Content-Type: application/json; charset=UTF-8
Referer: https://sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p=
Origin: https://sjpt.sejong.ac.kr
Cookie: SJPT_JSESSIONID=...; ssotoken=...

{}
```

| 헤더 | 값 | 설명 |
|------|-----|------|
| `Accept` | application/json | JSON 응답 요청 |
| `Content-Type` | application/json; charset=UTF-8 | JSON 요청 본문 |
| `Referer` | SSO 페이지 URL | 이전 페이지 참조 |
| `Origin` | https://sjpt.sejong.ac.kr | CORS Origin |

**응답 (성공 시)**
```http
HTTP/1.1 200 OK
Content-Type: application/json; charset=UTF-8

{
  "dm_UserInfo": {
    "INTG_USR_NO": "20171234",
    "INTG_USR_NM": "홍길동",
    "INTG_ENG_NM": "Hong Gildong"
  },
  "dm_UserInfoGam": {
    "DEPT_NM": "컴퓨터공학과",
    "USER_EMAIL": "student@example.com",
    "USER_PHONE_NO1": "010",
    "USER_PHONE_NO2": "1234",
    "USER_PHONE_NO3": "5678",
    "STATUS_DIV_CD": "COA008001"
  },
  "dm_UserInfoSch": {
    "DEPT_NM": "컴퓨터공학과",
    "NM_ENG": "Hong Gildong"
  }
}
```

**응답 (인증 실패 시)**
```http
HTTP/1.1 401 Unauthorized
```

---

## 📊 JSON 응답 구조 상세

### dm_UserInfo (통합 사용자 정보)

기본적인 사용자 정보를 포함합니다.

| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| `INTG_USR_NO` | String | 학번 (통합사용자번호) | "20171234" |
| `INTG_USR_NM` | String | 이름 (한글) | "홍길동" |
| `INTG_ENG_NM` | String | 이름 (영문) | "Hong Gildong" |

### dm_UserInfoGam (교무 시스템 정보)

학과, 연락처 등 상세 정보를 포함합니다.

| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| `DEPT_NM` | String | 학과명 | "컴퓨터공학과" |
| `USER_EMAIL` | String | 이메일 주소 | "student@example.com" |
| `USER_PHONE_NO1` | String | 전화번호 앞자리 | "010" |
| `USER_PHONE_NO2` | String | 전화번호 중간자리 | "1234" |
| `USER_PHONE_NO3` | String | 전화번호 뒷자리 | "5678" |
| `STATUS_DIV_CD` | String | 재학상태 코드 | "COA008001" |

**재학상태 코드 (STATUS_DIV_CD)**

| 코드 | 의미 |
|------|------|
| `COA008001` | 재학 |
| `COA008002` | 휴학 |
| `COA008003` | 졸업 |
| `COA008004` | 제적 |

### dm_UserInfoSch (학사 시스템 정보)

백업 정보 소스로 사용됩니다.

| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| `DEPT_NM` | String | 학과명 (백업) | "컴퓨터공학과" |
| `NM_ENG` | String | 영문 이름 (백업) | "Hong Gildong" |
| `USER_CLSF_CD_NM` | String | 사용자 구분 (결과의 `userCategory`) | "학부생" |

---

## 🔍 JSON 파싱 로직

### SejongSisParser 파싱 전략

**우선순위 기반 데이터 추출**

```java
// 학번 파싱
public String parseStudentId(String json) {
    JsonNode root = objectMapper.readTree(json);
    JsonNode dmUserInfo = root.path("dm_UserInfo");
    return getTextValue(dmUserInfo, "INTG_USR_NO");
}

// 이름 파싱
public String parseName(String json) {
    JsonNode root = objectMapper.readTree(json);
    JsonNode dmUserInfo = root.path("dm_UserInfo");
    return getTextValue(dmUserInfo, "INTG_USR_NM");
}

// 학과 파싱 - dm_UserInfoGam.DEPT_NM (우선) → dm_UserInfoSch.DEPT_NM (백업)
public String parseMajor(String json) {
    JsonNode root = objectMapper.readTree(json);
    JsonNode dmUserInfoGam = root.path("dm_UserInfoGam");
    JsonNode dmUserInfoSch = root.path("dm_UserInfoSch");

    String major = getTextValue(dmUserInfoGam, "DEPT_NM");
    if (isBlank(major)) {
        major = getTextValue(dmUserInfoSch, "DEPT_NM");
    }
    return major;
}
```

**연락처 정보 파싱**

```java
// 이메일 파싱
public String parseEmail(String json) {
    JsonNode root = objectMapper.readTree(json);
    JsonNode dmUserInfoGam = root.path("dm_UserInfoGam");
    return getTextValue(dmUserInfoGam, "USER_EMAIL");
}

// 전화번호 파싱
public String parsePhoneNumber(String json) {
    JsonNode root = objectMapper.readTree(json);
    JsonNode dmUserInfoGam = root.path("dm_UserInfoGam");
    return buildPhoneNumber(dmUserInfoGam); // "010-1234-5678"
}

// 영어 이름 파싱
public String parseEnglishName(String json) {
    JsonNode root = objectMapper.readTree(json);
    JsonNode dmUserInfo = root.path("dm_UserInfo");
    JsonNode dmUserInfoSch = root.path("dm_UserInfoSch");

    // 영어이름: dm_UserInfo.INTG_ENG_NM (우선) → dm_UserInfoSch.NM_ENG (백업)
    String englishName = getTextValue(dmUserInfo, "INTG_ENG_NM");
    if (isBlank(englishName)) {
        englishName = getTextValue(dmUserInfoSch, "NM_ENG");
    }
    return englishName;
}

// 전화번호 조합 (NO1-NO2-NO3)
private String buildPhoneNumber(JsonNode dmUserInfoGam) {
    String no1 = getTextValue(dmUserInfoGam, "USER_PHONE_NO1");
    String no2 = getTextValue(dmUserInfoGam, "USER_PHONE_NO2");
    String no3 = getTextValue(dmUserInfoGam, "USER_PHONE_NO3");

    if (isBlank(no1) && isBlank(no2) && isBlank(no3)) {
        return "";
    }

    StringBuilder sb = new StringBuilder();
    if (hasText(no1)) sb.append(no1);
    if (hasText(no2)) {
        if (sb.length() > 0) sb.append("-");
        sb.append(no2);
    }
    if (hasText(no3)) {
        if (sb.length() > 0) sb.append("-");
        sb.append(no3);
    }

    return sb.toString(); // "010-1234-5678"
}
```

---

## ⚠️ 에러 처리

### 인증 실패 감지

| HTTP 응답 코드 | 의미 | 처리 |
|---------------|------|------|
| 401 Unauthorized | 학번/비밀번호 불일치 | `AUTHENTICATION_FAILED` 예외 발생 |
| 200 (응답 본문 없음) | 세션 만료 | `DATA_FETCH_FAILED` 예외 발생 |
| 500 | 서버 오류 | `CONNECTION_FAILED` 예외 발생 |
| Timeout | 연결 시간 초과 | `CONNECTION_TIMEOUT` 예외 발생 |

### JSON 파싱 실패 감지

```java
// dm_UserInfo 필수 확인
if (dmUserInfo.isMissingNode()) {
    throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR,
        "dm_UserInfo 필드를 찾을 수 없습니다.");
}

// 유효하지 않은 JSON
try {
    JsonNode root = objectMapper.readTree(json);
} catch (JsonProcessingException e) {
    throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR, e);
}
```

---

## 🛠️ 코드 레퍼런스

### 관련 클래스 매핑

| 클래스 | 역할 | 위치 |
|--------|------|------|
| `SejongSisClient` | HTTP 요청 처리 | `client/SejongSisClient.java` |
| `SejongSisParser` | JSON 파싱 | `parser/SejongSisParser.java` |
| `SejongSisAuthResult` | SIS 인증 결과 모델 | `model/SejongSisAuthResult.java` |

### 주요 URL 상수

```java
// SejongSisClient.java
private static final String PORTAL_LOGIN_URL = "https://portal.sejong.ac.kr/jsp/login/login_action.jsp";
private static final String SJPT_SSO_URL = "https://sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p=";
private static final String SJPT_INIT_USER_INFO_URL = "https://sjpt.sejong.ac.kr/main/sys/UserInfo/initUserInfo.do";
```

---

## 🔄 DHC vs SIS 비교

| 항목 | DHC | SIS |
|------|-----|-----|
| 도메인 | classic.sejong.ac.kr | sjpt.sejong.ac.kr |
| 데이터 형식 | HTML | JSON |
| 학번 | O | O |
| 이름 | O | O |
| 학과 | O | O |
| 학년 | O | X |
| 재학상태 | O (텍스트) | △ (코드) |
| 이메일 | X | O |
| 전화번호 | X | O |
| 영어이름 | X | O |
| 고전독서 | O | X |

---

## 🔒 보안 고려사항

### SSL 인증서

SIS 서버도 DHC와 마찬가지로 SSL 인증서 문제가 발생할 수 있습니다.

```yaml
sejong:
  auth:
    ssl-verification: false  # 필요 시 비활성화
```

### 비밀번호 보안

- 비밀번호는 POST 요청 본문에 포함
- HTTPS로 암호화되어 전송
- 서버 측 로깅에 비밀번호 노출 금지

### 세션 관리

- 쿠키 기반 세션 관리
- 요청별 새로운 `CookieManager` 생성
- 세션 재사용 없음 (각 인증 요청마다 새 세션)

---

## 📊 성능 특성

| 항목 | 값 |
|------|-----|
| 평균 응답 시간 | 1-3초 |
| 타임아웃 기본값 | 10초 |
| 재시도 횟수 | 3회 |
| 응답 크기 | 약 500-1000 bytes (JSON) |

---

## 💡 사용 팁

### SIS만 필요한 경우

연락처 정보만 필요한 경우 SIS 전용 API를 사용하면 성능이 향상됩니다.

```java
// SIS만 호출 (더 빠름)
SejongSisAuthResult result = authEngine.authenticateWithSIS(studentId, password);
String email = result.getEmail();
String phoneNumber = result.getPhoneNumber();
String englishName = result.getEnglishName();

// 통합 호출 (DHC + SIS 모두)
SejongAuthResult result = authEngine.authenticate(studentId, password);
// SIS 실패 시 email, phoneNumber, englishName은 null
```

### 통합 인증에서 SIS 실패 처리

`authenticate()` 메서드는 SIS가 실패해도 DHC 정보만으로 결과를 반환합니다.

```java
SejongAuthResult result = authEngine.authenticate(studentId, password);

if (result.getEmail() != null) {
    // SIS 성공
    String email = result.getEmail();
} else {
    // SIS 실패 (DHC 정보만 사용)
    log.warn("SIS 연락처 정보를 가져오지 못했습니다.");
}
```

---

## 🔗 관련 문서

- [DHC 로그인 플로우](DHC_LOGIN_FLOW.md) - 대양휴머니티칼리지 인증 상세
- [README](../README.md) - 라이브러리 사용 가이드
