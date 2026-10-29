# DHC (Daeyang Humanity College) Login Flow Details

[한국어](DHC_LOGIN_FLOW.md) | English

## 📋 Overview

**DHC (Daeyang Humanity College, 대양휴머니티칼리지)** is Sejong University's liberal arts education system. It manages students' **classic reading certification** records.

| Item | Details |
|------|------|
| Domain | `classic.sejong.ac.kr` |
| Protocol | HTTPS |
| Authentication | Sejong Portal SSO |
| Data format | HTML |
| Provided data | Basic student info, classic reading certification status |

---

## 🔄 Authentication Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                            DHC Login Flow                               │
└─────────────────────────────────────────────────────────────────────────┘

  Client                  Portal                   SSO                    DHC
      │                     │                      │                      │
      │  1. POST login      │                      │                      │
      │ ─────────────────►  │                      │                      │
      │   (id, password,    │                      │                      │
      │    rtUrl=classic)   │                      │                      │
      │                     │                      │                      │
      │  ◄───────────────── │                      │                      │
      │   Set-Cookie        │                      │                      │
      │   (session cookie)  │                      │                      │
      │                     │                      │                      │
      │  2. GET SSO         │                      │                      │
      │ ─────────────────────────────────────────► │                      │
      │   (with cookies)    │                      │                      │
      │                     │                      │                      │
      │  ◄──────────────────────────────────────── │                      │
      │   SSO token issued  │                      │                      │
      │                     │                      │                      │
      │  3. GET student info│                      │                      │
      │ ────────────────────────────────────────────────────────────────► │
      │   (with cookies)    │                      │                      │
      │                     │                      │                      │
      │  ◄─────────────────────────────────────────────────────────────── │
      │   HTML response     │                      │                      │
      │   (student info +   │                      │                      │
      │    classic reading) │                      │                      │
      │                     │                      │                      │
```

---

## 📡 HTTP Request/Response Details

### Step 1: Portal Login

Log in to the Sejong University portal and set the redirect target to the DHC site with the `rtUrl` parameter.

**Request**
```http
POST https://portal.sejong.ac.kr/jsp/login/login_action.jsp HTTP/1.1
Host: portal.sejong.ac.kr
Referer: https://portal.sejong.ac.kr
Content-Type: application/x-www-form-urlencoded
Cookie: chknos=false

mainLogin=N&rtUrl=classic.sejong.ac.kr&id={studentId}&password={password}
```

| Parameter | Required | Description |
|---------|------|------|
| `mainLogin` | O | Main login flag (always "N") |
| `rtUrl` | O | Redirect URL after login (classic.sejong.ac.kr) |
| `id` | O | Student ID |
| `password` | O | Password |

**Response**
```http
HTTP/1.1 200 OK
Set-Cookie: JSESSIONID=...; Path=/; HttpOnly
Set-Cookie: ssotoken=...; Domain=.sejong.ac.kr; Path=/
```

**Important**: The cookies set in this step (`JSESSIONID`, `ssotoken`) are sent automatically with every later request.

---

### Step 2: SSO Redirect

Call the DHC SSO endpoint with the cookies issued by the portal.

**Request**
```http
GET https://classic.sejong.ac.kr/userCertified.do HTTP/1.1
Host: classic.sejong.ac.kr
Cookie: JSESSIONID=...; ssotoken=...
```

**Response**
```http
HTTP/1.1 200 OK
Set-Cookie: CLASSIC_JSESSIONID=...; Path=/
```

This step creates the session on the DHC site.

---

### Step 3: Classic Reading Certification Status Page Request

Request the page that contains the student info, using the authenticated session.

**Request**
```http
GET https://classic.sejong.ac.kr/classical/certificationStatus.do HTTP/1.1
Host: classic.sejong.ac.kr
Cookie: CLASSIC_JSESSIONID=...; ssotoken=...
```

**Response (success)**
```http
HTTP/1.1 200 OK
Content-Type: text/html; charset=UTF-8

<!DOCTYPE html>
<html>
<head>...</head>
<body>
  <!-- Student info table -->
  <div class="tbl">
    <table>
      <tr><th>학과</th><td>컴퓨터공학과</td></tr>
      <tr><th>학번</th><td>20171234</td></tr>
      <tr><th>성명</th><td>홍길동</td></tr>
      <tr><th>학년</th><td>4</td></tr>
      <tr><th>상태</th><td>재학</td></tr>
    </table>
  </div>

  <!-- Classic reading certification info -->
  <div class="certifications">...</div>
