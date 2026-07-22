# InvestLens Backend

보유 주식·ETF와 관련된 금융 뉴스를 수집하고, 한국어 요약·번역 및 뉴스 기반 영향 가능성을 제공하는 Spring Boot API입니다.

개발 및 커밋 규칙은 [`CONTRIBUTING.md`](CONTRIBUTING.md), Codex 역할 분배와 병렬 작업 규칙은
[`AGENTS.md`](AGENTS.md)와 [`docs/agent-team.md`](docs/agent-team.md)를 참고하세요.

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
- 외부 종목 마스터, RSS와 AI는 포트 인터페이스 뒤에 격리했습니다.
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
| GET | `/api/v1/instruments` | 한·미 종목 검색 (`query`, `type`, `market`, `limit`) | Bearer |
| GET | `/api/v1/instruments/{id}` | 종목 상세 | Bearer |
| GET | `/api/v1/instruments/{id}/chart` | 기간별 가격·OHLCV 차트 (`range`) | Bearer |
| GET | `/api/v1/instruments/{id}/news` | 종목 관련 기사 조회 및 과거 기사 초기 적재 | Bearer |
| GET | `/api/v1/instruments/{id}/news/sentiment` | 관련 기사 상승·하락 가능성 집계 | Bearer |
| GET | `/api/v1/portfolio` | 내 포트폴리오 | Bearer |
| POST | `/api/v1/portfolio` | 종목 등록 (`instrumentId`) | Bearer |
| DELETE | `/api/v1/portfolio/{id}` | 종목 삭제 | Bearer |
| GET | `/api/v1/news` | 맞춤 뉴스 (`direction`, `minScore`, page) | Bearer |
| GET | `/api/v1/news/{id}` | 번역·요약·영향 상세 | Bearer |

### 종목 검색

백엔드는 서버 시작 시와 매일 오전 3시(Asia/Seoul)에 외부 종목 목록을 PostgreSQL로 동기화합니다.

- 한국 개별 주식: KRX KIND 상장법인 목록
- 한국 ETF: 네이버 금융 ETF API
- 미국 주식·ETF: Nasdaq Trader의 Nasdaq Listed / Other Listed Symbol Directory

외부 소스가 일시적으로 실패하거나 비정상적으로 적은 목록을 반환하면 기존 종목 마스터를 유지합니다.
상장 목록에 없어진 종목은 비활성화하여 신규 검색 결과에서 제외하되 기존 포트폴리오 연결은 보존합니다.

```http
GET /api/v1/instruments?query=삼성&market=KR&type=STOCK&limit=20
GET /api/v1/instruments?query=QQQ&market=US&type=ETF&limit=20
```

- `market`: `KR` 또는 `US`
- `type`: `STOCK` 또는 `ETF`
- `limit`: 기본 50, 최소 1, 최대 100
- 응답 필드: `id`, `ticker`, `companyName`, `type`, `market`, `logoUrl`, `logoAttributionUrl`

### 종목 로고

`LOGO_DEV_PUBLISHABLE_KEY`를 설정하면 종목 검색과 상세 응답에 Logo.dev CDN의 `logoUrl`을 포함합니다.
미국 종목은 티커로 조회하고 한국 종목은 코스피·코스닥 suffix 오분류를 피하기 위해 회사명으로 조회합니다. 로고가 없는 경우
Logo.dev의 기본 모노그램 이미지가 반환되므로 프론트에서 깨진 이미지가 노출되지 않습니다.

무료 플랜을 상업 서비스에서 사용하는 동안에는 `logoAttributionUrl`을 이용해 화면 하단 등에
`Logos provided by Logo.dev` 링크를 표시해야 합니다. 키가 없거나 연동을 비활성화하면 두 필드는 `null`입니다.

### 종목 가격 차트

종목 상세 차트는 외부 시장 데이터 어댑터를 통해 한국·미국 주식과 ETF의 가격 이력을 제공합니다.
현재 무키 연동 소스를 사용하며, 외부 호출 제한과 장애가 API 전체로 전파되지 않도록 기간별로 30초~1시간 캐시합니다.
정식 서비스 전에는 이용 목적에 맞는 라이선스 시세 공급자로 `InstrumentChartSourcePort` 구현을 교체하세요.

