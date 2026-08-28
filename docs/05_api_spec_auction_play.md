# AuctionPlay - API 명세서

| 항목 | 내용 |
|---|---|
| 문서 종류 | API Specification |
| 프로젝트명 | AuctionPlay |
| 버전 | v1.3 |
| 작성일 | 2026.08.16 (최종 v1.3 — 2026.08.23) |
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
| P-06 | POST | `/api/products/{id}/view` | 조회 인정 (5초 체류) | 회원 | **현재** |

### 4.2 경매 (Auction)

| # | 메서드 | 경로 | 설명 | 단계 |
|---|---|---|---|---|
| A-01 | POST | `/api/auctions` | 경매 등록 | **현재** |
| A-02 | GET | `/api/auctions` | 경매 목록 | **현재** |
| A-03 | GET | `/api/auctions/{id}` | 경매 상세 | **현재** |
| A-04 | POST | `/api/auctions/{id}/bids` | 입찰 | **현재** ⭐ |
| A-05 | GET | `/api/auctions/{id}/bids` | 입찰 이력 | **현재** |

### 4.3 회원 (User)

| # | 메서드 | 경로 | 설명 | 단계 |
|---|---|---|---|---|
| U-01 | POST | `/api/auth/signup` | 회원가입 | 인증 단계 |
| U-02 | POST | `/api/auth/login` | 로그인 | 인증 단계 |
| U-03 | GET | `/api/users/me` | 내 정보 (포인트 포함) | 인증 단계 |
| U-04 | GET | `/api/users/me/bids` | 내 입찰 내역 | 인증 단계 |
| U-05 | GET | `/api/users/me/results` | 내 낙찰 내역 | 인증 단계 |

> **P-01~P-05 완료 (08.20). A-01~A-03 완료 (08.21). A-04·A-05·P-06 완료 (08.23).**
> **현 단계는 Scheduler — 경매 자동 상태 전이 (6-4장).**

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
| `INVALID_AUCTION_TIME` | 400 | 종료 시각은 시작 시각보다 뒤여야 합니다. |
| `AUCTION_CANCELLED` | 409 | 취소된 경매입니다. |
| `AUCTION_NOT_STARTED` | 409 | 아직 시작되지 않은 경매입니다. |
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

| 필드 | 타입 | 필수 | 제약 | 검증 어노테이션 |
|---|---|---|---|---|
| `name` | String | O | 1~100자 | `@NotBlank` `@Size(max=100)` |
| `description` | String | X | 최대 1000자 | `@Size(max=1000)` |
| `imageUrl` | String | X | 최대 500자 | `@Size(max=500)` |
| `itemCondition` | String | O | 최대 20자 | `@NotBlank` `@Size(max=20)` |
| `rarity` | String | O | 최대 20자 | `@NotBlank` `@Size(max=20)` |
| `estimatedValue` | Long | O | 1 이상 | `@NotNull` `@Min(1)` |

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

## 6-2. 경매 API 상세 명세 (현 단계 구현 대상)

> 상품 API와 계층 구조는 동일하다. **다른 점은 아래 네 가지**이며, 이 문서는 그 차이에 집중한다.
>
> | 추가 요소 | 상품에는 없던 것 |
> |---|---|
> | 연관관계 | `Auction` → `Product` (`@ManyToOne`). 요청은 `productId`로 받고 서버가 조회해 연결 |
> | 상태 | `AuctionStatus` enum. 등록 시 `SCHEDULED` 고정 |
> | 파생 값 | `currentPrice` 초기값 = `startPrice`. 클라이언트가 못 정한다 |
> | 교차 검증 | `endAt > startAt`. 어노테이션 하나로 못 하는 검증 |

---

### A-01. 경매 등록

```
POST /api/auctions
```

**요청 본문**

```json
{
  "productId": 1,
  "startPrice": 8500,
  "startAt": "2026-08-20T21:00:00",
  "endAt": "2026-08-20T21:10:00"
}
```

**요청 필드 검증**

| 필드 | 타입 | 필수 | 제약 | 검증 어노테이션 |
|---|---|---|---|---|
| `productId` | Long | O | 존재하는 상품 | `@NotNull` |
| `startPrice` | Long | O | 1 이상 | `@NotNull` `@Min(1)` |
| `startAt` | LocalDateTime | O | — | `@NotNull` |
| `endAt` | LocalDateTime | O | `startAt` 이후 | `@NotNull` |

> **`currentPrice`와 `status`를 요청에 두지 않는 이유**
> 현재가는 시작가에서 출발하는 것이 규칙이고, 상태는 시스템이 관리한다.
> 클라이언트가 정할 수 있게 두면 "시작가 8,500인데 현재가 50,000인 경매"처럼
> 성립하지 않는 데이터가 만들어질 수 있다. **정할 수 없는 값은 받지 않는다.**

**서버에서 처리하는 것**

| 순서 | 처리 |
|---|---|
| 1 | `productId`로 상품 조회 → 없으면 `PRODUCT_NOT_FOUND` (404) |
| 2 | `endAt > startAt` 확인 → 아니면 `INVALID_AUCTION_TIME` (400) |
| 3 | `currentPrice = startPrice` 설정 |
| 4 | `status = SCHEDULED` 설정 |
| 5 | 저장 후 응답 |

**응답 `201 Created`**

```json
{
  "id": 1,
  "status": "SCHEDULED",
  "startPrice": 8500,
  "currentPrice": 8500,
  "startAt": "2026-08-20T21:00:00",
  "endAt": "2026-08-20T21:10:00",
  "createdAt": "2026-08-20T20:41:12",
  "product": {
    "id": 1,
    "name": "1969년 아폴로 11호 기념 우표",
    "imageUrl": "https://example.com/images/stamp.jpg",
    "itemCondition": "GOOD",
    "rarity": "RARE",
    "estimatedValue": 85000
  }
}
```

