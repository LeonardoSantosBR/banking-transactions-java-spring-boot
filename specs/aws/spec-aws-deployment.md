# Spec Especial — Deploy simples na AWS Free Tier

Este documento descreve uma primeira integração do projeto com a AWS, priorizando simplicidade, baixo custo e valor para portfólio.

O objetivo inicial não é construir uma arquitetura completa de produção. É publicar a API Spring Boot em uma máquina virtual, mantendo o PostgreSQL no mesmo ambiente Docker e usando o Amazon SQS para mensageria. Serviços como RDS, NAT Gateway e Load Balancer ficam para uma evolução futura.

## 1. Objetivos

- Publicar a API Spring Boot em uma instância EC2.
- Executar a aplicação usando Docker.
- Manter PostgreSQL no `docker-compose.yml`.
- Usar Amazon SQS para transportar eventos assíncronos.
- Acessar a API por um endereço público da EC2.
- Demonstrar configuração de ambiente, deploy e operação básica na AWS.
- Evitar recursos que aumentem desnecessariamente o consumo da conta.

## 2. Arquitetura inicial

```text
Cliente
   |
   v
EC2 elegível para Free Tier
   |
   +-- Docker Compose
         +-- API Spring Boot
         +-- PostgreSQL
         +-- PostgreSQL
   |
   +--------------------> Amazon SQS
```

### Componentes

| Componente | Uso nesta fase |
|---|---|
| Amazon EC2 | Executar os containers da aplicação |
| Docker Compose | Orquestrar API e PostgreSQL |
| Amazon SQS | Transportar eventos assíncronos da Outbox |
| Security Group | Liberar somente as portas necessárias |
| IAM | Controlar o acesso administrativo à instância |
| CloudWatch básico | Acompanhar métricas da instância |

O código da aplicação deve continuar compatível com o ambiente local. A principal diferença será a execução do mesmo `docker-compose.yml` em uma EC2.

## 3. Escopo da primeira versão

### Incluído

- Instância EC2 pequena e elegível para o Free Tier da conta.
- Sistema operacional Linux.
- Docker e Docker Compose instalados na EC2.
- API Spring Boot executando em container.
- PostgreSQL executando em container.
- RabbitMQ pode continuar disponível apenas no ambiente local, se necessário durante o desenvolvimento.
- Uma fila SQS principal e uma Dead Letter Queue para o ambiente AWS.
- Variáveis de ambiente configuradas fora do código-fonte.
- Porta HTTP da API liberada no Security Group.
- Logs consultáveis pelo Docker e pelo sistema operacional.

### Não incluído inicialmente

- Amazon RDS.
- Amazon ECS ou EKS.
- NAT Gateway.
- Application Load Balancer.
- Auto Scaling.
- Alta disponibilidade.
- Domínio próprio e HTTPS.
- Pipeline de CI/CD.

Esses itens podem ser adicionados depois, quando o funcionamento básico estiver validado e houver necessidade real de evolução.

## 4. Execução na EC2

O deploy deve seguir este fluxo:

1. Criar uma instância EC2 elegível para o Free Tier.
2. Criar um Security Group permitindo SSH apenas do IP do desenvolvedor.
3. Liberar a porta HTTP utilizada pela API.
4. Instalar Docker e Docker Compose.
5. Criar uma fila SQS e uma Dead Letter Queue.
6. Configurar as permissões IAM da aplicação para enviar e consumir mensagens.
7. Clonar o projeto na instância ou copiar os arquivos necessários.
8. Configurar as variáveis de ambiente.
9. Executar `docker compose up -d`.
10. Validar a API pelo IP público da EC2.
11. Consultar os logs com `docker compose logs`.

## 5. Mensageria com Amazon SQS

O SQS será o primeiro serviço gerenciado utilizado pelo projeto. Ele será usado somente para eventos assíncronos; o PostgreSQL continuará no container da EC2 nesta fase.

### Filas

- `banking-transactions-events`: fila principal.
- `banking-transactions-events-dlq`: fila de mensagens que falharam após o limite de tentativas.

