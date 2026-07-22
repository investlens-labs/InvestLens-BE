# InvestLens Backend 에이전트 팀 운영

## 목표

InvestLens Backend의 기능 구현부터 회귀 테스트, Git 반영, Render 배포 검증까지 한 흐름으로 완료하면서
불필요한 병렬화와 파일 충돌을 피합니다. 팀은 고정 인원이 아니라 작업마다 필요한 역할만 활성화하는 동적 구조입니다.

## 기본 팀: 최대 4개 슬롯

### 1. 리더·통합 담당

- 사용자 요구사항을 API 동작, 성공 조건, 검증 명령으로 구체화합니다.
- 작업을 독립적인 파일 범위로 분리하고 각 역할의 소유권을 선언합니다.
- 공유 파일, Git, GitHub Actions, Render 배포와 운영 환경을 단독으로 관리합니다.
- 각 결과를 검토·통합하고 대상 테스트와 전체 빌드를 직접 확인합니다.
- 검증된 변경을 목적별로 커밋하고 `origin`에 push합니다.

### 2. API·인증 담당

주요 범위:

- `auth`, `user`, `portfolio`
- 각 기능의 Controller, request/response DTO, application service
- `common/security`, 예외 응답, OpenAPI 계약

검토 기준:

- 인증 필요 여부와 JWT 실패 응답이 일관적인가
- 사용자 소유 데이터 쿼리에 사용자 UUID가 포함되는가
- 입력 검증, HTTP 상태 코드, 오류 코드와 Swagger가 실제 동작과 일치하는가
- Controller가 Repository 또는 외부 API를 직접 호출하지 않는가

### 3. 데이터·외부연동 담당

주요 범위:

- `instrument`, `ingestion`, `analysis`, `news`
- JPA Repository, JDBC 저장소, Flyway 마이그레이션
- 종목 카탈로그, Yahoo 차트, Google News RSS, Logo.dev, Gemini/Ollama 어댑터
- 캐시, 타임아웃, 재시도, fallback과 스케줄러

검토 기준:

- 외부 연동이 포트 인터페이스 뒤에 격리되는가
- 장애·빈 응답·rate limit이 전체 API 실패로 전파되지 않는가
- 중복 방지, 최신순 정렬, 트랜잭션 경계와 DB 제약이 일치하는가
- 비밀키가 설정으로만 주입되고 로그·응답·저장소에 노출되지 않는가
- 공급자의 이용 약관, attribution, 데이터 지연과 비용 위험이 문서화되는가

### 4. 테스트·독립 검증 담당

- 구현자가 놓치기 쉬운 정상·경계·권한·외부 장애 시나리오를 추가합니다.
- 변경된 계약에 대한 MockMvc 또는 application/domain 테스트를 우선합니다.
- PostgreSQL과 H2 차이, Flyway 적용, 직렬화, 시간대와 정렬을 확인합니다.
- 제품 코드 변경이 필요하면 임의 수정하지 않고 재현 방법과 기대 결과를 리더에게 반환합니다.
- 최종 단계에서 build, Git diff, 배포 상태와 운영 smoke test를 독립 검증합니다.

## 작업 규모별 배치

| 작업 규모 | 조건 | 권장 배치 |
|---|---|---|
| 소형 | 한 도메인, 1~3개 파일, 명확한 수정 | 리더 단독 |
| 중형 | 한 기능의 API와 데이터/테스트가 분리 가능 | 리더 + 구현 1 + 검증 1 |
| 대형 | 두 도메인 이상, DB/API/외부연동 동시 변경 | 리더 + API·인증 + 데이터·외부연동 + 테스트 |
| 장애 대응 | 원인이 불명확하거나 운영에서만 재현 | 리더 + debugger, 필요 시 verifier |
| 외부 API 결정 | SDK·모델·공급자 선택 또는 버전 확인 | 리더 + dependency-expert/researcher + verifier |

에이전트 호출 비용보다 병렬 이득이 작으면 팀을 늘리지 않습니다. 서로 의존하는 작업은 순차 실행합니다.

## 작업별 권장 분배

