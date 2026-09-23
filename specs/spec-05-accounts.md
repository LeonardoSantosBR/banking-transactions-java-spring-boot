# Spec de Contas — API de Simulação Pix

Este documento descreve a implementação do cadastro e da consulta de contas bancárias vinculadas aos usuários.

## 1. Modelo de relacionamento

Um usuário pode possuir várias contas:

```text
users 1:N accounts
```

Cada conta pertence obrigatoriamente a um usuário por meio da coluna `user_id`.

## 2. Estrutura da tabela

A tabela `accounts` é criada pela migration `V2__create_accounts_table.sql` e contém:

| Coluna | Descrição |
|---|---|
| `id` | Identificador UUID da conta |
| `user_id` | Usuário proprietário da conta |
| `branch` | Número da agência bancária |
| `account_number` | Número da conta bancária |
| `balance` | Saldo atual, iniciado em zero |
| `version` | Versão usada pelo lock otimista do JPA |
| `created_at` | Data de criação |
| `updated_at` | Data da última atualização |

## 3. `branch` e `account_number`

`branch` representa a agência bancária. `account_number` representa o número da conta dentro dessa agência.

Uma conta é identificada pela combinação dos dois valores:

```text
branch + account_number
```

Essa combinação possui uma constraint `UNIQUE` no banco:

```sql
UNIQUE (branch, account_number)
```

Portanto:

- a mesma agência pode aparecer em várias contas;
- o mesmo número pode aparecer em agências diferentes;
- a combinação agência + número não pode ser duplicada.

Antes de salvar uma conta, o `AccountsService` verifica essa combinação pelo método `existsByBranchAndAccountNumber`. Em caso de duplicidade, a API retorna `409 CONFLICT` por meio da exceção `AccountAlreadyRegisteredException`.

## 4. Camadas implementadas

### Repository

`AccountsRepository` fornece as operações de persistência e consultas por usuário, além da validação de duplicidade da agência e do número da conta.

### Service

`AccountsService` é responsável por:

- validar se o usuário existe e está ativo;
- validar a combinação `branch` + `account_number`;
- criar contas com saldo inicial igual a zero;
- listar contas de um usuário;
- buscar uma conta específica pertencente ao usuário;
- excluir uma conta.

### Controller

`AccountsController` expõe os seguintes endpoints:

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/accounts/{userId}` | Cria uma conta para o usuário |
| `GET` | `/api/accounts/{userId}` | Lista as contas do usuário |
| `GET` | `/api/accounts/{userId}/{accountId}` | Busca uma conta específica |
| `DELETE` | `/api/accounts/{userId}/{accountId}` | Exclui uma conta |

O corpo usado na criação é:

```json
{
  "branch": "0001",
  "accountNumber": "123456"
}
```

O saldo não é recebido pelo cliente e começa como `0`, evitando que uma conta seja criada com um valor arbitrário.

## 5. Segurança

As rotas de contas exigem um JWT válido. O middleware compara o `userId` presente no token com o `userId` informado na URL.

Assim, um usuário autenticado não pode consultar, criar ou excluir contas vinculadas a outro usuário.

## 6. Concorrência e saldo

A entidade utiliza `@Version` na coluna `version`. Esse campo permite o lock otimista do JPA, evitando que duas operações concorrentes sobrescrevam alterações feitas sobre a mesma conta.

As alterações de saldo devem ser realizadas por operações transacionais específicas, mantendo a regra do banco que impede valores negativos.