> **응답에 상품을 중첩한 이유**
> 클라이언트가 경매 하나를 보여주려면 상품 정보가 반드시 필요하다.
> `productId`만 내려주면 클라이언트가 상품 API를 한 번 더 호출해야 한다(요청 2회).
> 이미 갖고 있는 정보를 중첩해서 한 번에 내려주는 편이 낫다.
>
> 단, **중첩에는 `ProductSummaryResponse`를 재사용**한다. 목록용으로 만든 DTO를
> 여기서도 쓰면 되고, `description`처럼 경매 화면에 불필요한 필드는 자연히 빠진다.

**에러**

| 상황 | 코드 | HTTP |
|---|---|---|
| 필수 누락 / 형식 오류 | `INVALID_INPUT` | 400 |
| 존재하지 않는 상품 | `PRODUCT_NOT_FOUND` | 404 |
| `endAt` ≤ `startAt` | `INVALID_AUCTION_TIME` | 400 |

---

### A-02. 경매 목록

```
GET /api/auctions?status=RUNNING&page=0&size=20&sort=endAt,asc
```

**쿼리 파라미터**

| 파라미터 | 필수 | 기본값 | 설명 |
|---|---|---|---|
| `status` | X | 전체 | `SCHEDULED` / `RUNNING` / `CLOSED` / `CANCELLED` |
| `page` | X | 0 | 페이지 번호 |
| `size` | X | 20 | 페이지당 건수 |
| `sort` | X | `endAt,asc` | 정렬 |

> **기본 정렬이 `id,desc`가 아니라 `endAt,asc`인 이유**
> 경매 목록에서 사용자가 가장 먼저 봐야 할 것은 **곧 끝나는 경매**다.
> 최신 등록순은 여기서 의미가 약하다. 도메인에 맞춰 기본값을 바꾼다.

**응답 `200 OK`**

```json
{
  "content": [
    {
      "id": 1,
      "status": "RUNNING",
      "currentPrice": 14000,
      "endAt": "2026-08-20T21:10:00",
      "bidCount": 7,
      "product": {
        "id": 1,
        "name": "1969년 아폴로 11호 기념 우표",
        "imageUrl": "https://example.com/images/stamp.jpg",
        "itemCondition": "GOOD",
        "rarity": "RARE",
        "estimatedValue": 85000
      }
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

> **목록에서 `startAt`·`startPrice`·`createdAt`을 뺀 이유**
> 카드 UI에 필요한 것은 현재가·마감시각·상태·입찰수다.
> 시작가는 상세에서만 의미가 있다. → `AuctionSummaryResponse`

> **`bidCount` 주의 — N+1 발생 지점**
> 경매마다 입찰 수를 세면 목록 20건에 카운트 쿼리가 20번 추가로 나간다.
> MVP 단계에서는 그대로 두되 **문제를 인지하고 기록**한다.
> 개선 방향: ① `@Query`로 조인 집계 ② `bids` 별도 조회 후 메모리 매핑
> ③ `auctions.bid_count` 비정규화 (동시성 제어 단계에서 재검토)

---

### A-03. 경매 상세

```
GET /api/auctions/{id}
```

**응답 `200 OK`** — A-01 응답 + 입찰 관련 정보

```json
{
  "id": 1,
  "status": "RUNNING",
  "startPrice": 8500,
  "currentPrice": 14000,
  "minBidAmount": 15000,
  "startAt": "2026-08-20T21:00:00",
  "endAt": "2026-08-20T21:10:00",
  "createdAt": "2026-08-20T20:41:12",
  "bidCount": 7,
  "product": {
    "id": 1,
    "name": "1969년 아폴로 11호 기념 우표",
    "imageUrl": "https://example.com/images/stamp.jpg",
    "itemCondition": "GOOD",
    "rarity": "RARE",
    "estimatedValue": 85000
  }
}
```

> **`minBidAmount`를 서버가 계산해 내려주는 이유**
> `currentPrice + 1000`은 클라이언트도 계산할 수 있다. 그런데 최소 증가액 규칙이
> 바뀌면(예: 금액대별 차등) 클라이언트를 전부 고쳐야 한다.
> **규칙은 서버에 두고 결과만 내려준다.** 클라이언트는 이 값을 표시하기만 하면 된다.
>
> 물론 입찰 시 서버는 이 값을 다시 검증한다. 내려준 값을 믿지 않는다.

**에러**

| 상황 | 코드 | HTTP |
|---|---|---|
| 존재하지 않는 ID | `AUCTION_NOT_FOUND` | 404 |

---

### 경매 수정·삭제를 만들지 않는 이유

상품에는 `PATCH`·`DELETE`를 만들었지만 경매에는 두지 않는다.

| 이유 | 설명 |
|---|---|
| 거래 이력 | 진행 중 경매의 시작가·마감시각을 바꾸면 이미 입찰한 사람과의 약속이 깨진다 |
| 상태 전이로 대체 | 취소는 삭제가 아니라 `status = CANCELLED`로 표현한다. 이력이 남아야 한다 |
| 종료는 시스템 몫 | `CLOSED` 전환은 Scheduler가 담당한다 (다음 단계) |

> 이것이 **거래 도메인과 마스터 데이터의 차이**다. 상품은 카탈로그라 고칠 수 있지만,
> 경매는 시간과 금액이 걸린 이벤트라 되돌리지 않고 상태만 앞으로 나아간다.

---

### DTO 구성

```
dto/request/
  AuctionCreateRequest       productId, startPrice, startAt, endAt

