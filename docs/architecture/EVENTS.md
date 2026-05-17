# Event-Driven Architecture Governance

## Core Principles
- **Eventual Consistency**: Use events to synchronize state across modules.
- **Idempotency**: All event consumers MUST be idempotent.
- **Outbox Pattern**: Use the Outbox pattern to ensure reliable event delivery alongside DB transactions.

## Event Types
- **Domain Events**: Internal to a module, triggered by aggregate changes.
- **Integration Events**: Published to other modules to trigger cross-context actions.

## Infrastructure
- **Message Broker**: RabbitMQ (standard) or Kafka (high-scale).
- **Dead Letter Queues (DLQ)**: Every queue must have a corresponding DLQ.
- **Retry Policy**: Exponential backoff with a maximum of 3 retries before moving to DLQ.

## Event Schema Governance
- Events must be versioned.
- Backward compatibility must be maintained for at least two versions.
