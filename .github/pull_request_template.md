<!-- 제목은 Conventional Commits 형식, 한국어: feat: 상품 조회 API 추가 (squash 머지 시 커밋 제목이 된다) -->

## 개요
<!-- 이 PR이 머지되면 무엇이 가능해지는지 1~3줄 -->

## 계획
- plan: <!-- plan/NNN-slug/plan.md 링크 -->
- task: <!-- plan/NNN-slug/task.md 링크. 진행 상태는 task.md가 기준이고 여기에 복사하지 않는다 -->
- 관련 이슈: <!-- Closes #N, 없으면 "없음" -->

## 주요 변경
<!-- Bounded Context·Aggregate·API·설정·마이그레이션 단위로 -->
-

## 검증
- [ ] `./gradlew check` 통과 (테스트·ArchUnit·Spotless·Checkstyle)
- [ ] Entity를 바꿨다면 `db/migration`에 Flyway 마이그레이션 추가
- [ ] 패키지·엔티티·API·외부 연동·설정이 바뀌었다면 `ARCHITECTURE.md` 반영

## 리뷰 포인트
<!-- 리뷰어가 특히 봐야 할 설계 결정 (plan.md 결정 기록의 해당 행) -->
