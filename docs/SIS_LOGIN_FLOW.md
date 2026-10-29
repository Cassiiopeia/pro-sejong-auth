# SIS (Student Information System) Login Flow

English | [한국어](SIS_LOGIN_FLOW.ko.md)

## 📋 Overview

**SIS (Student Information System, 학사정보시스템)** is Sejong University's academic management system. It provides a student's **contact information** (email, phone number, English name).

| Item | Value |
|------|------|
| Domain | `sjpt.sejong.ac.kr` |
| Protocol | HTTPS |
| Authentication | Sejong Portal SSO |
| Data format | JSON |
| API endpoint | `/main/sys/UserInfo/initUserInfo.do` |
| Provided data | Basic student info, contact info |

---

## 🔄 Authentication Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                            SIS Login Flow                               │
└─────────────────────────────────────────────────────────────────────────┘

    Client                Portal                   SSO                    SIS
      │                     │                      │                      │
      │  1. POST login      │                      │                      │
      │ ─────────────────► │                      │                      │
      │   (id, password,   │                      │                      │
      │    rtUrl=sjpt)     │                      │                      │
      │                     │                      │                      │
      │  ◄───────────────── │                      │                      │
      │   Set-Cookie       │                      │                      │
      │   (session cookie) │                      │                      │
      │                     │                      │                      │
      │  2. GET SSO        │                      │                      │
      │ ─────────────────────────────────────────►│                      │
      │   (with cookies)   │                      │                      │
      │                     │                      │                      │
      │  ◄─────────────────────────────────────── │                      │
      │   SSO token issued │                      │                      │
      │                     │                      │                      │
      │  3. POST initUserInfo.do                  │                      │
      │ ─────────────────────────────────────────────────────────────────►│
      │   (cookies + JSON body)                   │                      │
      │                     │                      │                      │
      │  ◄───────────────────────────────────────────────────────────────│
      │   JSON response    │                      │                      │
      │   (student info + contact)                │                      │
      │                     │                      │                      │
```

---

## 📡 HTTP Request/Response Details

### Step 1: Portal Login

Log in to the Sejong University portal. The `rtUrl` parameter sets the redirect target to the SIS SSO page.

**Request**
```http
POST https://portal.sejong.ac.kr/jsp/login/login_action.jsp HTTP/1.1
Host: portal.sejong.ac.kr
Referer: https://portal.sejong.ac.kr
Content-Type: application/x-www-form-urlencoded
Cookie: chknos=false

mainLogin=N&rtUrl=sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p=&id={studentId}&password={password}
```

| Parameter | Required | Description |
|---------|------|------|
| `mainLogin` | O | Main login flag (always "N") |
| `rtUrl` | O | Redirect URL after login (sjpt SSO path) |
| `id` | O | Student ID |
| `password` | O | Password |

**Response**
```http
HTTP/1.1 200 OK
Set-Cookie: JSESSIONID=...; Path=/; HttpOnly
Set-Cookie: ssotoken=...; Domain=.sejong.ac.kr; Path=/
```

---

### Step 2: SSO Page Request

Request the SIS SSO endpoint with the cookies issued by the portal.

**Request**
```http
GET https://sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p= HTTP/1.1
Host: sjpt.sejong.ac.kr
Referer: https://portal.sejong.ac.kr
Cookie: JSESSIONID=...; ssotoken=...
```

**Response**
```http
HTTP/1.1 200 OK
Set-Cookie: SJPT_JSESSIONID=...; Path=/
```

This step creates the session on the SIS site.

---

### Step 3: initUserInfo.do API Call

Call the user info JSON API with the authenticated session.

**Building addParam**

`addParam` is an empty parameter set encoded in Base64.

```java
// Source JSON
String json = "{\"_runIntgUsrNo\":\"\",\"_runPgLoginDt\":\"\",\"_runningSejong\":\"\"}";

// URL encoding
String urlEncoded = URLEncoder.encode(json, StandardCharsets.UTF_8);
// Result: %7B%22_runIntgUsrNo%22%3A%22%22%2C%22_runPgLoginDt%22%3A%22%22%2C%22_runningSejong%22%3A%22%22%7D

