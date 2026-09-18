# Plano de CRUD e endpoints

## Objetivo

Implementar a API de cadastro e gerenciamento de usuários, contas bancárias e chaves Pix, respeitando os relacionamentos definidos nas migrations.

## Ordem de implementação

1. CRUD de usuários
2. Criação e consulta de contas
3. Cadastro, consulta e desativação de chaves Pix
4. Validações e tratamento global de erros
5. Testes dos endpoints

## Usuários

### Endpoints

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/users` | Cadastra um usuário |
| `GET` | `/api/users` | Lista usuários ativos |
| `GET` | `/api/users/{id}` | Busca um usuário por ID |
| `PUT` | `/api/users/{id}` | Atualiza os dados do usuário |
| `DELETE` | `/api/users/{id}` | Realiza soft delete usando `deleted_at` |

### Regras

- `name`, `cpf` e `email` são obrigatórios.
- CPF e e-mail devem ser únicos.
- Usuários removidos logicamente não devem aparecer nas listagens padrão.
- O `DELETE` não deve apagar fisicamente o registro.

## Contas

Uma conta só pode ser criada depois que o usuário existir, pois `accounts.user_id` é uma chave estrangeira obrigatória.

### Endpoints

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/users/{userId}/accounts` | Cria uma conta para o usuário |
| `GET` | `/api/users/{userId}/accounts` | Lista as contas do usuário |
| `GET` | `/api/accounts/{id}` | Busca uma conta por ID |

### Regras

- `branch` e `accountNumber` são obrigatórios.
- A combinação agência + número da conta deve ser única.
- O saldo inicial deve ser zero.
- O saldo deve ser representado por `BigDecimal`.
- O campo `version` será usado para lock otimista.

## Chaves Pix

### Endpoints

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/accounts/{accountId}/pix-keys` | Adiciona uma chave Pix |
| `GET` | `/api/accounts/{accountId}/pix-keys` | Lista as chaves da conta |
| `DELETE` | `/api/pix-keys/{id}` | Desativa uma chave Pix |

### Regras

- Os tipos aceitos são `CPF`, `EMAIL`, `PHONE` e `RANDOM`.
- `keyValue` deve ser único no sistema.
- A remoção deve ser lógica, alterando `active` para `false`.
- Chaves desativadas devem permanecer no banco para preservar o histórico.
- Uma chave Pix pertence a uma única conta.

## Fluxo principal

```text
POST /api/users
        |
        v
POST /api/users/{userId}/accounts
        |
        v
POST /api/accounts/{accountId}/pix-keys
```

## Camadas sugeridas

Para cada recurso, criar:

- `Repository`: acesso ao banco com Spring Data JPA.
- `DTO`: objetos de entrada e saída da API.
- `Service`: regras de negócio e transações.
- `Controller`: endpoints HTTP.
- Testes unitários e de integração.

## Tratamento de erros

A API deve retornar respostas consistentes para:

- Dados inválidos: `400 Bad Request`.
- Recurso não encontrado: `404 Not Found`.
- CPF, e-mail ou chave Pix duplicados: `409 Conflict`.
- Erros inesperados: `500 Internal Server Error`.
