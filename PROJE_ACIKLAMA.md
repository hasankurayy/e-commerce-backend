# E-Commerce Mikroservis Backend — Proje Açıklaması

Bu dosya, proje boyunca karşılaşılan kavramları ve mimari kararları sade bir dille açıklar.
Proje sunumunda veya teknik mülakatta referans olarak kullanılabilir.

---

## Proje Nedir?

Spring Boot ile yazılmış mikroservis tabanlı bir e-ticaret backend'i.
Kullanıcı kayıt/giriş, ürün listeleme, sepet, sipariş ve ödeme işlemlerini kapsar.
Her servis bağımsız çalışır, kendi veritabanına sahiptir ve birbirleriyle API üzerinden konuşur.

---

## Mimari: Neden Mikroservis?

Monolitik bir uygulamada tüm kod tek bir projede olur. Bir modül bozulursa sistem durabilir,
bir modülü ölçeklendirmek için tüm uygulamayı ölçeklendirmek gerekir.

Mikroserviste her iş alanı (kullanıcı, ürün, sipariş...) ayrı bir uygulama olarak çalışır:
- Bir servis çöktüğünde diğerleri etkilenmez
- Sadece yoğun olan servis ölçeklendirilebilir
- Her servis farklı bir ekip tarafından geliştirilebilir

---

## Servisler ve Görevleri

| Servis | Port | Görev |
|---|---|---|
| api-gateway | 8080 | Tek giriş noktası. Dışarıdan gelen tüm istekler buradan geçer |
| config-server | 8888 | Tüm servislerin ayar dosyalarını merkezi tutar |
| eureka-server | 8761 | Servislerin birbirini bulmasını sağlar (servis kayıt defteri) |
| user-service | 8081 | Kayıt, giriş, JWT token üretimi |
| product-service | 8082 | Ürün ve kategori yönetimi |
| cart-service | 8083 | Sepet işlemleri |
| order-service | 8084 | Sipariş oluşturma ve takibi |
| payment-service | 8085 | iyzico ile ödeme işlemleri |
| notification-service | 8086 | Email/bildirim gönderme |

---

## Java Kavramları

### `record` nedir?

Java 16 ile gelen özellik. Sadece veri taşımak için kullanılan sınıfları çok daha kısa yazmayı sağlar.

Normalde bir veri sınıfı:
```java
public class ApiResponse {
    private boolean success;
    private String message;

    public ApiResponse(boolean success, String message) { ... }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    // equals(), hashCode(), toString() da yazmak lazım
}
```

`record` ile aynısı:
```java
public record ApiResponse(boolean success, String message) {}
```

Java otomatik olarak constructor, getter, `equals`, `hashCode`, `toString` üretir.
**Immutable'dır** — bir kez oluşturulduktan sonra alanları değiştirilemez.

Record içine `static` veya instance method da yazılabilir. Biz bunu factory pattern için kullandık:
```java
ApiResponse.success(data)    // new yerine anlamlı isim
ApiResponse.error("mesaj")
```

### Generic `<T>` nedir?

`ApiResponse<T>` tanımındaki `T`, tip parametresidir. Aynı sınıfın farklı tiplerle çalışmasını sağlar:

```java
ApiResponse<Product>       // data: Product
ApiResponse<List<Order>>   // data: sipariş listesi
ApiResponse<Void>          // data yok, sadece mesaj
```

Controller'da:
```java
return ResponseEntity.ok(ApiResponse.success(product));     // ApiResponse<Product>
return ResponseEntity.ok(ApiResponse.success(orders));      // ApiResponse<List<Order>>
```

### `@JsonInclude(JsonInclude.Include.NON_NULL)`

