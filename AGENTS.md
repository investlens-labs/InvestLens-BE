# InvestLens Backend Agent Instructions

이 저장소에서 작업하는 코딩 에이전트는 아래 규칙을 따릅니다. 세부 역할과 작업 분배 예시는
[`docs/agent-team.md`](docs/agent-team.md), 개발·커밋 컨벤션은 [`CONTRIBUTING.md`](CONTRIBUTING.md)를 기준으로 합니다.

## 기본 실행 원칙

1. 리더는 요구사항을 성공 조건과 검증 방법으로 바꾼 뒤 작업 규모를 판단합니다.
2. 작은 작업은 리더가 단독 처리하고, 독립적인 작업이 둘 이상일 때만 최대 4개 슬롯을 사용합니다.
3. 안전하고 되돌릴 수 있는 로컬 작업은 승인 요청 없이 구현, 테스트, 문서화까지 완료합니다.
4. 에이전트는 지정된 파일 범위만 수정하고, 공유 파일이나 다른 에이전트의 변경이 필요하면 리더에게 보고합니다.
5. API 계약이나 동작을 변경할 때 테스트와 Swagger 설명을 함께 갱신합니다.
6. 비밀키, `.env`, 운영 자격 증명과 Render/GitHub 토큰은 출력하거나 커밋하지 않습니다.
7. 사용자 또는 다른 에이전트의 변경을 임의로 되돌리거나 삭제하지 않습니다.

## 팀 구성

리더는 작업에 필요한 역할만 동적으로 활성화합니다.

| 역할 | 권장 에이전트 | 책임 |
|---|---|---|
| 리더·통합 | leader | 분해, 파일 소유권 배정, 통합, 최종 검증, 커밋·push, Render 배포 |
| API·보안 | executor / debugger | Controller, DTO, application service, JWT, 사용자 소유권, OpenAPI 계약 |
| 데이터·외부연동 | executor / dependency-expert | JPA, PostgreSQL, Flyway, 종목·차트·뉴스·AI 어댑터, 캐시·재시도 |
| 테스트·검증 | test-engineer / verifier | 회귀 테스트, 경계·장애 시나리오, 빌드, 배포 후 독립 검증 |

- 단일 파일 수정, 문서, 단순 버그는 리더 단독으로 처리합니다.
- 두 도메인 이상 또는 구현과 독립 검증을 병렬화할 가치가 있을 때 2~4개 슬롯을 사용합니다.
- 조사만 필요하면 `explore`, 외부 공식 문서가 필요하면 `researcher`, 원인 분석은 `debugger`를 우선합니다.
- 워커는 commit, push, 강제 Git 작업, Render 배포와 운영 환경 변수 변경을 수행하지 않습니다.

## 자동 분류와 실행

1. 리더는 요청을 API, DB, 외부연동, 보안, 운영, 문서의 변경 축으로 분류합니다.
2. 단순 문서 또는 독립 검증 가치가 낮은 단일 파일 변경만 단독 처리합니다.
3. 변경 축이 둘 이상이거나 구현과 검증을 독립시킬 수 있으면 기본적으로 2~4개 슬롯을 활성화합니다.
4. 모든 워커 작업에는 목표, 성공 조건, 소유 파일, 금지 파일, 선행조건, 검증 명령을 포함합니다.
5. 구현 결과는 별도 verifier가 검토하며, verifier 승인 전에는 리더가 commit하지 않습니다.
6. 워커가 실패하면 한 번 범위를 좁혀 재지시하고, 다시 실패하면 리더가 회수하거나 다른 역할로 교체합니다.

## 파일 소유권과 충돌 방지

- API·보안: `auth`, `user`, `portfolio`, 각 도메인의 `presentation`/`api`, `common/security`
- 데이터·외부연동: `instrument`, `ingestion`, `analysis`, `news`, `db/migration`
- 테스트·검증: 대응하는 `src/test` 파일. 제품 코드는 검증 결과를 보고한 뒤 리더가 수정하거나 명시적으로 재배정합니다.
- 리더 예약 공유 파일: `AGENTS.md`, `README.md`, `CONTRIBUTING.md`, `build.gradle`, `settings.gradle`,
  `src/main/resources/application.yml`, `common/config`, `.github/workflows/**`, `Dockerfile`,
  `docker-entrypoint.sh`, `compose.yml`, `render.yaml`, `ApiSmokeIntegrationTest.java`
- Flyway 마이그레이션 번호와 동일 파일 수정은 반드시 직렬화합니다.
- 작업 시작 전에 리더가 수정 가능 경로를 명시하며, 같은 파일을 둘 이상의 에이전트에 동시에 배정하지 않습니다.
- 워커는 종료 시 변경 파일, 구현 요약, 실행·미실행 테스트, API·DB·운영 위험을 리더에게 보고합니다.

## 검증 순서

1. 변경된 동작을 증명하는 가장 작은 대상 테스트
2. 관련 모듈 테스트
3. `./gradlew clean build`
4. `git diff --check`와 `git status --short`
5. 배포 변경이면 GitHub Actions 이미지 발행 성공 확인
6. Render 배포가 `live`인지 확인한 뒤 Health, Swagger, 변경 API와 필요 시 CORS/로그 점검

로컬 표준 검증은 `./scripts/verify.sh`, 배포 후 읽기 전용 운영 검증은
`./scripts/smoke-production.sh`를 사용합니다.

외부 API live 테스트는 일반 테스트와 분리하고, 실행하지 못한 경우 그 범위를 완료 보고에 명시합니다.

## Git·배포 규칙

1. 리더만 변경을 검토하고 목적별로 `<type> : <한국어 요약>` 형식의 커밋을 만듭니다.
2. 검증을 통과한 커밋은 별도 요청 없이 `origin`에 push합니다.
3. 강제 push, 기록 재작성, 기존 변경 삭제는 명시적인 요청 없이는 금지합니다.
4. Render가 컨테이너 이미지 기반이면 GitHub Actions가 생성한 현재 커밋 SHA 이미지를 지정해 배포합니다.
5. 배포 완료 전에는 완료로 보고하지 않으며, 운영 실패 시 로그를 확인하고 수정·재검증합니다.
6. 제품 코드, Flyway, 의존성, Docker 또는 런타임 설정의 master push는 CI가 자동 검증·이미지 발행·Render 배포합니다.
7. 문서·테스트 전용 변경은 제품 동작이 바뀌지 않으면 운영 재배포를 생략할 수 있습니다.
8. 배포 실패 시 직전 live 이미지를 유지하거나 복구하며, 적용된 Flyway는 수정하지 않고 새 migration으로 forward-fix합니다.

## 완료 조건

- 요구사항과 API 계약이 구현되었습니다.
- 대상 테스트와 전체 빌드가 통과했습니다.
- 문서와 Swagger가 실제 동작과 일치합니다.
- 변경이 목적별로 커밋되고 원격 저장소에 반영되었습니다.
- 배포 요청이 포함된 경우 운영 Health와 변경 API까지 검증되었습니다.
- 남은 외부 의존성, 비용, 라이선스 또는 운영 위험을 명시했습니다.
