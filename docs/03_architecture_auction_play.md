# AuctionPlay - API 명세서

| 항목 | 내용 |
|---|---|
| 문서 종류 | API Specification |
| 프로젝트명 | AuctionPlay |
| 버전 | v1.0 |
| 작성일 | 2026.08.16 |
| 참조 문서 | `auction_project_spec.md`, `04_db_design_auction_play.md` |
| Base URL | `http://localhost:8080` |
| 상태 | Draft |

---

## 0. 문서 목적

MVP 범위의 REST API를 요청·응답 형식 수준까지 확정한다.
이 문서를 그대로 보고 DTO / Controller / Service를 작성할 수 있는 수준을 목표로 한다.

---

## 1. 설계 원칙

| # | 원칙 | 내용 |
|---|---|---|
| 1 | 리소스 중심 URL | 동사가 아닌 명사. `/api/products` (O), `/api/getProducts` (X) |
| 2 | HTTP 메서드로 행위 표현 | GET 조회 / POST 생성 / PATCH 부분수정 / DELETE 삭제 |
| 3 | 계층 구조 표현 | 종속 리소스는 경로에 중첩 → `/api/auctions/{id}/bids` |
| 4 | Entity를 직접 노출하지 않음 | 요청·응답 모두 DTO 사용 (2장 참조) |
| 5 | 검증은 서버에서 | 프론트 검증은 UX용. 서버 검증이 진짜 방어선 |
| 6 | 에러 응답 형식 통일 | 5장 참조 |
| 7 | 목록 조회는 페이징 | 기본 20건 |

---

## 2. Entity를 직접 반환하지 않는 이유

**이 프로젝트의 핵심 설계 결정 중 하나.** 면접 단골 질문이므로 근거를 명확히 한다.

| 문제 | 설명 |
|---|---|
| 민감 정보 노출 | `User` Entity를 그대로 반환하면 `password` 해시가 응답에 포함됨 |
| 순환 참조 | `Auction` → `Bid` → `Auction` 무한 루프 → JSON 직렬화 실패 |
| LAZY 로딩 예외 | 트랜잭션 종료 후 프록시 접근 시 `LazyInitializationException` |
| API 스펙 불안정 | Entity 필드명을 바꾸면 API 응답 형식이 같이 바뀜 → 클라이언트 깨짐 |
| 과다 노출 | 내부 관리용 필드(`version` 등)까지 외부에 드러남 |

→ **요청은 Request DTO, 응답은 Response DTO로 변환한다.**

### DTO 명명 규칙

```
{도메인}{행위}Request      예) ProductCreateRequest
{도메인}{용도}Response     예) ProductDetailResponse, ProductSummaryResponse
```

목록용(Summary)과 상세용(Detail)을 분리한다. 목록에서 설명 전문을 내려줄 이유가 없다.

---

## 3. 공통 규격

### 3.1 요청 헤더

| 헤더 | 값 | 비고 |
|---|---|---|
| `Content-Type` | `application/json` | POST/PATCH 시 |
| `Authorization` | `Bearer {token}` | **인증 도입 이후 적용** (현 단계 미적용) |

> **현 단계 인증 정책**: Spring Security 기본 인증을 비활성화하고 모든 엔드포인트를 개방한다. 인증은 별도 단계에서 도입하며, 그때 이 문서를 v2.0으로 갱신한다.

### 3.2 페이징 파라미터

목록 조회 API 공통.

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `page` | int | 0 | 페이지 번호 (0부터 시작) |
| `size` | int | 20 | 페이지당 건수 (최대 100) |
| `sort` | string | `id,desc` | 정렬 기준 |

