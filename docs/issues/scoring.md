# 점수 산정 로직 — 이슈 후보 목록

- 작성일: 2026-08-04
- 범위: `domain/stargazing/` 전체(engine, client/openweather, client/gemini), `infra/lightpollution`,
  `src/main/resources/light_pollution_korea.csv`(구조만), README "별 관측 점수 산정 기준", `CLAUDE.md` 도메인 값 인용 규칙
- 성격: **추적 가능성 조사**. 계수의 타당성 평가나 출처 확정은 하지 않았음(다음 단계 작업).
- 원칙: 실제로 읽은 코드/파일만 근거로 사용. 확인하지 못한 것은 "확인 못 함"으로 표기.

---

## 1단계 — 상수 인벤토리

"출처 주석" 칸은 **있음 / 없음**만 기록한다. 값의 의미를 설명하는 주석(예: `// 10km`, `// 중간값(4)로 처리`)은
출처가 아니므로 **없음**으로 분류했다. 저장소 전체에서 논문/표준/URL을 인용한 주석은 **한 건도 발견되지 않았다.**

### A. 점수 계산 본체 (`StargazingScoringEngine`)

| 값 | 위치(파일:라인) | 무엇에 쓰이나 | 출처 주석 | README 기재값 | 일치 |
|---|---|---|---|---|---|
| `100` (PERFECT_SCORE) | [StargazingScoringEngine.java:25](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L25) | 기본 점수 | 없음 | "Base Score: 100점" | 일치 |
| `10000.0` (VISIBILITY_THRESHOLD_GOOD) | [:26](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L26) | 시정 1단계 경계(m) | 없음 | "10km 미만 -10점" | 일치 |
| `5000.0` (VISIBILITY_THRESHOLD_BAD) | [:27](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L27) | 시정 2단계 경계(m) | 없음 | "5km 미만 -20점" | 일치 |
| `10000` (식 안 매직넘버) | [:38](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L38) | visibility가 null일 때 대체값 | 없음 | 미기재 | 판정 불가 |
| `-6.0` (식 안 매직넘버) | [:42](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L42) | 태양 고도 컷오프(낮 판정) | 없음 | "시민박명(태양 고도 -6° 이상) 0점" | 경계 방향 불일치(코드 `> -6.0`) |
| `70` (식 안 매직넘버) | [:48](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L48) | 구름 하드 컷오프(%) | 없음 | "구름 70% 이상 0점" | 일치(`>= 70`) |
| `10` (식 안 매직넘버) | [:59-60](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L59-L60) | 구름 감점 시작점(%) | 없음 | "10% 미만 무시" | 일치 |
| `1` (기울기, 식에 암묵) | [:60-61](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L60-L61) | 구름 1%당 1점 감점 | 없음 | "(구름% - 10)" | 일치 |
| `60` (식 안 매직넘버) | [:68-69](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L68-L69) | 습도 감점 시작점(%) | 없음 | "60% 미만 무시" | README 일치 / DESIGN_DOC 불일치 |
| `2.0` (식 안 매직넘버) | [:69](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L69) | 습도 2%당 1점 | 없음 | "(습도% - 60) / 2" | 일치 |
| `20` (식 안 매직넘버) | [:78](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L78) | 시정 5km 미만 감점 | 없음 | "-20점" | 일치 |
| `10` (식 안 매직넘버) | [:81](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L81) | 시정 10km 미만 감점 | 없음 | "-10점" | 일치 |
| `0` (식 안 매직넘버) | [:88](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L88) | 달 고도 경계(지평선) | 없음 | "(지평선 위일 때)" | 일치 |
| `30` (식 안 매직넘버) | [:89](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L89) | 달 밝기 계수 | 없음 | "달 밝기 × 30, 최대 -30점" | 일치 |
| `1` (식 안 매직넘버) | [:90](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L90) | 달 감점 1점 미만이면 감점·사유 모두 생략 | 없음 | 미기재 | 미기재 |
| `100` (표시용) | [:92](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L92) | 달 밝기 %표시 배율 | 없음 | 미기재 | 해당 없음 |
| `1` (식 안 매직넘버) | [:99-100](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L99-L100) | 광해 감점 기준점(Class 1 = 무감점) | 없음 | "(Bortle Class - 1)" | 일치 |
| `6.25` | [:100](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L100) | 광해 등급당 감점 | 없음 | "× 6.25, 최대 -50점" | 일치 |
| `0` (하한 클램프) | [:106](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L106) | 음수 점수 방지 | 없음 | 미기재 | 미기재 |
| `100` (분기 조건) | [:109](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L109) | "완벽한 관측 조건" 메시지 조건 | 없음 | "90~100 완벽" 구간 표기만 | 미기재 |

### B. 광해(Bortle) 데이터 경로