// Base64 encoding
String addParam = Base64.getEncoder().encodeToString(urlEncoded.getBytes(StandardCharsets.UTF_8));
// Result: JTdCJTIyX3J1bkludGdVc3JObyUyMiUzQSUyMiUyMiUyQyUyMl9ydW5QZ0xvZ2luRHQlMjIlM0ElMjIlMjIlMkMlMjJfcnVubmluZ1Nlam9uZyUyMiUzQSUyMiUyMiU3RA==
```

**Request**
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

| Header | Value | Description |
|------|-----|------|
| `Accept` | application/json | Requests a JSON response |
| `Content-Type` | application/json; charset=UTF-8 | JSON request body |
| `Referer` | SSO page URL | Previous page reference |
| `Origin` | https://sjpt.sejong.ac.kr | CORS Origin |

**Response (success)**

Values come from the portal in Korean. In the sample below, "홍길동" is a placeholder Korean name and "컴퓨터공학과" means "Department of Computer Science and Engineering".

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

**Response (authentication failure)**
```http
HTTP/1.1 401 Unauthorized
```

---

## 📊 JSON Response Structure

### dm_UserInfo (integrated user info)

Contains basic user information.

| Field | Type | Description | Example |
|------|------|------|------|
| `INTG_USR_NO` | String | Student ID (integrated user number) | "20171234" |
| `INTG_USR_NM` | String | Name (Korean) | "홍길동" |
| `INTG_ENG_NM` | String | Name (English) | "Hong Gildong" |

### dm_UserInfoGam (academic affairs system info)

Contains detailed info such as department and contact details.

| Field | Type | Description | Example |
|------|------|------|------|
| `DEPT_NM` | String | Department name | "컴퓨터공학과" |
| `USER_EMAIL` | String | Email address | "student@example.com" |
| `USER_PHONE_NO1` | String | Phone number, first part | "010" |
| `USER_PHONE_NO2` | String | Phone number, middle part | "1234" |
| `USER_PHONE_NO3` | String | Phone number, last part | "5678" |
| `STATUS_DIV_CD` | String | Enrollment status code | "COA008001" |

**Enrollment status codes (STATUS_DIV_CD)**

| Code | Meaning |
|------|------|
| `COA008001` | Enrolled |
| `COA008002` | On leave of absence |
| `COA008003` | Graduated |
| `COA008004` | Expelled |

### dm_UserInfoSch (academic records system info)

Used as a fallback data source.

| Field | Type | Description | Example |
|------|------|------|------|
| `DEPT_NM` | String | Department name (fallback) | "컴퓨터공학과" |
| `NM_ENG` | String | English name (fallback) | "Hong Gildong" |
| `USER_CLSF_CD_NM` | String | User category (result field `userCategory`) | "학부생" |

---

## 🔍 JSON Parsing Logic

### SejongSisParser Parsing Strategy

**Priority-based data extraction**

```java
// Parse student ID
public String parseStudentId(String json) {
    JsonNode root = objectMapper.readTree(json);
    JsonNode dmUserInfo = root.path("dm_UserInfo");
    return getTextValue(dmUserInfo, "INTG_USR_NO");
}

// Parse name
public String parseName(String json) {
    JsonNode root = objectMapper.readTree(json);
    JsonNode dmUserInfo = root.path("dm_UserInfo");
    return getTextValue(dmUserInfo, "INTG_USR_NM");
}

// Parse department - dm_UserInfoGam.DEPT_NM (primary) → dm_UserInfoSch.DEPT_NM (fallback)
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

**Contact info parsing**