dto/response/
  AuctionDetailResponse      경매 전체 + minBidAmount + bidCount + product(Summary)
  AuctionSummaryResponse     id, status, currentPrice, endAt, bidCount + product(Summary)
```

`ProductSummaryResponse`는 신규 작성하지 않고 **상품 API의 것을 그대로 재사용**한다.

---

### 추가할 ErrorCode

```java
AUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다."),        // 이미 있음
INVALID_AUCTION_TIME(HttpStatus.BAD_REQUEST, "종료 시각은 시작 시각보다 뒤여야 합니다."),  // 신규
```

---

### 구현 순서

| 순서 | 작업 | 산출물 |
|---|---|---|
| 1 | ErrorCode 추가 | `INVALID_AUCTION_TIME` |
| 2 | Repository 메서드 | `findByStatus(AuctionStatus, Pageable)` |
| 3 | DTO 3개 | Request 1 + Response 2 |
| 4 | Service | `AuctionService` |
| 5 | Controller | `AuctionController` |
| 6 | 테스트 | `api-test.http`에 경매 블록 추가 |

---

### 검증 체크리스트

- [ ] `POST /api/auctions` → 201, `status: SCHEDULED`, `currentPrice == startPrice`
- [ ] 없는 `productId` → 404 `PRODUCT_NOT_FOUND`
- [ ] `endAt` ≤ `startAt` → 400 `INVALID_AUCTION_TIME`
- [ ] `GET /api/auctions` → 200, 응답에 `product` 중첩 포함
- [ ] `GET /api/auctions?status=SCHEDULED` → 해당 상태만 반환
- [ ] `GET /api/auctions/{id}` → `minBidAmount == currentPrice + 1000`
- [ ] `GET /api/auctions/999` → 404 `AUCTION_NOT_FOUND`
- [ ] 목록 조회 시 콘솔에서 **N+1 발생 여부 확인** (bidCount)

---

## 6-3. 입찰 API 상세 명세 (현 단계 구현 대상)

> **이 프로젝트의 핵심.** 지금까지의 CRUD와 성격이 다르다.
> 한 번의 입찰이 **5개 테이블을 동시에** 바꾸고, 하나라도 실패하면 전부 취소되어야 한다.

---

### 6-3-0. 선행 결정 사항

| 항목 | 결정 | 근거 |
|---|---|---|
| 사용자 식별 | 요청 본문에 `userId` 포함 | 인증 미구현 단계. 인증 도입 시 토큰에서 추출하도록 교체 |
| 포인트 처리 | **보류 방식(A)** — 입찰 시 차감, 밀리면 환급 | 실제 경매와 동일. 포인트 없이 무한 입찰하는 것을 막는다 |
| 조회 규칙 | **이번 단계에서 구현** | 이 서비스의 차별 규칙이므로 미루지 않는다 |
| 상태 판정 | **DB `status`가 아니라 현재 시각으로 판정** | Scheduler 없이도 테스트 가능. 도입 후에도 중복 방어로 유효 |

#### 상태 판정을 시각 기준으로 하는 이유

```java
// 이렇게 하지 않는다
if (auction.getStatus() != RUNNING) throw ...

// 이렇게 한다
if (now.isBefore(auction.getStartAt())) throw AUCTION_NOT_STARTED;
if (!now.isBefore(auction.getEndAt()))  throw AUCTION_CLOSED;
```

`status` 컬럼은 Scheduler가 갱신하는 값이다. 아직 Scheduler가 없으므로 모든 경매가 `SCHEDULED`에 머물러 있어 입찰 자체가 불가능하다.

시각 기준으로 판정하면
- Scheduler 없이 `startAt`을 과거로 두어 즉시 테스트할 수 있고
- Scheduler 도입 후에도 **"상태는 RUNNING인데 실제로는 종료 시각이 지난"** 경계 상황을 막는 이중 방어가 된다

단, `CANCELLED` 상태는 시각으로 알 수 없으므로 `status`로 확인한다.

---

### 6-3-1. 포인트 보류 방식 (A안) 동작

**시나리오**

```
[초기]  A: 100,000P   B: 100,000P   경매 현재가 8,500P

1. A가 10,000P 입찰
   → A: 90,000P (10,000 보류)     경매 현재가 10,000P
   → point_transactions: A / BID_HOLD / -10,000 / 잔액 90,000

2. B가 12,000P 입찰
   → B:  88,000P (12,000 보류)    경매 현재가 12,000P
   → A: 100,000P (보류 해제)      ← A는 밀렸으므로 환급
   → point_transactions: B / BID_HOLD / -12,000 / 잔액 88,000
   → point_transactions: A / REFUND   / +10,000 / 잔액 100,000
