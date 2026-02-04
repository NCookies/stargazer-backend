# Stargazer 모바일 앱 개발자 가이드

이 문서는 Stargazer 백엔드 API에 **iOS / Android** 네이티브 앱을 연동할 때 필요한 정보를 정리한 것입니다.

---

## 목차

1. [개요](#1-개요)
2. [인증 (Authentication)](#2-인증-authentication)
3. [공통 응답 형식](#3-공통-응답-형식)
4. [API 엔드포인트 요약](#4-api-엔드포인트-요약)
5. [요청/응답 스키마 요약](#5-요청응답-스키마-요약)
6. [에러 처리](#6-에러-처리)
7. [모바일 구현 시 필수 사항](#7-모바일-구현-시-필수-사항)
8. [OpenAPI / 클라이언트 코드 생성](#8-openapi--클라이언트-코드-생성)

---

## 1. 개요

### Base URL
- **로컬:** `http://localhost:8080`
- **운영:** `https://api.byeolbolil.xyz` (배포 환경에 따라 변경 가능)
- **[Swagger UI](https://api.byeolbolil.xyz/swagger-ui/index.html)**

### API 버전
- 모든 API prefix: **`/api/v1`**
- 예: `GET /api/v1/analyze`, `POST /api/v1/auth/login`

### 인증 방식
- **Access Token:** JWT, 요청 시 `Authorization: Bearer {accessToken}` 헤더에 포함
- **Refresh Token:** HttpOnly Cookie로 주고받음 (재발급·로그아웃 시 서버가 `Set-Cookie`로 설정)

### Content-Type
- 요청: `application/json` (Body가 있는 경우)
- 응답: `application/json`

---

## 2. 인증 (Authentication)

### 2.1 로그인
- **POST** `/api/v1/auth/login`
- **인증:** 불필요
- **Body (JSON):**
  - `email` (string, 필수): 이메일
  - `password` (string, 필수): 비밀번호
- **응답:**
  - **200:** Body에 `accessToken` (string). 동시에 `Set-Cookie` 헤더로 `refresh_token` 쿠키 설정
  - **401:** 이메일/비밀번호 불일치

### 2.2 회원가입
- **POST** `/api/v1/auth/register`
- **인증:** 불필요
- **Body (JSON):**
  - `email` (string, 필수): 이메일
  - `password` (string, 필수): 8자 이상, 영문·숫자·특수문자(`!@#$%^&*`) 각 1개 이상
  - `nickname` (string, 필수): 닉네임
- **응답:** 로그인과 동일 (Access Token + Set-Cookie로 Refresh Token)

### 2.3 액세스 토큰 재발급 (Reissue)
- **POST** `/api/v1/auth/reissue`
- **인증:** Cookie에 `refresh_token` 필요 (Body/Header에 Access Token 불필요)
- **Body:** 없음
- **응답:**
  - **200:** Body에 새 `accessToken`. `Set-Cookie`로 새 Refresh Token 설정 (기존 토큰은 폐기, RTR)
  - **401:** Refresh Token 만료/무효 → 재로그인 유도

### 2.4 로그아웃
- **POST** `/api/v1/auth/logout`
- **인증:** Cookie에 `refresh_token` 필요
- **Body:** 없음
- **응답:** **200** + `Set-Cookie`로 쿠키 삭제. 클라이언트는 Access Token을 메모리/저장소에서 삭제해야 함.

### 2.5 모바일에서 쿠키 처리 (필수)

서버는 Refresh Token을 **HttpOnly Cookie**로만 발급·갱신합니다. 모바일 앱은 **쿠키를 유지·재전송**해야 재발급·로그아웃이 동작합니다.

- **Android (OkHttp / Retrofit)**  
  - `JavaNetCookieJar` 등으로 `CookieHandler` 연동  
  - 재발급·로그아웃 요청 시 **동일 도메인에 대해 저장된 쿠키가 자동으로 포함**되도록 설정
- **iOS (URLSession / Alamofire)**  
  - `HTTPCookieStorage.shared` 사용 또는 세션/요청에 쿠키 저장 정책 설정  
  - 재발급·로그아웃 호출 시 해당 쿠키가 서버로 전송되도록 보장

쿠키를 저장·전송하지 않으면 로그인 직후 Refresh Token이 없어 재발급이 불가능합니다.

### 2.6 RTR 동시성 제어 (Race Condition 방지)

Refresh Token은 **1회용(RTR)** 이므로, Access Token 만료 시 여러 요청이 동시에 `/auth/reissue`를 호출하면 첫 요청만 성공하고 나머지는 `401 INVALID_REFRESH_TOKEN`이 될 수 있습니다.

권장 처리:
- **단일 재발급 플로우:** `isRefreshing` 플래그 또는 Mutex/Lock으로 동시에 하나의 reissue만 수행
- **대기 큐:** 재발급 중인 동안 만료로 실패한 API 요청을 큐에 넣고, 재발급 완료 후 새 Access Token으로 재시도

---

## 3. 공통 응답 형식

성공 시 대부분의 API는 **공통 래퍼**로 감싸져 반환됩니다 (Swagger/OpenAPI 경로 제외).

```json
{
  "success": true,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": { ... }
}
```

- **data:** 실제 응답 본문 (엔드포인트별 스키마)
- **status:** HTTP 상태 코드와 동일한 값
- 로그인/회원가입/재발급 응답도 위 형식으로 감싸지며, **data** 안에 `accessToken` 등이 들어갈 수 있음 (구현에 따라 다를 수 있으므로 Swagger 또는 실제 응답 확인 권장)

---

## 4. API 엔드포인트 요약

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| **별 관측** |
| GET | `/api/v1/analyze` | X | 지정 위치·날짜·시간의 관측 적합도 분석 |
| GET | `/api/v1/forecast` | X | 지정 위치의 주간(5일) 관측 예보 |
| **관측지** |
| GET | `/api/v1/spots` | X | 위도/경도/반경으로 주변 관측지 조회 |
| **인증** |
| POST | `/api/v1/auth/login` | X | 로그인 |
| POST | `/api/v1/auth/register` | X | 회원가입 |
| POST | `/api/v1/auth/reissue` | Cookie | Access Token 재발급 |
| POST | `/api/v1/auth/logout` | Cookie | 로그아웃 |
| **회원** |
| GET | `/api/v1/members/me` | JWT | 내 정보 조회 |
| GET | `/api/v1/members/exists/email` | X | 이메일 중복 검사 |
| **북마크** |
| GET | `/api/v1/bookmarks` | JWT | 북마크 목록 |
| POST | `/api/v1/bookmarks` | JWT | 북마크 추가 |
| PATCH | `/api/v1/bookmarks/{bookmarkId}` | JWT | 북마크 수정 |
| DELETE | `/api/v1/bookmarks/{bookmarkId}` | JWT | 북마크 삭제 |
| **추천** |
| GET | `/api/v1/recommends/bookmarks/today` | JWT | 오늘 관측 추천 북마크 (상위 5개) |

---

## 5. 요청/응답 스키마 요약

### 5.1 GET /api/v1/analyze (Query)
- **Query:** `lat` (필수), `lon` (필수), `date` (필수, yyyy-MM-dd), `time` (필수, HH:mm)
- **응답 data:**  
  `date`, `time`, `totalScore` (0–100), `reasons` (감점 사유 배열), `aiComment`, `weather`, `astronomy`, `lightPollution` 등

### 5.2 GET /api/v1/forecast (Query)
- **Query:** `lat` (필수), `lon` (필수). `date`, `time`는 선택(기본값 사용)
- **응답 data:** 주간 예보 (날짜별·시간대별 점수 등)

### 5.3 GET /api/v1/spots (Query)
- **Query:** `lat` (필수), `lon` (필수), `radius` (필수, 10–500 km)
- **응답 data:** 관측지 목록 (배열). 각 항목: id, name, 주소, 좌표, bortle 등급, 주차 가능 여부 등

### 5.4 POST /api/v1/auth/login (Body)
- **Body:** `{ "email": "string", "password": "string" }`
- **응답:** Access Token (Body) + Refresh Token (Set-Cookie)

### 5.5 POST /api/v1/auth/register (Body)
- **Body:** `{ "email": "string", "password": "string", "nickname": "string" }`
- **응답:** 로그인과 동일

### 5.6 GET /api/v1/members/me
- **Header:** `Authorization: Bearer {accessToken}`
- **응답 data:** 회원 정보 (id, email, nickname 등)

### 5.7 GET /api/v1/members/exists/email (Query)
- **Query:** `email` (필수)
- **응답 data:** `exists` (boolean) 등

### 5.8 북마크
- **POST /api/v1/bookmarks (Body)**  
  - `type`: `"SPOT"` | `"CUSTOM"`  
  - SPOT: `spotId`, `name`, `memo`(선택)  
  - CUSTOM: `name`, `latitude`, `longitude`, `address`(선택), `memo`(선택)
- **PATCH /api/v1/bookmarks/{bookmarkId} (Body)**  
  - `name`, `memo` 수정
- **응답 data:** 북마크 객체 (id, type, name, spot 또는 custom 좌표/주소, 메모 등)

### 5.9 GET /api/v1/recommends/bookmarks/today
- **Header:** `Authorization: Bearer {accessToken}`
- **응답 data:** 오늘 밤 관측 추천 북마크 리스트 (최대 5개). 첫 조회 시 5~7초 소요 가능 → 로딩 UI 권장.

---

## 6. 에러 처리

### 6.1 에러 응답 형식
에러 시 공통 형식 예시는 다음과 같습니다.

```json
{
  "success": false,
  "status": 400,
  "message": "검증 실패 메시지 또는 에러 코드 설명"
}
```

필드가 더 있을 수 있으므로, 실제 구현은 `GlobalExceptionHandler` 및 Swagger 응답 스키마를 참고하세요.

### 6.2 상태 코드별 클라이언트 동작 권장
- **401 Unauthorized**  
  - Access Token 만료 가능성 → `/auth/reissue` 호출 (Cookie 포함)  
  - 재발급 실패(401) 시 → 로그인 화면으로 이동, 로컬 Access Token·Refresh Token 제거
- **400 Bad Request**  
  - `message`를 사용자에게 표시하거나, 필드별 검증 메시지로 매핑
- **404 Not Found**  
  - 리소스 없음 (예: 잘못된 bookmarkId, spotId)
- **500 Internal Server Error**  
  - 재시도 또는 “일시적 오류” 안내

---

## 7. 모바일 구현 시 필수 사항

- [ ] **Base URL**  
  개발/스테이징/운영 환경별로 분리 설정
- [ ] **모든 인증 필요 API**  
  `Authorization: Bearer {accessToken}` 헤더 포함
- [ ] **쿠키 저장·전송**  
  로그인/회원가입/재발급/로그아웃 응답의 `Set-Cookie` 저장 및 재발급·로그아웃 요청 시 해당 쿠키 전송
- [ ] **Access Token 저장**  
  메모리 또는 보안 저장소. 재발급 성공 시 새 토큰으로 갱신
- [ ] **401 시 재발급 플로우**  
  한 번에 하나의 reissue만 수행하도록 동시성 제어
- [ ] **재발급 실패 시**  
  로그아웃 처리 후 로그인 화면으로 이동
- [ ] **공통 응답**  
  `success`, `status`, `message`, `data` 구조 파싱 후 `data`만 비즈니스 로직에 사용
- [ ] **추천 API**  
  첫 로딩 5~7초 가능성 → 로딩 인디케이터 또는 안내 문구 표시

---

## 8. OpenAPI / 클라이언트 코드 생성

- **OpenAPI JSON:** `GET {BaseURL}/v3/api-docs`  
  예: `https://api.byeolbolil.xyz/v3/api-docs`
- **Swagger UI:** `{BaseURL}/swagger-ui.html`  
  브라우저에서 API 명세·Try-it-out 확인

OpenAPI Generator 등으로 위 JSON을 사용해 **Kotlin**, **Swift**, **Dart** 등의 클라이언트 코드를 생성할 수 있습니다.  
모바일 전용 모델/네트워크 레이어는 위 가이드(인증 헤더, 쿠키, 공통 응답, 에러 처리)에 맞게 한 번 더 래핑하는 것을 권장합니다.

---

## 관련 문서

- [SECURITY_AUTH_GUIDE.md](./SECURITY_AUTH_GUIDE.md) - 인증·보안 상세 (RTR, 쿠키 정책)
- [SWAGGER_GUIDE.md](./SWAGGER_GUIDE.md) - Swagger UI 및 OpenAPI JSON 사용법
- [ARCHITECTURE_GUIDE.md](./ARCHITECTURE_GUIDE.md) - 백엔드 아키텍처 (참고용)