### Regras

- A tabela `outbox_events` continua sendo a origem confiável dos eventos.
- Um publicador deve enviar eventos `PENDING` para a fila.
- O evento só deve ser marcado como `PUBLISHED` depois da confirmação do envio.
- O consumidor deve confirmar a mensagem somente após o processamento concluído.
- O processamento deve ser idempotente para suportar reentrega da mensagem.
- A aplicação não deve usar credenciais AWS fixas no código.

O RabbitMQ pode continuar sendo usado localmente para desenvolvimento, mas não será executado na EC2 nesta primeira versão.

## 6. Configuração e segredos

Nenhuma senha ou segredo real deve ser versionado.

As configurações devem ser fornecidas por um arquivo `.env` criado diretamente na EC2 ou por variáveis de ambiente do sistema. O arquivo `.env` deve estar no `.gitignore`.

Exemplos de configurações:

```text
POSTGRES_DB=banking_transactions
POSTGRES_USER=postgres
POSTGRES_PASSWORD=<valor-local-da-ec2>
JWT_SECRET=<segredo-gerado-para-estudo>
AWS_REGION=<regiao-da-ec2>
AWS_SQS_TRANSACTION_QUEUE=<nome-ou-url-da-fila>
```

O `.env.example` pode ser versionado contendo apenas nomes de variáveis e valores fictícios.

## 7. Segurança mínima

- Usar uma chave SSH protegida para acessar a EC2.
- Permitir SSH somente a partir do IP do desenvolvedor.
- Não liberar publicamente as portas do PostgreSQL e do RabbitMQ.
- Não usar a conta root para executar a aplicação.
- Não adicionar chaves AWS ao repositório.
- Alterar a senha padrão do PostgreSQL antes do deploy.
- Permitir na IAM Role apenas o envio e o consumo da fila SQS utilizada.
- Remover a instância quando o experimento terminar.

## 8. Critérios de aceite

- A instância EC2 foi criada dentro da modalidade disponível no Free Tier.
- A API inicia com Docker Compose na EC2.
- O PostgreSQL executa e persiste dados por meio de um volume Docker.
- A aplicação consegue enviar e consumir uma mensagem da fila SQS.
- Uma mensagem com falha pode ser encaminhada para a DLQ.
- As migrations do Flyway são executadas com sucesso.
- O login e o cadastro de usuários funcionam pelo endpoint público.
- As rotas de contas funcionam com JWT.
- As portas do PostgreSQL e do RabbitMQ não estão abertas para a internet.
- O arquivo `.env` não está versionado.
- A aplicação pode ser parada e iniciada novamente sem perder os dados do volume.
- O README contém as instruções de deploy e desligamento da instância.

## 9. Evoluções futuras

Depois que o deploy simples estiver funcionando, as evoluções podem ser feitas gradualmente:

1. Usar Amazon ECR para armazenar a imagem Docker.
2. Migrar o PostgreSQL para Amazon RDS.
3. Armazenar configurações no Parameter Store.
4. Enviar logs para o CloudWatch.
5. Configurar HTTPS e domínio próprio.
6. Criar deploy automatizado com GitHub Actions.
7. Adicionar infraestrutura como código.

## 10. Controle de custos

Antes de criar qualquer recurso, verificar no console da AWS se ele está elegível para a modalidade Free Tier da conta.

Durante o desenvolvimento:

- configurar um alerta de orçamento;
- usar apenas uma instância EC2;
- evitar Elastic IP sem associação com uma instância;
- não criar NAT Gateway;
- não criar RDS ou Load Balancer nesta primeira fase;
- monitorar o limite mensal de requisições do SQS;
- parar ou terminar a instância quando ela não estiver sendo usada;
- verificar regularmente o painel de Billing;
- remover volumes e snapshots que não sejam mais necessários.

O objetivo desta fase é provar o deploy da aplicação na nuvem com o menor número possível de recursos pagos ou de cobrança potencial.
