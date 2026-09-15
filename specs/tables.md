# Spec do Banco de Dados — API Simulação Pix

Documento de referência do schema PostgreSQL usado pela API. Cada tabela é criada por uma migration Flyway (`V1` a `V5`, em `src/main/resources/db/migration/`), aplicada nessa ordem por causa das dependências de chave estrangeira.

---

## 1. `users`

Armazena os dados cadastrais da pessoa/cliente dona das contas.

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | UUID (PK) | Identificador único do usuário |
| `name` | VARCHAR(150) | Nome completo |
| `cpf` | VARCHAR(11) | CPF, único no sistema |
| `email` | VARCHAR(150) | E-mail, único no sistema |
| `created_at` | TIMESTAMPTZ | Data de criação |
| `updated_at` | TIMESTAMPTZ | Data da última atualização |
| `deleted_at` | TIMESTAMPTZ (nullable) | Preenchido no soft delete; `NULL` = usuário ativo |

**Constraints:** `UNIQUE(cpf)`, `UNIQUE(email)`
**Índices:** `deleted_at` (acelera filtros de usuários ativos)
**Relacionamentos:** um usuário pode ter várias `accounts`

---

## 2. `accounts`

Representa uma conta bancária vinculada a um usuário, guardando o saldo real em tipo monetário seguro.

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | UUID (PK) | Identificador único da conta |
| `user_id` | UUID (FK → `users.id`) | Dono da conta |
| `branch` | VARCHAR(10) | Agência |
| `account_number` | VARCHAR(20) | Número da conta |
| `balance` | DECIMAL(19,4) | Saldo atual (nunca `float`/`double`) |
| `version` | BIGINT | Contador de versão para lock otimista (`@Version` do JPA) |
| `created_at` | TIMESTAMPTZ | Data de criação |
| `updated_at` | TIMESTAMPTZ | Data da última atualização |

**Constraints:** `UNIQUE(branch, account_number)`, `CHECK(balance >= 0)` (trava saldo negativo direto no banco)
**Índices:** `user_id`
**Relacionamentos:** pertence a um `user`; pode ter várias `pix_keys`; é referenciada por `transactions` como pagadora ou recebedora

---

## 3. `pix_keys`

Chaves Pix cadastradas, separadas da conta para permitir múltiplas chaves por conta.

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | UUID (PK) | Identificador único da chave |
| `account_id` | UUID (FK → `accounts.id`) | Conta dona da chave |
| `key_type` | ENUM `pix_key_type` (`CPF`, `EMAIL`, `PHONE`, `RANDOM`) | Tipo da chave |
| `key_value` | VARCHAR(150) | Valor da chave (CPF formatado, e-mail, telefone ou chave aleatória/EVP) |
| `active` | BOOLEAN | Permite desativar a chave sem excluir o histórico |
| `created_at` | TIMESTAMPTZ | Data de criação |

**Constraints:** `UNIQUE(key_value)` (uma chave Pix pertence a uma única conta no sistema todo)
**Índices:** `account_id`; índice parcial em `key_value` (só linhas `active = true`) — otimiza a busca de conta de destino no fluxo de pagamento
**Relacionamentos:** pertence a uma `account`

---

## 4. `transactions`

Livro-razão (ledger) de todas as operações Pix — o registro histórico e auditável de cada transferência.

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | UUID (PK) | Identificador único da transação |
| `end_to_end_id` | VARCHAR(50) | Identificador padrão Pix (ex: `E09089456...`), único |
| `idempotency_key` | VARCHAR(100) | Chave enviada pelo client no `POST /api/pix`, única — evita duplicação em retries |
| `payer_account_id` | UUID (FK → `accounts.id`) | Conta pagadora |
| `payee_account_id` | UUID (FK → `accounts.id`) | Conta recebedora |
| `pix_key_used` | VARCHAR(150) | Snapshot da chave usada no momento do pagamento (não é FK, para preservar histórico mesmo se a chave for desativada depois) |
| `amount` | DECIMAL(19,4) | Valor transferido |
| `status` | ENUM `transaction_status` (`PROCESSING`, `SETTLED`, `REJECTED`, `REFUNDED`) | Estado atual da transação |
| `description` | VARCHAR(200) (nullable) | Descrição opcional informada pelo pagador |
| `created_at` | TIMESTAMPTZ | Data de criação |
| `updated_at` | TIMESTAMPTZ | Data da última atualização de status |

**Constraints:** `UNIQUE(end_to_end_id)`, `UNIQUE(idempotency_key)`, `CHECK(amount > 0)`, `CHECK(payer_account_id <> payee_account_id)` (impede Pix de uma conta para ela mesma)
**Índices:** `payer_account_id`, `payee_account_id`, `status`
**Relacionamentos:** referencia duas `accounts` (pagadora e recebedora); gera um `outbox_events`

**Fluxo de status:** `PROCESSING` (criada na reserva) → `SETTLED` (consumer confirma) **ou** `REJECTED` (consumer rejeita, com estorno na conta pagadora) → `REFUNDED` (uso futuro para devoluções pós-liquidação)

---

## 5. `outbox_events`

Implementa o Transactional Outbox Pattern: garante que a gravação da transação e a publicação do evento no RabbitMQ aconteçam de forma atômica, evitando o problema de dual-write.

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | UUID (PK) | Identificador único do evento |
| `transaction_id` | UUID (FK → `transactions.id`) | Transação relacionada |
| `event_type` | VARCHAR(100) | Nome do evento (ex: `PixSolicitadoEvent`) |
| `payload` | JSONB | Conteúdo do evento a ser publicado |
| `status` | ENUM `outbox_event_status` (`PENDING`, `PUBLISHED`, `FAILED`) | Estado da publicação |
| `created_at` | TIMESTAMPTZ | Data de criação do evento |
| `published_at` | TIMESTAMPTZ (nullable) | Data em que foi publicado no RabbitMQ |

**Constraints:** nenhuma UNIQUE em `transaction_id` (uma transação pode, no futuro, gerar mais de um evento)
**Índices:** índice parcial em `(status, created_at)` só para `status = 'PENDING'` — é exatamente a query que o processo `@Scheduled` (outbox publisher) roda repetidamente
**Relacionamentos:** pertence a uma `transaction`

---

## Diagrama de Relacionamentos (resumo textual)

```
users (1) ──< accounts (N)
accounts (1) ──< pix_keys (N)
accounts (1) ──< transactions (N)  [como payer_account]
accounts (1) ──< transactions (N)  [como payee_account]
transactions (1) ──< outbox_events (N)
```

## Tabela de controle (gerada automaticamente pelo Flyway)

`flyway_schema_history` — não faz parte do domínio da aplicação; é criada e mantida pelo próprio Flyway para registrar quais migrations já foram aplicadas e quando.

## RabbitMQ 
o RabbitMQ simula o SPI (Sistema de Pagamentos Instantâneos) do Banco Central — o "meio de campo" assíncrono entre o momento em que o pagamento é solicitado e o momento em que ele é efetivamente liquidado. A ideia é reproduzir o comportamento real do Pix: a requisição HTTP responde rápido (202 Accepted), mas a liquidação de fato acontece em segundo plano, com uma latência simulada.
Por que usar uma fila em vez de só chamar um método direto: desacopla a parte síncrona (responder rápido pro cliente) da parte assíncrona (processamento que pode demorar, falhar e precisar de retry) — exatamente como o Pix real funciona, onde o banco pagador não fica bloqueado esperando o Banco Central confirmar.