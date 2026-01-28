# 아키텍처 가이드

이 문서는 Stargazer 프로젝트의 4계층 아키텍처 설계 원칙과 코딩 가이드라인을 설명합니다.

## 목차

1. [아키텍처 개요](#아키텍처-개요)
2. [계층별 역할과 책임](#계층별-역할과-책임)
3. [의존성 방향 규칙](#의존성-방향-규칙)
4. [Application Service 가이드라인](#application-service-가이드라인)
5. [Domain Service 가이드라인](#domain-service-가이드라인)
6. [Repository 사용 가이드라인](#repository-사용-가이드라인)
7. [네이밍 규칙](#네이밍-규칙)
8. [실용적인 예시](#실용적인-예시)

---

## 아키텍처 개요

프로젝트는 **4계층 아키텍처**를 따릅니다:

```
Controller → Application → Domain → Repository
```

각 계층은 명확한 책임을 가지며, 단방향 의존성 구조를 유지합니다.

### 계층 구조

```
domain/
  {domain-name}/
    controller/          # HTTP 요청/응답 처리
    application/         # 유스케이스 오케스트레이션
      dto/              # Application 계층 DTO (Command 등)
    domain/             # 도메인 로직
    entity/             # 도메인 엔티티
    repository/         # 영속성 계층
    dto/                # 공통 DTO (Request/Response)
    exception/          # 도메인 예외
```

---

## 계층별 역할과 책임

### 1. Controller 계층

**역할**: HTTP 요청/응답 처리

**책임**:
- HTTP 요청 파라미터 바인딩 및 검증
- Application 계층 호출
- 응답 DTO 변환 및 반환
- **비즈니스 로직 금지**

**예시**:
```java
@RestController
@RequiredArgsConstructor
public class BookmarkController {
    
    private final BookmarkApplicationService bookmarkApplicationService;
    
    @PostMapping
    public BookmarkResponse addBookmark(
        @AuthenticationPrincipal MemberPrincipal principal,
        @Valid @RequestBody AddBookmarkRequest request
    ) {
        return bookmarkApplicationService.create(
            AddBookmarkCommand.from(principal.getMemberId(), request)
        );
    }
}
```

---

### 2. Application 계층

**역할**: 유스케이스 오케스트레이션 및 트랜잭션 관리

**책임**:
- 트랜잭션 경계 설정 (`@Transactional`)
- 여러 Domain Service 조합
- 다른 도메인의 Domain Service 호출 (이 계층에서만 허용)
- 단순 조회/저장 시 Repository 직접 호출 가능
- 유스케이스는 메서드 단위로 표현 (`create`, `updateName`, `delete` 등)

**예시**:
```java
@Service
@RequiredArgsConstructor
public class BookmarkApplicationService {
    
    private final BookmarkDomainService bookmarkDomainService;
    private final MemberDomainService memberDomainService;
    private final ObservationSpotDomainService observationSpotDomainService;
    private final BookmarkRepository bookmarkRepository;
    
    @Transactional
    public BookmarkResponse create(AddBookmarkCommand command) {
        // 다른 도메인의 Domain Service 호출
        Member member = memberDomainService.findById(command.memberId());
        ObservationSpot spot = observationSpotDomainService.findById(command.spotId());
        
        // Entity 생성 및 저장
        Bookmark newBookmark = Bookmark.builder()
            .member(member)
            .spot(spot)
            .build();
        
        return BookmarkResponse.from(bookmarkRepository.save(newBookmark));
    }
    
    @Transactional
    public BookmarkResponse updateName(Long memberId, Long bookmarkId, ModifyBookmarkCommand command) {
        // Domain Service를 통한 도메인 로직 실행
        Bookmark bookmark = bookmarkDomainService.findById(bookmarkId);
        bookmarkDomainService.validateOwner(memberId, bookmark);
        bookmarkDomainService.updateBookmark(bookmark, command.name(), command.memo());
        
        return BookmarkResponse.from(bookmark);
    }
}
```

---

### 3. Domain 계층

**역할**: 도메인 규칙, 검증, 상태 전이

**책임**:
- 도메인 비즈니스 로직 구현
- 도메인 규칙 검증
- Entity 상태 전이 관리
- **단일 도메인 책임만 가짐**
- **다른 도메인의 Domain Service 직접 참조 금지**

**예시**:
```java
@Service
@RequiredArgsConstructor
public class BookmarkDomainService {
    
    private final BookmarkRepository bookmarkRepository;
    
    public Bookmark findById(Long bookmarkId) {
        return bookmarkRepository.findById(bookmarkId)
            .orElseThrow(() -> new BookmarkException(BookmarkErrorCode.BOOKMARK_NOT_FOUND));
    }
    
    public void validateOwner(Long memberId, Bookmark bookmark) {
        if (!bookmark.isOwner(memberId)) {
            throw new BookmarkException(BookmarkErrorCode.BOOKMARK_UNAUTHORIZED);
        }
    }
    
    public void updateBookmark(Bookmark bookmark, String name, String memo) {
        bookmark.updateBookmark(name, memo);
    }
}
```

---

### 4. Repository 계층

**역할**: 영속성 책임

**책임**:
- 데이터 조회 및 저장
- **비즈니스 로직 금지**
- 단순 CRUD 작업만 수행

**예시**:
```java
@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    
    List<Bookmark> findAllByMember_Id(Long memberId);
}
```

---

## 의존성 방향 규칙

### 허용되는 의존성

```
Controller → Application → Domain → Repository
```

### 금지되는 의존성

- ❌ Domain → Application (역방향)
- ❌ Domain → Domain (다른 도메인 간 직접 참조)
- ❌ Repository → Domain (역방향)
- ❌ 순환 참조 (절대 금지)

### 도메인 간 통신

**올바른 방법**:
```java
// Application 계층에서 다른 도메인의 Domain Service 호출
@Service
public class BookmarkApplicationService {
    private final MemberDomainService memberDomainService;  // ✅ OK
    
    public void create(AddBookmarkCommand command) {
        Member member = memberDomainService.findById(command.memberId());  // ✅ OK
    }
}
```

**잘못된 방법**:
```java
// Domain 계층에서 다른 도메인의 Domain Service 직접 참조
@Service
public class BookmarkDomainService {
    private final MemberDomainService memberDomainService;  // ❌ 금지!
}
```

---

## Application Service 가이드라인

### Repository 직접 호출이 적절한 경우

#### 1. 단순 조회 (도메인 로직 없음)
```java
@Transactional(readOnly = true)
public List<BookmarkResponse> getBookmarkList(Long memberId) {
    return bookmarkRepository.findAllByMember_Id(memberId)
        .stream()
        .map(BookmarkResponse::from)
        .toList();
}
```

#### 2. 단순 저장 (Entity가 도메인 로직 포함)
```java
@Transactional
public BookmarkResponse create(AddBookmarkCommand command) {
    Bookmark newBookmark = Bookmark.builder()  // Entity 내부에서 검증
        .member(member)
        .type(command.type())
        .build();
    
    return BookmarkResponse.from(bookmarkRepository.save(newBookmark));
}
```

### Domain Service를 거쳐야 하는 경우

#### 1. 도메인 규칙/검증이 필요한 경우
```java
@Transactional
public BookmarkResponse updateName(Long memberId, Long bookmarkId, ModifyBookmarkCommand command) {
    // 도메인 로직: 소유자 검증 필요
    Bookmark bookmark = bookmarkDomainService.findById(bookmarkId);
    bookmarkDomainService.validateOwner(memberId, bookmark);  // ✅ Domain Service 사용
    bookmarkDomainService.updateBookmark(bookmark, command.name(), command.memo());
    
    return BookmarkResponse.from(bookmark);
}
```

#### 2. 재사용 가능한 도메인 로직
```java
// 여러 곳에서 사용되는 도메인 로직은 Domain Service에 배치
public Bookmark findById(Long bookmarkId) {
    return bookmarkRepository.findById(bookmarkId)
        .orElseThrow(() -> new BookmarkException(BookmarkErrorCode.BOOKMARK_NOT_FOUND));
}
```

### 트랜잭션 관리

- 모든 public 메서드에 `@Transactional` 명시
- 읽기 전용 작업: `@Transactional(readOnly = true)`
- 쓰기 작업: `@Transactional`

---

## Domain Service 가이드라인

### Domain Service를 사용하는 경우

#### 1. Entity에 속하지 않는 도메인 로직
```java
// 여러 Entity를 조합하거나 복잡한 비즈니스 규칙
public void validateOwner(Long memberId, Bookmark bookmark) {
    if (!bookmark.isOwner(memberId)) {
        throw new BookmarkException(BookmarkErrorCode.BOOKMARK_UNAUTHORIZED);
    }
}
```

#### 2. Repository 조회 + 도메인 로직
```java
public Bookmark findById(Long bookmarkId) {
    return bookmarkRepository.findById(bookmarkId)
        .orElseThrow(() -> new BookmarkException(BookmarkErrorCode.BOOKMARK_NOT_FOUND));
}
```

#### 3. 재사용 가능한 도메인 로직
```java
// 여러 Application Service에서 사용되는 로직
public void updateBookmark(Bookmark bookmark, String name, String memo) {
    bookmark.updateBookmark(name, memo);
}
```

### Domain Service를 사용하지 않는 경우

#### 1. 단순 Entity 메서드 호출
```java
// Entity에 이미 있는 메서드는 직접 호출
bookmark.updateBookmark(name, memo);  // ✅ Entity 메서드 직접 호출
```

#### 2. 단순 Repository 조회
```java
// 도메인 로직이 없는 단순 조회는 Application Service에서 직접 호출 가능
List<Bookmark> bookmarks = bookmarkRepository.findAllByMember_Id(memberId);
```

---

## Repository 사용 가이드라인

### Repository의 역할

- **영속성 책임만 가짐**
- 데이터 조회 및 저장
- 비즈니스 로직 금지

### Repository 사용 위치

#### Application Service에서 직접 호출
```java
// 단순 조회/저장
@Transactional(readOnly = true)
public List<BookmarkResponse> getBookmarkList(Long memberId) {
    return bookmarkRepository.findAllByMember_Id(memberId)  // ✅ 직접 호출
        .stream()
        .map(BookmarkResponse::from)
        .toList();
}
```

#### Domain Service를 통한 호출
```java
// 도메인 로직이 필요한 경우
public Bookmark findById(Long bookmarkId) {
    return bookmarkRepository.findById(bookmarkId)  // Domain Service를 통해 호출
        .orElseThrow(() -> new BookmarkException(BookmarkErrorCode.BOOKMARK_NOT_FOUND));
}
```

---

## 네이밍 규칙

### Application Service
- 패턴: `{Domain}ApplicationService`
- 예시: `BookmarkApplicationService`, `MemberApplicationService`

### Application 메서드
- 유스케이스는 메서드 단위로 표현
- 예시: `create()`, `updateName()`, `delete()`, `getBookmarkList()`

### Domain Service
- 패턴: `{Domain}DomainService`
- 예시: `BookmarkDomainService`, `MemberDomainService`

### Domain Service 메서드
- 동사로 시작하는 명확한 이름
- 예시: `findById()`, `validateOwner()`, `updateBookmark()`

### Command DTO
- Application 계층의 입력 DTO
- 패턴: `{Action}{Domain}Command`
- 예시: `AddBookmarkCommand`, `ModifyBookmarkCommand`
- **팩토리 메서드**: `from()` 메서드를 제공하여 Request DTO에서 변환
  ```java
  // Command 클래스
  public record AddBookmarkCommand(...) {
      public static AddBookmarkCommand from(Long memberId, AddBookmarkRequest request) {
          return new AddBookmarkCommand(
              memberId,
              request.type(),
              request.spotId(),
              // ...
          );
      }
  }
  
  // Controller에서 사용
  return bookmarkApplicationService.create(
      AddBookmarkCommand.from(principal.getMemberId(), request)
  );
  ```

---

## 실용적인 예시

### 예시 1: 단순 조회 (Repository 직접 호출)

```java
// Application Service
@Transactional(readOnly = true)
public List<BookmarkResponse> getBookmarkList(Long memberId) {
    return bookmarkRepository.findAllByMember_Id(memberId)
        .stream()
        .map(BookmarkResponse::from)
        .toList();
}
```

**이유**: 도메인 로직이 없는 단순 조회이므로 Repository 직접 호출이 적절합니다.

---

### 예시 2: 도메인 로직이 필요한 경우 (Domain Service 사용)

```java
// Controller
@PatchMapping("/{bookmarkId}")
public BookmarkResponse modifyBookmark(
    @AuthenticationPrincipal MemberPrincipal principal,
    @PathVariable Long bookmarkId,
    @Valid @RequestBody ModifyBookmarkRequest request
) {
    return bookmarkApplicationService.updateName(
        principal.getMemberId(),
        bookmarkId,
        ModifyBookmarkCommand.from(request)
    );
}

// Application Service
@Transactional
public BookmarkResponse updateName(Long memberId, Long bookmarkId, ModifyBookmarkCommand command) {
    Bookmark bookmark = bookmarkDomainService.findById(bookmarkId);
    bookmarkDomainService.validateOwner(memberId, bookmark);  // 도메인 규칙 검증
    bookmarkDomainService.updateBookmark(bookmark, command.name(), command.memo());
    
    return BookmarkResponse.from(bookmark);
}

// Domain Service
public void validateOwner(Long memberId, Bookmark bookmark) {
    if (!bookmark.isOwner(memberId)) {
        throw new BookmarkException(BookmarkErrorCode.BOOKMARK_UNAUTHORIZED);
    }
}
```

**이유**: 소유자 검증이라는 도메인 규칙이 필요하므로 Domain Service를 사용합니다.

---

### 예시 3: 다른 도메인 참조 (Application 계층에서만 허용)

```java
// Controller
@PostMapping
public BookmarkResponse addBookmark(
    @AuthenticationPrincipal MemberPrincipal principal,
    @Valid @RequestBody AddBookmarkRequest request
) {
    return bookmarkApplicationService.create(
        AddBookmarkCommand.from(principal.getMemberId(), request)
    );
}

// Application Service
@Transactional
public BookmarkResponse create(AddBookmarkCommand command) {
    // 다른 도메인의 Domain Service 호출 (Application 계층에서만 허용)
    Member member = memberDomainService.findById(command.memberId());
    ObservationSpot spot = observationSpotDomainService.findById(command.spotId());
    
    Bookmark newBookmark = Bookmark.builder()
        .member(member)
        .spot(spot)
        .build();
    
    return BookmarkResponse.from(bookmarkRepository.save(newBookmark));
}
```

**이유**: 여러 도메인을 조합하는 오케스트레이션은 Application 계층의 책임입니다.

---

### 예시 4: Entity가 도메인 로직을 포함하는 경우

```java
// Application Service
@Transactional
public BookmarkResponse create(AddBookmarkCommand command) {
    Bookmark newBookmark = Bookmark.builder()  // Entity 내부에서 검증 수행
        .member(member)
        .type(command.type())
        .spot(spot)
        .build();  // builder 내부에서 도메인 규칙 검증
    
    return BookmarkResponse.from(bookmarkRepository.save(newBookmark));
}

// Entity
@Builder
public Bookmark(...) {
    if (type == BookmarkType.SPOT && spot == null) {
        throw new BookmarkException(BookmarkErrorCode.INVALID_BOOKMARK_TYPE);
    }
    // ...
}
```

**이유**: Entity 생성 시점에 도메인 규칙이 적용되므로, Application Service에서 직접 Repository를 호출해도 됩니다.

---

## 체크리스트

새로운 기능을 추가할 때 다음을 확인하세요:

### Controller 작성 시
- [ ] Application Service만 호출하는가?
- [ ] 비즈니스 로직이 없는가?
- [ ] HTTP 요청/응답 처리만 하는가?

### Application Service 작성 시
- [ ] `@Transactional`이 적절히 설정되었는가?
- [ ] 다른 도메인 참조 시 Domain Service를 호출하는가?
- [ ] 단순 조회/저장은 Repository 직접 호출이 적절한가?
- [ ] 도메인 로직이 필요한 경우 Domain Service를 사용하는가?

### Domain Service 작성 시
- [ ] 단일 도메인 책임만 가지는가?
- [ ] 다른 도메인의 Domain Service를 직접 참조하지 않는가?
- [ ] 도메인 규칙/검증을 담당하는가?

### Repository 작성 시
- [ ] 영속성 책임만 가지는가?
- [ ] 비즈니스 로직이 없는가?

---

## 참고 사항

- **실용성 우선**: 과도한 추상화는 피하고, 실용적인 구조를 유지합니다.
- **일관성**: 같은 패턴의 코드는 일관되게 작성합니다.
- **명확성**: 코드만으로도 의도가 명확해야 합니다.
- **유지보수성**: 변경에 유연하게 대응할 수 있는 구조를 유지합니다.

---

## 관련 문서

- [DESIGN_DOC.md](./DESIGN_DOC.md) - 프로젝트 전체 설계 문서
- [SECURITY_AUTH_GUIDE.md](./SECURITY_AUTH_GUIDE.md) - 보안 및 인증 가이드
