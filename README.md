# Snip — URL Shortener

A production-ready URL shortener built with Spring Boot, Redis, and PostgreSQL. Features a token bucket rate limiter, Redis caching, persistent storage, and a clean UI — all containerised with Docker Compose.

---

## Live Demo

> Deploy to Railway or Fly.io and paste your link here.

---

## Features

- **URL Shortening** — converts long URLs to 7-character Base62 short codes
- **Custom Aliases** — users can choose their own short code (e.g. `/my-link`)
- **Redirect** — `GET /{code}` resolves and redirects to the original URL
- **Redis Caching** — cache-first lookup for sub-millisecond redirects
- **Token Bucket Rate Limiter** — per-IP rate limiting (10 requests, 1 token/sec refill)
- **Persistent Storage** — all mappings saved to PostgreSQL
- **Recent Links History** — browser-side history via localStorage
- **Fully Containerised** — runs with a single `docker-compose up --build`

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4 |
| Cache | Redis 7 |
| Database | PostgreSQL 15 |
| ORM | Spring Data JPA + Hibernate |
| Rate Limiting | Token Bucket (in-memory, per IP) |
| Containerisation | Docker + Docker Compose |
| Frontend | Vanilla HTML/CSS/JS (served as static resource) |

---

## Architecture

```
Client Request
      │
      ▼
RateLimiterFilter (Token Bucket — 10 req/IP, refill 1/sec)
      │
      ├── 429 Too Many Requests (if limit exceeded)
      │
      ▼
UrlController
      │
      ├── POST /shorten ──► UrlService ──► Redis (cache) + PostgreSQL (persist)
      │
      └── GET /{code}  ──► Redis (cache hit → return)
                               └── PostgreSQL (cache miss → fetch + re-cache → return)
                                         └── 302 Redirect to original URL
```

---

## Project Structure

```
src/main/java/com/example/urlshortner/
├── controller/
│   └── UrlController.java          # REST endpoints
├── service/
│   └── UrlService.java             # Business logic, Base62 encoding
├── repository/
│   └── UrlMappingRepository.java   # Spring Data JPA repository
├── model/
│   └── UrlMapping.java             # JPA entity
├── ratelimit/
│   └── RateLimiterFilter.java      # Token bucket rate limiter
└── UrlshortnerApplication.java

src/main/resources/
├── static/
│   └── index.html                  # Frontend UI
└── application.properties
```

---

## Getting Started

### Prerequisites

- Java 21
- Docker Desktop

### Run locally with Docker Compose

```bash
# 1. Clone the repo
git clone https://github.com/yourusername/url-shortener.git
cd url-shortener

# 2. Build the JAR
./mvnw clean package -DskipTests

# 3. Start all services (app + postgres + redis)
docker-compose up --build
```

Open `http://localhost:8080` in your browser.

### Run without Docker (requires local Redis + PostgreSQL)

Update `application.properties` with your local DB credentials, then:

```bash
./mvnw spring-boot:run
```

---

## API Reference

### Shorten a URL

```http
POST /shorten
Content-Type: application/json

{
  "url": "https://www.example.com/very/long/path",
  "alias": "my-link"   // optional
}
```

**Response:**
```json
{
  "shortCode": "my-link",
  "shortUrl": "http://localhost:8080/my-link"
}
```

### Redirect

```http
GET /{code}
```

Returns `302 Found` with `Location` header pointing to the original URL.

### Rate Limit Response

```http
HTTP/1.1 429 Too Many Requests

{"error": "Rate limit exceeded"}
```

---

## Rate Limiter — How It Works

Implemented as a `OncePerRequestFilter` using the **Token Bucket** algorithm:

- Each IP gets a bucket with **10 tokens**
- Every request consumes 1 token
- Tokens refill at **1 per second**
- Once the bucket is empty, requests return `429` until tokens refill
- Static resources (`/`, `.html`, `.js`, `.css`) are excluded from rate limiting

```java
// Per-IP token bucket stored in ConcurrentHashMap
TokenBucket bucket = buckets.computeIfAbsent(ip, k -> new TokenBucket(10, 1));
if (!bucket.tryConsume()) {
    response.setStatus(429);
    return;
}
```

---

## Caching Strategy

Redis is used as a **cache-aside** layer in front of PostgreSQL:

1. On `GET /{code}` — check Redis first
2. Cache hit → return immediately (no DB query)
3. Cache miss → query PostgreSQL → store in Redis with 24h TTL → return
4. On `POST /shorten` — write to both PostgreSQL and Redis

This gives O(1) average-case redirect performance.

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_USER` | `postgres` | PostgreSQL username |
| `DB_PASSWORD` | `secret` | PostgreSQL password |
| `REDIS_HOST` | `localhost` | Redis host |


---

## Screenshots

> Add screenshots of your UI here after deployment.

---

## Future Improvements

- [ ] Click analytics (track hits per short code via Redis INCR)
- [ ] URL expiry (TTL-based expiration)
- [ ] QR code generation for each short link
- [ ] User accounts and link management dashboard
- [ ] Swagger UI for API documentation

---

## Author

**Sathvik** — [GitHub](https://github.com/AvulaSathvik12345)


