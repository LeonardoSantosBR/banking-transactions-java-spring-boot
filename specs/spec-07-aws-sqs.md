# Spec de Mensageria — Transactional Outbox e Amazon SQS

## Objetivo

Desacoplar a criação da transação Pix do processamento assíncrono, garantindo que a transação e seu evento sejam persistidos no PostgreSQL antes do envio ao Amazon SQS.

## Fluxo

```text
TransactionsController
        v
TransactionsService
        +--> transactions
        +--> outbox_events (PENDING)
                         v
              OutboxPublisher (@Scheduled)
                         v
                    Amazon SQS
                         v
              TransactionEventConsumer
                         v
                    transactions
```

A transação e o evento devem ser gravados na mesma transação do banco. Se o SQS estiver indisponível, o evento permanece na Outbox para retry.

## Filas

```text
banking-transactions-events.fifo
banking-transactions-events-dlq.fifo
```

A fila principal deve apontar para a DLQ por meio de uma redrive policy. O `visibility timeout` deve ser maior que o tempo máximo de processamento. A DLQ recebe mensagens após o limite de tentativas configurado.

## Componentes

- `OutboxEventsRepository`: consulta e atualiza eventos.
- `OutboxEventsService`: cria e publica eventos.
- `SqsService`: encapsula o AWS SDK.
- `OutboxPublisher`: busca eventos `PENDING` e envia lotes ao SQS.
- `TransactionEventConsumer`: processa mensagens e atualiza transações.

## Publicação

O publisher deve buscar eventos `PENDING`, enviá-los ao SQS e marcar `PUBLISHED` somente após confirmação. Em caso de falha, o evento deve continuar disponível para nova tentativa.

Se ocorrer falha depois do envio e antes da atualização do banco, o evento pode ser reenviado; por isso, o consumidor deve ser idempotente.

## Mensagem

```json
{
  "eventId": "uuid-do-outbox-event",
  "eventType": "PixTransactionRequested",
  "transactionId": "uuid-da-transacao",
  "idempotencyKey": "req-123456789-2026",
  "createdAt": "2026-09-28T12:00:00Z"
}
```

Para FIFO, usar `MessageDeduplicationId = eventId` e `MessageGroupId = payerAccountId`. Mensagens do mesmo grupo serão processadas em ordem; grupos diferentes podem ser processados em paralelo.

## Consumidor

O consumidor deve validar a mensagem, consultar a transação, verificar seu status atual, aplicar a transição permitida e excluir a mensagem somente após sucesso. Em falha, a mensagem deve reaparecer após o `visibility timeout`.

Transições esperadas:

```text
PROCESSING -> SETTLED
PROCESSING -> REJECTED
SETTLED -> REFUNDED
```

Mensagens duplicadas devem ser ignoradas usando `eventId`, `transactionId`, `idempotencyKey` e o status atual da transação.

## Segurança

Não armazenar credenciais AWS no código ou no repositório. Localmente, usar perfil AWS ou variáveis de ambiente. Em produção, usar IAM Role.

Permissões mínimas esperadas: `sqs:SendMessage`, `sqs:ReceiveMessage`, `sqs:DeleteMessage`, `sqs:ChangeMessageVisibility` e `sqs:GetQueueAttributes`.

## Ordem de implementação

1. Criar fila FIFO e DLQ FIFO.
2. Configurar redrive policy e visibility timeout.
3. Adicionar o AWS SDK SQS.
4. Implementar `SqsService`.
5. Implementar repository e service da Outbox.
6. Salvar transação e Outbox atomicamente.
7. Implementar publisher agendado.
8. Implementar consumidor.
9. Testar publicação, retry, duplicidade e DLQ.
