# 🌌 Stargazer 설계 문서 및 로드맵

> **Version:** 1.1.0 (Revised)
> **Last Updated:** 2025.12.31
> **Status:** Phase 1 Development (Refactoring & Stabilization)

## 1. 프로젝트 개요 (Overview)
**Stargazer**는 "오늘 밤, 별 보러 가도 될까?"라는 질문에 답을 주는 **별 관측 적합도 분석 및 명소 추천 서비스**입니다.
천문학적 지식이 없는 일반인(커플, 드라이브족, 스마트폰 사진가)을 대상으로 하며, **차량으로 접근 가능한 최적의 관측 명소**를 추천하고 직관적인 관측 점수를 제공합니다.

### 🎯 핵심 목표
1.  **데이터의 직관화:** 복잡한 기상/천문/광해 데이터를 종합하여 0~100점의 **절대 점수**로 변환 제공.
2.  **드라이브 최적화:** 차량 이동을 기본으로 가정하되, 진입로 난이도나 주차 정보 등 운전자에게 실질적으로 필요한 명소 정보 제공.
3.  **기술적 심도:** 대용량 광해 데이터 처리(GIS), 외부 API 트래픽 최적화(Caching), 아키텍처 패턴 적용 등 백엔드 기술적 챌린지 해결.

---

## 2. 기술 스택 (Tech Stack)
* **Backend:** Java 17, Spring Boot 3.x, JPA
* **Database:** MySQL 8.0 (Spatial Index), Redis (Caching)
* **Infra:** AWS EC2, RDS, GitHub Actions (CI/CD)
* **External API:** OpenWeatherMap, Google Gemini, Kakao Map API
* **Data Source:** VIIRS Light Pollution Data (2023), 전국 별 관측 명소 데이터(자체 구축)

---

## 3. 개발 로드맵 (Roadmap)

### 🗓️ Phase 1: 안정화 및 배포 (Priority: High)
> **목표:** MVP 리팩토링 및 서버 안정성 확보, 실사용 가능한 배포 환경 구축
- [x] **아키텍처 리팩토링:** Facade 패턴 적용, 도메인형 패키지 구조 전환 (진행 중)
- [x] **광해 데이터 최적화:** CSV 메모리 로딩 방식 제거 → MySQL Spatial Data(Point) 이관 및 공간 인덱스(R-Tree) 적용
- [x] **점수 알고리즘 개선:** 절대 평가 기준 도입 (몽골=100점, 국내 최상급=90점) 및 상세 감점 사유 로직 구현
- [x] **지도 API 교체:** Google Maps → Kakao Map API (국내 데이터 정확도 확보)
- [ ] **AWS 배포:** EC2/RDS 구축 및 도메인 연결

### 🗓️ Phase 2: 콘텐츠 및 탐색 기능 강화 (Priority: Medium)
> **목표:** 사용자에게 유용한 정보(데이터) 제공 및 체류 시간 증대
- [ ] **명소 데이터 구축 (Seed Data):** 전국 주요 관측지(30~50곳) DB화 (주차 가능 여부, 광해 등급 포함)
- [ ] **내 주변 명소 찾기:** 사용자 위치 기준 반경 N km 이내 명소 조회 (MySQL `ST_Distance_Sphere` 활용)
- [ ] **주간 예보 상세화:** 명소 클릭 시 해당 위치의 주간(5일) 관측 예보 및 점수 추이 제공
- [ ] **정보성 콘텐츠:** "별 등급이란?", "스마트폰 촬영 꿀팁(갤럭시/아이폰)" 가이드 제공

### 🗓️ Phase 3: 개인화 (Priority: Low)
> **목표:** 재방문 유도 및 데이터 축적
- [ ] **회원가입/로그인:** JWT 기반 인증 시스템
- [ ] **북마크:** 가고 싶은 명소 저장 기능
- [ ] **금주의 추천:** 기상 데이터 기반 "이번 주말 드라이브 가기 좋은 명소" 자동 추천 배치(Batch)

---

## 4. 상세 기능 명세 (Functional Specifications)

### 4.1. 📊 관측 적합도 분석 (Core Logic)
* **점수 산정 방식 (Absolute Scoring):**
  * 기존 상대 평가를 제거하고 전 지구적 절대 기준 적용.
  * **만점 기준(100점):** 광해 Class 1, 구름 0%, 습도 30% 미만, 월령 0(삭).
  * **국내 현실적 목표:** 강원도 산간 맑은 날 기준 약 85~90점 설정.
* **상세 분석 리포트:**
  * 단순 점수 외에 **"왜 이 점수인가?"**에 대한 근거 목록 제공.
  * *예: [감점 요인] 구름이 많음(-30), 달이 밝음(-20), 도심지 광해(-40)*
* **주간(Daytime) 처리:**
  * `SunCalc`로 태양 고도 계산. 시민박명(-6도) 이상일 경우 "관측 불가"로 Fast-Fail 처리.

### 4.2. 📍 위치 및 명소 서비스 (Location Service)
* **광해 데이터 처리 (Technical Challenge):**
  * **AS-IS:** CSV 파일 전체 메모리 로딩 (OOM 위험).
  * **TO-BE:** MySQL `geometry` 타입 저장. `MBRContains` 등을 활용한 효율적 쿼리 구현.
* **내 주변 명소 검색:**
  * Haversine 공식 대신 DB 레벨의 공간 함수 활용하여 성능 최적화.
  * 필터링 조건: 직선거리 순, 광해 등급 순 (어두운 곳 우선).
* **지도 연동:**
  * Kakao Map SDK 활용하여 명소 마커 표시.
  * 마커 클릭 시 요약 정보(현재 점수, 주차 가능 여부 등) 표시.

### 4.3. 📝 정보 제공 (Content)
* **관측 가이드 (Tooltip/Modal):**
  * **Bortle Scale:** 1~9등급의 의미 시각화 (예: 4등급=은하수 희미하게 보임).
  * **Moon Phase:** 월령 아이콘 및 관측 난이도 설명.
* **촬영 가이드:**
  * 기종별(Galaxy Pro Mode, iPhone Long Exposure) 설정값 가이드 카드 뉴스 형태 제공.

---

## 5. 데이터 모델 설계 (Draft)

### `Spot` (관측 명소)
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | Long | PK |
| `name` | Varchar | 명소 이름 (예: 안반데기) |
| `location` | Point | 위도/경도 (Spatial Type) |
| `address` | Varchar | 지번/도로명 주소 |
| `bortle_grade` | Int | 해당 지역 광해 등급 (미리 계산) |
| `has_parking` | Boolean | 주차 가능 여부 (운전자 필수 정보) |
| `is_public_transport_accessible` | Boolean | 대중교통 접근 가능 여부 (보조 정보) |
| `description` | Text | 장소 설명 및 특징 |

### `LightPollution` (광해 데이터)
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | Long | PK |
| `location` | Point | 격자 중심 좌표 |
| `bortle_class` | Int | 1~9 등급 |
| `radiance` | Double | 실제 광해 수치 |

---

## 6. API 설계 (Endpoint Draft)

* `GET /api/v1/analyze?lat={lat}&lon={lon}` : 특정 좌표 실시간/예보 분석
* `GET /api/v1/spots` : 명소 리스트 조회 (정렬: 거리순, 점수순 / 필터: 주차가능)
* `GET /api/v1/spots/{spotId}/forecast` : 특정 명소의 주간 관측 예보
* `GET /api/v1/contents/guide` : 관측/촬영 가이드 데이터