```http
GET /api/v1/instruments/{instrumentId}/chart?range=1M
```

- `range`: `1D`, `1W`, `1M`, `3M`, `1Y`, `5Y`
- 서버가 기간별 간격을 `5m`, `15m`, `1d`, `1wk` 중에서 자동 선택합니다.
- 응답은 현재가, 전일 종가, 등락 금액·등락률, 통화, 시간대, 데이터 지연 시간과 OHLCV 포인트를 포함합니다.

### 종목 관련 기사

종목 상세 화면에서 아래 API를 처음 호출하면 Google News RSS 검색 결과에서 최근 90일 기사를 최대 20건 가져와
중복 URL을 제거하고 해당 종목과 연결한 뒤 반환합니다. 같은 서버 인스턴스에서는 종목별로 30분 동안 외부 검색을
반복하지 않습니다. 기사 본문 전체를 복제하지 않고 RSS가 제공하는 제목·발췌·원문 링크만 저장합니다.

```http
GET /api/v1/instruments/{instrumentId}/news?language=ko&page=0&size=20
```

`language`은 `ko`, `en`, `ja`, `zh`를 지원합니다. Gemini가 활성화되어 있으면 언어별 번역 제목과
2~3문장 요약을 생성하고 DB에 캐시합니다. 응답은 최신순 페이지이며 `originalUrl`, `title`,
`translatedTitle`, `summary`, `language`, `localized`, `publishedAt`, `impacts`를 포함합니다.
`title`과 `originalUrl`은 항상 원문 정보이며 `localized=false`이면 AI 번역이 일시적으로 제공되지 않은 상태입니다.
각 `impacts` 항목의 `aiAnalyzed`가 `true`일 때만 `direction`, `score`, `reason`과
`upProbability`, `downProbability`, `neutralProbability`가 Gemini의 실제 평가입니다. 세 확률은 합계가 100이며,
예상 주가 등락률이 아니라 해당 기사에 대한 단기 시장 반응 가능성입니다.
`false`이면 `NEUTRAL + 1점` fallback이며 `analysisModel`은 비어 있습니다.

종목별 관련 기사 전체의 단기 반응 가능성을 집계하려면 다음 API를 사용합니다.

```http
GET /api/v1/instruments/{instrumentId}/news/sentiment
```

응답의 `upPercentage`, `downPercentage`, `neutralPercentage`는 AI가 분석을 완료한 관련 기사별 확률의
평균값이며 세 값의 합은 100입니다. `analyzedArticleCount`는 실제 AI 분석에 포함된 기사 수이고,
`relatedArticleCount`는 해당 종목과 연결된 기사 영향 수입니다. `aiAnalyzed=false`이면 AI 결과가 없어
퍼센트를 계산하지 못한 상태이므로 0을 실제 하락·상승 확률로 표시하면 안 됩니다.

```json
{
  "aiAnalyzed": true,
  "analyzedArticleCount": 12,
  "relatedArticleCount": 15,
  "upPercentage": 48,
  "downPercentage": 22,
  "neutralPercentage": 30,
  "analysisModel": "gemini-3.5-flash",
  "disclaimer": "최근 관련 기사에 대한 AI 기반 단기 시장 반응 가능성입니다. 주가 예측이나 투자 조언이 아닙니다."
}
```

영향 점수 기준은 다음과 같습니다.

- `1`: 단순 언급 또는 실질 영향 근거가 거의 없음
- `2`: 간접적이거나 제한적인 낮은 영향
- `3`: 영업·수요·비용·규제·재무에 명확하고 의미 있는 영향
- `4`: 주요 사업 또는 재무 결과에 직접적이고 중대한 영향
- `5`: 전사적·존립적이거나 즉각적으로 매우 중대한 사건

```bash
GEMINI_ENABLED=true
GEMINI_API_KEY=<Google AI Studio API key>
GEMINI_MODEL=gemini-3.5-flash
```

Google News RSS 검색은 무키 프로토타입 소스이므로 정식 상용 서비스 전에는 계약된 뉴스 공급자로
`InstrumentNewsSourcePort` 구현을 교체하세요.

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