### 인증 또는 포트폴리오 기능

- API·인증: 구현과 보안 테스트
- 테스트: 다른 사용자 데이터 접근, 중복 등록, 만료·위조 JWT 회귀 검증
- 리더: `ApiSmokeIntegrationTest`, 문서, 통합과 Git

### 종목 검색 또는 차트 기능

- 데이터·외부연동: 카탈로그/시세 포트와 어댑터, 캐시
- API·인증: query parameter와 응답 계약
- 테스트: KR/US, STOCK/ETF, 빈 검색어, 외부 장애와 범위 경계
- 리더: Swagger, 전체 빌드와 배포 smoke test

### 뉴스 수집·번역·영향 분석

- 데이터·외부연동: RSS, 중복 제거, Gemini/Ollama, 번역 캐시와 점수 검증
- API·인증: 언어·페이지 계약과 응답 필드
- 테스트: 최신순, 원문 링크, AI fallback 구분, 잘못된 모델 응답
- 리더: 환경 변수·비용·라이선스 문서와 운영 로그 확인

### DB 스키마 변경

- 리더가 먼저 다음 Flyway 번호를 예약합니다.
- 데이터 담당 한 명만 migration 파일을 작성합니다.
- 테스트 담당은 기존 데이터 호환성과 제약 위반 시나리오를 검증합니다.
- 기존 migration 파일은 수정하지 않고 새 파일을 추가합니다.

### 배포 또는 CORS 변경

- 리더만 `render.yaml`, workflow, 운영 환경과 배포 명령을 수정합니다.
- 테스트 담당은 Health, Swagger, 실제 API, 허용·비허용 Origin을 확인합니다.
- 이미지 기반 Render 서비스는 push 후 생성된 커밋 SHA 태그가 실제 서비스에 반영됐는지 확인합니다.

## 충돌 방지 프로토콜

1. 리더가 각 작업에 목표, 수정 가능 경로, 금지 경로, 검증 명령을 포함합니다.
2. 같은 제품 파일을 두 역할에 동시에 할당하지 않습니다.
3. 공유 파일과 Flyway 번호는 리더가 예약하고 순차 반영합니다.
4. 작업 중 범위가 넓어지면 워커는 수정 전에 리더에게 경계 확장을 보고합니다.
5. 워커는 commit/push/deploy하지 않고 변경 파일, 테스트 결과, 위험을 보고합니다.
6. 리더는 diff를 읽고 통합한 뒤 새 상태에서 다시 테스트합니다.

## 표준 검증 매트릭스

```bash
# 가장 작은 회귀 테스트
./gradlew test --tests '<테스트 클래스>'

# 전체 단위·통합 테스트와 빌드
./gradlew clean build

# 공백 오류와 의도하지 않은 변경 확인
git diff --check
git status --short
```

추가 검증:

- API/보안: MockMvc, 인증 없음·만료·위조·다른 사용자 접근
- DB: Flyway fresh migration, unique/check/FK 제약, PostgreSQL 전용 쿼리
- 외부 API: fixture 기반 테스트를 기본으로 하고 live test는 명시적으로 분리
- 배포: GitHub Actions 성공 → Render `live` → Health → Swagger → 변경 API → 로그
- CORS: 실제 프론트 Origin의 preflight 응답과 `Access-Control-Allow-Origin` 확인

## 리더의 완료 보고 형식

```text
모드/배치: 단독 또는 사용한 역할
변경: 구현·수정 파일과 사용자에게 보이는 결과
검증: 실행한 테스트·빌드·운영 확인 결과
Git: 커밋 해시와 push 여부
배포: Render 상태와 확인한 URL/API
위험: 실행하지 못한 live 검증, 외부 공급자·비용·라이선스 이슈
```

## 금지 사항

- 작업 편의를 위한 보안 비활성화, CORS 전체 허용 또는 비밀키 하드코딩
- 테스트를 통과시키기 위한 실제 계약·검증 우회
- 이미 적용된 Flyway migration 수정
- 워커의 독자적인 commit, push, 강제 Git 작업 또는 운영 배포
- 검증되지 않은 상태를 완료로 보고
