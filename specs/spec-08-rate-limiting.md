# Spec-08 — Rate limiting de login com Bucket4j e Redis

## Objetivo

Reduzir tentativas automatizadas de descobrir senhas limitando chamadas a `POST /api/auth/login` e `POST /api/users`, endpoints públicos de autenticação e cadastro. Bucket4j aplica o algoritmo token bucket e usa Redis para compartilhar contadores entre instâncias da API.

Redis é um serviço de armazenamento rápido em memória. Nesta funcionalidade, guarda somente o estado dos limites (tokens disponíveis e horário de reposição); não autentica usuários, não guarda senhas e não substitui o PostgreSQL, que continua sendo a fonte dos dados bancários e de usuários.

## Fluxo

1. Um filtro HTTP identifica requisições `POST /api/auth/login` e `POST /api/users`, lê o CPF do corpo JSON e preserva o corpo para o controller.
2. O serviço consome um token do bucket do IP e outro do CPF. Login e cadastro usam buckets separados.
3. A chave do CPF é normalizada e protegida com HMAC antes de ser usada no Redis; o CPF não deve aparecer como chave ou em logs.
4. Se ambos os limites permitirem a tentativa, a cadeia HTTP continua até a autenticação normal.
5. Se um limite for excedido, a requisição termina com HTTP `429 Too Many Requests` e uma mensagem genérica.

O filtro limita o corpo das requisições abrangidas a 8 KB. Corpos maiores recebem HTTP `413 Payload Too Large`.

## Limites iniciais

| Identificador | Capacidade | Reposição | Chave Redis |
|---|---:|---|---|
| IP de origem | 30 tentativas | gradual, em 15 minutos | endereço IP do cliente |
| CPF no login | 5 tentativas | gradual, em 15 minutos | HMAC do CPF normalizado, escopo `login` |
| IP no cadastro | 10 tentativas | gradual, em 15 minutos | endereço IP, escopo `registration` |
| CPF no cadastro | 3 tentativas | gradual, em 15 minutos | HMAC do CPF normalizado, escopo `registration` |

Cada chamada de login ou cadastro consome tokens, mesmo quando é bem-sucedida. Os limites são valores iniciais e podem ser ajustados conforme o tráfego legítimo observado. O IP usado é `remoteAddr`; cabeçalho `X-Forwarded-For` não deve ser aceito sem configuração explícita de proxy confiável.

## Redis e configuração

Para desenvolvimento local, o serviço `redis` do `docker-compose.yml` escuta em `127.0.0.1:6379`, com volume próprio. Iniciar com:

```powershell
docker compose up -d redis
```

A aplicação conecta usando as propriedades Spring Data Redis:

```properties
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.username=${REDIS_USERNAME:}
spring.data.redis.password=${REDIS_PASSWORD:}
```

Redis local sem autenticação pode deixar usuário e senha vazios. Redis hospedado pode exigir credenciais, que devem ser fornecidas pelo serviço e mantidas fora do Git.

A chave HMAC é configuração da aplicação, não uma conta Redis:

```properties
app.security.login-rate-limit-hmac-key=${LOGIN_RATE_LIMIT_HMAC_KEY}
```

Em produção, definir `LOGIN_RATE_LIMIT_HMAC_KEY` como segredo aleatório próprio, não reutilizar a senha do Redis ou o segredo JWT. Não registrar CPF, senha, token JWT ou credenciais Redis nos logs.

Bucket4j define expiração dos buckets Redis após o período de reposição. A configuração local do Redis habilita AOF; portanto, o estado operacional pode sobreviver a reinícios do container enquanto as chaves ainda forem válidas.

## Falha do Redis

O rate limiting é fail-closed: se a aplicação não conseguir consultar ou atualizar Redis, o login não deve prosseguir sem limite. Retornar HTTP `503 Service Unavailable` com mensagem genérica. Registrar a falha operacional sem registrar credenciais, CPF ou senha.

## Respostas

Limite excedido:

```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Too many login attempts. Try again later."
}
```

Redis indisponível:

```json
{
  "status": 503,
  "error": "Service Unavailable",
  "message": "Login is temporarily unavailable."
}
```

Essas respostas não devem indicar se o CPF existe.

## Critérios de aceite

- O rate limiting se aplica aos endpoints públicos `POST /api/auth/login` e `POST /api/users`.
- O limite por IP e o limite por CPF são compartilhados entre instâncias que usam o mesmo Redis.
- O CPF não é armazenado diretamente como chave Redis.
- Tentativas dentro dos limites seguem para o fluxo existente; login e cadastro mantêm suas respostas normais.
- Exceder qualquer limite retorna `429` e interrompe a cadeia HTTP.
- Redis indisponível retorna `503` e não permite login sem proteção.
- Requisições acima de 8 KB retornam `413`.
- Configurações e credenciais locais não são adicionadas ao controle de versão.