| 값 | 위치(파일:라인) | 무엇에 쓰이나 | 출처 주석 | README 기재값 | 일치 |
|---|---|---|---|---|---|
| `4` (폴백 등급) | [LightPollutionDataProvider.java:26](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/provider/LightPollutionDataProvider.java#L26) | 광해 데이터 미발견 시 기본 등급 | 없음 | 미기재 | 미기재 |
| `0.01` (도) | [LightPollutionRepository.java:25-26](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/repository/LightPollutionRepository.java#L25-L26) | 최근접 탐색 사각 범위 | 없음 | 미기재 | 미기재 |
| `4326` (SRID) | [LightPollutionRepository.java:28](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/repository/LightPollutionRepository.java#L28), [LightPollution.java:29](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/entity/LightPollution.java#L29), [LightPollutionMigrationRunner.java:29](../../src/main/java/xyz/ncookie/stargazer/infra/lightpollution/LightPollutionMigrationRunner.java#L29) | 좌표계 식별자 | 없음 | 미기재 | 미기재 |
| CSV `bortle` 컬럼(사전 계산된 등급) | [light_pollution_korea.csv:1](../../src/main/resources/light_pollution_korea.csv#L1) 헤더 `lat,lon,radiance,bortle`, 적재는 [LightPollutionMigrationRunner.java:56-62](../../src/main/java/xyz/ncookie/stargazer/infra/lightpollution/LightPollutionMigrationRunner.java#L56-L62) | 광해 감점의 입력값 전체 | 없음(생성 스크립트는 저장소 밖 `P:\stargazer-viirs`에 존재 — 아래 F 참조) | 미기재 | 판정 불가 |
| 한계등급 문자열 `6.5 / 6.0 / 5.5 / 4.5 / 3.0` | [BortleGrade.java:12-20](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/enums/BortleGrade.java#L12-L20) | 등급별 밝기·한계등급 표시 | 없음 | 미기재 | 미기재 |

### C. 표시·부가 계산에 쓰이는 숫자

| 값 | 위치(파일:라인) | 무엇에 쓰이나 | 출처 주석 | README 기재값 | 일치 |
|---|---|---|---|---|---|
| `6.0`, `20.0` (식 안 매직넘버) | [StargazingDomainService.java:46](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/domain/StargazingDomainService.java#L46) `6.0 - (clouds/20.0)` | 예보의 "별 등급"(한계등급) 표시 | 없음 | 미기재 | 미기재 |
| `20000 / 10000 / 5000 / 2000` | [VisibilityGrade.java:10-14](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/enums/VisibilityGrade.java#L10-L14) | 시정 라벨(최상~나쁨) | 없음 | 미기재 | 감점 기준(5k/10k)과 별도 표 |
| `10 / 170 / 80 / 100` (각도) | [MoonPhase.java:36-51](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/enums/MoonPhase.java#L36-L51) | 달 위상 명칭 경계 | 없음 | 미기재 | 미기재 |

### D. 입력 데이터 결측·대체값

| 값 | 위치(파일:라인) | 무엇에 쓰이나 | 출처 주석 | README 기재값 | 일치 |
|---|---|---|---|---|---|
| 구름 `100`, 습도 `50.0`, temp `0.0`, 시정 `10000` | [WeatherDataFactory.java:15-20](../../src/main/java/xyz/ncookie/stargazer/global/util/WeatherDataFactory.java#L15-L20) | API 실패 시 더미 날씨 | 없음 | 미기재 | 미기재 |
| `10000` | [OpenWeatherResponseMapper.java:25](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/dto/mapper/OpenWeatherResponseMapper.java#L25) | 예보 visibility 결측 대체 | 없음 | 미기재 | 미기재 |
| `60` (분) | [WeatherDataProvider.java:35](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/provider/WeatherDataProvider.java#L35) | 현재 날씨 API vs 예보 API 분기 | 없음 | 미기재 | 미기재 |
| `-6.0` (중복 하드코딩) | [StargazingApplicationService.java:120](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/application/StargazingApplicationService.java#L120), [RecommendApplicationService.java:174](../../src/main/java/xyz/ncookie/stargazer/domain/recommend/application/RecommendApplicationService.java#L174) | 야간 시간대 필터 | 없음 | "시민박명 -6°" | 값 일치, 명칭 불일치 |
| L1 `5m` / bortleZone L2 `10m` / weatherForecast L2 `1h` | [CacheConfig.java:36](../../src/main/java/xyz/ncookie/stargazer/global/config/CacheConfig.java#L36), [:40](../../src/main/java/xyz/ncookie/stargazer/global/config/CacheConfig.java#L40), [:48](../../src/main/java/xyz/ncookie/stargazer/global/config/CacheConfig.java#L48) | 점수 입력값(날씨·광해)의 신선도 | 없음 | 미기재 | 미기재 |

### E. CSV 구조 (전체 로드 없이 헤더/집계만 확인)

- 헤더: `lat,lon,radiance,bortle` (4컬럼), 데이터 585,018행
- 좌표 범위: lat 33.0 ~ 38.996, lon 124.013 ~ 130.996 / 격자 간격 약 0.004도, 결측(바다·비관측) 구간 존재
- `bortle` 분포: 2등급 3,855 / 3등급 134,907 / 4등급 232,105 / 5등급 93,494 / 6등급 54,969 / 7등급 37,248 / 8등급 17,071 / 9등급 11,369
  → **Class 1은 0행**
- `radiance` 범위: 0.3246 ~ 674.544
- `radiance` → `bortle` 변환 규칙은 **저장소 안에는 없고**, 저장소 밖 생성 스크립트에 있다(아래 F).

### F. CSV 생성 스크립트 (저장소 밖: `P:\stargazer-viirs`, git 관리 대상 아님)

저장소의 `light_pollution_korea.csv`는 `P:\stargazer-viirs\light_pollution_korea_500m.csv`와 **MD5 동일**
(`5077892f813eb6ce48a51e6ff937e559`) — 즉 `extract_korea_bortle.py`의 출력물이 그대로 들어와 있다.

| 값 | 위치(파일:라인) | 무엇에 쓰이나 | 출처 주석 | README 기재값 | 일치 |
|---|---|---|---|---|---|
| 임계 `0.25 / 0.50 / 1.00 / 2.50 / 5.00 / 10.0 / 25.0 / 50.0` | `extract_korea_bortle.py:27-37` | radiance → Bortle 1~9 변환 | 없음 — 주석은 "VIIRS Radiance(10^-9 W/cm^2/sr) 기준 **근사치**"(:25)라고만 적혀 있고 참조 문헌 없음 | 미기재 | 미기재 |
| 동일 임계 사본 | `extract_global_bortle.py:25-34` | 동일 변환(전 지구용) | 없음 | 미기재 | **두 사본의 `radiance <= 0` 처리가 다름** |
| `0` 반환(데이터 없음) | `extract_korea_bortle.py:28` | 바다 등 무데이터 표기 | 없음 | 미기재 | global 사본에는 이 분기가 없음(0 이하도 1등급) |
| `STEP = 0.004167` | `extract_korea_bortle.py:21` | 격자 간격(15 arc-second ≈ 460~500m) | 없음(단위 계산 근거는 주석에 있음 :19-20) | 미기재 | 미기재 |
| `LAT 33.0~39.0 / LON 124.0~131.0` | `extract_korea_bortle.py:13-14` | 추출 대상 영역 | 없음 | 미기재 | 코드에는 이 범위가 어디에도 없음 |
| `radiance <= 0.1` 필터 | `extract_korea_bortle.py:69` | 무데이터 행 제외 | 없음 | 미기재 | 미기재 |
| `round(lat, 3)`, `round(lon, 3)` | `extract_korea_bortle.py:76-77` | 좌표 소수 3자리 절삭(≈111m 양자화) | 없음(주석은 "용량 절약") | 미기재 | 격자 간격(0.004167)보다 큰 반올림 오차 발생 |
| 입력 데이터 | `input_viirs.tif`, `VNL_npp_2024_global_vcmslcfg_v2_c202502261200.average_masked.dat.tif.gz` | 원본 VIIRS 래스터 | 파일명에 버전 정보만 있고, 출처·라이선스·다운로드 경로 기록 없음 | 미기재 | 미기재 |

> 참고: 스크립트의 좌표 순서 처리(`coords.append((lon, lat))  # rasterio는 (x, y) 순서`, :59)는 rasterio 규약과 일치한다.
> 즉 뒤에 나오는 좌표 순서 문제는 **CSV 생성 단계가 아니라 Java 조회 쿼리 단계**의 문제다.

---

## 2단계 — 규칙 충돌 점검 결과 (코드에서 직접 확인한 것만)

| 점검 항목 | 확인 결과 | 근거 |
|---|---|---|
| 하드 컷오프와 감점 로직의 중복 적용 | **중복 없음(사실).** 컷오프는 즉시 `return`하므로 감점 구간에 진입하지 않는다. | [engine:43](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L43), [:49](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L49) |
| 감점 합계가 100 초과 가능한가 | **가능(사실).** 구름 59 + 습도 20 + 시정 20 + 달 30 + 광해 50 = 최대 179점. | engine:60, 69, 78, 89, 100 |
| 음수 점수가 나오는가 | **최종값은 0으로 클램프됨(사실).** 단 클램프 전 내부값은 음수가 될 수 있다. | [engine:106](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L106) |
| 구름 정확히 70% | **0점(사실).** `>= 70`이라 컷오프. README "70% 이상"과 일치. | [engine:48](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L48) |
| 구름 정확히 10% | **감점 없음(사실).** `> 10`. README "10% 미만 무시"와 실질 동일(10%의 감점액이 0이므로). | engine:59 |
| 태양 고도 정확히 -6.0° | **밤으로 판정(사실).** `> -6.0`이므로 -6.0은 통과. README는 "-6° 이상이면 0점"이라 서술 → 경계 방향 불일치. | [engine:42](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L42), README:45 |
| 습도 정확히 60% / 달 고도 정확히 0° | **감점 없음(사실).** 둘 다 strict 비교(`> 60`, `> 0`). | engine:68, 88 |
| 시정 정확히 10000m / 5000m | **각각 감점 없음 / -10점(사실).** `<` 비교. | engine:77, 80 |
| 날씨 API 실패 시 점수 | **0점 + "구름이 하늘을 덮었습니다 (100%)"(사실).** 더미가 구름 100%이므로 컷오프에 걸린다. | [WeatherDataFactory:17](../../src/main/java/xyz/ncookie/stargazer/global/util/WeatherDataFactory.java#L17), [OpenWeatherMapClient:43-46](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/client/openweather/OpenWeatherMapClient.java#L43-L46) |
| 예보 API 실패 시 | **`/forecast`는 빈 목록으로 200 응답(사실).** | [ApplicationService:109-111](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/application/StargazingApplicationService.java#L109-L111) |
| 입력이 null일 때 | **엔진은 visibility만 방어(사실).** `weather`/`weather.main()` null 체크는 어디에도 없고, 매퍼는 null을 반환할 수 있다. visibility null은 엔진은 통과하지만 표시·AI 경로에서 언박싱된다. | engine:37-38, [Mapper:12-14](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/dto/mapper/OpenWeatherResponseMapper.java#L12-L14), ApplicationService:81, Gemini:76 |
| 광해 데이터 없을 때 점수 | **Bortle 4로 계산되어 -18.75점(사실).** 사유 문자열에도 "Class 4"로 표기되어 실측과 구분 불가. | [Provider:24-26](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/provider/LightPollutionDataProvider.java#L24-L26), engine:100-102 |
| 같은 개념이 두 곳에서 다르게 계산 | **3건 확인(사실).** ①한계등급: `6.0 - 구름/20`(예보) vs `BortleGrade` 표(분석). ②시정 분류: 엔진 2단계(5k/10k) vs `VisibilityGrade` 4단계(2k/5k/10k/20k). ③최종 점수: 엔진 계산값 vs Gemini 응답의 `final_score`. | DomainService:46, BortleGrade:12-20 / engine:26-27, VisibilityGrade:10-14 / engine:106, [Gemini:93](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/client/gemini/GeminiAnalysisClient.java#L93) |
| `-6.0` 임계 중복 | **3곳 하드코딩(사실).** 상수화되어 있지 않다. | engine:42, ApplicationService:120, RecommendApplicationService:174 |

---

## 3단계 — 이슈 목록

### 상위 이슈 (재작업 단위)

아래 2건은 개별 이슈를 묶는 상위 이슈다. **개별 이슈는 그대로 유지**하며, 상위 이슈가 미뤄지더라도 개별 항목은
독립적으로 닫을 수 있다. 진행 순서는 **A → B 순차**로 합의됨.

---

### [P1] (상위 A) 광해 데이터 파이프라인 재작업 — 등급 변환을 코드로 이관

- 현상: 현재 광해 등급은 저장소 밖 Python 스크립트가 계산한 값을 CSV로 받아 그대로 신뢰하는 구조다. 등급을 바꾸려면 11GB 원본 래스터부터 다시 처리해야 하고, 임계값은 버전 관리·테스트 대상이 아니다.
- 근거: 아래 하위 이슈들의 근거를 합친 것. 핵심 전제는 **원측정값 `radiance`가 이미 DB에 적재돼 있고 아무도 읽지 않는다**는 사실([LightPollution.java:36](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/entity/LightPollution.java#L36), [LightPollutionMigrationRunner.java:58-62](../../src/main/java/xyz/ncookie/stargazer/infra/lightpollution/LightPollutionMigrationRunner.java#L58-L62), 저장소 전체에 `getRadiance()` 호출부 0건).
- 왜 문제인가: 점수 최대 감점 요인(-50점)의 산출 규칙이 버전 관리 밖에 있어 재현·검증·변경이 모두 불가능하다. 데이터 재생성 없이도 이 구조를 바꿀 수 있는데(radiance가 이미 DB에 있음) 그러지 않고 있다.
- 확실성: 사실(현행 구조와 radiance 미사용). 재작업 후의 결과는 당연히 미검증.
- 완료 조건: 등급 변환이 Java 코드 안에서 이뤄지고, 임계표가 테스트로 고정되며, 임계값 변경이 데이터 재생성 없이 가능해진다. 하위 이슈가 모두 닫힌다.
- 사람이 결정해야 할 것: 등급을 조회 시점에 계산할지 적재 시점에 계산해 둘지. (변경 유연성 vs 조회 비용)
- 제안 방향: `radiance`를 읽어 등급을 계산하도록 조회 경로를 바꾸고, CSV의 `bortle` 컬럼 의존을 끊는다. 임계값 자체는 이 단계에서 바꾸지 않는다(구조 이관과 값 변경을 섞지 않는다).
- 규모: L
- 선행 이슈: 점수 엔진 특성화 테스트(P3)
- 하위 이슈:
  - [P1] 광해 최근접 조회 쿼리의 좌표 순서 불일치
  - [P1] 데이터 범위 밖 좌표의 조용한 Bortle 4 폴백
  - [P1] 광해 부분 적재 고착
  - [P2] radiance→Bortle 규칙이 저장소 밖 + 임계값 출처 없음
  - [P2] 변환 스크립트 이중화 및 무데이터 처리 상이
  - [P3] `radiance` 컬럼이 적재되지만 사용되지 않음
  - [P4] 광해 캐시 키 정규화 부재
  - [P4] 탐색 박스 ±0.01도 근거 없음

### [P2] (상위 B) 감점 모델 재설계 — 계수 근거 확정과 점수 정의 정리

- 현상: 감점 요인 목록(광해·구름·달·습도·시정)은 정해져 있으나, 각 계수·임계값·컷오프의 근거가 없고 총합 상한 정책도 정의돼 있지 않다. 같은 개념이 두 곳에서 다르게 계산되는 곳도 있다.
- 근거: 아래 하위 이슈들의 근거를 합친 것. 요약은 이 문서 1단계 인벤토리(출처 주석 있음: **0건**)와 2단계 충돌 점검표.
- 왜 문제인가: 사용자에게 0~100의 숫자를 제시하면서 그 숫자가 왜 그 값인지 설명할 수 없다. 이 저장소의 현재 목표에 정면으로 걸린다.
- 확실성: 사실(근거 부재와 이중 계산). 어떤 값이 옳은지는 이 단계에서 판단하지 않음.
- 완료 조건: `docs/SCORING.md`가 존재하고, 코드의 모든 점수 상수가 그 문서의 항목과 1:1로 대응하며, 출처를 못 찾은 값은 "출처 불명"으로 명시된다. 하위 이슈가 모두 닫힌다.
- 사람이 결정해야 할 것: **핵심.** 각 계수의 근거 채택 여부, 절대 감점 + 클램프 구조를 유지할지 가중합/정규화로 갈지, 총합 상한 정책.
- 제안 방향: 근거 확정(사람) → 문서화 → 코드 반영 순서. 상위 A가 끝나 광해 임계표가 코드로 들어온 뒤 착수해야 광해 부분을 두 번 건드리지 않는다.
- 규모: L
- 선행 이슈: 상위 A, 점수 엔진 특성화 테스트(P3)
- 하위 이슈:
  - [P1] 한계등급 이중 계산
  - [P2] 광해 `6.25` 출처 없음
  - [P2] 달 `×30`·절삭 규칙 출처 없음
  - [P2] 구름 `70`·`10`·기울기 출처 없음
  - [P2] 습도 `60`·`2` 출처 없음 + DESIGN_DOC 불일치
  - [P2] 시정 분류표 이원화
  - [P2] 감점 총합 100 초과
  - [P2] 표시 감점 합계와 점수 불일치
  - [P2] 문서-코드 불일치
  - [P3] 100점 분기 도달 불가

---

### 개별 이슈

### [P1] 광해 최근접 조회 쿼리의 좌표 순서가 적재 좌표 순서와 다르다

- 현상: **하나의 쿼리 안에서 좌표 축 순서가 두 가지로 섞여 있다.** 적재는 `(x=lon, y=lat)`, 같은 쿼리의 `ST_MakeEnvelope`도 `(lon, lat)`인데, `ORDER BY`의 `ST_GeomFromText`만 `POINT(lat lon)` 순서로 비교 대상 좌표를 만든다. CSV 생성 스크립트는 rasterio 규약대로 `(lon, lat)`이 맞으므로, 불일치는 Java 조회 계층에서 발생한다.
- 근거:
  - [LightPollutionMigrationRunner.java:61](../../src/main/java/xyz/ncookie/stargazer/infra/lightpollution/LightPollutionMigrationRunner.java#L61) — `createPoint(new Coordinate(lon, lat))` → **x=lon, y=lat**
  - [LightPollutionRepository.java:25-26](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/repository/LightPollutionRepository.java#L25-L26) — `POINT(:lon - 0.01, :lat - 0.01)` → **x=lon, y=lat** (적재와 동일)
  - [LightPollutionRepository.java:31](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/repository/LightPollutionRepository.java#L31) — `ST_GeomFromText(CONCAT('POINT(', :lat, ' ', :lon, ')'), 4326)` → **x=lat, y=lon** (앞의 둘과 반대)
  - `extract_korea_bortle.py:59` — `coords.append((lon, lat))`, CSV 생성 단계는 정상
- 왜 문제인가: 광해 등급은 점수에서 최대 -50점을 차지하는 최대 감점 요인인데, 그 등급을 고르는 "가장 가까운 격자점" 정렬 기준이 다른 축 규약으로 계산된다. 서울(lat 37.5, lon 127.0)이라면 정렬 기준점은 (37.5, 127.0)이 아니라 (127.0, 37.5)로 들어간다. 또 이 값은 MBRContains 박스와도 다른 규약이라, 한 쿼리 안에서 두 규약이 공존한다는 사실 자체가 유지보수 위험이다.
- 확실성: **사실**(세 지점의 축 순서가 코드상 서로 다르다는 것). 다만 **실행 결과는 확인 못 함** — MySQL은 SRID 4326에서 WKT를 (latitude, longitude) 축 순서로 해석하고 `ST_MakeEnvelope`는 SRID 0만 받는 등 함수별 규약이 달라, 최종적으로 어떤 행이 선택되는지(혹은 좌표 범위 오류가 나는지)는 실제 DB에서 확인해야 한다.
- 확인 방법(선행): 실행 중인 MySQL에서 알려진 좌표 3~5개로 `findNearest`를 직접 실행해 반환 행의 lat/lon과 등급을 눈으로 확인한다. 이걸 먼저 하지 않으면 어느 쪽이 "고쳐야 할 쪽"인지 결정할 수 없다.
- 완료 조건: 적재·조회·정렬 3곳의 좌표 축 순서가 하나의 규약으로 통일되고, 알려진 좌표 몇 개에 대해 기대 격자점이 선택되는 것이 검증된다.
- 사람이 결정해야 할 것: 없음
- 제안 방향: 축 순서 규약을 한 곳에 문서화하고 쿼리를 그 규약에 맞춘다. DB에서 실제 반환 행을 찍어보는 검증이 선행되어야 한다.
- 규모: M
- 선행 이슈: 없음

### [P1] 날씨 API 실패가 "구름 100%"라는 실제 관측 사실로 위장된다

- 현상: 외부 API 호출이 실패하면 구름 100%의 더미 날씨가 반환되고, 그대로 하드 컷오프에 걸려 사용자에게는 "구름이 하늘을 덮었습니다 (100%)"라는 **기상 사유**로 0점이 표시된다.
- 근거:
  - [WeatherDataFactory.java:14-21](../../src/main/java/xyz/ncookie/stargazer/global/util/WeatherDataFactory.java#L14-L21) — `new OpenWeatherResponse.Clouds(100)`
  - [OpenWeatherMapClient.java:43-46](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/client/openweather/OpenWeatherMapClient.java#L43-L46) — catch 후 더미 반환
  - [StargazingScoringEngine.java:47-50](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L47-L50)
- 왜 문제인가: 장애 상태와 실제 흐린 날씨를 사용자도 운영자도 구분할 수 없다. 맑은 날에 "구름 100%"라는 틀린 정보를 확신에 찬 문장으로 제공한다.
- 확실성: 사실
- 완료 조건: 데이터를 못 가져온 경우와 실제로 흐린 경우가 응답에서 구분된다(별도 상태/사유 표기 또는 에러 응답).
- 사람이 결정해야 할 것: 실패 시 정책 — 에러 응답으로 바꿀지, 0점을 유지하되 "데이터 없음"으로 표기할지.
- 제안 방향: 더미 날씨로 점수를 만드는 경로 자체를 없애고, 실패를 값이 아니라 상태로 표현한다.
- 규모: M
- 선행 이슈: 없음

### [P1] 과거 시각이나 5일 초과 미래를 요청해도 무관한 예보로 점수를 만든다

- 현상: 요청 시각과 예보 항목의 시간 차에 상한이 없다. 가장 가까운 항목을 무조건 고르므로, 2020년이나 1년 뒤를 요청해도 예보 목록의 양 끝 항목으로 점수가 계산되어 정상 응답이 나간다. 요청 DTO에도 날짜 범위 검증이 없다.
- 근거:
  - [WeatherDataProvider.java:51-68](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/provider/WeatherDataProvider.java#L51-L68) — `minDiff` 비교만 있고 허용 오차 상한 없음
  - [StargazingRequest.java:28-36](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/dto/request/StargazingRequest.java#L28-L36) — 위경도에는 범위 제약이 있으나 date/time에는 `@NotNull`뿐
- 왜 문제인가: 사용자가 받은 점수가 요청한 시점과 무관한 값인데 그 사실이 응답 어디에도 드러나지 않는다.
- 확실성: 사실
- 완료 조건: 지원 가능한 시간 범위를 벗어난 요청이 400 또는 명시적 "예보 없음"으로 처리되고, 범위가 문서에 적힌다.
- 사람이 결정해야 할 것: 지원 시간 범위(예: 지금 ~ +5일)와 범위 밖 요청의 응답 형태.
- 제안 방향: 요청 검증에 범위 제약을 추가하고, 최근접 탐색에도 허용 오차 상한을 둔다.
- 규모: S
- 선행 이슈: 없음

### [P1] "한계등급"이 두 곳에서 다르게 계산되어 화면마다 다른 값이 나온다

- 현상: 예보 API는 구름량만으로 `6.0 - (구름%/20)` 등급을 만들고, 분석 API는 Bortle 등급 표에서 한계등급 문자열을 가져온다. 같은 지점·같은 시각에 대해 두 API가 다른 한계등급을 말한다.
- 근거:
  - [StargazingDomainService.java:46](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/domain/StargazingDomainService.java#L46) — `String.format("%.1f등급", 6.0 - (weatherData.clouds().all() / 20.0))`
  - [BortleGrade.java:12-20](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/enums/BortleGrade.java#L12-L20) — Class 9 → "3.0등급"
  - [StargazingApplicationService.java:92-93](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/application/StargazingApplicationService.java#L92-L93)
- 왜 문제인가: 광해 Class 9(도심)에서 구름 0%면 예보는 "6.0등급", 분석은 "3.0등급"을 표시한다. 어느 쪽도 근거를 설명할 수 없고 서로 모순된다.
- 확실성: 사실(두 계산식이 존재하고 서로 독립적이라는 점). 실제 응답 값 비교는 실행 검증하지 않음.
- 완료 조건: 한계등급 산출 경로가 하나로 통일되고, 두 API가 같은 입력에 같은 값을 반환한다.
- 사람이 결정해야 할 것: 한계등급을 무엇의 함수로 정의할지(광해만 / 광해+구름+달).
- 제안 방향: 한계등급 계산을 단일 지점으로 모으고 예보의 임시 식을 제거한다. 정의 확정은 상수 출처 정리 이슈와 함께 다룬다.
- 규모: M
- 선행 이슈: 광해 계수 출처 이슈, 달·구름 계수 출처 이슈

### [P1] 광해 데이터 범위 밖 좌표가 조용히 Bortle 4로 계산된다 (지원 범위 미정의)

- 현상: 요청은 전 세계 위경도를 허용하는데 광해 데이터는 한국(lat 33.0~38.996, lon 124.013~130.996)뿐이고, 못 찾으면 조용히 4등급(-18.75점)으로 계산된다. 시각은 항상 Asia/Seoul로 고정 해석된다.
- 근거:
  - [LightPollutionDataProvider.java:24-26](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/provider/LightPollutionDataProvider.java#L24-L26) — `.orElse(4)`
  - [StargazingRequest.java:18-25](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/dto/request/StargazingRequest.java#L18-L25) — -90~90 / -180~180 허용
  - [StargazingApplicationService.java:50-54](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/application/StargazingApplicationService.java#L50-L54) — `ZoneId.of("Asia/Seoul")` 고정
  - CSV 좌표 범위(위 인벤토리 E)
- 왜 문제인가: 해외 좌표에 대해 근거 없는 광해 등급 + 잘못된 현지 시각으로 점수와 사유 문자열("광해 등급 Class 4")이 생성되는데, 추정값이라는 표시가 전혀 없다.
- 확실성: 사실
- 완료 조건: 서비스 지원 좌표 범위가 정의되어 코드(검증)와 문서에 동시에 반영되고, 범위 밖 요청이 명시적으로 처리된다.
- 사람이 결정해야 할 것: 한국 전용으로 못 박을지, 해외도 허용하되 "광해 데이터 없음"으로 표시할지.
- 제안 방향: 지원 범위를 먼저 문서로 확정한 뒤 검증·폴백·타임존 세 곳을 그 정의에 맞춘다.
- 규모: M
- 선행 이슈: 없음

### [P1] 광해 데이터 적재가 중단되면 부분 적재 상태로 영구 고착된다

- 현상: 마이그레이션은 `count() > 0`이면 건너뛴다. 1000건 단위 `saveAll`이 개별 커밋되고 메서드에 트랜잭션 경계가 없으므로, 중간에 중단되면 다음 기동부터는 "이미 존재함"으로 판단해 나머지를 절대 적재하지 않는다. 파싱 실패 행도 로그만 남기고 건너뛴다.
- 근거:
  - [LightPollutionMigrationRunner.java:33-36](../../src/main/java/xyz/ncookie/stargazer/infra/lightpollution/LightPollutionMigrationRunner.java#L33-L36)
  - [:64-70](../../src/main/java/xyz/ncookie/stargazer/infra/lightpollution/LightPollutionMigrationRunner.java#L64-L70) — 배치 저장 + `catch { log.error }`
- 왜 문제인가: 광해 데이터가 부분 결손되면 해당 지역 요청이 전부 폴백 4등급으로 계산되지만, 겉으로는 정상 동작처럼 보인다. 배포 이후 조용히 틀린 점수를 주는 상태가 된다.
- 확실성: 사실(코드 흐름). 실제 중단 시 동작은 실행 검증하지 않음.
- 완료 조건: 적재 완결 여부를 판단할 수 있는 기준이 생기고(예상 행 수 대비 검증 등), 미완결 시 재적재되거나 기동이 실패한다.
- 사람이 결정해야 할 것: 앱 기동 시 적재를 유지할지, 별도 마이그레이션 수단으로 분리할지.
- 제안 방향: 완결 판정을 `count() > 0`이 아닌 명시적 기준으로 바꾸고, 실패 행 누적 시 중단시킨다.
- 규모: M
- 선행 이슈: 없음

### [P2] 광해 감점식 `(Bortle-1) × 6.25`에 출처가 없다

- 현상: 최대 -50점을 좌우하는 계수와 기준점이 상수 추출도, 출처 주석도 없이 식에 박혀 있다.
- 근거: [StargazingScoringEngine.java:99-103](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L99-L103) — `double lpPenalty = (realBortle - 1) * 6.25;`
- 왜 문제인가: 점수 최대 감점 요인의 근거를 설명할 수 없다. `CLAUDE.md:111-124`가 명시적으로 금지한 상태(도메인 값에 추적 가능한 출처 필요)에 해당한다.
- 확실성: 사실(출처 주석 없음). 값의 타당성은 평가하지 않음.
- 완료 조건: 값의 근거가 문서화되고 사용 지점에 출처가 표기되거나, 근거를 못 찾았다는 사실이 명시된다.
- 사람이 결정해야 할 것: 근거 확정 자체(다음 단계 `docs/SCORING.md` 작업).
- 제안 방향: 상수로 추출하고 출처 표기 자리를 만든다. 값 변경은 근거 확정 후에만.
- 규모: S
- 선행 이슈: 없음

### [P2] 달 감점 `밝기 × 30`과 "1점 미만 절삭" 규칙에 출처가 없다

- 현상: 계수 30이 식에 박혀 있고, 감점이 1점 미만이면 감점도 사유도 통째로 생략된다. 후자는 README에 언급이 없다.
- 근거: [StargazingScoringEngine.java:88-93](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L88-L93)
- 왜 문제인가: 달 밝기와 하늘 밝기의 관계를 선형으로 본 근거가 없고, 달 고도(지평선 위 여부만 봄)도 반영되지 않는다. 절삭 규칙은 문서에 없는 숨은 규칙이다.
- 확실성: 사실
- 완료 조건: 계수와 절삭 규칙의 근거가 문서화되고 README와 일치한다.
- 사람이 결정해야 할 것: 근거 확정, 절삭 규칙 유지 여부.
- 제안 방향: 상수 추출 + 출처 표기 자리 확보. 절삭 규칙은 README에 명시하거나 제거한다.
- 규모: S
- 선행 이슈: 없음

### [P2] 구름 컷오프 70%와 "10% 초과 1%당 1점" 기울기에 출처가 없다

- 현상: 컷오프 70, 무시 구간 10, 기울기 1이 모두 식 안 매직넘버다.
- 근거: [StargazingScoringEngine.java:46-63](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L46-L63)
- 왜 문제인가: 69%는 -59점(41점), 70%는 0점으로 1%p 차이에 41점이 갈리는데 그 불연속의 근거가 없다.
- 확실성: 사실
- 완료 조건: 컷오프·기울기의 근거가 문서화되고 상수로 추출된다.
- 사람이 결정해야 할 것: 근거 확정, 컷오프 불연속을 유지할지.
- 제안 방향: 상수 추출 후 출처 표기 자리 확보. 값 변경은 근거 확정 후.
- 규모: S
- 선행 이슈: 없음

### [P2] 습도 감점 `(습도-60)/2`에 출처가 없고 DESIGN_DOC과 어긋난다

- 현상: 시작점 60과 제수 2에 출처가 없다. `docs/DESIGN_DOC.md:57`은 만점 조건을 "습도 30% 미만"으로 적고 있어 코드(60% 미만 무감점)와 다르다.
- 근거:
  - [StargazingScoringEngine.java:66-72](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L66-L72)
  - [docs/DESIGN_DOC.md:57](../DESIGN_DOC.md#L57)
- 왜 문제인가: 같은 저장소의 두 문서와 코드가 서로 다른 기준을 말한다.
- 확실성: 사실
- 완료 조건: 근거가 문서화되고 README·DESIGN_DOC·코드가 하나의 값을 말한다.
- 사람이 결정해야 할 것: 근거 확정.
- 제안 방향: 상수 추출 + 문서 정합. DESIGN_DOC의 30% 서술이 폐기된 것인지 확인 필요.
- 규모: S
- 선행 이슈: 없음

### [P2] 시정 분류가 두 개의 서로 다른 표로 존재한다

- 현상: 점수 감점은 5km/10km 2단계인데, 사용자에게 보여주는 라벨은 2km/5km/10km/20km 4단계다. 두 표는 서로를 참조하지 않는다.
- 근거:
  - [StargazingScoringEngine.java:26-27](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L26-L27), [:77-83](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L77-L83)
  - [VisibilityGrade.java:10-14](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/enums/VisibilityGrade.java#L10-L14)
- 왜 문제인가: 라벨 "최상"(20km↑)과 "매우 좋음"(10km↑)이 점수상 동일 취급이라, 라벨과 점수의 관계를 설명할 수 없다. 두 표 중 하나만 바뀌면 조용히 어긋난다.
- 확실성: 사실
- 완료 조건: 시정 등급표가 하나가 되고 감점이 그 표에서 파생된다.
- 사람이 결정해야 할 것: 등급 구간을 몇 단계로 둘지.
- 제안 방향: 등급 enum을 단일 출처로 삼고 감점을 등급에 매단다.
- 규모: S
- 선행 이슈: 없음

### [P2] radiance→Bortle 변환 규칙이 저장소 밖에만 있고 임계값에 출처가 없다

- 현상: 광해 등급은 CSV 4번째 컬럼의 사전 계산값을 그대로 신뢰해 적재된다. 변환 규칙은 **저장소 밖** `P:\stargazer-viirs\extract_korea_bortle.py`에만 존재하며, 임계값(0.25 / 0.50 / 1.00 / 2.50 / 5.00 / 10.0 / 25.0 / 50.0)에 대한 근거는 `"VIIRS Radiance(10^-9 W/cm^2/sr) 기준 **근사치**"`라는 주석 한 줄뿐이다. 원본 래스터(`input_viirs.tif`, VNL v2 2024)의 다운로드 경로·라이선스·전처리 이력도 기록이 없다.
- 근거:
  - 저장소 CSV와 `P:\stargazer-viirs\light_pollution_korea_500m.csv`의 MD5가 동일(`5077892f813eb6ce48a51e6ff937e559`) → 이 스크립트의 출력물이 그대로 사용 중임이 확인됨
  - `extract_korea_bortle.py:23-37` — 변환 함수와 "근사치" 주석
  - [LightPollutionMigrationRunner.java:56-62](../../src/main/java/xyz/ncookie/stargazer/infra/lightpollution/LightPollutionMigrationRunner.java#L56-L62) — 등급을 그대로 신뢰해 적재
  - [CLAUDE.md:113-124](../../CLAUDE.md#L113-L124) — 이 변환 규칙이 출처 불명이라는 서술
- 왜 문제인가: 점수 최대 감점 요인(-50점)의 입력값을 만드는 규칙이 버전 관리 밖에 있고, 그 값이 "근사치"라고만 표시되어 있다. 스크립트가 담긴 드라이브가 사라지면 데이터 갱신·재현이 불가능하다.
- 확실성: 사실(스크립트 내용과 CSV 해시 일치를 직접 확인). 임계값의 타당성은 평가하지 않음.
- 완료 조건: 변환 규칙이 저장소 안에서 재현 가능해지고(스크립트 + 원본 데이터 획득 절차), 각 임계값에 출처가 표기되거나 "출처 불명"이 명시된다.
- 사람이 결정해야 할 것: 임계값의 근거 확정, 스크립트를 저장소로 들일지 여부(원본 `.tif`는 11GB라 데이터 자체는 제외 필요).
- 제안 방향: 스크립트 2개와 원본 획득 절차를 저장소에 편입하고, 임계표를 `docs/SCORING.md`로 옮겨 출처 표기 자리를 만든다.
- 규모: M
- 선행 이슈: 없음

### [P2] 변환 스크립트가 두 벌로 갈라져 있고 무데이터 처리가 서로 다르다

- 현상: 같은 `radiance_to_bortle` 함수가 한국용·전 지구용 스크립트에 복사돼 있는데 동작이 다르다. 한국용은 `radiance <= 0`을 **0등급**(데이터 없음)으로, 전 지구용은 같은 값을 **1등급**(가장 어두움)으로 분류한다. 한국용의 0등급 분기는 앞단의 `radiance <= 0.1` 필터 때문에 실제로는 도달할 수 없다.
- 근거:
  - `extract_korea_bortle.py:27-37` — `if radiance <= 0: return 0`
  - `extract_global_bortle.py:25-34` — 같은 분기 없음, 0 이하도 `return 1`
  - `extract_korea_bortle.py:69` — `if radiance <= 0.1: continue`
  - 현행 CSV 등급 분포에 0등급·1등급 행은 0건(인벤토리 E)
- 왜 문제인가: 전 지구용 스크립트로 데이터를 재생성하면 바다·무데이터 픽셀이 "가장 어두운 최상급 관측지(Class 1)"가 된다. 또 0등급이 CSV에 들어오면 엔진은 `realBortle > 1` 조건 때문에 **광해 감점을 아예 하지 않으면서**, 표시용 `BortleGrade.from(0)`은 기본값 Class 9("매우 밝음")를 반환해 점수와 표시가 정반대로 어긋난다.
- 근거(코드 측): [StargazingScoringEngine.java:99](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L99), [BortleGrade.java:26-31](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/enums/BortleGrade.java#L26-L31)
- 확실성: 사실(두 스크립트의 분기 차이와 엔진·enum의 동작). 현행 데이터에는 0등급이 없으므로 지금 당장 발생하는 오류는 아님.
- 완료 조건: 변환 함수가 한 곳에만 존재하고, 유효 등급 범위(1~9)를 벗어난 값이 적재 단계에서 거부된다.
- 사람이 결정해야 할 것: 무데이터 픽셀의 표현 방식(행 제외 vs 별도 등급).
- 제안 방향: 스크립트 통합 + 적재 시 등급 범위 검증을 추가한다.
- 규모: S
- 선행 이슈: radiance→Bortle 출처 이슈

### [P2] 화면에 표시된 감점 사유의 합계가 실제 점수와 맞지 않는다

- 현상: 사유 문자열은 항목별로 반올림(습도·달·광해)하거나 절삭(구름)해 표시하는데, 실제 점수는 반올림하지 않은 값을 모두 뺀 뒤 마지막에 한 번 반올림한다.
- 근거: [StargazingScoringEngine.java:62](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L62), [:71](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L71), [:92](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L92), [:102](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L102), [:106](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L106)
- 왜 문제인가: 예를 들어 습도 65%·Bortle 4면 실제 점수는 78.75→79인데, 표시는 "-3점", "-19점"이라 사용자가 더하면 78이 된다. "왜 이 점수인가"를 제품 핵심으로 내세우는데(DESIGN_DOC:59-61) 그 설명이 점수와 맞지 않는다.
- 확실성: 사실(계산 경로). 실제 응답 문자열은 실행 검증하지 않음.
- 완료 조건: 표시된 감점의 합과 최종 점수가 항상 일치한다.
- 사람이 결정해야 할 것: 정수 감점으로 통일할지, 소수점 표시로 갈지.
- 제안 방향: 감점을 만드는 시점에 한 번만 반올림하고 그 값을 점수와 표시 양쪽에 쓴다.
- 규모: S
- 선행 이슈: 없음

### [P2] 태양 고도 -6.0 임계가 3곳에 하드코딩되고 명칭이 서로 다르다

- 현상: 같은 -6.0이 엔진·예보·추천 세 곳에 각각 박혀 있다. 주석은 각각 "시민박명"(엔진)과 "천문학적 황혼"(예보)으로 서로 다르게 부른다. 또 코드는 `> -6.0`이라 정확히 -6.0°는 밤으로 보는데 README는 "-6° 이상이면 0점"으로 서술한다.
- 근거:
  - [StargazingScoringEngine.java:42](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L42) — `// 시민박명(-6도) 이상이면 '낮'`
  - [StargazingApplicationService.java:118-120](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/application/StargazingApplicationService.java#L118-L120) — `// -6도 이하(천문학적 황혼 이하)인 시간대만 계산`
  - [RecommendApplicationService.java:172-176](../../src/main/java/xyz/ncookie/stargazer/domain/recommend/application/RecommendApplicationService.java#L172-L176)
  - README:45
- 왜 문제인가: 한 값이 세 벌이라 한 곳만 바꾸면 API마다 판정이 갈린다. 명칭 불일치는 이 값이 무엇을 의미하는지에 대한 이해가 코드 안에서 합의되지 않았음을 보여준다.
- 확실성: 사실
- 완료 조건: 임계값이 단일 상수가 되고, 명칭과 경계 방향이 코드·주석·README에서 하나로 일치한다.
- 사람이 결정해야 할 것: 경계값 -6.0 정확히일 때의 판정 방향.
- 제안 방향: 상수화 후 세 호출부가 공유하게 하고 주석 명칭을 통일한다.
- 규모: S
- 선행 이슈: 없음

### [P2] 감점 총합이 100을 넘을 수 있어 0점이 여러 상태를 뭉갠다

- 현상: 항목별 최대 감점을 더하면 179점이라 실제 감점은 100을 초과할 수 있고, 최종적으로 0으로 클램프된다. 0점이 "구름 컷오프", "낮", "감점 누적 초과" 중 무엇인지 점수만으로는 구분되지 않는다.
- 근거: [StargazingScoringEngine.java:106](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L106) — `Math.max(0, currentScore)`, 감점 근거는 :60, :69, :78, :89, :100
- 왜 문제인가: 감점의 합이 100을 넘도록 설계된 것인지, 각 항목의 최대 감점 배분이 의도된 것인지 설명할 수 없다. README의 "최대 감점" 표는 항목별 상한만 적고 총합에 대해서는 아무 말도 하지 않는다(README:48-54).
- 확실성: 사실
- 완료 조건: 총합 상한에 대한 설계 의도가 문서화되고, 0점의 원인이 응답에서 구분된다.
- 사람이 결정해야 할 것: 가중합/정규화로 갈지, 현행 절대 감점 + 클램프를 유지할지.
- 제안 방향: 결정 전에는 코드를 바꾸지 말고, 현행 동작(최대 179점 감점 가능)을 문서에 먼저 명시한다.
- 규모: M
- 선행 이슈: 상수 출처 이슈 전부

### [P2] Gemini 응답의 `final_score`·등급 텍스트를 신뢰해 담지만 아무도 쓰지 않는다

- 현상: AI 응답의 `final_score`를 파싱해 결과 객체에 담고 주석도 "AI가 계산한 최종 점수 사용"이라 적혀 있지만, 실제 응답은 엔진 점수를 쓴다. `brightness`·`limiting_mag`도 마찬가지로 담기지만 화면에는 `BortleGrade` 값이 나간다. 프롬프트는 AI에게 이 값들을 생성하라고 계속 요구한다.
- 근거:
  - [GeminiAnalysisClient.java:92-98](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/client/gemini/GeminiAnalysisClient.java#L92-L98) — `root.path("final_score").asInt(finalScore) // AI가 계산한 최종 점수 사용`
  - [GeminiAnalysisClient.java:64-70](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/client/gemini/GeminiAnalysisClient.java#L64-L70) — JSON 포맷 요구
  - [StargazingApplicationService.java:75-77](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/application/StargazingApplicationService.java#L75-L77), [:92-93](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/application/StargazingApplicationService.java#L92-L93)
- 왜 문제인가: 점수의 출처가 코드상 두 개로 보인다. 누군가 `aiResult.finalScore()`를 쓰는 순간 사용자 점수가 LLM 출력으로 바뀌는데, 그 위험이 주석 때문에 오히려 정당해 보인다.
- 확실성: 사실
- 완료 조건: 점수의 단일 출처가 코드에서 자명해진다(AI 점수 파싱 제거 또는 명시적으로 무시 표기).
- 사람이 결정해야 할 것: AI에게 등급 텍스트 생성을 계속 요구할지(현재 미사용).
- 제안 방향: 미사용 필드와 프롬프트의 해당 요구를 함께 정리한다. 코멘트 생성만 남기는 방향.
- 규모: S
- 선행 이슈: 없음

### [P2] README·DESIGN_DOC의 점수 서술이 코드와 부분적으로 어긋난다

- 현상: 확인된 불일치 — ①DESIGN_DOC "습도 30% 미만" vs 코드 60%, ②README 구름 항목의 "최대 감점: 관측 불가"(실제 최대 -59점), ③README "태양 고도 -6° 이상 0점" vs 코드 `> -6.0`, ④달 감점 1점 미만 절삭 규칙 미기재, ⑤광해 폴백 4등급 미기재.
- 근거: [README.md:44-56](../../README.md#L44-L56), [docs/DESIGN_DOC.md:57](../DESIGN_DOC.md#L57), engine:42, :60, :68, :90, Provider:26
- 왜 문제인가: 인수인계 시 문서를 신뢰할 수 없다. "설명 가능한 코드베이스"라는 목표에 정면으로 걸린다.
- 확실성: 사실
- 완료 조건: README의 점수 절이 현행 코드 동작과 일치하고, DESIGN_DOC의 폐기된 서술이 정리된다.
- 사람이 결정해야 할 것: DESIGN_DOC을 이력 문서로 둘지, 갱신 대상으로 둘지.
- 제안 방향: 코드를 사실로 두고 문서를 맞춘다. 값 변경은 출처 확정 이슈에서 따로.
- 규모: S
- 선행 이슈: 없음

### [P3] "완벽한 관측 조건" 분기에 도달할 수 없다

- 현상: 100점 + 감점 사유 없음일 때만 표시되는 메시지가 있는데, CSV의 최소 Bortle이 2이고 데이터 미발견 시 폴백도 4이므로 광해 감점(최소 -6.25)이 항상 발생한다. 따라서 이 분기는 실행되지 않는다.
- 근거: [StargazingScoringEngine.java:99](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L99) — `if (realBortle > 1)`, [:109-111](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L109-L111), CSV 등급 분포(인벤토리 E)
- 왜 문제인가: 죽은 코드이자, 국내에서 도달 가능한 최고 점수가 94점(Class 2)이라는 사실이 어디에도 문서화되지 않았다.
- 확실성: 사실
- 완료 조건: 분기가 제거되거나 도달 가능한 조건으로 바뀌고, 국내 최고 도달 점수가 문서에 적힌다.
- 사람이 결정해야 할 것: 없음
- 제안 방향: 분기 제거 또는 임계 조정. README의 점수 구간 가이드에 실제 도달 범위를 병기한다.
- 규모: S
- 선행 이슈: 없음

### [P3] `radiance` 컬럼이 적재되지만 어디에서도 사용되지 않는다

- 현상: 광해 원측정값 radiance가 CSV에서 파싱되어 엔티티·DB에 저장되지만, 저장소 전체에 `getRadiance()` 호출부가 없다. 조회는 사전 계산된 `bortleClass`만 읽는다.
- 근거:
  - [LightPollutionMigrationRunner.java:58](../../src/main/java/xyz/ncookie/stargazer/infra/lightpollution/LightPollutionMigrationRunner.java#L58), [:62](../../src/main/java/xyz/ncookie/stargazer/infra/lightpollution/LightPollutionMigrationRunner.java#L62)
  - [LightPollution.java:36](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/entity/LightPollution.java#L36) — `private double radiance;`
  - [LightPollutionDataProvider.java:24-26](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/provider/LightPollutionDataProvider.java#L24-L26) — `LightPollution::getBortleClass`만 사용
  - `getRadiance` 검색 결과 0건(선언부 외)
- 왜 문제인가: 미사용 컬럼이라는 점보다, **이것이 재작업의 비용을 크게 줄이는 자산인데 활용되지 않고 있다**는 점이 중요하다. 585,018개 격자점의 원측정값이 이미 DB에 있으므로, 등급 변환을 코드로 옮기는 데 원본 래스터(11GB) 재처리나 CSV 재생성이 필요 없다.
- 확실성: 사실
- 완료 조건: radiance가 등급 산출의 실제 입력으로 쓰이거나, 쓰지 않기로 결정하고 컬럼을 제거한다.
- 사람이 결정해야 할 것: 없음(상위 A에서 함께 결정됨)
- 제안 방향: 상위 A의 첫 단계로 조회 경로가 radiance를 읽게 만든다.
- 규모: S
- 선행 이슈: 없음

### [P3] visibility null 방어가 점수 엔진에만 있고 표시·AI 경로에는 없다

- 현상: `visibility`는 `Integer`(nullable)인데 엔진만 null을 10000으로 대체하고, 응답 조립과 Gemini 프롬프트는 그대로 언박싱한다.
- 근거:
  - [OpenWeatherResponse.java:11](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/client/openweather/OpenWeatherResponse.java#L11)
  - [StargazingScoringEngine.java:38](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L38)
  - [StargazingApplicationService.java:81](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/application/StargazingApplicationService.java#L81), [GeminiAnalysisClient.java:76](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/client/gemini/GeminiAnalysisClient.java#L76)
- 왜 문제인가: 같은 필드에 대한 결측 처리 정책이 파일마다 다르다. 응답에 visibility가 없는 경우 분석 API가 NPE로 실패할 수 있다(발생 조건은 확인 못 함 — 실제 API가 이 필드를 생략하는지 미검증).
- 확실성: 사실(방어 코드 부재). NPE 발생 여부는 추정.
- 완료 조건: 결측 대체값이 한 곳에서 결정되고 모든 소비자가 같은 값을 본다.
- 사람이 결정해야 할 것: 없음
- 제안 방향: 외부 응답을 내부 모델로 정규화하는 지점에서 한 번만 대체한다.
- 규모: S
- 선행 이슈: 없음

### [P3] 날씨 응답 자체가 null이면 점수 계산이 NPE로 죽는다

- 현상: `getForObject`는 null을 반환할 수 있고 매퍼도 null을 반환하는 경로가 있는데, 엔진은 `weather.main()`을 곧바로 호출한다. null 검사는 없다.
- 근거:
  - [OpenWeatherMapClient.java:42](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/client/openweather/OpenWeatherMapClient.java#L42)
  - [OpenWeatherResponseMapper.java:12-14](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/dto/mapper/OpenWeatherResponseMapper.java#L12-L14) — `return null; // 혹은 예외 처리`
  - [StargazingScoringEngine.java:37-38](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L37-L38)
- 왜 문제인가: 예외 경로가 "더미로 대체"와 "null 반환"으로 갈려 있어 실패 시 동작을 예측할 수 없다.
- 확실성: 사실(null 체크 부재와 null 반환 경로의 존재). 실제 null 응답 발생 여부는 확인 못 함.
- 완료 조건: 엔진 입력이 non-null임이 계약으로 보장되거나, 진입부에서 명시적으로 처리된다.
- 사람이 결정해야 할 것: 없음(단, 실패 정책 이슈와 함께 다루는 편이 낫다)
- 제안 방향: 매퍼의 null 반환을 없애고 엔진 진입부 계약을 명시한다.
- 규모: S
- 선행 이슈: 날씨 API 실패 위장 이슈

### [P3] 예보 API 실패가 빈 목록 200 응답으로 나간다

- 현상: 예보 데이터를 못 가져오면 빈 `DailyForecast` 목록으로 정상 응답한다. 클라이언트는 "관측 가능한 시간대가 없음"과 구분할 수 없다.
- 근거: [StargazingApplicationService.java:107-111](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/application/StargazingApplicationService.java#L107-L111), [OpenWeatherMapClient.java:64-69](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/client/openweather/OpenWeatherMapClient.java#L64-L69)
- 왜 문제인가: 장애가 정상 응답으로 은폐된다.
- 확실성: 사실
- 완료 조건: 실패와 "결과 없음"이 응답에서 구분된다.
- 사람이 결정해야 할 것: 실패 시 응답 형태(에러 vs 상태 플래그).
- 제안 방향: 날씨 API 실패 정책 이슈와 같은 규칙을 적용한다.
- 규모: S
- 선행 이슈: 날씨 API 실패 위장 이슈

### [P3] 점수 엔진에 대한 테스트가 하나도 없다

- 현상: 테스트 소스는 컨텍스트 로드 테스트 1개뿐이다. 경계값(구름 70, 태양 -6.0, 습도 60, 시정 5000/10000, 달 고도 0)에 대한 회귀 방어가 없다.
- 근거: `src/test/java/xyz/ncookie/stargazer/StargazerApplicationTests.java` (테스트 파일 전체 목록에서 유일)
- 왜 문제인가: 상수 출처 정리 과정에서 값을 손대야 하는데, 현행 동작을 고정해 둔 것이 없어 변경의 영향을 알 수 없다.
- 확실성: 사실
- 완료 조건: 현행 동작을 그대로 고정하는 경계값 테스트가 존재한다(값을 바꾸지 않는 특성화 테스트).
- 사람이 결정해야 할 것: 없음
- 제안 방향: 상수 정리 착수 **전에** 현행 결과를 스냅샷하는 테스트를 먼저 둔다.
- 규모: M
- 선행 이슈: 없음(다만 상수 출처 이슈들의 선행 작업으로 두는 것을 권장)

### [P3] 달 위상 각도 경계와 suncalc 위상 부호 규약이 검증되지 않았다

- 현상: 위상 명칭 경계(±10, ±80, ±100, ±170)가 매직넘버이고, "0이 보름, ±180이 삭"이라는 규약은 주석으로만 존재한다.
- 근거: [MoonPhase.java:28-53](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/enums/MoonPhase.java#L28-L53), 값 생성부는 [AstronomyCalculator.java:23](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/component/AstronomyCalculator.java#L23)
- 왜 문제인가: 규약이 틀렸다면 상현/하현이 통째로 뒤집혀 표시되는데 점수에는 영향이 없어 발견되지 않는다.
- 확실성: 추정. commons-suncalc의 `getPhase()` 부호 규약은 **확인 못 함**(라이브러리 문서를 이번 단계에서 확인하지 않음).
- 완료 조건: 규약이 확인되고, 알려진 날짜(예: 특정 보름/삭)로 위상 명칭이 검증된다.
- 사람이 결정해야 할 것: 없음
- 제안 방향: 라이브러리 규약 확인 후 경계 상수를 이름 있는 상수로 추출한다.
- 규모: S
- 선행 이슈: 없음

### [P4] 광해 캐시 키가 원시 double 문자열이라 사실상 캐시가 동작하지 않는다

- 현상: 캐시 키가 `#lat + '-' + #lon`이라 소수점 이하가 다르면 전부 다른 키가 된다. L1은 500개 상한이다.
- 근거: [LightPollutionDataProvider.java:21](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/provider/LightPollutionDataProvider.java#L21), [CacheConfig.java:36](../../src/main/java/xyz/ncookie/stargazer/global/config/CacheConfig.java#L36) — `maximumSize=500`
- 왜 문제인가: 광해 데이터는 사실상 정적인데 좌표가 조금만 달라도 매번 공간 쿼리를 탄다. 참고로 예보 캐시는 `%.4f` 포맷으로 좌표를 정규화한다(OpenWeatherMapClient:55) — 같은 문제에 대한 처리가 두 곳에서 다르다.
- 확실성: 사실(키 생성식). 실제 히트율은 확인 못 함.
- 완료 조건: 좌표 정규화 규칙이 두 캐시에서 동일해진다.
- 사람이 결정해야 할 것: 격자 정규화 단위(CSV 격자가 약 0.004도).
- 제안 방향: 예보 캐시와 같은 방식으로 좌표를 격자에 스냅해 키를 만든다.
- 규모: S
- 선행 이슈: 없음

### [P4] 광해 탐색 박스 ±0.01도가 CSV 격자와 무관하게 고정돼 있다

- 현상: 주석은 "약 1km x 1km"라 적혀 있지만 위도 0.01도와 경도 0.01도는 위도 37도 부근에서 실제 거리가 다르다. CSV 격자는 약 0.004도이고 결측 구간(예: lon 126.146 → 126.159)이 존재한다.
- 근거: [LightPollutionRepository.java:15-33](../../src/main/java/xyz/ncookie/stargazer/domain/stargazing/repository/LightPollutionRepository.java#L15-L33), CSV 격자 확인(인벤토리 E)
- 왜 문제인가: 박스 안에 데이터가 없으면 조용히 폴백 4등급이 된다. 박스 크기와 격자 간격·결측 분포의 관계가 설명되어 있지 않다.
- 확실성: 사실(값과 격자 간격). 실제 폴백 발생 빈도는 확인 못 함.
- 완료 조건: 탐색 반경의 근거가 격자 간격과 함께 문서화되고, 폴백 발생이 관측 가능해진다.
- 사람이 결정해야 할 것: 폴백 대신 확대 재탐색을 할지.
- 제안 방향: 먼저 폴백 발생률을 계측할 수 있게 하고, 반경 조정은 그 수치를 보고 결정한다.
- 규모: S
- 선행 이슈: 광해 좌표 순서 이슈

---

## 우선순위별 요약

### 상위 이슈 (개별 이슈를 묶는 단위, 아래 표와 중복 계상하지 않음)

| 우선순위 | 이슈 | 하위 | 규모 |
|---|---|---|---|
| **P1** | (상위 A) 광해 데이터 파이프라인 재작업 | 8건 | L |
| **P2** | (상위 B) 감점 모델 재설계 | 10건 | L |

### 개별 이슈 27건

| 우선순위 | 건수 | 이슈 |
|---|---|---|
| **P0** | 0 | 없음 (배포 후 사고로 직결되는 항목은 이 범위에서 발견되지 않음) |
| **P1** | 6 | 광해 조회 좌표 순서 불일치 / 날씨 API 실패의 "구름 100%" 위장 / 범위 밖 시각 요청의 무관한 예보 사용 / 한계등급 이중 계산 / 데이터 범위 밖 좌표의 조용한 Bortle 4 폴백 / 광해 부분 적재 고착 |
| **P2** | 12 | 광해 6.25 출처 없음 / 달 ×30·절삭 출처 없음 / 구름 70·10·기울기 출처 없음 / 습도 60·2 출처 없음 + DESIGN_DOC 불일치 / 시정 분류표 이원화 / radiance→Bortle 규칙이 저장소 밖 + 임계값 출처 없음 / 변환 스크립트 이중화 및 무데이터 처리 상이 / 표시 감점 합계와 점수 불일치 / -6.0 3중 하드코딩 및 명칭 불일치 / 감점 총합 100 초과 / 문서-코드 불일치 / Gemini `final_score` 미사용 파싱 |
| **P3** | 7 | 100점 분기 도달 불가 / `radiance` 컬럼 미사용 / visibility null 방어 비대칭 / 날씨 응답 null 미방어 / 예보 실패의 빈 목록 200 / 점수 엔진 테스트 부재 / 달 위상 규약 미검증 |
| **P4** | 2 | 광해 캐시 키 정규화 부재 / 탐색 박스 ±0.01도 근거 없음 |

### 착수 순서

합의된 순서는 **테스트 → 상위 A → 상위 B**이며, 어느 상위 이슈에도 속하지 않는 개별 이슈는 그 사이에 끼워 넣는다.

1. **점수 엔진 특성화 테스트(P3)** — 현행 동작을 먼저 고정. 이후 모든 작업의 안전망.
   상위 A·B 양쪽의 선행 조건이다.
2. **상위 A: 광해 파이프라인 재작업(P1)** — radiance가 이미 DB에 있으므로 데이터 재생성 없이 착수 가능.
   단, 좌표 순서 문제는 **DB에서 실제 반환 행을 확인하는 것이 선행**되어야 한다(어느 쪽이 틀렸는지 미확정).
   이 단계에서 임계값을 바꾸지 않는다 — 구조 이관과 값 변경을 섞으면 회귀 원인을 분리할 수 없다.
3. **상위 A에 속하지 않는 P1 3건** — 날씨 API 실패 위장 / 범위 밖 시각 요청 / 한계등급 이중 계산.
   (한계등급 건은 상위 B의 하위이기도 하므로 B에서 함께 처리 가능)
4. **상위 B: 감점 모델 재설계(P2)** — 계수 근거 확정(사람) → `docs/SCORING.md` → 코드 반영.
   A가 끝난 뒤 착수해야 광해 부분을 두 번 건드리지 않는다.
5. 남은 **P3/P4**.

> 이 문서는 출처를 확정하지 않았다. 계수의 근거(논문/표준/데이터셋 문서)를 찾는 작업은 다음 단계이며,
> 그 전까지는 어떤 계수도 "타당하다/부당하다"고 판단하지 않는다.
