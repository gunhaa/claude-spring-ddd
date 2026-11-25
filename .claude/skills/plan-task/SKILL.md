---
name: plan-task
description: 이 프로젝트의 계획을 plan.md + task.md 한 쌍으로 만들고, 갱신하고, 끝나면 plan/archive로 옮긴다. 새 기능·작업 단위를 계획할 때, 작업 진행 상태를 체크할 때, 계획을 마무리·보관할 때 사용. 예: "OO 기능 계획 세워줘", "task 업데이트해줘", "이 계획 아카이브해줘", "/plan-task new 상품 조회 API", "/plan-task new 주문 취소 500 수정 #12".
---

# plan-task

계획은 **작업 단위마다 폴더 하나**에 `plan.md`(무엇·왜)와 `task.md`(실행 체크리스트) 한 쌍으로 관리한다.

```
plan/
├── 001-<slug>/         # 진행 중인 계획
│   ├── plan.md
│   └── task.md
├── 002-<slug>/
└── archive/            # 끝난 계획 보관
    └── 00N-<slug>/
```

## 규칙
- 폴더 이름: `NNN-<kebab-case-slug>` (예: `001-product-api`). 번호는 `plan/`과 `plan/archive/`를 **모두** 확인해 가장 큰 번호 + 1, 세 자리로 맞춘다.
- 템플릿: 이 스킬 폴더의 `templates/plan.md`, `templates/task.md`를 복사해서 채운다. `{{...}}` 자리표시자는 하나도 남기지 않는다. 내용을 모르는 칸은 지우지 말고 "미정"으로 둔다.
- 날짜는 절대 날짜(`YYYY-MM-DD`)로 적는다.
- 상태 표기: `[ ]` 할 일 · `[~]` 진행 중 · `[x]` 완료 · `[-]` 취소(이유를 한 줄로 덧붙임).
- 내용은 한국어로 쓰고, 코드 식별자와 기술 용어는 원문 그대로 둔다.

## 동작

### new: 새 계획 만들기
1. 다음 번호와 slug를 정한다. slug가 애매하면 사용자에게 제안하고 진행한다.
2. `plan/NNN-<slug>/`에 두 템플릿을 복사해 채운다.
   - `plan.md`: 목표, 범위(포함·제외), 설계, 결정 기록, 열린 질문
   - `task.md`: `plan.md`의 설계를 **TDD 한 사이클(Red→Green→Refactor)로 끝낼 수 있는 크기**의 작업으로 쪼갠다. 단계(Phase)로 묶고, 각 단계에 완료 기준을 적는다.
   - 코드 구현은 `tdd-agent-teams` 스킬로 진행한다. 이 스킬은 작업을 만들고 기록만 한다.
3. 상태는 `진행 중`, 시작일은 오늘로 적는다. 인자에 이슈 번호(`#N`)가 있으면 `관련 이슈`에 적고, 이슈 본문(`gh issue view N`)을 목표·범위·완료 기준의 출발점으로 쓴다.
4. **Draft PR**: `pr-draft` 스킬의 open을 실행한다(브랜치 `NNN-slug` 생성 → 계획 커밋 → push → `gh pr create --draft`). remote가 없으면 브랜치·커밋까지만 한다.

### update: 진행 상태 갱신
- 작업이 끝나면 `task.md`의 체크박스를 바꾸고, 필요하면 `> 메모:` 줄로 결과(예: 테스트 통과, 커밋)를 남긴다.
- 설계 결정이 생기면 `plan.md`의 결정 기록 표에 날짜·결정·이유를 추가한다.
- 작업 중 새로 할 일이 생기면 알맞은 단계에 `[ ]`로 추가한다. 몰래 범위를 넓히지 말고 사용자에게 알린다.

### archive: 완료된 계획 보관
1. `task.md`에 `[ ]` 또는 `[~]`가 남아 있으면 **옮기기 전에** 사용자에게 보여주고, 다른 계획으로 넘길지 `[-]` 취소로 닫을지 묻는다.
2. **최종 검증**: `./gradlew check`를 직접 실행한다(테스트·ArchUnit·Spotless·Checkstyle, 기준은 `AGENTS.md` 1절 ⑤). 판정 단계이므로 `spotlessApply`는 붙이지 않는다.
   - 하나라도 실패하면 **archive를 멈추고** 실패 내용을 사용자에게 보여준다. 고칠 일은 `task.md`에 `[ ]`로 추가한다. 서식만 틀렸으면 `spotlessApply`로 고치고 바뀐 파일을 알린 뒤 `check`를 다시 실행한다.
   - `build/test-results/test/TEST-*ArchitectureTest.xml`에서 ArchUnit 규칙이 실제로 실행됐는지(`tests` > 0, `failures`·`errors` = 0) 확인한다.
   - 통과하면 `task.md` 맨 아래에 `> 최종 검증: <날짜> ./gradlew check 통과, 테스트 <n>/<n>`을 남긴다.
3. `plan.md`의 상태를 `완료`로 바꾸고 완료일을 적는다. 회고 칸(잘된 점, 아쉬운 점, 다음 계획으로 넘길 것)을 채운다.
4. 루트의 `ARCHITECTURE.md`가 이 계획으로 바뀐 구조를 모두 반영하는지 코드와 대조해 확인한다. 빠진 게 있으면 현재 상태로 고친다.
5. 폴더를 통째로 `plan/archive/`로 옮긴다: `mv plan/NNN-<slug> plan/archive/`. 번호는 바꾸지 않는다.
6. **PR Ready**: `pr-draft` 스킬의 ready를 실행한다(archive 커밋 → push → PR 본문에 실제 변경·최종 검증 반영 → `gh pr ready`). 머지는 사용자가 한다.

### status: 현황 보기
`plan/*/task.md`를 읽고 계획마다 완료 수/전체 수, 진행 중인 항목을 짧게 요약한다. `archive/`는 사용자가 요청할 때만 포함한다.
