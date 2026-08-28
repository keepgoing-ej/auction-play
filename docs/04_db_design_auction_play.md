# AuctionPlay - 아키텍처 설계서

| 항목 | 내용 |
|---|---|
| 문서 종류 | Architecture Design |
| 프로젝트명 | AuctionPlay |
| 버전 | v1.0 |
| 작성일 | 2026.08.17 |
| 참조 문서 | `04_db_design_auction_play.md`, `05_api_spec_auction_play.md` |

---

## 0. 문서 목적

계층 구조, 패키지 구조, 기술 선택 근거를 명시한다.
"왜 이렇게 만들었나"에 답할 수 있는 근거를 남기는 것이 목적이다.

---

## 1. 전체 구조

```
[Client]
    │  HTTP (JSON)
    ▼
┌─────────────────────────────────────────┐
│  Controller                             │  요청 수신 / 검증 트리거 / 상태코드
│  - @RestController                      │
│  - @Valid                               │
└──────────────┬──────────────────────────┘
               │  DTO
               ▼
┌─────────────────────────────────────────┐
│  Service                                │  비즈니스 로직 / 트랜잭션 / DTO 변환
│  - @Service @Transactional              │
└──────────────┬──────────────────────────┘
               │  Entity
               ▼
┌─────────────────────────────────────────┐
│  Repository                             │  DB 접근
│  - JpaRepository                        │
└──────────────┬──────────────────────────┘
               │  JPA / Hibernate
               ▼
┌─────────────────────────────────────────┐
│  MySQL 8.0 (Docker, port 3307)          │
└─────────────────────────────────────────┘

        ┌──────────────────────────┐
        │  GlobalExceptionHandler  │ ← 모든 계층의 예외를 가로챔
        │  @RestControllerAdvice   │
        └──────────────────────────┘
```

---

## 2. 계층별 책임

| 계층 | 하는 일 | 하지 않는 일 |
|---|---|---|
| Controller | HTTP 수신, `@Valid` 트리거, 상태코드 결정, Service 호출 | 비즈니스 판단, DB 접근, DTO 변환 |
| Service | 비즈니스 로직, 트랜잭션 경계, Entity↔DTO 변환, 예외 발생 | HTTP 관련 처리, 상태코드 결정 |
| Repository | DB 접근 | 로직 판단, 예외 처리 |
| Entity | 자기 상태 변경 로직 (`update()` 등) | 외부 의존 |

### 2.1 DTO 변환을 Service에서 하는 이유

Controller에서 변환하면 Entity가 컨트롤러까지 올라온다. 이때 트랜잭션은 이미 끝난 상태라, LAZY 프록시에 접근하면 `LazyInitializationException`이 터진다.
`open-in-view: false`로 설정한 것과 같은 맥락이다. **영속성 컨텍스트는 서비스 계층 안에서 닫힌다.**

### 2.2 Entity에 로직을 두는 이유

`@Setter`를 열지 않고 `product.update(...)` 같은 메서드를 둔다.

| Setter 방식 | 메서드 방식 |
|---|---|
| 어디서든 값 변경 가능 | 변경 지점이 한 곳 |
| 변경 이유가 코드에 안 남음 | 메서드명이 의도를 설명 |
| 규칙 추가 시 모든 호출부 수정 | 메서드 내부만 수정 |

특히 포인트·경매 상태처럼 **정합성이 중요한 값**은 절대 Setter로 열지 않는다.

---

## 3. 패키지 구조

```
com.auction.auction_play
├── AuctionPlayApplication.java
│
├── domain/                          도메인 엔티티 + Enum
│   ├── User, Product, Auction, Bid
│   ├── AuctionResult, PointTransaction, ProductView
│   ├── AuctionStatus (enum)
│   └── PointTransactionType (enum)
│
├── repository/                      DB 접근
│   └── {Entity}Repository × 7
│
├── service/                         비즈니스 로직
│   └── ProductService, AuctionService, BidService ...
│
├── controller/                      HTTP 엔드포인트
│   └── ProductController, AuctionController ...
│
├── dto/
│   ├── request/                     들어오는 데이터 (검증 어노테이션)
│   └── response/                    나가는 데이터 (final + @Builder)
│
├── exception/                       예외 처리 기반
│   ├── ErrorCode (enum)
│   ├── BusinessException
│   ├── ErrorResponse
│   └── GlobalExceptionHandler
│
├── config/                          설정
│   └── SecurityConfig
│
└── scheduler/                       배치 (예정)
    └── AuctionCloseScheduler
```

### 3.1 계층형 vs 도메인형

이 프로젝트는 **계층형(layer-first)** 을 택했다.

| | 계층형 (선택) | 도메인형 |
|---|---|---|
| 구조 | `service/ProductService` | `product/ProductService` |
| 장점 | 초보자에게 직관적, 계층 책임이 눈에 보임 | 도메인 응집도 높음, 대규모에 유리 |
| 단점 | 도메인 커지면 패키지가 비대해짐 | 초기 설계 부담 |

**근거**: 엔티티 7개 규모이고, 계층 책임 분리를 학습·설명하는 것이 이 프로젝트의 목적 중 하나다. 도메인이 20개를 넘어가면 도메인형이 낫다.

---

## 4. 기술 선택 근거

