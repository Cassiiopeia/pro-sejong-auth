# 보안 정책

한국어 | [English](#security-policy)

이 라이브러리는 세종대 포털 비밀번호를 다루므로 보안 제보를 중요하게 처리합니다.

## 지원 버전

가장 최근 배포 버전만 보안 수정을 받습니다 ([README](README.md)의 Maven Central, PyPI, npm 배지 참고).

## 취약점 신고

**공개 이슈로 올리지 마세요.** GitHub에서 비공개로 신고해 주세요.

1. 이 레포의 [Security 탭](https://github.com/Cassiiopeia/pro-sejongpeia/security)으로 이동
2. **Report a vulnerability** 클릭 ([바로가기](https://github.com/Cassiiopeia/pro-sejongpeia/security/advisories/new))
3. 문제 내용, 영향 버전, 재현 방법 작성

실제 학번, 비밀번호, 개인정보는 절대 넣지 말고 예시값을 쓰세요.

7일 안에 첫 답변을 드립니다. 수정이 배포되면 제보자를 밝혀 공지합니다(원하면 익명).

## 범위

대상: 이 라이브러리 코드. 예를 들어 로그, 예외, 결과를 통한 비밀번호나 개인정보 노출, 안전하지 않은 TLS 처리, 호출 간 세션 혼선.

대상 아님: 세종대 포털 자체의 취약점. 학교에 신고해 주세요.

---

## Security Policy

This library handles Sejong University portal passwords, so security reports are taken seriously.

- **Supported versions**: only the latest released version (see the Maven Central, PyPI and npm badges in the [README](README.en.md)) receives security fixes.
- **Reporting**: do not open a public issue. Report privately from the [Security tab](https://github.com/Cassiiopeia/pro-sejongpeia/security) via **Report a vulnerability** ([direct link](https://github.com/Cassiiopeia/pro-sejongpeia/security/advisories/new)). Describe the issue, affected version and steps to reproduce.
- Never include real student IDs, passwords or other personal data. Use placeholders.
- You can expect a first response within 7 days. Once a fix is released, the advisory is published with credit to the reporter unless you prefer to stay anonymous.
- **Scope**: this library's code, for example leaking passwords or personal data through logs, exceptions or results, unsafe TLS handling, or session mix-ups between calls. Vulnerabilities in the Sejong University portal itself are out of scope; report those to the university.
