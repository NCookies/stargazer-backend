# Swagger API 문서 생성 가이드

## 개요
이 프로젝트는 SpringDoc OpenAPI를 사용하여 Swagger API 문서를 자동 생성합니다.

## Swagger UI 접근
애플리케이션 실행 후 다음 URL로 접근할 수 있습니다:
- **로컬**: http://localhost:8080/swagger-ui.html
- **프로덕션**: https://api.byeolbolil.xyz/swagger-ui.html

## OpenAPI JSON 파일 생성 방법

### 방법 1: 브라우저에서 직접 다운로드
1. 애플리케이션을 실행합니다.
2. 브라우저에서 다음 URL로 접근합니다:
   - **로컬**: http://localhost:8080/v3/api-docs
   - **프로덕션**: https://api.byeolbolil.xyz/v3/api-docs
3. 브라우저에서 JSON 파일을 저장합니다 (Ctrl+S 또는 우클릭 > 다른 이름으로 저장).
4. 파일명을 `openapi.json`으로 저장합니다.

### 방법 2: curl 명령어 사용 (Windows PowerShell)
```powershell
# 로컬 서버에서 다운로드
curl -o openapi.json http://localhost:8080/v3/api-docs

# 프로덕션 서버에서 다운로드
curl -o openapi.json https://api.byeolbolil.xyz/v3/api-docs
```

### 방법 3: Gradle Task로 자동 생성 (추천)
프로젝트 루트에 다음 스크립트를 추가할 수 있습니다:

```gradle
// build.gradle에 추가
task generateOpenApi {
    doLast {
        def apiDocsUrl = "http://localhost:8080/v3/api-docs"
        def outputFile = file("${projectDir}/openapi.json")
        
        println "Fetching OpenAPI docs from ${apiDocsUrl}..."
        def connection = new URL(apiDocsUrl).openConnection()
        connection.setRequestProperty("Accept", "application/json")
        
        def json = connection.inputStream.text
        outputFile.text = json
        println "OpenAPI JSON saved to ${outputFile.absolutePath}"
    }
}
```

사용 방법:
```bash
# 애플리케이션 실행 후 다른 터미널에서
./gradlew generateOpenApi
```

### 방법 4: Maven 플러그인 사용 (Maven 프로젝트인 경우)
```xml
<plugin>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-maven-plugin</artifactId>
    <version>1.4</version>
    <executions>
        <execution>
            <phase>integration-test</phase>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## 생성된 JSON 파일 활용
생성된 `openapi.json` 파일은 다음 용도로 사용할 수 있습니다:
1. **프론트엔드 API 클라이언트 자동 생성**: OpenAPI Generator 등을 사용하여 TypeScript/JavaScript 클라이언트 코드 생성
2. **API 문서 공유**: Postman, Insomnia 등 API 테스트 도구에 import
3. **API 테스트 자동화**: 다양한 테스트 도구에서 스키마 기반 테스트 작성

## 주의사항
- 애플리케이션이 실행 중이어야 JSON 파일을 생성할 수 있습니다.
- 프로덕션 환경에서 다운로드할 경우, 실제 배포된 서버의 최신 API 스펙을 반영합니다.
- 개발 중에는 로컬 서버에서 다운로드하는 것을 권장합니다.