```

**핵심 규칙**

| 규칙 | 내용 |
|---|---|
| 보류 대상 | **직전 최고 입찰자 1명만** 보류 상태를 유지한다 |
| 환급 시점 | 새 최고가가 등장하는 즉시 이전 최고 입찰자에게 환급 |
| 동일인 재입찰 | 이전 보류액을 환급하고 새 금액을 다시 보류 (차액만 처리하지 않는다) |
| 낙찰 시 | 보류 상태가 그대로 `PURCHASE`로 확정 (Scheduler 단계) |

> **동일인 재입찰을 차액 처리하지 않는 이유**
> 차액만 계산하면 원장에 "얼마를 보류 중인지"가 흐려진다.
> 환급 → 재보류로 처리하면 `point_transactions`만 읽어도 흐름이 그대로 복원된다. 원장의 목적은 추적 가능성이다.

---

### A-04. 입찰

```
POST /api/auctions/{auctionId}/bids
```

**경로 변수**

| 변수 | 타입 | 설명 |
|---|---|---|
| `auctionId` | Long | 경매 ID |

**요청 본문**

```json
{
  "userId": 1,
  "amount": 10000
}
```

**요청 필드 검증**

| 필드 | 타입 | 필수 | 제약 | 어노테이션 |
|---|---|---|---|---|
| `userId` | Long | O | — | `@NotNull` |
| `amount` | Long | O | 1 이상 | `@NotNull` `@Min(1)` |

> `userId`를 본문으로 받는 것은 **임시 방식**이다. 인증 도입 시 `@AuthenticationPrincipal`로 교체하고 이 필드는 제거한다. 현재 구조로는 남의 ID로 입찰이 가능하므로 운영에 올릴 수 없다.

---

### 검증 순서 (11단계)

Service에서 이 순서로 수행한다. **순서에도 이유가 있다.**

| # | 검증 | 실패 시 | 비고 |
|---|---|---|---|
| 1 | 경매 존재 | `AUCTION_NOT_FOUND` 404 | |
| 2 | 취소되지 않았는가 | `AUCTION_CANCELLED` 409 | `status`로 확인 |
| 3 | 시작 시각 이후인가 | `AUCTION_NOT_STARTED` 409 | 현재 시각 기준 |
| 4 | 종료 시각 이전인가 | `AUCTION_CLOSED` 409 | 현재 시각 기준 |
| 5 | 사용자 존재 | `USER_NOT_FOUND` 404 | |
| 6 | 상품 조회 기록 존재 | `PRODUCT_NOT_VIEWED` 403 | **이 서비스 고유 규칙** |
| 7 | 현재가 초과 | `INVALID_BID_AMOUNT` 400 | |
| 8 | 최소 증가액 충족 | `INVALID_BID_AMOUNT` 400 | `amount >= currentPrice + 1000` |
| 9 | 포인트 충분 | `INSUFFICIENT_POINT` 400 | |
| 10 | 이전 최고 입찰자 환급 | — | 있을 경우만 |
| 11 | 저장·갱신·차감 | — | 아래 참조 |

**순서 근거**

- **싼 검증부터.** 1~5는 단순 조회, 6~9는 계산, 10~11은 쓰기다. 실패할 요청은 최대한 빨리 돌려보낸다.
- **경매 → 사용자 → 금액 순.** 경매가 없으면 사용자를 조회할 이유가 없다.
- **7과 8을 분리한 이유.** 둘 다 `INVALID_BID_AMOUNT`지만 메시지가 달라야 한다. "현재가보다 낮습니다"와 "1,000P 이상 올려야 합니다"는 사용자에게 다른 정보다.

---

### 11단계 상세 — 한 트랜잭션에서 일어나는 일

```
① 이전 최고 입찰자가 있으면 환급
   users.point           += 이전 입찰액
   point_transactions    INSERT (REFUND, +금액, 잔액)

② 새 입찰자 포인트 보류
   users.point           -= amount
   point_transactions    INSERT (BID_HOLD, -amount, 잔액)

③ 입찰 기록
   bids                  INSERT

④ 경매 현재가 갱신
   auctions.current_price = amount
