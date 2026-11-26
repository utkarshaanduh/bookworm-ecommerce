# Book Worm - E-Bookstore Backend

Spring Boot 3.3 / Java 17 / PostgreSQL REST API for the capstone e-bookstore.

## Journey coverage (from the capstone slides)

| Journey | Endpoint(s) |
|---|---|
| Login / registration | `POST /api/auth/register`, `/login`, `/logout`, `GET /me` |
| Landing page + recommendations from order history | `GET /api/home`, `/api/recommendations` |
| Categories / brands / search / filters / sort | `GET /api/categories`, `/brands`, `/books?...` |
| Product page + related reads + reviews | `GET /api/books/{id}`, `POST /api/books/{id}/reviews` |
| Tentative delivery date | `estimatedDelivery` on every book |
| Cart, wishlist | `/api/cart...`, `/api/wishlist...` |
| Address, coupon, gift points, payment | `/api/addresses`, `/api/checkout/preview`, `POST /api/orders` |
| Order history + Buy In Again | `GET /api/orders`, `POST /api/orders/{id}/buy-again` |
| Cancel within 48 hrs (refund) | `POST /api/orders/{id}/cancel` |
| Admin catalogue | `/api/admin/books` (ADMIN role) |

Full contract: [docs/openapi.yaml](docs/openapi.yaml). Live Swagger UI at `/swagger-ui.html` once running.

## Data model

`User 1-* Address`, `User 1-* CartItem *-1 Book`, `User 1-* WishlistItem *-1 Book`,
`Category 1-* Book`, `Book 1-* Review *-1 User`, `User 1-* Order 1-* OrderItem *-1 Book`,
`Order 1-1 Payment`, `Coupon` (standalone).

Business rules (configurable in `application.properties`): 12% tax, Rs 40 delivery under Rs 500,
1 gift point = Rs 1, earn 1 point per Rs 10 paid, 48-hour cancellation window.
Payment is a **simulated** gateway (always succeeds).

## Run locally

1. Install JDK 17, Maven 3.9+, PostgreSQL.
2. Create the database: `createdb -U postgres bookworm` (or `CREATE DATABASE bookworm;` in pgAdmin).
3. Set the password in `src/main/resources/application.properties` or via env vars `DB_URL`, `DB_USER`, `DB_PASSWORD`.
4. `mvn clean install` then `mvn spring-boot:run`.

Seed accounts (first start only): `demo@bookworm.com / demo1234`, `admin@bookworm.com / admin123`.
Coupons: `WELCOME100` (Rs 100), `BOOKS50` (Rs 50).

## Quick test

```
curl -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"email\":\"demo@bookworm.com\",\"password\":\"demo1234\"}"
curl localhost:8080/api/books?category=Mystery
curl -X POST localhost:8080/api/cart/items -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d "{\"bookId\":1}"
```
