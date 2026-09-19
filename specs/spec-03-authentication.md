# Autenticação por CPF e senha com JWT

## Objetivo

Implementar autenticação de usuários usando CPF e senha, armazenando somente o hash da senha no banco e retornando um token JWT após um login válido.

## Migration

Adicionar a migration `V6__add_password_to_users.sql`:

```sql
ALTER TABLE users ADD COLUMN password VARCHAR(255) NOT NULL;
```

Essa migration pressupõe que não existam usuários antigos sem senha. Em um banco já utilizado, os usuários existentes devem ser removidos ou receber uma senha antes da aplicação da restrição `NOT NULL`.

## Usuário e senha

- O campo `password` será obrigatório no cadastro.
- A senha nunca será armazenada em texto puro.
- O hash será gerado usando BCrypt.
- A entidade terá o campo `password`, mas ele não será exposto nas respostas HTTP.
- A senha poderá ser alterada durante a atualização do usuário.
- Se a senha não for enviada na atualização, a senha atual será mantida.

## DTOs

### Cadastro

`POST /api/users`

```json
{
  "name": "Leonardo Silva",
  "cpf": "12345678901",
  "email": "leonardo@email.com",
  "password": "senha123"
}
```

### Atualização

`PUT /api/users/{id}`

```json
{
  "name": "Leonardo Silva",
  "cpf": "12345678901",
  "email": "novo@email.com",
  "password": "novaSenha123"
}
```

O campo `password` é opcional na atualização.

### Resposta de usuário

As respostas de criação, consulta e atualização não devem conter o campo `password`.

## Login

### Endpoint

`POST /api/auth/login`

### Requisição

```json
{
  "cpf": "12345678901",
  "password": "senha123"
}
```

### Resposta de sucesso

HTTP `200 OK`:

```json
{
  "token": "<jwt>"
}
```

## Regras de autenticação

- O CPF será usado para localizar o usuário.
- Somente usuários ativos, com `deleted_at IS NULL`, poderão fazer login.
- A senha informada será comparada com o hash BCrypt armazenado.
- CPF inexistente e senha incorreta devem retornar a mesma resposta genérica:
  - HTTP `401 Unauthorized`;
  - mensagem: `Invalid CPF or password`.
- A API não deve informar se o CPF ou a senha foi o dado inválido.

## JWT

- A geração será feita usando JJWT.
- O token terá o ID do usuário como subject.
- O CPF poderá ser incluído como claim.
- O token terá data de emissão e expiração.
- A validade padrão será de 1 hora.
- O segredo e a validade serão configurados externamente:

```properties
app.jwt.secret=<base64-secret>
app.jwt.expiration=3600
```

- O segredo não deve ser fixado diretamente no código em ambientes reais.

## Camadas

Para a autenticação, criar ou manter:

- `AuthController`: endpoint de login.
- `UsersService`: cadastro, atualização de senha e validação das credenciais.
- `UsersRepository`: busca de usuário ativo por CPF.
- `JwtService`: geração dos tokens JWT.
- `SecurityConfig`: configuração do `PasswordEncoder` BCrypt.
- DTOs separados para requisição e resposta.

## Critérios de aceite

- A migration cria a coluna `password` corretamente.
- O cadastro salva um hash BCrypt, nunca a senha original.
- Login com CPF e senha corretos retorna HTTP `200` e o campo `token`.
- CPF inexistente retorna HTTP `401`.
- Senha incorreta retorna HTTP `401`.
- Usuário removido logicamente não consegue fazer login.
- Respostas do CRUD não contêm `password`.
- Atualização de senha gera um novo hash BCrypt.
- O JWT contém subject, emissão e expiração.
- O token expira após o período configurado.
- A aplicação inicia com `ddl-auto=validate` e o Flyway executa as migrations sem divergências.