```

**하나라도 실패하면 전부 롤백되어야 한다.**

포인트만 빠지고 입찰이 기록되지 않거나, 현재가는 올랐는데 포인트가 그대로면 데이터가 어긋난다. `@Transactional`이 처음으로 진짜 의미를 갖는 지점이다.

> **`BusinessException`을 `RuntimeException`으로 만든 이유가 여기서 드러난다.**
> Spring의 `@Transactional`은 unchecked 예외에서만 롤백한다. checked였다면 검증 실패 시 앞서 실행된 UPDATE가 커밋되어 데이터가 깨진다.

---

**응답 `201 Created`**

```json
{
  "bidId": 12,
  "auctionId": 1,
  "userId": 1,
  "amount": 10000,
  "createdAt": "2026-08-21T18:30:12.482",
  "currentPrice": 10000,
  "minBidAmount": 11000,
  "myRemainingPoint": 90000,
  "isTopBidder": true
}
```

> **응답에 `myRemainingPoint`와 `minBidAmount`를 포함한 이유**
> 입찰 직후 사용자가 알고 싶은 것은 "내 포인트가 얼마 남았고, 다음엔 얼마부터 낼 수 있나"다.
> 이 두 값이 없으면 클라이언트가 회원 조회와 경매 조회를 추가로 호출해야 한다. **한 번의 행동에 필요한 정보는 한 번의 응답에 담는다.**

**에러**

| 상황 | 코드 | HTTP |
|---|---|---|
| 필수 누락 / 형식 오류 | `INVALID_INPUT` | 400 |
| 경매 없음 | `AUCTION_NOT_FOUND` | 404 |
| 취소된 경매 | `AUCTION_CANCELLED` | 409 |
| 시작 전 | `AUCTION_NOT_STARTED` | 409 |
| 종료됨 | `AUCTION_CLOSED` | 409 |
| 사용자 없음 | `USER_NOT_FOUND` | 404 |
| 상품 미조회 | `PRODUCT_NOT_VIEWED` | 403 |
| 금액 부적합 | `INVALID_BID_AMOUNT` | 400 |
| 포인트 부족 | `INSUFFICIENT_POINT` | 400 |

---

### A-05. 입찰 이력

```
GET /api/auctions/{auctionId}/bids?page=0&size=20
```

**응답 `200 OK`**

```json
{
  "content": [
    {
      "bidId": 12,
      "amount": 10000,
      "createdAt": "2026-08-21T18:30:12.482",
      "bidder": { "userId": 1, "nickname": "keepgoing" }
    }
  ],
  "page": 0, "size": 20, "totalElements": 3, "totalPages": 1,
  "first": true, "last": true
}
```

**정렬**: `createdAt DESC` — 최근 입찰이 위로. 경매 화면에서 최신 흐름을 봐야 한다.

> **`bidder`를 중첩한 이유** — 닉네임만 필요하지 `email`이나 `point`를 노출할 이유가 없다. `UserSummaryResponse`를 새로 만들어 필요한 필드만 담는다.

> **여기도 N+1 주의 지점.** `bid.getUser()`가 LAZY라 건마다 회원 조회가 나간다. 경매 API에서 배운 대로 **처음부터 fetch join으로 작성**한다.

**에러**

| 상황 | 코드 | HTTP |
|---|---|---|
| 경매 없음 | `AUCTION_NOT_FOUND` | 404 |

---

### P-06. 상품 조회 인정

```
POST /api/products/{productId}/view
```

입찰 자격을 얻기 위한 사전 단계. 상품 상세에 5초 이상 머문 뒤 프론트가 호출한다.

**요청 본문**

```json
{ "userId": 1 }
```

**응답 `200 OK`**

```json
{ "productId": 1, "userId": 1, "viewed": true }
```

**동작**

| 상황 | 처리 |
|---|---|
| 최초 호출 | `product_views` INSERT |
| 이미 기록 있음 | 아무것도 하지 않고 200 반환 |
| 없는 상품 | `PRODUCT_NOT_FOUND` 404 |
| 없는 사용자 | `USER_NOT_FOUND` 404 |

> **중복 호출을 409가 아니라 200으로 처리하는 이유**
> 이 API의 목적은 "조회 자격을 확보한다"이지 "새 기록을 만든다"가 아니다.
> 사용자가 같은 상품에 두 번 들어오는 것은 **오류가 아니라 정상 행동**이다. 결과(자격 있음)가 같으면 성공으로 응답하는 것이 맞다.
>
> 이런 성질을 **멱등성(idempotent)** 이라 한다. 몇 번을 호출해도 결과가 같다.
> DB의 `(user_id, product_id)` UNIQUE 제약은 동시 요청에 대한 최종 방어선으로 남는다.

> **5초 측정은 서버가 하지 않는다.**
> HTTP는 요청 시점만 알 뿐 체류 시간을 알 수 없다. 프론트가 타이머를 돌리고 5초 후 이 API를 호출한다.
> 클라이언트를 신뢰하는 구조라 우회가 가능하지만, 이 규칙의 목적은 보안이 아니라 **"물건을 보지 않고 입찰하는 것을 막는" 게임 규칙**이므로 허용 가능한 수준으로 판단했다.

---

### 추가할 ErrorCode

```java
AUCTION_CANCELLED(HttpStatus.CONFLICT, "취소된 경매입니다."),
AUCTION_NOT_STARTED(HttpStatus.CONFLICT, "아직 시작되지 않은 경매입니다."),
```

기존 `AUCTION_CLOSED`, `INVALID_BID_AMOUNT`, `INSUFFICIENT_POINT`, `PRODUCT_NOT_VIEWED`, `USER_NOT_FOUND`는 이미 정의되어 있다.

---

### 필요한 Repository 메서드

```java
// BidRepository
@Query("select b from Bid b join fetch b.user where b.auction.id = :auctionId")
Page<Bid> findByAuctionIdWithUser(Long auctionId, Pageable pageable);

@Query("select b from Bid b join fetch b.user where b.auction.id = :auctionId " +
       "order by b.amount desc, b.createdAt asc limit 1")
Optional<Bid> findTopBid(Long auctionId);

// ProductViewRepository
boolean existsByUserIdAndProductId(Long userId, Long productId);

// UserRepository — 기본 findById 사용
```

> **`findTopBid`의 정렬이 `amount desc, createdAt asc`인 이유**
> 금액이 같으면 **먼저 도착한 쪽이 위**다. STEP 1에서 정한 "동시 최고가는 먼저 도착한 요청이 낙찰" 규칙을 쿼리에 반영한 것이다.
> `bids.created_at`을 `DATETIME(6)`(밀리초)으로 설계한 이유가 여기 있다.

---

### DTO 구성

```
dto/request/
  BidCreateRequest          userId, amount
  ProductViewRequest        userId

dto/response/
  BidCreateResponse         bidId, auctionId, userId, amount, createdAt,
                            currentPrice, minBidAmount, myRemainingPoint, isTopBidder
  BidSummaryResponse        bidId, amount, createdAt, bidder(UserSummaryResponse)
  UserSummaryResponse       userId, nickname
  ProductViewResponse       productId, userId, viewed
```

---

### Entity에 추가할 메서드

Setter를 쓰지 않으므로 상태 변경 메서드가 필요하다.

```java
// Auction
public void updateCurrentPrice(Long amount) {
    this.currentPrice = amount;
}

// User
public void deductPoint(Long amount) {
    this.point -= amount;
}