Jackson (Spring'in JSON kütüphanesi) bir nesneyi JSON'a çevirirken `null` olan alanları da yazar.
Bu annotation ile null alanlar JSON'dan çıkarılır — daha temiz bir API yanıtı elde edilir.

**Annotation olmadan hata response:**
```json
{ "success": false, "message": "Bulunamadı", "data": null }
```

**Annotation ile:**
```json
{ "success": false, "message": "Bulunamadı" }
```

---

## Spring Kavramları

### `@RestControllerAdvice` — Global Hata Yönetimi

Her controller'da ayrı ayrı `try-catch` yazmak yerine, tek bir sınıfta tüm hataları yakalarız.
`GlobalExceptionHandler` bunu sağlar. Bir exception fırlatıldığında Spring otomatik olarak
ilgili `@ExceptionHandler` metodunu çağırır ve uygun HTTP status + mesaj döner.

```
ResourceNotFoundException → 404 Not Found
BusinessException         → 400 Bad Request
ValidationException       → 400 + hangi alan hatalı bilgisiyle
```

### `@ConfigurationProperties` — Tip-güvenli Konfigürasyon

`application.yml`'deki değerleri bir Java sınıfına bağlar. String yerine tip-güvenli nesne kullanılır.

```yaml
# application.yml
jwt:
  secret: my-secret-key
  access-token-expiration-ms: 900000
```

```java
// JwtProperties record otomatik doldurulur
JwtProperties props = ...; 
props.secret()                  // "my-secret-key"
props.accessTokenExpirationMs() // 900000
```

---

## Güvenlik Mimarisi

### JWT (JSON Web Token) — Access Token

Kullanıcı giriş yaptığında user-service bir JWT üretir. Bu token:
- **15 dakika** geçerlidir
- İmzalıdır (sunucu gizli anahtar ile)
- Veritabanına **kaydedilmez** (stateless)
- İçinde `userId`, `email`, `roles` bilgileri taşır

Her sonraki istekte kullanıcı bu token'ı `Authorization: Bearer <token>` header'ında gönderir.
API Gateway token'ı doğrular, geçerliyse isteği ilgili servise iletir.

### Refresh Token

Access token 15 dakikada bir sona erer. Kullanıcının her 15 dakikada tekrar giriş yapmasını istemeyiz.
Bunun için refresh token:
- **7 gün** geçerlidir
- **Veritabanına kaydedilir** (kullanılabilir, iptal edilebilir)
- JWT değil, rastgele UUID'dir
- Kullanıldığında eskisi silinir, yenisi oluşturulur (rotasyon)

### API Gateway'in Rolü

Dışarıdan gelen tüm istekler gateway'den geçer. Gateway:
1. Token'ı doğrular
2. Token geçerliyse `X-User-Id`, `X-User-Email`, `X-User-Roles` header'larını ekler
3. İsteği ilgili servise iletir

İç servisler (product, cart, order...) bu header'lara güvenir. Spring Security bağımlılığı sadece
user-service'te vardır — diğer servisler daha basit kalır.

---

## Maven Multi-Module Proje

Tüm servisler tek bir Maven projesi altında toplanır. Bu yapının avantajları:
- Tek `mvn install` ile tüm proje derlenir
- `common-lib` gibi ortak modüller paylaşılır
- Versiyon yönetimi merkezi — her serviste ayrı versiyon yazmak gerekmez
- CI/CD'de tek pipeline ile tüm servisler test edilip deploy edilir

---

## Veritabanı Stratejisi (Database per Service)

Her servise ait ayrı bir PostgreSQL veritabanı vardır. Bu mikroservis mimarisinin temel kuralıdır:
- Servisler birbirinin veritabanına doğrudan erişemez
- Bir servisin DB şeması değiştiğinde diğerleri etkilenmez
- Her servis kendi verisinin tam sahibidir

Localde tek PostgreSQL container içinde 6 ayrı database oluşturulur.

---

## Servisler Arası İletişim

### Feign Client (Senkron)

Bir servis başka bir servisten veri almak istediğinde HTTP ile çağırır. Feign bu çağrıyı
sanki lokal bir metod çağırır gibi yazmayı sağlar:

```java
// cart-service, product-service'i böyle çağırır:
@FeignClient(name = "product-service")
public interface ProductFeignClient {
    @GetMapping("/api/products/{id}")
    ProductResponse getProduct(@PathVariable Long id);
}
```

Eureka sayesinde `product-service` ismi otomatik olarak doğru IP:port'a çözülür.

### RabbitMQ (Asenkron)

Bir servis başka bir servise "bir şey oldu" diye haber vermek istediğinde mesaj kuyruğu kullanır.
Örneğin ödeme tamamlandığında payment-service, order-service'e bildirir — ama cevabını beklemez.

```
payment-service → PAYMENT_COMPLETED eventi yayınlar
order-service   → bu eventi dinler, sipariş durumunu PAID yapar
notification-service → aynı eventi dinler, kullanıcıya email gönderir
```

Avantajı: payment-service, order-service'in ayakta olup olmadığını umursamaz.

---

## Docker Compose

Tüm servislerin local'de tek komutla ayağa kalkmasını sağlar:
- PostgreSQL (6 veritabanıyla)
- RabbitMQ (mesaj kuyruğu)
- Redis (rate limiting için)
- Config Server, Eureka, Gateway ve tüm iş servisleri

```bash
docker-compose up
```

---

## Jib — Dockerfile Olmadan Docker Image

Google'ın geliştirdiği Maven plugin'i. Normal Docker build'den farkı:
- Dockerfile yazmak gerekmez
- Docker daemon çalışmak zorunda değil
- Doğrudan registry'e push edebilir (CI/CD için ideal)

```bash
mvn compile jib:dockerBuild   # local Docker'a image yükle
mvn compile jib:build         # doğrudan ECR/registry'e push et
```

---

## CI/CD (GitHub Actions)

**CI (Continuous Integration):** Her push'ta otomatik çalışır. `mvn clean verify` ile tüm testleri koşar.

**CD (Continuous Deployment):** `main` branch'e push'ta çalışır:
1. Tüm servisler Jib ile Docker image olarak build edilir
2. AWS ECR'a push edilir
3. Elastic Beanstalk'a deploy edilir

---

## AWS Deployment

- **Elastic Beanstalk:** Uygulamanın çalıştığı platform (multi-container Docker)
- **RDS PostgreSQL:** Managed veritabanı servisi
- **Amazon MQ:** Managed RabbitMQ (production'da container yerine)
- **ElastiCache Redis:** Managed Redis (rate limiting için)
- **SSM Parameter Store:** Gizli bilgileri (JWT secret, DB şifresi) güvenli saklar

---

## Test Stratejisi

### Unit Test (Mockito)

Her servisin iş mantığı (`@Service` sınıfları), gerçek veritabanı veya dış servis olmadan test edilir.
Bağımlılıklar (repository, feign client) Mockito ile taklit edilir (mock):

```java
@ExtendWith(MockitoExtension.class)
class CartServiceTest {
    @Mock CartRepository cartRepository;     // sahte repository
    @Mock ProductFeignClient productClient;  // sahte feign client
    @InjectMocks CartService cartService;    // gerçek servis, sahte bağımlılıklarla

    @Test
    void givenEmptyCart_whenGetCart_thenReturnEmptyResponse() { ... }
}
```

İsimlendirme kuralı `given_when_then`: testin ne varsaydığını, ne yaptığını ve ne beklediğini açıklar.

### Integration Test (TestContainers)

Gerçek bir PostgreSQL veritabanıyla HTTP endpoint'lerini uçtan uca test eder.
TestContainers, test sırasında gerçek bir Docker container ayağa kaldırır:

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class AuthIntegrationTest extends AbstractIntegrationTest {
    // Gerçek PostgreSQL container, gerçek HTTP isteği, gerçek JWT üretimi
    @Test
    void givenValidCredentials_whenLogin_thenReturnTokenPair() { ... }
}
```

`AbstractIntegrationTest` base class: her test sınıfı `extends` eder, container tek seferinde başlar
(`static` field), tüm test metodları aynı container'ı paylaşır — hız için.

`@DynamicPropertySource`: container'ın rastgele portunu `spring.datasource.url`'e dinamik olarak bağlar.

---

## Spring Boot Auto-Configuration

`common-lib` paylaşılan bir kütüphanedir — bir framework gibi davranması gerekir.
`GlobalExceptionHandler` her serviste otomatik devreye girmelidir ama servislerin kendi
`@ComponentScan`'ı sadece kendi paketini tarar.

Çözüm: **Spring Boot Auto-Configuration**.

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` dosyasına
auto-configuration sınıfı eklenir. Spring Boot başlarken bu dosyayı okur, listedeki sınıfları
otomatik olarak application context'e ekler:

```
common-lib
└── META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
    → com.ecommerce.common.config.CommonLibAutoConfiguration
        → @Import(GlobalExceptionHandler.class)
```

Bu sayede her servis `common-lib`'i dependency olarak eklediğinde exception handler da otomatik gelir.
Spring'in kendi `spring-boot-autoconfigure` kütüphanesi de aynı mekanizmayla çalışır.

---

*Bu dosya proje geliştirildikçe güncellenmektedir.*
