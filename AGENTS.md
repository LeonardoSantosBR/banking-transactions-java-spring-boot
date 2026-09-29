# Contribution Guidelines

## Project context

This repository is a Java 21 Spring Boot API that simulates Pix banking transactions. Read the relevant document in `specs/` before changing domain behavior.

## Required practices

- Preserve the layered structure: controller, DTO, service, repository, entity, and exception.
- Keep business rules in services, not controllers.
- Use constructor injection for Spring dependencies.
- Use `BigDecimal` for monetary values; never use `float` or `double`.
- Use UUIDs for entity identifiers.
- Add a Flyway migration for every schema change. Never edit an applied migration.
- Keep transaction creation idempotent using `idempotency_key`.
- Do not accept server-controlled fields such as transaction status or `end_to_end_id` during creation.
- Preserve transaction history; do not physically delete settled records in production flows.

## AWS and secrets

- Never commit access keys, secret keys, passwords, JWT secrets, or queue credentials.
- Use the AWS SDK default credential provider chain locally and IAM Roles in AWS environments.
- Restrict SQS permissions to the project queues.
- Use FIFO with explicit `MessageGroupId` and `MessageDeduplicationId` for transaction events.
- Keep retry handling and idempotent consumers even with FIFO deduplication.

## Outbox rules

- Persist the transaction and its `outbox_events` record in the same database transaction.
- Publish only events with status `PENDING`.
- Mark an event `PUBLISHED` only after SQS confirms the send.
- Assume a crash can happen after sending to SQS and before updating PostgreSQL; consumers must handle duplicates.
- Delete an SQS message only after successful processing.

## Validation

Before handing off a change:

1. Run `./mvnw.cmd test` on Windows or `./mvnw test` on Unix-like systems.
2. Check that Flyway migrations are ordered and valid.
3. Review routes and DTO validation.
4. Confirm that no secrets or local configuration files are staged.

If Maven cannot run, report that clearly instead of claiming the build passed.
