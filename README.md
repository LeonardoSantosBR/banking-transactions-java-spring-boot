# Banking Transactions API

A Spring Boot API that simulates a Pix banking system, focused on authentication, account management, Pix keys, transaction consistency, and asynchronous processing.

## Current capabilities

- User registration, update, listing, login, and soft deletion.
- JWT authentication and authorization by user identity.
- Bank account and Pix key CRUD.
- Pix transaction CRUD with idempotency validation.
- PostgreSQL schema managed by Flyway migrations.
- AWS SQS FIFO and Dead Letter Queue configuration.

## Architecture

```text
Client -> REST API -> PostgreSQL
                    -> Transactional Outbox -> SQS FIFO -> Consumer
```

Transactions are intended to be persisted together with an outbox event. A publisher sends pending events to Amazon SQS, and a consumer processes them idempotently.

## Technology stack

- Java 21, Spring Boot, Spring Web MVC
- Spring Data JPA / Hibernate
- Spring Security and JWT
- PostgreSQL and Flyway
- Amazon SQS SDK for Java 2.x
- Maven

## Requirements

- Java 21+
- Docker and Docker Compose
- AWS CLI with access to the project SQS queues

## Configuration

Keep secrets outside the repository. The AWS SDK uses the standard credential provider chain, including the profile configured by `aws configure`.

```properties
aws.region=us-east-1
aws.sqs.transaction-queue-url=https://sqs.us-east-1.amazonaws.com/<account>/banking-transactions-events.fifo
aws.sqs.transaction-dlq-url=https://sqs.us-east-1.amazonaws.com/<account>/banking-transactions-events-dlq.fifo
```

```bash
aws configure
aws sts get-caller-identity
```

## Running locally

```bash
docker compose up -d
```

On Windows:

```powershell
./mvnw.cmd spring-boot:run
./mvnw.cmd test
```

Flyway applies migrations automatically on startup.

## Main endpoints

```text
POST   /api/users
POST   /api/auth/login
POST   /api/accounts/{userId}
GET    /api/accounts/{userId}
POST   /api/pix-keys/{accountId}
GET    /api/pix-keys/{accountId}
POST   /api/transactions
GET    /api/transactions
GET    /api/transactions/{id}
PUT    /api/transactions/{id}
```

Protected endpoints require `Authorization: Bearer <jwt>`.

## Domain concepts

- `idempotencyKey`: prevents duplicate transaction creation when a client retries.
- `txid`: identifies a Pix charge, especially one associated with a dynamic QR Code.
- `endToEndId`: identifies the Pix payment across the payment flow.
- `outbox_events`: stores integration events before they are sent to SQS.

Detailed specifications are available in the [`specs`](specs) directory.
