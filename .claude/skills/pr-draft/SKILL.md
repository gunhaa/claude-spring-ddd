---
name: pr-draft
description: plan-task 계획 하나에 대응하는 GitHub Draft PR을 연다(open)·Ready로 바꾼다(ready). plan을 새로 만든 직후(plan-task new 마지막 단계)와 계획을 archive한 직후(plan-task archive 마지막 단계)에 쓴다. 예: "이 계획 PR 올려줘", "/pr-draft open plan/002-order-cancel", "/pr-draft ready 002".
---

# pr-draft

계획 하나 = 브랜치 하나 = PR 하나. PR은 **계획을 만들 때 Draft로 열고**, 계획을 archive할 때 Ready로 바꾼다.
리뷰어는 코드보다 `plan.md`(설계·결정 기록)를 먼저 보고, task마다 push되는 커밋과 CI 결과로 진행을 따라간다.

| 항목 | 규칙 |
|---|---|
| 브랜치 | `NNN-slug` (계획 폴더 이름과 같게), `main`에서 만든다 |
| PR 제목 | Conventional Commits, 한국어: `feat: <계획 제목>`. squash 머지 시 이 제목이 커밋 제목이 된다. 타입은 CONVENTIONS.md 8절 |
| PR 본문 | `.github/pull_request_template.md`의 절을 채운다. 진행 상태(task 체크리스트)는 복사하지 않고 `task.md` 링크만 둔다 |
| 이슈 연결 | `plan.md`의 `관련 이슈`가 있으면 본문에 `Closes #N` |

**외부에 공개되는 동작(push, PR 생성·변경)은 실행 전에 제목·본문 초안을 사용자에게 보여주고 확인을 받는다.** 한 번 확인하면 그 PR의 open 또는 ready 한 번 전체(push 포함)에 적용된다.

## 0. 공통 확인
1. `gh auth status`로 로그인을 확인한다.
2. `git remote get-url origin`이 없으면 GitHub에 올릴 곳이 없다. 로컬 작업(브랜치·커밋)까지만 하고 멈춘 뒤 이렇게 안내한다: "remote가 없어 push·PR을 건너뜀. `gh repo create`나 `git remote add origin <url>` 후 `/pr-draft open plan/NNN-slug`로 다시 실행".
3. 저장소 URL은 `gh repo view --json url -q .url`로 얻는다. 본문의 파일 링크에 쓴다.

## open: Draft PR 열기
`plan-task new`가 `plan/NNN-slug/{plan.md, task.md}`를 만든 뒤 부른다.

1. **브랜치**
   - 지금 `main`이면: 작업 트리에 이 계획 폴더 말고 다른 변경이 있는지 `git status --short`로 본다. 있으면 멈추고 사용자에게 어떻게 할지 묻는다(다른 작업이 섞이면 안 된다).
   - remote가 있으면 `git pull --ff-only`로 main을 최신으로 맞춘 뒤 `git switch -c NNN-slug`.
   - 이미 `NNN-slug` 브랜치면 그대로 쓴다. 다른 브랜치면 멈추고 묻는다.
2. **커밋**: 계획 파일만 커밋한다.
   `git add plan/NNN-slug && git commit -m "docs: NNN <계획 제목> 계획"`
3. **초안 작성**: `plan.md`에서 채운다.
   - 개요 ← 1. 목표
   - 계획 ← `<repo-url>/blob/NNN-slug/plan/NNN-slug/plan.md`, `.../task.md`, 관련 이슈
   - 주요 변경 ← 3. 설계의 요지 ("구현 전"이라고 적는다)
   - 검증 ← 템플릿 체크박스 그대로 (모두 빈 칸)
   - 리뷰 포인트 ← 4. 결정 기록과 5. 열린 질문. 열린 질문이 있으면 리뷰어가 답할 수 있게 그대로 옮긴다
4. 제목·본문을 사용자에게 보여주고 확인받는다.
5. `git push -u origin NNN-slug` → `gh pr create --draft --base main --title "<제목>" --body-file <임시 파일>`. 본문은 스크래치 디렉터리의 임시 파일로 넘긴다(따옴표·백틱이 깨지지 않게).
6. PR 번호와 URL을 `plan.md`의 `PR` 칸에 적고 커밋·push한다: `docs: NNN PR 링크 기록`.
7. 사용자에게 PR URL을 알린다.

## 진행 중 (tdd-agent-teams가 처리)
task 한 사이클이 끝날 때마다 리드가 커밋하고, 브랜치에 upstream이 있으면 push한다(tdd-agent-teams 4절). PR 본문은 고치지 않는다. 진행 상태는 `task.md`와 커밋 목록, CI 결과가 보여준다.

## ready: 리뷰 요청으로 전환
`plan-task archive`가 최종 검증을 통과하고 폴더를 `plan/archive/`로 옮긴 뒤 부른다.

1. 지금 브랜치가 `NNN-slug`인지, 이 브랜치의 PR이 있는지 `gh pr view --json number,url,isDraft,body`로 확인한다. 없으면 open부터 하라고 안내하고 멈춘다.
2. archive 변경을 커밋한다: `git add -A plan && git commit -m "docs: NNN <계획 제목> 계획 완료"`.
3. **본문 갱신 초안**:
   - 계획 링크를 archive 경로로 바꾼다: `<repo-url>/blob/main/plan/archive/NNN-slug/plan.md` (머지 뒤 영구히 유효한 주소. 머지 전에는 Files changed 탭에서 본다고 한 줄 덧붙인다)
   - 주요 변경 ← 실제로 바뀐 것: `git diff --stat main...HEAD`와 `ARCHITECTURE.md` 변경 절을 Bounded Context·Aggregate·API·설정·마이그레이션 단위로 요약
   - 검증 ← `task.md` 맨 아래 `> 최종 검증:` 줄을 인용하고, 해당하는 체크박스를 채운다. 해당 없는 항목은 `- [x] ~ (해당 없음)`
   - 리뷰 포인트 ← 회고와 결정 기록 중 리뷰어가 판단해야 할 것
4. 초안을 보여주고 확인받는다.
5. `git push` → `gh pr edit <번호> --body-file <임시 파일>` → `gh pr ready <번호>`.
6. `gh pr checks <번호>`로 CI 상태를 한 번 보여준다(기다리지 않는다). 머지는 사용자가 한다. 이 스킬은 머지하지 않는다.
