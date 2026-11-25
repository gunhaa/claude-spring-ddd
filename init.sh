#!/usr/bin/env bash
# 템플릿을 새 프로젝트로 초기화한다.
#   사용법: ./init.sh <base-package> [project-name]
#   예시:   ./init.sh com.acme.shop            → 프로젝트 이름 shop
#           ./init.sh com.acme.shop shop-api   → 프로젝트 이름 shop-api
#
# 바꾸는 것
#   - Java 패키지 com.example.app → <base-package> (디렉터리 이동 포함)
#   - 메인 클래스 AppApplication → <Pascal(project-name)>Application
#   - Gradle group·rootProject.name, spring.application.name
#   - DB 이름(.env.example·compose 기본값), MySQL 컨테이너 이름
#   - 소스·설정·문서·.claude(스킬·에이전트) 안의 위 이름들 (README.md는 새로 쓸 문서라 제외)
#   - 템플릿 자체의 끝난 계획(plan/archive/*) 삭제. 새 프로젝트의 계획 번호는 001부터 시작한다
# 끝나면 init.sh 자신을 지운다.
set -euo pipefail

OLD_PKG="com.example.app"
OLD_PATH="com/example/app"
OLD_CLASS="AppApplication"

usage() {
  sed -n '2,5p' "$0" | sed 's/^# \{0,1\}//'
  exit 1
}

[[ $# -ge 1 && $# -le 2 ]] || usage

PKG="$1"
if [[ ! "$PKG" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]]; then
  echo "오류: 패키지 이름이 올바르지 않습니다: $PKG (예: com.acme.shop)" >&2
  exit 1
fi

NAME="${2:-${PKG##*.}}"
if [[ ! "$NAME" =~ ^[a-z][a-z0-9-]*$ ]]; then
  echo "오류: 프로젝트 이름은 소문자·숫자·하이픈만 쓸 수 있습니다: $NAME" >&2
  exit 1
fi

cd "$(dirname "$0")"

if [[ ! -d "src/main/java/$OLD_PATH" ]]; then
  echo "오류: src/main/java/$OLD_PATH 가 없습니다. 이미 초기화한 프로젝트로 보입니다." >&2
  exit 1
fi

GROUP="${PKG%.*}"
NEW_PATH="${PKG//.//}"
DB_NAME="${NAME//-/_}"
# shop-api → ShopApiApplication
CLASS="$(echo "$NAME" | perl -pe 's/(^|-)([a-z0-9])/\U$2/g')Application"

echo "패키지      : $OLD_PKG → $PKG"
echo "메인 클래스 : $OLD_CLASS → $CLASS"
echo "프로젝트    : $NAME (group $GROUP, DB $DB_NAME, 컨테이너 $NAME-mysql)"

# 1) 소스 디렉터리 이동 + 클래스 파일 이름 변경
for root in src/main/java src/test/java; do
  # 새 패키지가 옛 패키지의 하위 경로일 수 있으므로 임시 위치를 거쳐 옮긴다
  tmp="$(mktemp -d)"
  mv "$root/$OLD_PATH"/* "$tmp"/
  rmdir -p "$root/$OLD_PATH" 2>/dev/null || true
  mkdir -p "$root/$NEW_PATH"
  mv "$tmp"/* "$root/$NEW_PATH"/
  rmdir "$tmp"
  for f in "$root/$NEW_PATH/$OLD_CLASS"*.java; do
    mv "$f" "${f/$OLD_CLASS/$CLASS}"
  done
done

# 2) 식별자 치환
export OLD_PKG OLD_PATH OLD_CLASS PKG NEW_PATH CLASS GROUP NAME DB_NAME
# import-control.xml 의 정규식에는 점이 이스케이프된 형태(com\.example\.app)로 들어 있다
export OLD_PKG_RE="${OLD_PKG//./\\.}" PKG_RE="${PKG//./\\.}"
replace_ids() {
  perl -pi -e '
    s/\Q$ENV{OLD_PKG}\E/$ENV{PKG}/g;
    s/\Q$ENV{OLD_PKG_RE}\E/$ENV{PKG_RE}/g;
    s/\Q$ENV{OLD_PATH}\E/$ENV{NEW_PATH}/g;
    s/\Q$ENV{OLD_CLASS}\E/$ENV{CLASS}/g;
  ' "$@"
}
# 옛 이름이 들어 있는 파일을 모두 찾는다(고정 목록을 두면 새 문서·스킬이 생길 때 빠진다)
files=()
while IFS= read -r f; do files+=("$f"); done < <(
  grep -rlE 'com\.example\.app|com\\\.example\\\.app|com/example/app|AppApplication' \
    src config .claude AGENTS.md ARCHITECTURE.md CONVENTIONS.md 2>/dev/null || true
)
if (( ${#files[@]} > 0 )); then
  replace_ids "${files[@]}"
fi

perl -pi -e 's/^group = ".*"/group = "$ENV{GROUP}"/' build.gradle.kts
perl -pi -e 's/^rootProject\.name = ".*"/rootProject.name = "$ENV{NAME}"/' settings.gradle.kts
perl -pi -e 's/^(    name: )app$/$1$ENV{NAME}/' src/main/resources/application.yml
perl -pi -e 's/^DB_NAME=app$/DB_NAME=$ENV{DB_NAME}/' .env.example
perl -pi -e '
  s/container_name: app-mysql/container_name: $ENV{NAME}-mysql/;
  s/\$\{DB_NAME:-app\}/\${DB_NAME:-$ENV{DB_NAME}}/;
' compose.yaml
perl -pi -e '
  s/\[app \(Spring Boot\)\]/[$ENV{NAME} (Spring Boot)]/;
  s/\[MySQL app-mysql\]/[MySQL $ENV{NAME}-mysql]/;
' ARCHITECTURE.md

# 3) 템플릿 자체의 끝난 계획 삭제 (plan/archive/.gitkeep은 남긴다)
find plan/archive -mindepth 1 -maxdepth 1 -type d -exec rm -rf {} +

# 4) 자기 자신 삭제
rm -- "$0"

echo
echo "완료. 다음 단계:"
echo "  1. README.md를 이 프로젝트 설명으로 바꾼다."
echo "  2. ./gradlew check 로 확인한다. (Docker 필요, 테스트는 Testcontainers가 MySQL을 띄운다)"
echo "  3. 변경 사항을 커밋한다."
