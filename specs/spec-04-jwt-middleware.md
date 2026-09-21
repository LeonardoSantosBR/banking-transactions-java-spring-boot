# Spec-04 — Middleware de validação JWT

## Objetivo

Implementar um middleware HTTP responsável por validar tokens JWT e proteger endpoints que acessam dados de um usuário específico.

O middleware deve garantir que o usuário autenticado pelo token seja o mesmo usuário identificado pelo parâmetro `{id}` da requisição.

## Header de autenticação

As rotas protegidas devem receber o token no header HTTP:

```http
Authorization: Bearer <jwt>
```

O middleware deve rejeitar requisições sem `Authorization`, sem o esquema `Bearer`, ou com token vazio ou malformado.

## Validação do JWT

O token deve ser validado usando o segredo configurado em `app.jwt.secret`.

A validação deve confirmar assinatura, estrutura, expiração e `subject` válido. O `subject` representa o ID do usuário autenticado e deve ser convertido para `UUID`.

Tokens inválidos, expirados ou adulterados não devem permitir a execução do controller.

## Rotas públicas

As seguintes rotas não exigem JWT:

```http
POST /api/users
POST /api/auth/login
```

O login permanece público porque emite o token usado nas demais requisições.

## Rotas protegidas

As seguintes rotas exigem um JWT válido:

```http
GET /api/users/{id}
PUT /api/users/{id}
DELETE /api/users/{id}
```

O endpoint `GET /api/users` fica fora do escopo desta spec.

## Validação de identidade

Para cada rota protegida, o middleware deve:

1. extrair o token do header `Authorization`;
2. validar o token;
3. extrair o `UUID` pelo `subject`;
4. extrair o `{id}` da URL;
5. comparar os dois valores;
6. permitir a requisição somente quando forem iguais.

O controller não deve ser executado quando o token não representar o usuário indicado na URL.

## Respostas de erro

### Token ausente ou inválido

Para token ausente, malformado, expirado, adulterado ou com `subject` inválido, retornar `401 Unauthorized`.

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or missing JWT"
}
```

A mensagem não deve expor detalhes internos da validação.

### Usuário diferente do token

Quando o JWT for válido, mas o `subject` for diferente do `{id}` da URL, retornar `403 Forbidden`.

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "User is not authorized to access this resource"
}
```

## Implementação esperada

- Criar um filtro HTTP baseado em `OncePerRequestFilter`.
- Aplicar o filtro somente às rotas protegidas.
- Reutilizar o `JwtService` para validar o token e extrair o usuário autenticado.
- Interromper a cadeia de filtros quando a autenticação ou autorização falhar.
- Não consultar o banco apenas para validar a assinatura do token.
- Manter segredo e expiração configuráveis externamente.

## Critérios de aceite

- Requisição sem `Authorization` para rota protegida retorna `401`.
- Header sem `Bearer` retorna `401`.
- Token malformado, expirado ou com assinatura inválida retorna `401`.
- Token com `subject` inválido retorna `401`.
- Token válido permite acesso quando o `subject` corresponde ao `{id}`.
- Token válido para outro usuário retorna `403`.
- Requisições rejeitadas não chegam ao controller.
- Cadastro e login continuam acessíveis sem JWT.
- `GET /api/users` não é alterado por esta spec.
- Senha e conteúdo completo do token nunca aparecem nas respostas de erro.

## Testes previstos

Adicionar testes para token ausente, formato inválido, expiração, assinatura inválida, `subject` inválido, usuário próprio, usuário diferente, rotas públicas e garantia de que requisições rejeitadas não chegam ao controller.

## Fora do escopo

- refresh token;
- revogação ou blacklist;
- perfis ou permissões administrativas;
- proteção do endpoint de listagem;
- alteração do fluxo de login ou geração do JWT definido na spec-03.