**페이징 응답 공통 형식**

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 137,
  "totalPages": 7,
  "first": true,
  "last": false
}
```

> Spring Data의 `Page<T>`를 그대로 반환하면 내부 구조(`pageable`, `sort` 객체 등)가 노출되고 불필요하게 장황하다. 위 형식으로 감싸는 `PageResponse<T>`를 만들어 사용한다.

### 3.3 날짜 형식

`ISO-8601` — `2026-08-16T20:30:00`

---

## 4. API 목록 (전체)

### 4.1 상품 (Product)

| # | 메서드 | 경로 | 설명 | 권한 | 단계 |
|---|---|---|---|---|---|
| P-01 | POST | `/api/products` | 상품 등록 | 관리자 | **현재** |
| P-02 | GET | `/api/products` | 상품 목록 | 전체 | **현재** |
| P-03 | GET | `/api/products/{id}` | 상품 상세 | 전체 | **현재** |
| P-04 | PATCH | `/api/products/{id}` | 상품 수정 | 관리자 | **현재** |
| P-05 | DELETE | `/api/products/{id}` | 상품 삭제 | 관리자 | **현재** |
| P-06 | POST | `/api/products/{id}/view` | 조회 인정 (5초 체류) | 회원 | 입찰 단계 |

### 4.2 경매 (Auction)

| # | 메서드 | 경로 | 설명 | 단계 |
|---|---|---|---|---|
| A-01 | POST | `/api/auctions` | 경매 등록 | 다음 |
| A-02 | GET | `/api/auctions` | 경매 목록 | 다음 |
| A-03 | GET | `/api/auctions/{id}` | 경매 상세 | 다음 |
| A-04 | POST | `/api/auctions/{id}/bids` | 입찰 | 입찰 단계 |
| A-05 | GET | `/api/auctions/{id}/bids` | 입찰 이력 | 입찰 단계 |

### 4.3 회원 (User)

| # | 메서드 | 경로 | 설명 | 단계 |
|---|---|---|---|---|
| U-01 | POST | `/api/auth/signup` | 회원가입 | 인증 단계 |
| U-02 | POST | `/api/auth/login` | 로그인 | 인증 단계 |
| U-03 | GET | `/api/users/me` | 내 정보 (포인트 포함) | 인증 단계 |
| U-04 | GET | `/api/users/me/bids` | 내 입찰 내역 | 인증 단계 |
| U-05 | GET | `/api/users/me/results` | 내 낙찰 내역 | 인증 단계 |

> **현 단계 구현 범위는 P-01 ~ P-05.**

---

## 5. 에러 응답 표준

### 5.1 공통 형식

```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다.",
  "timestamp": "2026-08-16T20:30:00"
}
```

검증 실패 시에는 필드별 상세를 추가한다.

```json
{
  "code": "INVALID_INPUT",
  "message": "입력값이 올바르지 않습니다.",
  "timestamp": "2026-08-16T20:30:00",
  "errors": [
    { "field": "name", "message": "상품명은 필수입니다." },
    { "field": "estimatedValue", "message": "예상 가치는 1 이상이어야 합니다." }
  ]
}
```

### 5.2 HTTP 상태 코드

| 코드 | 의미 | 사용 예 |
|---|---|---|
| 200 | 성공 | 조회, 수정 |
| 201 | 생성 성공 | 등록 |
| 204 | 성공, 본문 없음 | 삭제 |
| 400 | 검증 실패 | 형식 오류, 필수 누락 |
| 401 | 인증 실패 | 토큰 없음/만료 (인증 도입 후) |
| 403 | 권한 없음 | 관리자 전용 API 접근 |
| 404 | 리소스 없음 | 존재하지 않는 ID |
| 409 | 충돌 | 중복, 상태 충돌 |
| 500 | 서버 오류 | 미처리 예외 |

### 5.3 에러 코드 정의

| 코드 | HTTP | 메시지 |
|---|---|---|
| `INVALID_INPUT` | 400 | 입력값이 올바르지 않습니다. |
| `PRODUCT_NOT_FOUND` | 404 | 상품을 찾을 수 없습니다. |
| `PRODUCT_IN_AUCTION` | 409 | 경매 이력이 있는 상품은 삭제할 수 없습니다. |
| `AUCTION_NOT_FOUND` | 404 | 경매를 찾을 수 없습니다. |
| `AUCTION_NOT_RUNNING` | 409 | 진행 중인 경매가 아닙니다. |
| `AUCTION_CLOSED` | 409 | 종료된 경매입니다. |
| `INVALID_BID_AMOUNT` | 400 | 입찰 금액이 올바르지 않습니다. |
| `INSUFFICIENT_POINT` | 400 | 포인트가 부족합니다. |
| `PRODUCT_NOT_VIEWED` | 403 | 상품을 먼저 확인해야 입찰할 수 있습니다. |
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없습니다. |
| `INTERNAL_ERROR` | 500 | 서버 오류가 발생했습니다. |

> 에러 코드는 `ErrorCode` enum으로 관리하고, HTTP 상태와 메시지를 함께 보유한다. 이렇게 하면 예외 발생 지점에서 코드만 던지면 되고, 상태 코드 매핑이 한곳에 모인다.

---

## 6. 상품 API 상세 명세 (현 단계 구현 대상)

### P-01. 상품 등록

```
POST /api/products
```

**요청 본문**

```json
{
  "name": "1969년 아폴로 11호 기념 우표",
  "description": "발행 당시 미사용 상태로 보관된 기념 우표입니다.",
  "imageUrl": "https://example.com/images/stamp.jpg",
  "itemCondition": "GOOD",
  "rarity": "RARE",
  "estimatedValue": 85000
}
```

**요청 필드 검증**

| 필드               | 타입     | 필수  | 제약       | 검증 어노테이션                     |
| ---------------- | ------ | --- | -------- | ---------------------------- |
| `name`           | String | O   | 1~100자   | `@NotBlank` `@Size(max=100)` |
| `description`    | String | X   | 최대 1000자 | `@Size(max=1000)`            |
| `imageUrl`       | String | X   | 최대 500자  | `@Size(max=500)`             |
| `itemCondition`  | String | O   | 최대 20자   | `@NotBlank` `@Size(max=20)`  |
| `rarity`         | String | O   | 최대 20자   | `@NotBlank` `@Size(max=20)`  |
| `estimatedValue` | Long   | O   | 1 이상     | `@NotNull` `@Min(1)`         |

**응답 `201 Created`**

```json
{
  "id": 1,
  "name": "1969년 아폴로 11호 기념 우표",
  "description": "발행 당시 미사용 상태로 보관된 기념 우표입니다.",
  "imageUrl": "https://example.com/images/stamp.jpg",
  "itemCondition": "GOOD",
  "rarity": "RARE",
  "estimatedValue": 85000,
  "createdAt": "2026-08-16T20:30:00"
}
```

**에러**

| 상황 | 코드 | HTTP |
|---|---|---|
| 필수 누락 / 형식 오류 | `INVALID_INPUT` | 400 |

---

### P-02. 상품 목록

```
GET /api/products?page=0&size=20&sort=id,desc
```

**쿼리 파라미터**

| 파라미터 | 필수 | 기본값 | 설명 |
|---|---|---|---|
| `page` | X | 0 | 페이지 번호 |
| `size` | X | 20 | 페이지당 건수 |
| `sort` | X | `id,desc` | 정렬 |

**응답 `200 OK`**

```json
{
  "content": [
    {
      "id": 3,
      "name": "1969년 아폴로 11호 기념 우표",
      "imageUrl": "https://example.com/images/stamp.jpg",
      "itemCondition": "GOOD",
      "rarity": "RARE",
      "estimatedValue": 85000
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 3,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

> **목록에는 `description`과 `createdAt`을 포함하지 않는다.** 카드 UI 렌더링에 불필요하고, 설명이 1000자까지 가능해 응답 크기만 키운다. → `ProductSummaryResponse`

---

### P-03. 상품 상세

```
GET /api/products/{id}
```

**경로 변수**

| 변수 | 타입 | 설명 |
|---|---|---|
| `id` | Long | 상품 ID |

**응답 `200 OK`** — P-01 응답과 동일 형식 (`ProductDetailResponse`)

**에러**

| 상황 | 코드 | HTTP |
|---|---|---|
| 존재하지 않는 ID | `PRODUCT_NOT_FOUND` | 404 |

---

### P-04. 상품 수정

```
PATCH /api/products/{id}
```

**요청 본문** — 변경할 필드만 전송

```json
{
  "estimatedValue": 92000,
  "itemCondition": "FAIR"
}
```

**요청 필드**

| 필드 | 필수 | 비고 |
|---|---|---|
| `name` | X | 전송 시 1~100자 |
| `description` | X | 전송 시 최대 1000자 |
| `imageUrl` | X | 전송 시 최대 500자 |
| `itemCondition` | X | 전송 시 최대 20자 |
| `rarity` | X | 전송 시 최대 20자 |
| `estimatedValue` | X | 전송 시 1 이상 |

> **PUT이 아니라 PATCH인 이유**: PUT은 리소스 전체 교체가 원칙이라, 일부 필드만 보내면 나머지가 null로 덮여야 한다. 상품 수정은 대개 일부 필드만 바꾸므로 부분 수정을 의미하는 PATCH가 적합하다.
>
> **구현 주의**: null인 필드는 "변경 없음"으로 처리한다. 즉 서비스에서 `if (request.getName() != null)` 형태로 분기한다.

**응답 `200 OK`** — 수정된 전체 상품 정보 (`ProductDetailResponse`)

**에러**

| 상황 | 코드 | HTTP |
|---|---|---|
| 존재하지 않는 ID | `PRODUCT_NOT_FOUND` | 404 |
| 형식 오류 | `INVALID_INPUT` | 400 |

---

### P-05. 상품 삭제

```
DELETE /api/products/{id}
```

**응답 `204 No Content`** — 본문 없음

**에러**

| 상황 | 코드 | HTTP |
|---|---|---|
| 존재하지 않는 ID | `PRODUCT_NOT_FOUND` | 404 |
| 경매 이력 존재 | `PRODUCT_IN_AUCTION` | 409 |

> **경매 이력이 있으면 삭제를 막는 이유**: DB 설계상 `products → auctions`는 RESTRICT다. 그냥 삭제를 시도하면 FK 제약 위반으로 DB 예외(500)가 터진다. 서비스 계층에서 미리 확인해 **409와 명확한 메시지로 응답**하는 편이 API 사용자에게 유용하다.

---

## 7. 계층 구조 및 책임

```
Controller  →  Service  →  Repository  →  DB
    ↓            ↓
   DTO        Entity
```

| 계층 | 책임 | 하지 않는 것 |
|---|---|---|
| **Controller** | HTTP 요청 수신, 검증 트리거(`@Valid`), 상태 코드 결정 | 비즈니스 로직 |
| **Service** | 비즈니스 로직, 트랜잭션 경계, Entity ↔ DTO 변환 | HTTP 관련 처리 |
| **Repository** | DB 접근 | 로직 판단 |

**변환 위치**: Entity → DTO 변환은 **Service에서** 수행한다.
Controller에서 하면 Entity가 컨트롤러까지 올라와 LAZY 로딩 예외 위험이 생기고, 트랜잭션 범위 밖에서 프록시를 건드리게 된다. (`open-in-view: false`로 설정한 것과 같은 맥락)

**트랜잭션 규칙**

```java
@Transactional(readOnly = true)   // 클래스 레벨 기본값
public class ProductService {

    @Transactional                 // 쓰기 메서드에만 개별 지정
    public ProductDetailResponse create(...) { }
}
```

조회에 `readOnly = true`를 주면 변경 감지(dirty checking)를 생략해 성능상 이점이 있고, 실수로 수정하는 것도 막는다.

---

## 8. 구현 순서 (현 단계)

| 순서 | 작업 | 산출물 |
|---|---|---|
| 1 | 예외 처리 기반 | `ErrorCode`, `BusinessException`, `ErrorResponse`, `GlobalExceptionHandler` |
| 2 | 공통 응답 | `PageResponse<T>` |
| 3 | DTO | `ProductCreateRequest`, `ProductUpdateRequest`, `ProductDetailResponse`, `ProductSummaryResponse` |
| 4 | Service | `ProductService` |
| 5 | Controller | `ProductController` |
| 6 | Security 임시 개방 | `SecurityConfig` |
| 7 | 테스트 | `.http` 파일 또는 Postman |

> 예외 처리를 먼저 만드는 이유: Service에서 "상품 없음"을 던질 곳이 있어야 조회 로직을 완성할 수 있다. 나중에 붙이면 임시 예외를 썼다가 전부 고쳐야 한다.

---

## 9. 패키지 구조

```
com.auction.auction_play
├── domain/                    (완료)
├── repository/                (완료)
├── controller/
│   └── ProductController
├── service/
│   └── ProductService
├── dto/
│   ├── request/
│   │   ├── ProductCreateRequest
│   │   └── ProductUpdateRequest
│   └── response/
│       ├── ProductDetailResponse
│       ├── ProductSummaryResponse
│       └── PageResponse
├── exception/
│   ├── ErrorCode
│   ├── BusinessException
│   ├── ErrorResponse
│   └── GlobalExceptionHandler
└── config/
    └── SecurityConfig
```

---

## 10. 검증 체크리스트

- [ ] `POST /api/products` → 201, 응답에 id 포함
- [ ] 필수 필드 누락 → 400, `errors` 배열에 필드별 메시지
- [ ] `GET /api/products` → 200, 페이징 형식 확인
- [ ] `GET /api/products/999` → 404, `PRODUCT_NOT_FOUND`
- [ ] `PATCH` 일부 필드만 전송 → 나머지 필드 값 유지 확인
- [ ] `DELETE` → 204, 재조회 시 404
- [ ] 응답에 Entity 내부 필드(`version` 등) 미노출 확인

---

## 변경 이력

| 버전 | 일자 | 변경 내용 |
|---|---|---|
| v1.0 | 2026.08.16 | 최초 작성. 상품 API(P-01~P-05) 상세 명세 |
