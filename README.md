# InvestLens Backend

보유 주식·ETF와 관련된 금융 뉴스를 수집하고, 한국어 요약·번역 및 뉴스 기반 영향 가능성을 제공하는 Spring Boot API입니다.

개발 및 커밋 규칙은 [`CONTRIBUTING.md`](CONTRIBUTING.md)를 참고하세요.

> InvestLens의 영향 방향과 점수는 투자 조언이나 주가 예측이 아닙니다.

## 기술 스택

- Java 17, Spring Boot 3.5
- Spring Web / Validation / Security / Data JPA
- PostgreSQL, Flyway
- springdoc-openapi (Swagger UI)
- Ollama HTTP API (Gemma/Llama 등 로컬 모델)
- Gradle Wrapper, JUnit 5, MockMvc, H2(test)

## 아키텍처

기능 중심 모듈러 모놀리스입니다. 각 기능은 API(또는 presentation), application, domain, infrastructure로 나뉩니다.

```text
com.investlens
├── auth / user          # 회원가입, 로그인, JWT, 사용자
├── instrument           # 주식·ETF 마스터 및 검색
├── portfolio            # 사용자별 관심/보유 종목
├── news                 # 맞춤 피드, 뉴스 상세, 영향 정보
├── ingestion            # 설정된 RSS 수집과 중복 제거
├── analysis             # NewsAnalyzerPort, Ollama 어댑터
└── common               # 오류, 보안, OpenAPI, JPA 설정
```

- Controller는 Repository를 직접 호출하지 않습니다.
- 외부 RSS와 AI는 포트 인터페이스 뒤에 격리했습니다.
- AI 출력의 ticker, enum, score(1~5)를 서버에서 다시 검증합니다.
- 사용자 소유권은 항상 사용자 UUID가 포함된 쿼리로 확인합니다.
- 기사 전문 대신 RSS 발췌와 원문 URL을 중심으로 저장합니다.

## 실행

### 1. PostgreSQL 실행

```bash
docker compose up -d postgres
```

### 2. 환경 변수

`.env.example`을 참고하세요. 운영 환경에서는 `JWT_SECRET`을 반드시 32바이트 이상의 임의 값으로 교체해야 합니다.

### 3. 서버 실행

```bash
export JWT_SECRET='replace-with-at-least-32-random-bytes'
./gradlew bootRun
```

- API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`

## Render 배포

저장소의 `render.yaml` Blueprint는 무료 Render Web Service와 PostgreSQL 16을 함께 생성합니다.

현재 개발 서버는 다음 주소에 배포되어 있습니다.

- API Base URL: `https://investlens-be.onrender.com/api/v1`
- Swagger UI: `https://investlens-be.onrender.com/swagger-ui.html`
- OpenAPI JSON: `https://investlens-be.onrender.com/v3/api-docs`
- Health: `https://investlens-be.onrender.com/actuator/health`

1. [Render Blueprint 생성 화면](https://dashboard.render.com/blueprints)에서 이 저장소를 연결합니다.
2. `render.yaml`을 선택하고 생성되는 리소스를 확인한 뒤 배포합니다.
3. 배포 완료 후 아래 주소로 상태와 API 명세를 확인합니다.

```text
https://<service-name>.onrender.com/actuator/health
https://<service-name>.onrender.com/swagger-ui.html
https://<service-name>.onrender.com/v3/api-docs
```

`JWT_SECRET`과 PostgreSQL 접속 정보는 Render가 생성하며 저장소에 저장되지 않습니다. 프론트엔드 배포 후에는 Render의 `CORS_ALLOWED_ORIGINS`에 실제 프론트엔드 주소를 추가해야 합니다.

무료 Web Service는 유휴 상태에서 중지되어 첫 요청이 느릴 수 있고, 무료 PostgreSQL은 생성 후 30일이 지나면 만료됩니다. 장기 운영 전에는 유료 DB 또는 별도의 영구 DB로 전환하세요.

## API 요약

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| POST | `/api/v1/auth/signup` | 회원가입 | 공개 |
| POST | `/api/v1/auth/login` | JWT 로그인 | 공개 |
| GET | `/api/v1/users/me` | 내 정보 | Bearer |
| GET | `/api/v1/instruments` | 종목 검색 (`query`, `type`) | Bearer |
| GET | `/api/v1/instruments/{id}` | 종목 상세 | Bearer |
| GET | `/api/v1/portfolio` | 내 포트폴리오 | Bearer |
| POST | `/api/v1/portfolio` | 종목 등록 (`instrumentId`) | Bearer |
| DELETE | `/api/v1/portfolio/{id}` | 종목 삭제 | Bearer |
| GET | `/api/v1/news` | 맞춤 뉴스 (`direction`, `minScore`, page) | Bearer |
| GET | `/api/v1/news/{id}` | 번역·요약·영향 상세 | Bearer |

초기 종목 마스터에는 NVDA, AAPL, MSFT, AMZN, GOOGL, META, TSLA, QQQ, VOO, SCHD, SMH가 포함됩니다.

## Ollama 분석 활성화

```bash
ollama pull gemma3:4b
OLLAMA_ENABLED=true OLLAMA_MODEL=gemma3:4b ./gradlew bootRun
```

Ollama가 비활성화된 개발 환경에서는 뉴스가 수집 가능하도록 `local-fallback` 분석기가 중립/낮은 영향으로 표시합니다. 운영 분석에는 반드시 Ollama를 활성화하세요. AI 장애나 잘못된 JSON은 불완전한 impact로 저장하지 않고 뉴스의 분석 상태를 `FAILED`로 남깁니다.

## RSS 수집 설정

피드 URL은 사용자 입력을 받지 않고 서버 설정의 allow-list만 사용합니다. 제공자 이용 약관과 RSS 제공 여부를 확인한 URL만 등록하세요.

```yaml
app:
  news-ingestion:
    enabled: true
    cron: "0 0/30 * * * *"
    feeds:
      - name: your-approved-source
        url: https://approved.example.com/feed.xml
```

각 피드 실패는 다른 피드 수집을 중단하지 않으며, URL 중복 기사와 등록 종목이 언급되지 않은 기사는 저장하지 않습니다.

## 데이터베이스

Flyway가 다음 스키마와 제약을 관리합니다.

- `users`
- `instruments`
- `portfolio_items` — `(user_id, instrument_id)` unique
- `news_articles` — canonical URL hash unique
- `news_related_instruments` — AI 분석 상태와 독립적인 기사-종목 관련성
- `news_impacts` — `(news_id, instrument_id)` unique, score check `1..5`

## 검증

```bash
./gradlew clean test
./gradlew clean build
```

테스트는 JWT 위변조/만료, 포트폴리오 중복·소유권, 영향 점수 경계, RSS XML 보안 파싱, 회원가입→로그인→포트폴리오 통합 흐름, 인증 차단과 OpenAPI 생성을 확인합니다.