public void addPoint(Long amount) {
    this.point += amount;
}
```

> `deductPoint` 안에서 잔액을 검사할지 고민할 수 있으나, **검증은 Service에서** 한다.
> Entity가 `BusinessException`을 던지면 도메인이 애플리케이션 예외에 의존하게 된다. Entity는 상태 변경만 책임진다.

---

### 구현 순서

| 순서 | 작업 | 예상 |
|---|---|---|
| 1 | ErrorCode 2개 추가 | 5분 |
| 2 | Entity 메서드 3개 (`Auction`, `User`) | 10분 |
| 3 | Repository 메서드 3개 | 15분 |
| 4 | DTO 6개 | 25분 |
| 5 | 테스트 데이터 준비 (회원 등록 수단) | 20분 |
| 6 | `ProductViewService` + Controller (P-06) | 30분 |
| 7 | `BidService` — 11단계 검증 + 트랜잭션 ⭐ | 60~90분 |
| 8 | `BidController` (A-04, A-05) | 20분 |
| 9 | 테스트 | 30분 |

**5번이 필요한 이유** — 지금 `users` 테이블이 비어 있다. 회원가입 API는 인증 단계 작업이므로, 임시 회원 등록 수단이 있어야 입찰을 테스트할 수 있다.
DB에 직접 INSERT하거나 간단한 테스트용 API를 만든다. **후자를 택한다** — 반복 실행이 쉽고, 인증 단계에서 정식 회원가입으로 확장할 기반이 된다.

---

### 검증 체크리스트

**정상 흐름**
- [ ] 회원 2명 생성 → 각 100,000P
- [ ] `POST /api/products/1/view` → 200
- [ ] A 입찰 10,000 → 201, A 포인트 90,000, 현재가 10,000
- [ ] B 입찰 12,000 → 201, B 포인트 88,000, **A 포인트 100,000으로 복구**
- [ ] `GET /api/auctions/1/bids` → 2건, 최신순
- [ ] 포인트 원장 4건 (`BID_HOLD` 2, `REFUND` 1 … 확인)

**실패 케이스**
- [ ] 조회 기록 없이 입찰 → 403 `PRODUCT_NOT_VIEWED`
- [ ] 현재가보다 낮은 금액 → 400
- [ ] 최소 증가액 미만 (현재가 +500) → 400
- [ ] 포인트 초과 금액 → 400 `INSUFFICIENT_POINT`
- [ ] 종료된 경매 (`endAt` 과거) → 409 `AUCTION_CLOSED`
- [ ] 시작 전 경매 (`startAt` 미래) → 409 `AUCTION_NOT_STARTED`
- [ ] 없는 사용자 → 404

**정합성**
- [ ] `users.point` + 보류 중인 금액 = 100,000 (항상)
- [ ] `point_transactions.balance_after` 최신값 == `users.point`
- [ ] 실패한 입찰 후 포인트가 변하지 않았는가 ← **롤백 확인**

**성능**
- [ ] 입찰 이력 조회 시 회원 조회 N+1이 없는가

---

## 6-4. Scheduler — 경매 자동 상태 전이 (현 단계 구현 대상)

> 지금까지는 **사용자 요청이 있어야** 뭔가 일어났다.
> 이 단계는 **아무도 요청하지 않아도** 시스템이 스스로 움직인다.

---

### 6-4-0. 왜 필요한가

경매는 시간이 지나면 저절로 끝나야 한다. 아무도 "종료" 버튼을 누르지 않는다.

현재 상태의 문제:

| 문제 | 현상 |
|---|---|
| 모든 경매가 `SCHEDULED` | `?status=RUNNING` 필터가 항상 0건 |
| 종료 시각이 지나도 `CLOSED`가 안 됨 | 목록에서 끝난 경매가 계속 보임 |
| `auction_results`가 비어 있음 | 낙찰자·수익률을 알 수 없음 |
| 보류 포인트가 영원히 묶임 | 낙찰도 유찰도 확정되지 않음 |

---

### 6-4-1. 선행 결정 사항

| 항목 | 결정 | 근거 |
|---|---|---|
| 실행 주기 | **10초** (`fixedDelay`) | 경매 단위가 5~10분. 최대 10초 지연은 체감되지 않음 |
| 시작 전이 | **Scheduler가 처리** (`SCHEDULED` → `RUNNING`) | `status` 필터가 실제 상태와 일치해야 목록이 쓸모 있음 |
| 낙찰 포인트 | **보류 상태를 그대로 확정** | 입찰 시 이미 차감됨. 환급 후 재차감은 불필요한 왕복 |
| 확정 원장 | `PURCHASE`, `amount = 0` | 잔액 변동 없는 **상태 전환 기록** |

#### 실행 주기 선택 기준

| 주기 | 용도 |
|---|---|
| 1~5초 | 실시간에 가까운 처리. 부하 큼 |
| **10~30초** | **마감이 촘촘한 시스템** (경매, 예약, 쿠폰) ← 여기 |
| 1~5분 | 일반적인 상태 정리 |
| 시간/일 단위 | 정산, 집계, 배치 수집 |

기준은 **"얼마나 늦어도 되나"**다. 경매 종료가 1분 늦으면 목록에 끝난 경매가 계속 보이고, 그 사이 입찰 시도가 들어온다.

#### `fixedDelay` vs `fixedRate` vs `cron`

| 방식 | 동작 |
|---|---|
| `fixedRate` | **시작 시점** 기준 10초마다. 이전 작업이 안 끝나도 다음이 시작됨 |
| **`fixedDelay`** | **종료 시점** 기준 10초 후. 이전 작업이 끝나야 다음이 시작 |
| `cron` | 특정 시각 (매일 03:00 등) |

**`fixedDelay`를 쓴다.** `fixedRate`는 처리가 10초를 넘기면 작업이 겹쳐 돌아 **낙찰이 중복 처리**될 수 있다.

---

### 6-4-2. 처리 흐름

```
매 10초마다
  │
  ├─ [1] 시작 처리
  │      status = SCHEDULED AND startAt <= now
  │      → status = RUNNING
  │
  └─ [2] 종료 처리
         status = RUNNING AND endAt <= now
         │
         ├─ 입찰 있음 → 낙찰
         │    status = CLOSED
         │    AuctionResult 생성 (winner, finalPrice, estimatedValue, profit)
         │    PointTransaction (PURCHASE, amount=0) — 보류 확정
         │
         └─ 입찰 없음 → 유찰
              status = CANCELLED
              AuctionResult 생성 (winner=null, finalPrice=null, profit=null)
