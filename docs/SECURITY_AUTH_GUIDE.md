# Authentication Architecture Guide

본 문서는 본 프로젝트의 **OAuth2 기반 로그인 + JWT 인증 + Refresh Token Rotation(RTR)** 구조에 대한 설계 원칙, 보안 정책, 그리고 **클라이언트(Web/Mobile) 연동 가이드**를 정리한 문서이다.

---

## 1. 인증 방식 개요

### 사용 기술
- **Framework:** Spring Boot, Spring Security
- **Auth:** OAuth2 Client (Kakao / Google / Naver)
- **Token:** JWT (Access / Refresh Token)
- **Storage:** Redis (Refresh Token 관리), MySQL (회원 정보)
- **Client:** React (Web), Mobile App (iOS/Android)

### 핵심 원칙
1. **Stateless:** 서버는 세션을 사용하지 않는다.
2. **Unified Cookie Strategy (통일된 보안 전략):**
   - **Web & Mobile 공통:** Refresh Token은 반드시 **HttpOnly Cookie**로만 주고받는다.
   - **Access Token:** 요청 시 **HTTP Header (`Authorization`)**에 담아서 보낸다.
3. **RTR (Refresh Token Rotation):** 토큰 재발급 시 Refresh Token도 함께 교체하며, 이전 토큰은 즉시 폐기한다.
4. **Service Decoupling:** 인증 서비스(Service)는 HTTP 객체(Request/Response)에 의존하지 않는다.

---

## 2. 토큰 운영 전략

| 구분 | Access Token | Refresh Token |
| --- | --- | --- |
| 저장 위치 | 메모리 변수 (Web) / 내부 저장소 (Mobile) | **HttpOnly Cookie** |
| 전송 방식 (Request) | Header (`Authorization: Bearer ...`) | Cookie (`refresh_token=...`) |
| 발급 방식 (Response) | JSON Body | Header (`Set-Cookie`) |
| 수명 | 짧음 (15분 ~ 30분) | 긺 (2주) |
| JS/App 접근 | 가능 | **불가 (HttpOnly)** |
| 재발급 전략 | 만료 시 재발급 | **Rotation (1회용)** |

---

## 3. 인증 흐름 상세

### 3.1 로그인 (OAuth2)
1. 클라이언트가 소셜 로그인 버튼 클릭 → 백엔드 리다이렉트
2. 인증 성공 시 `OAuth2LoginSuccessHandler` 동작
3. 공통 처리
   - **Refresh Token:** `Set-Cookie` 헤더를 통해 HttpOnly Cookie로 발급
   - **Access Token:**  
     - Web: 리다이렉트 URL 파라미터  
     - Mobile(App API): JSON Body

### 3.2 토큰 재발급 (Reissue) – RTR 적용
- **요청 조건:** Access Token 만료(401)
- **서버 로직**
  1. Cookie에서 `refresh_token` 추출
  2. Redis 조회 및 유효성 검사
  3. 기존 Refresh Token 즉시 삭제 (Rotation)
  4. 새로운 Access + Refresh Token 생성
- **응답**
  - `Set-Cookie` 헤더로 새로운 Refresh Token 설정
  - JSON Body로 새로운 Access Token 반환

### 3.3 로그아웃
1. Cookie에서 `refresh_token` 추출
2. Redis에서 해당 토큰 삭제
3. `Set-Cookie (Max-Age=0)`로 쿠키 제거
4. **주의:** Access Token은 서버에서 블랙리스트 처리하지 않으므로, 클라이언트에서 반드시 메모리 삭제 필요

---

## 4. 구현 패턴 (Backend Developer 참고)

### 4.1 컨트롤러 구조
클라이언트 타입(`X-Client-Type`)을 구분하지 않고 단일 로직으로 처리한다.

```java
@PostMapping("/reissue")
public ResponseEntity<ReissueTokenResponse> reissue(@RefreshToken String refreshToken) {
    // 1. 서비스 로직 (기존 삭제 후 새 토큰 발급)
    TokenDto tokenDto = authService.reissueAccessToken(refreshToken);

    // 2. 쿠키 생성 (Web/Mobile 공통)
    ResponseCookie rtCookie = cookieUtil.createRefreshTokenCookie(tokenDto.refreshToken());

    // 3. 응답 (쿠키 + Body)
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, rtCookie.toString())
        .body(new ReissueTokenResponse(tokenDto.accessToken()));
}
````

### 4.2 ArgumentResolver

`@RefreshToken` 어노테이션은 **오직 쿠키**에서만 값을 추출하도록 구현한다.
(Header 검사 로직은 제거한다.)

---

## 5. 클라이언트(Web / Mobile) 개발자 가이드

### 필수 확인 사항 (Checklist)

#### 1. Mobile – 쿠키 매니저(CookieJar) 설정 필수

모바일 앱은 기본적으로 쿠키를 유지하지 않으므로, HTTP 라이브러리에 쿠키 매니저 설정이 필요하다.

* **Android (OkHttp / Retrofit)**
  `JavaNetCookieJar` 등을 사용하여 `Set-Cookie` 응답을 자동 저장 및 재전송
* **iOS (Alamofire)**
  `HTTPCookieStorage.shared` 사용 또는 세션 쿠키 허용 정책 확인

이 설정이 없으면 로그인 직후 Refresh Token이 소실되어 재발급이 불가능하다.

#### 2. Common – RTR 동시성 제어 (Race Condition 방지)

Refresh Token은 1회용이므로, 여러 요청이 동시에 `reissue`를 호출하면 안 된다.

* **문제 상황:**
  첫 요청이 RT를 사용 후 폐기 → 이후 요청은 INVALID_REFRESH_TOKEN 발생
* **해결 방법:**

    * `isRefreshing` 플래그 사용
    * Mutex / Lock
    * 요청 큐(Queue) 구성

#### 3. Common – Access Token 전송 위치

* 모든 API 요청에 Access Token은 반드시 Header로 전송
* 형식:

  ```
  Authorization: Bearer {Access_Token}
  ```

#### 4. Web – CORS 설정

* `credentials: "include"` 필수
* 백엔드 CORS 설정에서 `allowCredentials=true` 확인
* 프론트엔드/백엔드 도메인 간 쿠키 공유 가능 여부 검증

---

## 6. 에러 코드 정의 (Auth)

| Code                  | Message      | 상황                | 조치                   |
| --------------------- | ------------ | ----------------- | -------------------- |
| 401                   | Unauthorized | Access Token 만료   | `reissue` API 호출     |
| INVALID_REFRESH_TOKEN | 유효하지 않은 RT   | RT 만료, 조작, 이미 사용됨 | 강제 로그아웃 및 로그인 페이지 이동 |
