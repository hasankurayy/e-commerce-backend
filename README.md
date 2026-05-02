# E-Commerce Microservices Backend

Canlı demo: **[n55-frontend.vercel.app](https://n55-frontend.vercel.app)**

---

## Test Kartı (Iyzico Sandbox)

| Alan | Değer |
|------|-------|
| Kart No | `5528790000000008` |
| Son Kullanma | `12/30` |
| CVV | `123` |
| 3D Şifre | `a` |

---

## Mimari

8 bağımsız Spring Boot servisi API Gateway arkasında çalışır. Servisler birbirini Eureka üzerinden keşfeder, senkron iletişim için Feign Client, asenkron iletişim için RabbitMQ kullanır.

```
Client → API Gateway (:8080)
           ├── JWT doğrulama + header enjeksiyonu
           ├── Eureka Service Discovery (:8761)
           ├── Config Server (:8888)
           └── Business Services
               ├── user-service     → Auth, JWT issuance
               ├── product-service  → Ürün & kategori yönetimi
               ├── cart-service     → Sepet işlemleri
               ├── order-service    → Sipariş akışı
               ├── payment-service  → Iyzico entegrasyonu
               └── notification-service → Email (RabbitMQ event)
```

**Altyapı:** PostgreSQL (database-per-service), Redis (gateway cache), RabbitMQ

---

## Proje Gereksinimleri Karşılama

### RESTful API
Her servis kendi bounded context'ine sahip REST controller'lar içerir. `common-lib` modülü `ApiResponse<T>` ve `PageResponse<T>` sarmalayıcılarıyla tutarlı response formatı sağlar.

### PostgreSQL & Veritabanı
Her servis izole bir PostgreSQL veritabanına sahiptir (database-per-service pattern). Schema migrations Flyway ile yönetilir (`*/resources/db/migration/`).

### Pagination
`product-service` — `ProductController` Spring Data'nın `Pageable` desteğiyle sayfalama uygular. Frontend'e `PageResponse<T>` döner; `page`, `size`, `totalElements`, `totalPages` alanları içerir.

### Sepet İşlemleri
`cart-service` — sepete ekleme, miktar güncelleme, ürün çıkarma ve sepeti temizleme işlemlerini yönetir. Ürün bilgisi için `product-service`'e Feign Client ile senkron çağrı yapar.

### Sipariş Yönetimi
`order-service` — sepetten sipariş oluşturur, `PENDING_PAYMENT → CONFIRMED → SHIPPED → DELIVERED` akışını yönetir. Sipariş oluşturulduğunda RabbitMQ'ya event yayar.

### Ödeme Entegrasyonu
`payment-service` — Iyzico Checkout Form (3D Secure) entegrasyonu. `IyzicoService` form başlatır, callback ile ödeme sonucunu işler, başarılı ödemede `order-service`'e RabbitMQ event gönderir.

### Güvenlik (JWT)
`user-service` — kayıt, giriş ve token yenileme endpoint'leri. `common-lib/JwtUtil` ile 15 dakika erişim / 7 gün yenileme token üretir. API Gateway her istekte JWT doğrular, `X-User-Id`, `X-User-Email`, `X-User-Roles` header'larını downstream servislere iletir — servisler JWT'yi tekrar doğrulamaz.

### Testler
- **Unit testler** — Mockito ile service katmanı (`**/service/*Test.java`)
- **Integration testler** — TestContainers ile gerçek PostgreSQL üzerinde (`user-service/integration/`)

### Dokümantasyon
Her servis SpringDoc OpenAPI ile Swagger UI sunar. Gateway üzerinden erişim: `http://localhost:8080/{service}/swagger-ui.html`

### Loglama
SLF4J + Logback. `GlobalExceptionHandler` (`common-lib`) tüm istisnaları yakalar, yapılandırılmış log üretir ve standart hata yanıtına dönüştürür.

---

## Teknolojiler

| Katman | Teknoloji |
|--------|-----------|
| Framework | Spring Boot 3.3.5, Java 17 |
| Servis Keşfi | Spring Cloud Netflix Eureka |
| Config | Spring Cloud Config Server (native) |
| Gateway | Spring Cloud Gateway |
| İletişim | OpenFeign (sync), RabbitMQ (async) |
| Veritabanı | PostgreSQL 16, Flyway |
| Cache | Redis 7 |
| Güvenlik | JWT (jjwt 0.12.6), Spring Security |
| Ödeme | Iyzico iyzipay-java SDK |
| Mapping | MapStruct |
| Test | JUnit 5, Mockito, TestContainers |
| Dokümantasyon | SpringDoc OpenAPI 3 |
| Build | Maven multi-module, Jib (Dockerfile'sız image) |
| CI/CD | GitHub Actions → Docker Hub → Oracle Cloud |

---

## CI/CD Pipeline

`main` branch'e her push'ta:
1. GitHub Actions tetiklenir
2. `mvn install jib:build` ile tüm servisler Docker Hub'a push edilir
3. Oracle Cloud VM'e SSH ile bağlanılır, `docker compose pull && up -d` çalıştırılır

```
git push → GitHub Actions → Docker Hub → Oracle Cloud VM
```

Jenkins yerine GitHub Actions tercih edildi: ayrı sunucu gerektirmez, repo ile entegredir.

---

## Lokal Kurulum

```bash
# Altyapıyı başlat (PostgreSQL, RabbitMQ, Redis)
cd docker && docker-compose up -d postgres rabbitmq redis

# Tüm servisleri derle
mvn install -DskipTests

# Sırayla başlat
mvn spring-boot:run -pl config-server
mvn spring-boot:run -pl eureka-server
mvn spring-boot:run -pl api-gateway
mvn spring-boot:run -pl user-service    # ve diğerleri
```

**Testler:**
```bash
mvn test                          # Unit testler
mvn verify -P test                # Integration testler (Docker gerekli)
mvn test -pl user-service -Dtest=AuthServiceTest  # Tek test
```
