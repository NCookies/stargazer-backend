# Authentication Architecture

본 문서는 본 프로젝트에서 사용 중인 **OAuth2 기반 로그인 + JWT 인증 + Refresh Token 재발급 + Rotation 구조**에 대한
설계 의도, 전체 흐름, 보안 고려사항, 핵심 구현 코드를 정리한 문서이다.

---

## 1. 인증 방식 개요

### 사용 기술

* Spring Boot
* Spring Security
* OAuth2 Client (Kakao / Google / Naver)
* JWT (Access / Refresh Token)
* Redis (Refresh Token 저장)
* React (Frontend)
* Stateless 인증 구조

### 기본 원칙

* 서버는 **세션을 사용하지 않는다**
* Access Token은 **짧은 수명**
* Refresh Token은 **HttpOnly Cookie**
* JS에서 Refresh Token에 **접근하지 않는다**
* Refresh Token은 **서버 단에서 무효화 가능**

---

## 2. 전체 인증 흐름

### 2.1 OAuth 로그인 흐름

```text
[Client (React)]
  |
  | 1. OAuth 로그인 요청
  v
[OAuth Provider]
  |
  | 2. 인증 성공
  v
[Backend]
  |
  | 3. OAuth2SuccessHandler
  |   - Member 조회/생성
  |   - Access Token 발급
  |   - Refresh Token 발급
  |   - Refresh Token → HttpOnly Cookie
  v
[Client /oauth/callback]
```

### 핵심 포인트

* OAuth 성공 후 **Refresh Token은 자동으로 쿠키에 저장**
* JS는 Refresh Token을 **읽을 수 없음**
* Access Token만 클라이언트 메모리에서 사용

---

## 3. Access / Refresh Token 전략

| 구분    | Access Token | Refresh Token    |
| ----- | ------------ | ---------------- |
| 저장 위치 | React 메모리    | HttpOnly Cookie  |
| 수명    | 짧음           | 김                |
| JS 접근 | 가능           | 불가               |
| 사용 목적 | API 인증       | Access Token 재발급 |

---

## 4. Token 재발급 (Reissue) 흐름

### 4.1 클라이언트 요청

```ts
fetch("http://localhost:8080/api/auth/reissue", {
  method: "POST",
  credentials: "include"
});
```

* request body ❌
* query parameter ❌
* 쿠키는 브라우저가 자동 전송

---

### 4.2 서버 처리

```java
@PostMapping("/api/auth/reissue")
public ResponseEntity<TokenResponse> reissue(HttpServletRequest request) {

    String refreshToken = refreshTokenResolver.resolve(request);
    if (refreshToken == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String newAccessToken = authService.reissueAccessToken(refreshToken);
    return ResponseEntity.ok(new TokenResponse(newAccessToken));
}
```

---

## 5. JWT 인증 필터 구조

### JwtAuthenticationFilter

```java
@Override
protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain chain
) throws IOException, ServletException {

    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
        chain.doFilter(request, response);
        return;
    }

    String token = resolveToken(request);

    if (token != null && jwtTokenProvider.validate(token)) {
        Long memberId = jwtTokenProvider.getMemberId(token);

        Authentication auth =
            new UsernamePasswordAuthenticationToken(memberId, null, List.of());

        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    chain.doFilter(request, response);
}
```

### 설계 이유

* JWT는 Stateless → SecurityContext는 요청 단위
* OPTIONS(preflight)는 인증 대상 아님
* 인증 실패 시 redirect ❌

---

## 6. CORS / Preflight 처리 전략

### 핵심 원칙

* Preflight 요청(OPTIONS)은 **무조건 허용**
* 인증/토큰 검사 ❌
* redirect ❌

---

## 7. Refresh Token 저장 전략 (Redis)

### Key 구조 (최종)

```text
RT:{refreshToken} -> memberId
```

### 이유

* Refresh Token 단위로 무효화 가능
* Rotation 적용 용이
* 탈취된 토큰만 정확히 폐기 가능
* 추후 멀티 디바이스 로그인 용이

---

## 8. 로그아웃 처리 흐름

### 서버 처리

```java
public void logout(HttpServletRequest request) {

    String refreshToken = refreshTokenResolver.resolve(request);
    if (refreshToken == null) {
        return;
    }

    refreshTokenRedisRepository.delete(refreshToken);
}
```

* Access Token은 서버에 저장하지 않음
* 남은 Access Token은 만료 시 자연 소멸 (블랙리스트 사용 X)

---

## 9. Refresh Token Reissue + Rotation 설계

### 기본 원칙

* Refresh Token은 **1회성**
* 재발급 시 기존 Refresh Token 삭제

### 서버 로직

```java
public String reissueAccessToken(String refreshToken) {

    Long memberId = jwtTokenProvider.getMemberId(refreshToken);

    Long storedMemberId = refreshTokenRedisRepository.find(refreshToken);
    if (storedMemberId == null) {
        throw new MemberException(MemberErrorCode.INVALID_REFRESH_TOKEN);
    }

    refreshTokenRedisRepository.delete(refreshToken);

    String newRefreshToken = jwtTokenProvider.createRefreshToken(memberId);
    refreshTokenRedisRepository.save(newRefreshToken, memberId, REFRESH_EXPIRE_MS);

    return jwtTokenProvider.createAccessToken(memberId);
}
```

---

## 10. 회원 정보 조회 API

```java
@GetMapping("/api/v1/members/me")
public MemberMeResponse me(Authentication authentication) {
    Long memberId = (Long) authentication.getPrincipal();
    return memberService.getMe(memberId);
}
```

---

## 11. 한 줄 요약

> OAuth2 로그인 이후 JWT 기반 Stateless 인증을 사용하며,
> Refresh Token은 HttpOnly Cookie + Redis로 관리하고
> Rotation을 통해 재사용을 원천 차단한다.
