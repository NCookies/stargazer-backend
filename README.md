# 🌌 Stargazer - 별 관측 적합도 분석 서비스

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.x-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)

> **"오늘 밤, 별 보러 가도 될까?"**  
> 위치 기반 기상·천문·광해 데이터를 종합 분석하여 최적의 별 관측 시간과 장소를 추천하는 **REST API 백엔드** 서비스입니다.

---

## 📖 프로젝트 개요

단순히 날씨가 맑다고 별이 잘 보이는 것은 아닙니다. 달의 밝기(월령), 주변 광해, 대기 투명도 등이 복합적으로 작용합니다.  
Stargazer는 **OpenWeatherMap**, **Google Gemini AI**, **VIIRS 광해 데이터**를 결합해 **0~100점 관측 적합도 점수**와 **AI 코멘트**를 제공하며, **회원·북마크·관측지·오늘의 추천** 기능을 지원합니다.

### 🎯 대상
- 웹/모바일 클라이언트를 위한 **REST API 서버**
- 별 관측·드라이브·스마트폰 천체 촬영에 관심 있는 일반 사용자

---

## 🚀 구현된 핵심 기능

| 구분 | 기능 | 설명 |
|------|------|------|
| **별 관측** | 실시간 분석 | 위도/경도·날짜·시간 기준 관측 적합도 점수, 감점 사유, AI 코멘트 |
| **별 관측** | 주간 예보 | 해당 위치 향후 5일간 밤 시간대 예보 |
| **관측지** | 주변 명소 조회 | 위도/경도·반경(10~500km) 기준 관측지 목록 (공간 인덱스) |
| **인증** | 로그인/회원가입 | 이메일·비밀번호 기반, JWT Access Token + Refresh Token(쿠키) |
| **인증** | 토큰 재발급·로그아웃 | RTR(Refresh Token Rotation), HttpOnly 쿠키 |
| **회원** | 내 정보·이메일 중복 검사 | `GET /me`, `GET /exists/email` |
| **북마크** | CRUD | 관측지(SPOT) 또는 사용자 정의(CUSTOM) 위치 저장·수정·삭제 |
| **추천** | 오늘의 추천 북마크 | 로그인 사용자 북마크 중 오늘 밤 관측 적합도 상위 5곳 |

---

## 🌟 별 관측 점수 산정 기준

전 세계 최적 환경(**몽골 고비 사막, Bortle Class 1, 구름 0%**)을 **100점** 기준으로 하고, 방해 요소를 **절대 평가**로 감점합니다.

### 기본 원칙
- **Base Score:** 100점
- **Cut-off:** 구름 70% 이상 또는 시민박명(태양 고도 -6° 이상) 시 **0점**

### 감점 요인 (요약)
| 요인 | 적용 방식 | 최대 감점 |
|------|------------|-----------|
| 광해 | (Bortle Class - 1) × 6.25 | -50점 |
| 구름 | (구름% - 10), 10% 미만 무시 | 관측 불가 |
| 달 | 달 밝기 × 30 (지평선 위일 때) | -30점 |
| 습도 | (습도% - 60) / 2, 60% 미만 무시 | -20점 |
| 시정 | 10km 미만 -10점, 5km 미만 -20점 | -20점 |

점수 구간 가이드: **90~100** 완벽, **70~89** 국내 최상급, **50~69** 교외 적합, **0~49** 관측 불리.

---

## 🛠️ 기술 스택

| 구분 | 기술 |
|------|------|
| **Language** | Java 17 |
| **Framework** | Spring Boot 4.x, Spring Web MVC, Spring Data JPA, Spring Security |
| **인증** | JWT (Access/Refresh), HttpOnly Cookie, Redis(Refresh Token 저장) |
| **DB** | MySQL 8.0 (Spatial), Redis (캐시·세션) |
| **외부 API** | OpenWeatherMap, Google Gemini |
| **기타** | SunCalc(천문 계산), Hibernate Spatial, Caffeine·Bucket4j(캐시·Rate Limit), SpringDoc OpenAPI |

---

## 🔧 설치 및 실행

### 요구 사항
- Java 17+
- MySQL 8.0+
- Redis (선택: 로컬 기본값 `localhost:6379`)

### 환경 변수

`.env` 또는 `application.yml`에 다음을 설정합니다.

```properties
# DB
MYSQL_ENDPOINT=localhost
MYSQL_USERNAME=stargazer
MYSQL_PASSWORD=your_password

# Redis (선택)
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_KEY=your_jwt_secret_key

# 외부 API
WEATHER_API_KEY=your_openweathermap_key
GEMINI_API_KEY=your_google_gemini_key

# OAuth2 (웹 소셜 로그인용, 앱 API에는 미사용)
# SERVER_BASE_URL, CLIENT_REDIRECT_BASE_URL
# GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET
# KAKAO_CLIENT_ID, KAKAO_CLIENT_SECRET
# NAVER_CLIENT_ID, NAVER_CLIENT_SECRET
```

### 실행

```bash
./gradlew bootRun
```

기본 포트: **8080**

---

## 📡 API 및 문서

- **Base URL (로컬):** `http://localhost:8080`
- **Base URL (운영):** `https://api.byeolbolil.xyz` (배포 환경에 따라 변경 가능)
- **API Prefix:** `/api/v1`

| 문서 | 설명 |
|------|------|
| [Swagger UI](https://api.byeolbolil.xyz/swagger-ui/index.html) | API 명세 및 Try-it-out |
| [OpenAPI JSON](http://localhost:8080/v3/api-docs) | 스펙 다운로드·클라이언트 코드 생성용 |
| [docs/ARCHITECTURE_GUIDE.md](docs/ARCHITECTURE_GUIDE.md) | 4계층 아키텍처·코딩 가이드 |
| [docs/SECURITY_AUTH_GUIDE.md](docs/SECURITY_AUTH_GUIDE.md) | 인증·JWT·쿠키·RTR 정책 |
| [docs/SWAGGER_GUIDE.md](docs/SWAGGER_GUIDE.md) | Swagger/OpenAPI 사용법 |
| [docs/MOBILE_DEVELOPER_GUIDE.md](docs/MOBILE_DEVELOPER_GUIDE.md) | **모바일 앱 연동 가이드** |

---

## 📁 프로젝트 구조 (요약)

```
src/main/java/xyz/ncookie/stargazer/
├── domain/                    # 도메인별 패키지
│   ├── stargazing/             # 별 관측 분석·예보
│   ├── spot/                   # 관측지 조회
│   ├── member/                 # 회원·인증
│   ├── bookmark/               # 북마크
│   └── recommend/              # 오늘의 추천
├── global/                     # 공통 설정·예외·보안
│   ├── security/               # JWT, OAuth2, Cookie
│   ├── exception/
│   └── advice/
infra/                          # Terraform 등 인프라
docs/                           # 설계·가이드 문서
```

---

## 🖼️ 스크린샷

![스크린샷 1](assets/images/39.119.82.172_4000_%20%282%29.png)
![스크린샷 2](assets/images/39.119.82.172_4000_%20%281%29.png)

---

## 📌 관련 링크

- **Notion 기획서:** [링크]
- **API 명세:** Swagger UI 또는 [MOBILE_DEVELOPER_GUIDE.md](docs/MOBILE_DEVELOPER_GUIDE.md) 참고