</body>
</html>
```

The portal returns Korean labels and values. In this sample: `학과` (department), `학번` (student ID), `성명` (name), `학년` (year), `상태` (status), `컴퓨터공학과` (Department of Computer Science and Engineering), `재학` (enrolled). `홍길동` is a placeholder name.

**Response (authentication failure)**
```http
HTTP/1.1 401 Unauthorized
```

---

## 🔍 HTML Parsing Logic

### Basic Student Info Parsing (SejongStudentInfoParser)

This logic extracts student info from the HTML.

**Target HTML structure**
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

`재학상태` (enrollment status) is a header label on the portal page.

**Parsing logic**
```java
// Parse with Jsoup
Document doc = Jsoup.parse(html);

// Select the student info table
Element table = doc.selectFirst("div.tbl table");

// Extract data from each row
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

**Extracted result**
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

### Classic Reading Certification Parsing (SejongClassicReadingParser)

**Target HTML structure**
```html
<!-- Certification status by area -->
<div class="certArea">
  <div class="area1">
    <span class="title">서양의 역사와 사상</span>
    <ul class="books">
      <li>플라톤의 국가 (2023-05-15)</li>
      <li>아리스토텔레스의 정치학 (2023-06-20)</li>
    </ul>
  </div>
  <!-- ... other areas -->
</div>

<!-- Exam history -->
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

The portal returns Korean values. In this sample: `서양의 역사와 사상` (Western History and Thought), `동양의 역사와 사상` (Eastern History and Thought), `플라톤의 국가` (Plato's Republic), `아리스토텔레스의 정치학` (Aristotle's Politics), `합격` (pass).

**Extracted result**
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

## ⚠️ Error Handling

### Detecting Authentication Failure

| HTTP response code | Meaning | Handling |
|---------------|------|------|
| 401 Unauthorized | Student ID/password mismatch | Throws `AUTHENTICATION_FAILED` |
| 200 (empty HTML) | Session expired | Throws `SESSION_ERROR` |
| 500 | Server error | Throws `CONNECTION_FAILED` |
| Timeout | Connection timed out | Throws `CONNECTION_TIMEOUT` |

### Detecting Parsing Failure

```java
// Required element is missing
if (table == null || table.select("tr").isEmpty()) {
    throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR,
        "Student info table not found");
}
```

---

## 🛠️ Code Reference

### Class Mapping

| Class | Role | Location |
|--------|------|------|
| `SejongPortalClient` | Handles HTTP requests | `auth/client/SejongPortalClient.java` |
| `SejongStudentInfoParser` | Parses student info HTML | `auth/parser/SejongStudentInfoParser.java` |
| `SejongClassicReadingParser` | Parses classic reading HTML | `auth/parser/SejongClassicReadingParser.java` |
| `SejongDhcAuthResult` | DHC auth result model | `auth/model/SejongDhcAuthResult.java` |
| `SejongStudentInfo` | Student info model | `auth/model/SejongStudentInfo.java` |
| `SejongClassicReading` | Classic reading info model | `auth/model/SejongClassicReading.java` |

### Configuration Properties

```yaml
sejong:
  auth:
    portal-login-url: https://portal.sejong.ac.kr/jsp/login/login_action.jsp
    sso-redirect-url: https://classic.sejong.ac.kr/userCertified.do
    classic-status-url: https://classic.sejong.ac.kr/classical/certificationStatus.do
```

---

## 🔒 Security Considerations

### SSL Certificate

The Sejong University portal's SSL certificate can have intermittent problems. You can use the `ssl-verification: false` setting in production, but using a trusted certificate is recommended where possible.

### Password Security

- Passwords are processed in memory and discarded immediately
- Passwords are never written to logs
- HTTPS only

### Session Management

- Cookies are managed by a `CookieManager` created per request
- Cookies are discarded automatically after the request completes
- Sessions are not reused

---

## 📊 Performance

| Item | Value |
|------|-----|
| Average response time | 2-4 seconds |
| Default timeout | 10 seconds |
| Retry count | 3 |
| HTTP connection | No Keep-Alive (new connection per request) |

---

## 🔗 Related Documents

- [SIS Login Flow](SIS_LOGIN_FLOW.en.md) - SIS (academic information system) authentication details
- [README](../README.en.md) - Library usage guide
