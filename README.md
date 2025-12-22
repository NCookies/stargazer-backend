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

## 📊 관측 적합도 산출 로직 (Scoring Algorithm)

단순히 "날씨가 맑음"이라고 해서 별이 잘 보이는 것은 아닙니다.
Stargazer는 기상 데이터뿐만 아니라 천문, 광해 데이터를 종합적으로 분석하여 **0~100점**의 현실적인 관측 적합도를 산출합니다.

### 1. 기본 기상 점수 (Weather Score)
OpenWeatherMap API 데이터를 기반으로 기초 점수를 산정합니다.
* **구름(Cloud Cover):** 가장 치명적인 변수입니다. 구름량이 10% 이상일 경우 급격하게 감점됩니다.
* **시정(Visibility):** 대기의 투명도를 의미합니다. 5km 미만일 경우 미세먼지나 안개로 간주하여 감점합니다.

### 2. 광해 페널티 (Light Pollution Penalty) 💡
*"서울의 맑은 밤하늘과 강원도의 맑은 밤하늘은 다르다"*
</br>
행정구역 단위가 아닌, **위성 데이터(VIIRS 2023)** 를 가공한 정밀 좌표 데이터를 사용합니다.
* 사용자의 위도/경도를 기반으로 **Bortle Scale(1~9등급)**을 조회합니다.
* **도심지(Class 7~9):** 날씨가 아무리 좋아도 별이 보이지 않으므로 최대 **-50점** 페널티를 부과합니다.
* **교외(Class 5~6):** 밝은 별 위주로 관측 가능하므로 소폭 감점(-10~25점)합니다.
* **시골(Class 1~4):** 감점 없이 은하수 관측까지 가능한 환경으로 판단합니다.

### 3. 천문 조건 보정 (Astronomical Factors) 🌑
`SunCalc` 알고리즘을 활용하여 태양과 달의 위치를 계산해 점수에 반영합니다.
* **월령(Moon Phase):** 달이 보름달에 가까울수록(밝을수록), 그리고 달이 지평선 위에 떠 있을 경우 하늘을 밝게 만들어 별 관측을 방해하므로 감점합니다.
* **박명(Twilight) 구분:** * **주간(Daytime):** 태양 고도가 -6도 이상(시민 박명)인 경우 **0점** (관측 불가).
  * **여명(Nautical Twilight):** 해가 졌더라도 완전히 어두워지지 않은 시간대(-6° ~ -12°)에는 대폭 감점하여 "완전한 밤"을 기다리도록 유도합니다.

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