| 기술 | 선택 | 근거 |
|---|---|---|
| Java 21 | LTS 최신 | 취업 시장 기준 17 이상. record, pattern matching 등 활용 가능 |
| Spring Boot 4.1.0 | 최신 | 최신 버전 경험 확보. 다만 자료 부족 트레이드오프 감수 |
| JPA / Hibernate | ORM | 연관관계·트랜잭션·영속성 컨텍스트 학습이 프로젝트 목표에 포함 |
| MySQL 8 | RDBMS | 트랜잭션·락 지원. 동시성 제어(핵심 기능)의 전제 |
| Docker | DB 실행 | 프로젝트별 DB 격리. 다른 프로젝트와 포트 충돌 회피(3307) |
| Gradle | 빌드 | Maven 대비 간결한 설정 |
| Lombok | 보일러플레이트 | Getter/Builder 반복 제거 |
| Bean Validation | 입력 검증 | 검증 로직을 DTO에 선언적으로 명시 |

---

## 5. 핵심 설정과 의도

### 5.1 `open-in-view: false`

기본값(true)은 영속성 컨텍스트를 뷰 렌더링까지 유지한다.

**끈 이유 2가지**
1. 트랜잭션 밖에서 LAZY 로딩이 일어나는 것을 막는다 → 의도치 않은 쿼리 발생 차단
2. DB 커넥션을 요청 끝까지 붙잡지 않는다 → 커넥션 반환이 빨라짐

대신 필요한 데이터는 **서비스 안에서 전부 DTO로 변환해서** 내보낸다.

### 5.2 `@Transactional(readOnly = true)` 기본값

클래스 레벨에 걸고, 쓰기 메서드에만 `@Transactional`을 덧붙인다.

| 효과 | 설명 |
|---|---|
| 성능 | 변경 감지(dirty checking) 스냅샷 생성 생략 |
| 안전 | 조회 메서드에서 실수로 수정해도 반영되지 않음 |

### 5.3 `ddl-auto: update`

개발 단계 한정. Entity 변경이 잦은 시기에 DDL을 손으로 관리하지 않기 위함.
**운영에서는 `validate` + 마이그레이션 도구(Flyway 등)** 로 전환해야 한다.

### 5.4 타임존 명시

```
jdbc:mysql://localhost:3307/auctiondb?serverTimezone=Asia/Seoul
```

경매 종료 판정이 `endAt` 시각 비교에 의존한다. 애플리케이션과 DB의 타임존이 어긋나면 **종료 시점이 밀리거나 종료된 경매에 입찰이 들어가는** 정합성 사고로 직결된다.

---

## 6. 예외 처리 흐름

```
Service                Controller           GlobalExceptionHandler
   │                       │                         │
   │ throw BusinessException                         │
   │ (ErrorCode.PRODUCT_NOT_FOUND)                   │
   └──────────────────────────────────────────────▶ │
                                                     │ ErrorCode에서
                                                     │ status + message 추출
                                                     ▼
                                            ResponseEntity
                                            404 + { code, message, timestamp }
```

**핸들러 3단 구성**

| 순서 | 대상 | 응답 | 로그 |
|---|---|---|---|
| 1 | `BusinessException` | ErrorCode의 status | `warn` (의도된 흐름) |
| 2 | `MethodArgumentNotValidException` | 400 + 필드별 에러 배열 | - |
| 3 | `Exception` (그 외 전부) | 500 + 일반 메시지 | `error` (버그 가능성) |

**3번의 존재 이유**: 예상 못한 예외가 그대로 나가면 스택트레이스에 내부 클래스명·테이블명이 노출된다. **로그에는 전부 남기고 응답에는 일반 메시지만** 내보낸다.

---

## 7. 동시성 제어 설계 (예정)

프로젝트의 핵심 기술 도전. 아직 구현 전이며 아래 순서로 진행한다.

```
1. 락 없이 입찰 구현
2. 동시 요청 테스트로 문제 재현 (최고가 꼬임 / 포인트 불일치)
3. 락 적용
4. 테스트로 해결 검증
5. 선택 근거 문서화
```

### 검토 대상

| 방식 | 특징 | 이 프로젝트 적합성 |
|---|---|---|
| 비관적 락 (`PESSIMISTIC_WRITE`) | 조회 시점에 행 잠금. 대기 발생 | 경합이 잦은 인기 경매에 적합 |
| 낙관적 락 (`@Version`) | 충돌 시 예외 → 재시도 | 경합이 드물 때 유리 |
| DB 격리 수준 조정 | 광범위한 영향 | 부작용 커서 후순위 |

`auctions.version` 컬럼은 이미 만들어 두었다 (`@Version`은 해당 단계에서 부착).

**설계 전제**: `auctions.current_price`를 비정규화해 둔 이유가 여기 있다. 매 입찰마다 `MAX(bids.amount)` 집계를 돌리면 락 범위가 넓어진다. 현재가를 컬럼으로 들고 있어야 **해당 경매 행 하나만 잠그고** 갱신할 수 있다.

---

## 8. 향후 추가 예정

| 구성요소 | 시점 |
|---|---|
| `SecurityConfig` (JWT 인증) | 인증 단계 |
| `AuctionCloseScheduler` | Scheduler 단계 |
| 통합 테스트 / 동시성 테스트 | 테스트 단계 |
| React 프론트엔드 | MVP 후반 |

---

## 변경 이력

| 버전 | 일자 | 변경 내용 |
|---|---|---|
| v1.0 | 2026.08.17 | 최초 작성 |
