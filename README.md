# 🌌 Stargazer - 별 관측 적합도 분석 서비스 (MVP)

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)

> **"오늘 밤, 별 보러 가도 될까?"** > 위치 기반 기상 데이터와 광해(Light Pollution), 천문 데이터를 종합적으로 분석하여 최적의 별 관측 시간을 추천해주는 백엔드 서비스입니다.

---

## 📖 프로젝트 개요
단순히 날씨가 맑다고 별이 잘 보이는 것은 아닙니다. 달의 밝기(월령), 주변의 빛 공해(광해), 대기의 투명도 등이 복합적으로 작용합니다.  
이 프로젝트는 **OpenWeatherMap API, Google Gemini AI, 그리고 정밀 광해 데이터(VIIRS)**를 결합하여 사용자에게 **직관적인 '관측 적합도 점수'와 'AI 기반 코멘트'**를 제공합니다.

## 🚀 핵심 기능
* **위치 기반 관측 적합도 분석:** 위도/경도를 기반으로 기상, 천문, 광해 데이터를 종합하여 0~100점의 점수 산출
* **주간 예보 (Weekly Forecast):** 향후 5일간의 밤 시간대 관측 적합도 예보 제공
* **AI 기반 관측 코멘트:** Google Gemini Pro를 활용하여 현재 조건에 맞는 조언 제공
* **정밀 광해 정보 제공:** 단순 행정구역 기준이 아닌, 위성 데이터(VIIRS) 기반의 Bortle Scale(1~9등급) 매핑
* **낮/밤 자동 감지:** SunCalc를 이용한 정밀한 일출/일몰 계산 및 주간(Daytime) 필터링

---

## 🛠️ 기술 스택 (Tech Stack)
* **Language:** Java 17
* **Framework:** Spring Boot 3.x
* **Data Access:** Spring Data JPA, MySQL
* **External API:**
    * OpenWeatherMap (Weather/Forecast)
    * Google Gemini (Generative AI)
* **Libraries:**
    * `shredder/suncalc` (천문 계산)
    * `apache-commons-csv` (광해 데이터 처리)

---

## 🌟 별 관측 점수 산정 기준

본 서비스는 전 세계에서 별이 가장 잘 보이는 **'몽골 고비 사막(Bortle Class 1, 구름 0%)'의 환경을 100점 만점** 기준으로 설정하고, 각 관측지의 방해 요소를 정밀하게 감점하는 **절대 평가 방식**을 사용합니다.

### 1. 기본 원칙
- **Base Score:** 100점 (완벽한 조건)
- **Cut-off:** 구름이 70% 이상이거나 태양 고도가 -6도(시민박명) 이상인 경우 즉시 **0점** 처리

### 2. 상세 감점 로직 (Deduction Logic)

단순한 구간 감점이 아닌, 관측 데이터에 비례하는 **선형 보간(Linear Interpolation)** 공식을 적용하여 미세한 날씨 변화를 점수에 반영합니다.

| 구분 | 감점 요인 | 적용 공식 / 기준 | 최대 감점 |
| :--- | :--- | :--- | :--- |
| **💡 광해** | **Light Pollution** | `(Bortle Class - 1) * 6.25` <br> *예: Bortle 4 (양평) → -19점* | -50점 |
| **☁️ 구름** | **Cloud Cover** | `(구름양% - 10)` (10% 미만 무시) <br> *예: 구름 25% → -15점* | 관측 불가 |
| **🌕 달** | **Moon Phase** | `달 밝기(0~1.0) * 30` (지평선 위일 때만) <br> *예: 반달(0.5) → -15점* | -30점 |
| **💧 습도** | **Humidity** | `(습도% - 60) / 2` (60% 미만 무시) <br> *예: 습도 90% → -15점* | -20점 |
| **🌫 시정** | **Visibility** | 10km 미만: -10점 / 5km 미만: -20점 | -20점 |

### 3. 점수 가이드
- **💯 90 ~ 100점:** 몽골, 사막 수준의 완벽한 조건. 평생 잊지 못할 밤.
- **🌟 70 ~ 89점:** 국내 최상급 (안반데기, 영양 등). 은하수가 육안으로 선명함.
- **✨ 50 ~ 69점:** 교외 관측지. 별자리 관측 및 사진 촬영에 적합.
- **☁️ 0 ~ 49점:** 관측 불리. 달이나 밝은 행성 위주 관측 권장.
---

### 📝 점수 산출 예시
> **Case: 서울 도심(Bortle 8), 구름 없음(0%), 보름달**
> * 기상 점수: 100점 (맑음)
> * 광해 페널티: -50점 (도심지 불빛)
> * 월령 페널티: -30점 (달빛이 밝음)
> * **최종 점수: 20점** 👉 *"날씨는 맑지만 별을 보기는 어렵습니다. 대신 밝은 달을 관측해보세요."*

---

## 🔧 설치 및 실행 (Getting Started)

### Prerequisites

* Java 17+
* MySQL 8.0+

### Environment Variables

프로젝트 루트에 `.env` 파일 생성 또는 `application.yml`에 다음 키 설정이 필요합니다.

```properties
WEATHER_API_KEY=your_openweathermap_key
GEMINI_API_KEY=your_google_gemini_key
```

### Run

```bash
./gradlew bootRun
```

---

## 스크린샷

![39.119.82.172_4000_ (2).png](assets/images/39.119.82.172_4000_%20%282%29.png)
![39.119.82.172_4000_ (1).png](assets/images/39.119.82.172_4000_%20%281%29.png)

---

## 🔗 관련 링크

* **Notion 기획서:** [링크]
* **API 명세서:** [링크]

```