```

---

### 6-4-3. 낙찰 처리 상세

**최고 입찰자 결정**

```java
bidRepository.findTopBid(auctionId)   // amount desc, createdAt asc
```

이미 입찰 API에서 쓰던 쿼리를 재사용한다. **금액이 같으면 먼저 도착한 쪽**이 낙찰이다 (STEP 1 규칙).

**AuctionResult 생성**

| 필드 | 값 |
|---|---|
| `auction` | 해당 경매 |
| `winner` | 최고 입찰자 |
| `finalPrice` | 최고 입찰액 |
| `estimatedValue` | **낙찰 시점의** `product.estimatedValue` (스냅샷) |
| `profit` | `estimatedValue - finalPrice` |

> **`estimatedValue`를 복사해 두는 이유**
> 상품 가치는 나중에 관리자가 바꿀 수 있다. 낙찰 시점 값을 고정하지 않으면 **과거 낙찰의 수익률이 사후에 변한다.**
> DB 설계서 2.5절에서 "의도적 비정규화"로 명시한 부분이 여기서 쓰인다.

**포인트 확정**

입찰 시점에 이미 `BID_HOLD`로 차감돼 있다. 낙찰되면 그 상태가 그대로 확정된다.

```java
PointTransaction.builder()
        .user(winner)
        .type(PointTransactionType.PURCHASE)
        .amount(0L)                        // 잔액 변동 없음
        .balanceAfter(winner.getPoint())   // 현재 값 그대로
        .build();
```

> **`amount = 0`인 이유**
> 원장에서 `SUM(amount)`가 곧 현재 잔액이어야 한다. 여기서 다시 차감하면 이중 차감이 된다.
> 이 행의 목적은 **"보류가 구매로 확정됐다"는 상태 전환 기록**이다.
>
> 대안으로 `BID_HOLD` 행의 타입을 `PURCHASE`로 바꾸는 방법도 있지만, **원장은 수정하지 않는다**는 원칙에 어긋난다.

---

### 6-4-4. 유찰 처리

입찰이 하나도 없는 경우.

| 필드 | 값 |
|---|---|
| `status` | `CANCELLED` |
| `winner` | `null` |
| `finalPrice` | `null` |
| `estimatedValue` | 상품의 값 (기록은 남긴다) |
| `profit` | `null` |

> **유찰도 `AuctionResult`를 만드는 이유**
> "이 경매는 어떻게 끝났나"는 결과가 없으면 알 수 없다. 결과 행이 없으면 "아직 처리 안 됨"과 구분이 안 된다.
> `winner_id`를 nullable로 설계한 이유가 여기 있다 (DB 설계서 2.3절).

---

### 6-4-5. 중복 실행 방지 ⚠️

**Scheduler가 겹쳐 돌면 낙찰이 두 번 처리된다.**

| 방어선 | 내용 |
|---|---|
| 1차 | `fixedDelay` — 이전 작업이 끝나야 다음이 시작 |
| 2차 | 조회 조건이 `status = RUNNING` — 이미 `CLOSED`면 다시 안 잡힘 |
| 3차 | `auction_results.auction_id` **UNIQUE** — DB가 두 번째 INSERT를 거부 |

3차가 최종 방어선이다. DB 설계 때 1:1 관계에 UNIQUE를 건 이유가 여기서 실현된다.

> 다만 **서버를 여러 대 띄우면** `fixedDelay`로는 막을 수 없다. 각 서버가 독립적으로 Scheduler를 돌리기 때문이다.
> 이 경우 분산 락(Redis, ShedLock)이나 DB 락이 필요하다. **현 단계는 단일 서버 전제**이며, README에 한계로 명시한다.

---

### 6-4-6. 트랜잭션 경계

```java
@Scheduled(fixedDelay = 10000)
public void run() {                          // 트랜잭션 없음
    List<Auction> targets = 조회();
    for (Auction auction : targets) {
        closeOne(auction.getId());           // 건별로 트랜잭션
    }
}

@Transactional
public void closeOne(Long auctionId) { ... }
```

**건별로 트랜잭션을 나누는 이유**

전체를 하나의 트랜잭션으로 묶으면, **경매 하나가 실패할 때 나머지도 전부 롤백**된다. 100건 중 1건이 문제여도 99건이 못 끝난다.

건별로 나누면 실패한 것만 다음 주기에 재시도된다. **실패 격리**다.

> **주의 — 자기 호출 문제**
> 같은 클래스 안에서 `this.closeOne()`을 호출하면 `@Transactional`이 **동작하지 않는다.**
> Spring이 프록시로 트랜잭션을 걸기 때문에, 프록시를 거치지 않는 내부 호출은 무시된다.
>
> 해결: Scheduler와 실제 처리 로직을 **다른 클래스로 분리**한다.
> `AuctionCloseScheduler`(호출) → `AuctionCloseService`(처리)

---

### 6-4-7. 필요한 Repository 메서드

```java
// AuctionRepository
@Query("select a from Auction a where a.status = :status and a.startAt <= :now")
List<Auction> findStartTargets(AuctionStatus status, LocalDateTime now);