```java
// Parse email
public String parseEmail(String json) {
    JsonNode root = objectMapper.readTree(json);
    JsonNode dmUserInfoGam = root.path("dm_UserInfoGam");
    return getTextValue(dmUserInfoGam, "USER_EMAIL");
}

// Parse phone number
public String parsePhoneNumber(String json) {
    JsonNode root = objectMapper.readTree(json);
    JsonNode dmUserInfoGam = root.path("dm_UserInfoGam");
    return buildPhoneNumber(dmUserInfoGam); // "010-1234-5678"
}

// Parse English name
public String parseEnglishName(String json) {
    JsonNode root = objectMapper.readTree(json);
    JsonNode dmUserInfo = root.path("dm_UserInfo");
    JsonNode dmUserInfoSch = root.path("dm_UserInfoSch");

    // English name: dm_UserInfo.INTG_ENG_NM (primary) → dm_UserInfoSch.NM_ENG (fallback)
    String englishName = getTextValue(dmUserInfo, "INTG_ENG_NM");
    if (isBlank(englishName)) {
        englishName = getTextValue(dmUserInfoSch, "NM_ENG");
    }
    return englishName;
}

// Join phone number parts (NO1-NO2-NO3)
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

## ⚠️ Error Handling

### Authentication Failure Detection

| HTTP status | Meaning | Handling |
|---------------|------|------|
| 401 Unauthorized | Student ID/password mismatch | Throws `AUTHENTICATION_FAILED` |
| 200 (no response body) | Session expired | Throws `DATA_FETCH_FAILED` |
| 500 | Server error | Throws `CONNECTION_FAILED` |
| Timeout | Connection timed out | Throws `CONNECTION_TIMEOUT` |

### JSON Parsing Failure Detection

```java
// dm_UserInfo is required
if (dmUserInfo.isMissingNode()) {
    throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR,
        "dm_UserInfo field not found");
}

// Invalid JSON
try {
    JsonNode root = objectMapper.readTree(json);
} catch (JsonProcessingException e) {
    throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR, e);
}
```

---

## 🛠️ Code Reference

### Related Classes

| Class | Role | Location |
|--------|------|------|
| `SejongSisClient` | Sends HTTP requests | `client/SejongSisClient.java` |
| `SejongSisParser` | Parses JSON | `parser/SejongSisParser.java` |
| `SejongSisAuthResult` | SIS auth result model | `model/SejongSisAuthResult.java` |

### Main URL Constants

```java
// SejongSisClient.java
private static final String PORTAL_LOGIN_URL = "https://portal.sejong.ac.kr/jsp/login/login_action.jsp";
private static final String SJPT_SSO_URL = "https://sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p=";
private static final String SJPT_INIT_USER_INFO_URL = "https://sjpt.sejong.ac.kr/main/sys/UserInfo/initUserInfo.do";
```

---

## 🔄 DHC vs SIS

| Item | DHC | SIS |
|------|-----|-----|
| Domain | classic.sejong.ac.kr | sjpt.sejong.ac.kr |
| Data format | HTML | JSON |
| Student ID | O | O |
| Name | O | O |
| Department | O | O |
| Grade (year) | O | X |
| Enrollment status | O (text) | △ (code) |
| Email | X | O |
| Phone number | X | O |
| English name | X | O |
| Classic reading | O | X |

---

## 🔒 Security Considerations

### SSL Certificate

Like DHC, the SIS server may have SSL certificate issues.

```yaml
sejong:
  auth:
    ssl-verification: false  # disable if needed
```

### Password Security

- The password is sent in the POST request body
- It is encrypted in transit over HTTPS
- Never log the password on the server side

### Session Management

- Cookie-based session management
- A new `CookieManager` is created per request
- No session reuse (a new session for each authentication request)

---

## 📊 Performance

| Item | Value |
|------|-----|
| Average response time | 1-3 s |
| Default timeout | 10 s |
| Retry count | 3 |
| Response size | About 500-1000 bytes (JSON) |

---

## 💡 Usage Tips

### When You Only Need SIS

If you only need contact info, the SIS-only API is faster.

```java
// SIS only (faster)
SejongSisAuthResult result = authEngine.authenticateWithSIS(studentId, password);
String email = result.getEmail();
String phoneNumber = result.getPhoneNumber();
String englishName = result.getEnglishName();

// Integrated call (both DHC and SIS)
SejongAuthResult result = authEngine.authenticate(studentId, password);
// If SIS fails, email, phoneNumber and englishName are null
```

### Handling SIS Failure in Integrated Auth

`authenticate()` returns a result built from DHC data only, even when SIS fails.

```java
SejongAuthResult result = authEngine.authenticate(studentId, password);

if (result.getEmail() != null) {
    // SIS succeeded
    String email = result.getEmail();
} else {
    // SIS failed (DHC data only)
    log.warn("Could not fetch SIS contact info.");
}
```

---

## 🔗 Related Docs

- [DHC Login Flow](DHC_LOGIN_FLOW.md) - Daeyang Humanity College authentication details
- [README](../README.md) - Library usage guide
