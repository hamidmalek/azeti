# Azeti Note-Taking API

A secure, rate-limited note-taking REST API built with **Spring Boot** and **Kotlin**.

## ✨ Features

- JWT-based Authentication (Register/Login)
- Create, Read, Update, and Delete Notes
- Per-user Rate Limiting (via Bucket4J)
- H2 Database for local persistence
- Flyway-based database migration
- Validation and error handling

## 🚀 Getting Started

### Prerequisites

- JDK 17+
- Gradle or use `./gradlew` (wrapper included)

### Run the Application

```bash
./gradlew bootRun
```

The API will be available at: `http://localhost:8080`

## 🧪 API Endpoints

### 🔐 Authentication

| Method | Endpoint             | Description          |
|--------|----------------------|----------------------|
| POST   | `/api/auth/register` | Register a new user  |
| POST   | `/api/auth/login`    | Login and get JWT    |

### 📝 Notes (Requires Authentication)

| Method | Endpoint             | Description          |
|--------|----------------------|----------------------|
| POST   | `/api/notes`         | Create a new note    |
| PUT    | `/api/notes/{id}`    | Update a note        |
| DELETE | `/api/notes/{id}`    | Delete a note        |
| GET    | `/api/notes/latest`  | Get latest 1000 notes|

To use protected endpoints, include the JWT in the `Authorization` header:

```
Authorization: Bearer <your-token-here>
```

## 🛠️ Architecture

### Core Design Decisions

- **Spring Boot + Kotlin** for concise, readable syntax and full Spring ecosystem support.
- **Stateless JWT Authentication** ensures scalability and session-less access control.
- **Rate Limiting** via Bucket4J to throttle requests per user.
- **Layered Architecture**:
  - **Controller** handles REST APIs.
  - **Service** encapsulates business logic.
  - **Repository** manages database interactions.
- **Validation** using JSR-303 annotations.
- **Flyway** for repeatable and version-controlled schema migrations.
- **Indexes** improve performance on queries by user and expiration fields.

## 🧰 Technologies Used

- Kotlin
- Spring Boot
- Spring Security
- JWT (via `jjwt`)
- H2 Database
- Hibernate/JPA
- Bucket4J
- Flyway

## 🧪 Running Tests

```bash
./gradlew test
```
