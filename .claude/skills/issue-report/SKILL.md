---
name: issue-report
description: 문제(버그, 테스트 실패, 운영 에러)나 작업 제안을 .github/ISSUE_TEMPLATE 양식에 맞춰 GitHub 이슈로 작성한다. traceId·에러 응답·실패 출력을 모아 채우고, 중복을 확인한 뒤 확인을 받아 등록한다. 예: "이거 이슈로 남겨줘", "/issue-report bug 주문 취소 시 500", "/issue-report task 상품 검색".
---

# issue-report

이슈는 `.github/ISSUE_TEMPLATE/`의 두 양식 중 하나로 쓴다. 웹에서 쓰든 이 스킬로 쓰든 절이 같아야 한다.

| 종류 | 양식 | 라벨 | 쓰는 경우 |
|---|---|---|---|
| `bug` | `bug.yml` | `bug` | 기대와 다르게 동작함, 테스트·CI 실패, 운영 에러 |
| `task` | `task.yml` | `enhancement` | 새 기능·개선·리팩터링 제안. 나중에 `/plan-task new`의 출발점 |

종류를 주지 않았으면 대화 내용으로 고르고, 애매하면 묻는다.

## 1. 정보 모으기 (bug)
대화와 저장소에서 찾고, 없는 것만 사용자에게 묻는다. 추측으로 채우지 않는다.

| 양식 칸 | 찾는 곳 |
|---|---|
| 요약 | 사용자 설명, 실패한 테스트의 `@DisplayName` |
| Bounded Context | 스택 트레이스·실패 테스트의 패키지 `com.example.app.<context>`. 공통 기반이면 `common` |
| 환경 | local / prod / CI / 테스트 |
| 재현 절차 | 요청(메서드·경로·본문) 또는 `./gradlew test --tests '<클래스>'` |
| 기대 결과 | 완료 기준, `@DisplayName`, API 문서(ARCHITECTURE.md 6절) |
| 실제 결과 | 에러 응답 본문(ProblemDetail의 `code`·`detail`), 테스트 실패 메시지(`build/test-results/test/TEST-*.xml`) |
| traceId | 에러 응답의 `traceId` 또는 `X-Request-Id` 헤더. HTTP 요청이 아니면 "없음" |
| 관련 로그 | 로그에서 `trace.id`가 같은 줄만 골라 넣는다 |
| 원인 추정·참고 | 확인한 사실과 추정을 구분해 적는다("추정:"으로 시작) |

- **비밀 값은 지운다**: 비밀번호, 토큰, `.env` 값, 개인정보. 확신이 없으면 `***`로 가린다.
- 로그와 출력은 필요한 부분만 넣는다(긴 스택 트레이스는 처음 원인 줄과 `Caused by` 줄 위주).

## 2. 정보 모으기 (task)
목표·Bounded Context·범위(포함/제외)·완료 기준을 채운다. 완료 기준은 검증 가능한 문장으로 쓴다(tdd-agent-teams 0단계와 같은 기준). 새 Bounded Context가 필요하면 그렇다고 적는다(AGENTS.md 6절: 사용자 합의 필요).

## 3. 중복 확인
`gh issue list --state open --search "<핵심 단어>"`로 열린 이슈를 찾는다. 같은 문제가 있으면 새로 만들지 않고 그 이슈 번호를 보여주며 댓글로 덧붙일지 묻는다(`gh issue comment`).

## 4. 등록
1. `git remote get-url origin`이 없으면 등록하지 않는다. 초안을 스크래치 디렉터리에 파일로 저장하고 경로를 알려준 뒤 멈춘다.
2. 본문은 양식 칸 이름을 `### <칸 이름>` 제목으로 써서 웹 양식으로 만든 이슈와 같은 모양으로 만든다(칸 순서도 양식과 같게). `render: text` 칸은 코드 블록으로 감싼다.
3. 제목은 한 줄 요약(한국어). 접두어 없이 쓴다.
4. **제목·본문 초안을 사용자에게 보여주고 확인받는다.** 이슈 등록은 외부에 공개되는 동작이다.
5. `gh issue create --title "<제목>" --body-file <임시 파일> --label <라벨>`. 라벨이 저장소에 없어 실패하면 `--label` 없이 다시 만들고 그렇게 했다고 알린다.
6. 이슈 URL을 알린다. 고칠 일이면 다음 단계를 제안한다: `/plan-task new <제목> #<번호>` (계획의 `관련 이슈`에 기록되고, Draft PR 본문에 `Closes #<번호>`가 들어가 머지 시 이슈가 닫힌다).
