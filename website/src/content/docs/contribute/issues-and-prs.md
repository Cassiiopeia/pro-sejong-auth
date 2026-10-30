---
title: 이슈와 PR
description: 이슈를 먼저 올리는 방법, 올리면 안 되는 정보, 라벨과 Discussions, 보안 신고, PR 규칙, 라이선스를 정리합니다.
---

버그 제보, 포털 변경 대응, 문서 개선 모두 환영합니다. 이슈와 PR은 한국어, 영어 모두 괜찮습니다.

## 이슈 먼저

- 버그: 에러 코드(`SejongAuthException.getErrorCode()` 등)와 사용한 메서드, 언어와 버전을 적어 주세요.
- 포털이 바뀌어 인증이 깨졌다면 [라이브 데모](../../#demo)로 먼저 재현되는지 확인해 주세요.
- 큰 변경은 PR 전에 이슈로 방향을 맞춥니다.
- 새 학교 정보 출처는 `새 정보 출처 제안` 템플릿을 씁니다. 절차는 [새 정보 출처 추가하기](../add-data-source/)에 있습니다.

:::danger
학번, 비밀번호, 이름, 연락처, 세션 값 같은 개인정보와 실제 응답 원문은 이슈, PR, 댓글 어디에도 올리지 마세요. 필드 이름과 가상 값만 쓰세요.
:::

## 라벨

| 라벨 | 용도 |
|---|---|
| `good first issue` | 처음 시작하는 사람에게 맞는 이슈 |
| `help wanted` | 도움이 필요한 이슈 |
| `data-source` | 새 학교 정보 출처 제안 |

## 어디에 올리나

| 하고 싶은 일 | 가는 곳 |
|---|---|
| 사용법 질문, 제안, 새 정보 출처 아이디어 | [Discussions](https://github.com/Cassiiopeia/pro-sejongpeia/discussions) |
| 버그 제보 (재현할 수 있는 문제) | [이슈](https://github.com/Cassiiopeia/pro-sejongpeia/issues/new/choose) |
| 보안 취약점 | 공개 이슈 대신 [비공개 신고](https://github.com/Cassiiopeia/pro-sejongpeia/security/advisories/new) |
| 직접 고치거나 추가하고 싶음 | [CONTRIBUTING.md](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/CONTRIBUTING.md) |

올리기 전에 [FAQ](../../faq/)와 [에러 코드](../../auth/error-codes/)를 먼저 확인해 주세요. 이 프로젝트는 개인 오픈소스이고 답변은 자원봉사로 이뤄지므로 시간이 걸릴 수 있습니다.

## 보안 제보

이 라이브러리는 세종대 포털 비밀번호를 다루므로 보안 제보를 중요하게 처리합니다. **공개 이슈로 올리지 마세요.**

1. 레포의 [Security 탭](https://github.com/Cassiiopeia/pro-sejongpeia/security)으로 이동합니다.
2. **Report a vulnerability**를 누릅니다.
3. 문제 내용, 영향 버전, 재현 방법을 적습니다. 실제 학번, 비밀번호, 개인정보는 넣지 않습니다.

7일 안에 첫 답변을 드립니다. 가장 최근 배포 버전만 보안 수정을 받습니다. 대상은 이 라이브러리 코드입니다. 예를 들어 로그, 예외, 결과를 통한 비밀번호나 개인정보 노출, 안전하지 않은 TLS 처리, 호출 간 세션 혼선입니다. 자세한 내용은 [SECURITY.md](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/SECURITY.md)입니다.

## PR 규칙

- 브랜치는 `main` 기준으로 만듭니다.
- 동작이 바뀌면 테스트를 함께 추가합니다. 파서 변경은 `spec/fixtures/`에 샘플을 두고 모든 언어가 같은 기대값을 쓰게 합니다.
- 포털이 바뀐 경우 세 언어를 한 PR에서 같이 고칩니다. 자세한 순서는 [레포 구조](../structure/)에 있습니다.
- 로그와 예외 메시지에 비밀번호나 개인정보가 남지 않는지 확인합니다.
- 새 기능이면 기능 노출 체크리스트를 확인합니다.

## AI·에이전트로 만든 PR

코딩 에이전트가 쓴 PR도 환영합니다. PR을 올리는 사람이 레포의 [AGENTS.md](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/AGENTS.md)에 적힌 명령을 직접 실행해 통과를 확인하고, PR 설명에 사용한 에이전트를 적어 주세요. 개인정보와 학교의 실제 응답은 넣지 않고(합성 값만), 검토 기준은 사람이 쓴 PR과 같습니다.

## 행동 강령과 라이선스

참여자는 [행동 강령](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/CODE_OF_CONDUCT.md)을 따릅니다. 기여한 코드는 [MIT 라이선스](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/LICENSE)로 배포됩니다.