@Query("select a from Auction a join fetch a.product " +
       "where a.status = :status and a.endAt <= :now")
List<Auction> findCloseTargets(AuctionStatus status, LocalDateTime now);
```

> `findCloseTargets`에 `join fetch`를 쓰는 이유: 낙찰 처리에서 `product.estimatedValue`를 읽는다. 없으면 건마다 상품 조회가 나간다 (N+1).

> **`(status, end_at)` 인덱스가 여기서 쓰인다.** DB 설계 때 만들어 둔 것으로, 이 조회가 10초마다 반복되므로 인덱스가 없으면 경매가 쌓일수록 풀스캔이 된다.

---

### 6-4-8. Entity에 추가할 메서드

```java
// Auction
public void start() {
    this.status = AuctionStatus.RUNNING;
}

public void close() {
    this.status = AuctionStatus.CLOSED;
}

public void cancel() {
    this.status = AuctionStatus.CANCELLED;
}
```

> 상태 전이는 각각 다른 사건이므로 메서드를 나눈다. `setStatus(x)`보다 `close()`가 무슨 일인지 명확하고, 나중에 "종료 시 알림 발송" 같은 규칙을 추가할 자리가 생긴다.

---

### 6-4-9. 설정

```java
@SpringBootApplication
@EnableScheduling            // ← 추가
public class AuctionPlayApplication { ... }
```

이 어노테이션이 없으면 `@Scheduled`가 **아무 반응 없이 무시된다.** 에러도 안 난다.

---

### 6-4-10. 만들 파일

```
scheduler/
└── AuctionCloseScheduler      @Scheduled — 호출만

service/
└── AuctionCloseService        @Transactional — 실제 처리
```

---

### 6-4-11. 구현 순서

| 순서 | 작업 | 예상 |
|---|---|---|
| 1 | `@EnableScheduling` 추가 | 2분 |
| 2 | Entity 메서드 3개 (`start`, `close`, `cancel`) | 5분 |
| 3 | Repository 메서드 2개 | 10분 |
| 4 | `AuctionCloseService` — 시작/낙찰/유찰 | 50~70분 |
| 5 | `AuctionCloseScheduler` | 10분 |
| 6 | 테스트 | 30분 |

---

### 6-4-12. 검증 체크리스트

**시작 전이**
- [ ] `startAt`이 과거인 `SCHEDULED` 경매 → 10초 내 `RUNNING`
- [ ] `?status=RUNNING` 조회 시 나타남

**낙찰**
- [ ] `endAt` 지난 경매 → `CLOSED`
- [ ] `AuctionResult` 1건 생성
- [ ] `winner` = 최고 입찰자
- [ ] `finalPrice` = 최고 입찰액
- [ ] `profit` = `estimatedValue - finalPrice`
- [ ] `PURCHASE` 원장 생성, **포인트 잔액 변동 없음**

**유찰**
- [ ] 입찰 0건 경매 → `CANCELLED`
- [ ] `AuctionResult`에 `winner = null`

**중복 방지**
- [ ] 여러 주기가 지나도 `AuctionResult`가 1건 유지
- [ ] `CLOSED` 경매가 다시 처리되지 않음

**로그**
- [ ] 처리 대상이 없을 때 불필요한 로그가 안 찍히는가
- [ ] 처리 시 어떤 경매가 어떻게 끝났는지 남는가

---

### 6-4-13. 테스트 방법

Scheduler는 **시간이 지나야** 동작하므로 테스트 방식이 다르다.

**방법 1 — 짧은 경매를 만든다**

```json
{
  "productId": 1,
  "startPrice": 5000,
  "startAt": "2026-08-23T11:00:00",
  "endAt": "2026-08-23T11:02:00"
}
```

`endAt`을 **현재 시각 + 2분**으로 잡고, 입찰한 뒤 기다린다.

**방법 2 — 이미 지난 시각으로 만든다**

`startAt`, `endAt`을 모두 과거로 잡으면 다음 주기에 바로 처리된다. 입찰을 넣을 수 없으므로 **유찰 테스트**에 쓴다.

> 낙찰 테스트는 방법 1이 필요하다. 입찰이 들어가야 하기 때문이다.

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

## 10. 검증 체크리스트 — 상품 API 완료 ✅ (2026.08.20)

- [x] `POST /api/products` → 201, 응답에 id 포함
- [x] 필수 필드 누락 → 400, `errors` 배열에 필드별 메시지
- [x] `GET /api/products` → 200, 페이징 형식 확인
- [x] `GET /api/products/999` → 404, `PRODUCT_NOT_FOUND`
- [x] `PATCH` 일부 필드만 전송 → 나머지 필드 값 유지 확인
- [x] `DELETE` → 204, 재조회 시 404
- [x] 응답에 Entity 내부 필드(`version` 등) 미노출 확인

> 경매 API 체크리스트는 6-2장 말미 참조.

---

## 변경 이력

| 버전 | 일자 | 변경 내용 |
|---|---|---|
| v1.0 | 2026.08.16 | 최초 작성. 상품 API(P-01~P-05) 상세 명세 |
| v1.1 | 2026.08.20 | 상품 API 구현·검증 완료 표기. 경매 API(A-01~A-03) 상세 명세 추가 |
| v1.2 | 2026.08.21 | 경매 API 구현·검증 완료 표기. 입찰 API(A-04, A-05) 및 조회 인정(P-06) 상세 명세 추가 |
| v1.3 | 2026.08.23 | 입찰 API 구현·검증 완료 표기. Scheduler(경매 자동 상태 전이) 명세 추가 |
