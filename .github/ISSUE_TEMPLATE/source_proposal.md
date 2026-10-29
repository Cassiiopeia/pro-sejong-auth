---
name: 🗂️ 새 정보 출처 제안
about: 학교가 공개로 주는 정보를 라이브러리에 추가하자고 제안합니다
labels: [작업전]
assignees: [Cassiiopeia]
---

<!-- 제목 예시: ⚙️[기능추가][API] 도서관 좌석 현황 조회 추가 -->
<!-- 개인정보(학번, 이름, 연락처, 세션 값)와 실제 응답 원문은 붙이지 마세요. 필드 이름과 가상 값만 적어 주세요. -->

📝 어떤 정보인가요
---

- 정보 이름과 쓰임새:
- 출처 위치 (주소):
- 로그인 필요 여부: 필요 없음 / 필요함
- 개인정보 포함 여부: 없음 / 있음 (있다면 어떤 필드)

🛠️ 요청 형태
---

- 응답 형태: JSON / HTML 표 (JSON이 아니고 HTML만 있는 정보는 기본 원본으로 삼지 않습니다)
- 요청 방식과 필수 입력 (예: GET, 날짜 범위):
- 확인한 필드 이름 (값은 가상으로):
- 갱신 주기를 알고 있다면:

⚙️ 작업 내용
---

- [ ] `spec/PUBLIC_DATA.md`에 메서드와 필드 대응 추가 ([CONTRIBUTING](../blob/main/CONTRIBUTING.md#새-정보-출처-추가하기) 절차 따름)
- [ ] 합성 fixture와 기대 결과
- [ ] Java, Python, Node 구현과 fixture 테스트
- [ ] 형태 점검(`scripts/probe-public-data.py`) 항목
- [ ] 노출 체크: README 한·영, `python/README.md`, `node/README.md`, 소개 사이트, 패키지 설명과 키워드, 릴리즈 노트 한 줄
