# Spec de Transações Pix

Este documento descreve o cadastro, a consulta e o ciclo de vida das transações Pix da aplicação.

## 1. Objetivo

A tabela `transactions` funciona como o ledger das operações Pix. Cada registro representa uma tentativa de transferência entre uma conta pagadora e uma conta recebedora.

A transação deve preservar os dados necessários para auditoria, conciliação, idempotência e processamento assíncrono.

## 2. Identificadores

### `id`

Identificador interno da entidade no banco de dados. É um UUID gerado pela aplicação/JPA e não representa o identificador oficial do Pix.

### `idempotency_key`

Chave enviada pelo cliente para evitar duplicidade em retries. Se o cliente repetir uma requisição com a mesma chave, a aplicação não deve criar uma segunda transação.

Essa coluna possui restrição `UNIQUE`.

Exemplo:

```text
req-123456789-2026
```

### `txid`

Identificador da cobrança Pix, principalmente usado em QR Code dinâmico. Permite que o recebedor associe o pagamento a um pedido, fatura ou cobrança específica.

O `txid` não substitui o `end_to_end_id`.

### `end_to_end_id`

Identificador da transação Pix no fluxo entre as instituições financeiras. Ele permite rastrear e conciliar o pagamento efetivamente processado.

Na implementação inicial, a aplicação gera um valor temporário compatível com o tamanho da coluna. Quando houver integração com um PSP ou instituição Pix real, o valor deverá seguir o formato e a responsabilidade definidos pela integração oficial.

## 3. Dados de QR Code

| Campo | Descrição |
|---|---|
| `qr_code_type` | Tipo de iniciação: `STATIC`, `DYNAMIC` ou `COPY_AND_PASTE` |
| `qr_code_payload` | Conteúdo original lido no QR Code ou Pix Copia e Cola |
| `txid` | Identificador da cobrança associada ao QR Code |

Esses campos são opcionais porque uma transação também pode ser iniciada por chave Pix ou por outra modalidade de entrada.

## 4. Regras de criação

- A `idempotency_key` é obrigatória e deve ser única.
- A conta pagadora deve existir.
- A conta recebedora deve existir.
- A conta pagadora e a conta recebedora devem ser diferentes.
- `pix_key_used` deve preservar a chave utilizada no momento do pagamento.
- `amount` deve ser maior que zero.
- `currency` utiliza `BRL` como valor padrão.
- O status inicial é `PROCESSING`.
- O `end_to_end_id` deve ser único.
- O saldo não deve ser alterado por um CRUD genérico; alterações de saldo pertencem ao fluxo transacional de reserva e liquidação.

## 5. Endpoints

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/transactions` | Cria uma transação Pix em processamento |
| `GET` | `/api/transactions` | Lista transações |
| `GET` | `/api/transactions/{id}` | Consulta uma transação |
| `PUT` | `/api/transactions/{id}` | Atualiza status e descrição |
| `DELETE` | `/api/transactions/{id}` | Remove uma transação no CRUD inicial |

O `DELETE` existe para o CRUD inicial, mas não deve ser usado para transações reais já liquidadas. O ledger de produção deve ser append-only, preservando o histórico.

## 6. Exemplo de criação

```json
{
  "idempotencyKey": "req-123456789-2026",
  "payerAccountId": "uuid-da-conta-pagadora",
  "payeeAccountId": "uuid-da-conta-recebedora",
  "pixKeyUsed": "user@example.com",
  "amount": "150.50",
  "qrCodeType": "DYNAMIC",
  "qrCodePayload": "00020126...",
  "txid": "PEDIDO123456",
  "currency": "BRL",
  "channel": "MOBILE_APP",
  "description": "Pagamento de serviços prestados"
}
```

## 7. Ciclo de vida

```text
PROCESSING
    ├──> SETTLED
    └──> REJECTED

SETTLED
    └──> REFUNDED (fluxo futuro)
```

- `PROCESSING`: transação criada e aguardando processamento.
- `SETTLED`: pagamento confirmado e liquidado.
- `REJECTED`: pagamento rejeitado; eventual reserva deve ser estornada.
- `REFUNDED`: devolução de um pagamento já liquidado.

## 8. Processamento assíncrono

Após a criação da transação, o fluxo futuro deverá criar um evento em `outbox_events`. Um publicador enviará o evento ao Amazon SQS.

O consumidor deverá:

- processar a mensagem de forma idempotente;
- confirmar a mensagem somente após concluir o processamento;
- atualizar o status da transação;
- manter a mensagem disponível para retry em caso de falha;
- encaminhar mensagens que excederem as tentativas para a Dead Letter Queue.

## 9. Segurança e consistência

- O cliente não deve informar o `status` na criação.
- O cliente não deve informar o `end_to_end_id` na criação.
- A identidade do pagador deve ser obtida a partir do JWT e validada contra a conta pagadora.
- Dados do recebedor não devem ser confiados apenas no payload recebido pelo cliente.
- A chave Pix e o QR Code devem ser validados antes da liquidação.
- Operações de saldo devem utilizar transação de banco e lock otimista da conta.
