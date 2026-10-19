# Spec de Notificações — Amazon SNS

## Objetivo

Enviar uma notificação quando uma transação Pix for liquidada, usando o padrão
Transactional Outbox e o Amazon SNS. O SNS deve permitir que diferentes
consumidores recebam o mesmo evento, como e-mail, webhook ou uma fila SQS de
notificações.

## Fluxo

```text
Transação liquidada
        ↓
Outbox event PENDING
        ↓
OutboxPublisher
        ↓
Amazon SNS Topic
        ↓
Subscription confirmada
        ↓
E-mail, SQS ou outro consumidor
```

O evento deve ser persistido no banco junto com a transação. A aplicação não
deve depender do SNS para confirmar a transação HTTP.

## Configuração

A aplicação deve receber a região e o ARN do tópico por configuração:

```properties
aws.region=us-east-1
aws.sns.transaction-topic-arn=${AWS_SNS_TRANSACTION_TOPIC_ARN:}
```

O ARN deve seguir o formato:

```text
arn:aws:sns:<region>:<account-id>:<topic-name>
```

As credenciais devem ser obtidas pelo AWS SDK Default Credentials Provider
Chain. Nenhuma chave de acesso deve ser armazenada no código ou no repositório.

## Tipo de tópico

O tópico de notificações deve ser `Standard`, pois a notificação de e-mail não
precisa de ordenação estrita. A ordenação e a deduplicação dos eventos de
processamento da transação continuam sendo responsabilidade do fluxo SQS FIFO.

Nome sugerido:

```text
banking-transactions-notifications
```

## Publicação

O `SnsService` deve:

- receber o `SnsClient` por injeção de dependência;
- validar se o ARN do tópico está configurado;
- serializar o evento como JSON;
- incluir `eventId`, `eventType`, `transactionId`, `idempotencyKey`, `createdAt`
  e `payload`;
- publicar no tópico configurado;
- propagar a falha quando o SNS não confirmar a publicação.

O `OutboxEventsService` deve publicar no SNS antes de marcar o evento como
`PUBLISHED`. Se a publicação falhar, o evento deve permanecer disponível para
retry conforme a política da Outbox.

## Assinatura por e-mail

Para testes e demonstrações:

1. Criar uma subscription no tópico SNS.
2. Selecionar o protocolo `Email`.
3. Informar o endereço do destinatário.
4. Confirmar a subscription pelo link enviado pela AWS.
5. Verificar se o status está `Confirmed`.

Mensagens publicadas enquanto a subscription estiver pendente não devem ser
consideradas entregues ao destinatário.

## Idempotência e falhas

- O `eventId` deve ser usado para identificar o evento de forma única.
- O consumidor deve tolerar mensagens duplicadas.
- Uma falha antes da confirmação do SNS não deve marcar o evento como
  `PUBLISHED`.
- Uma falha depois da publicação e antes da atualização do banco pode causar
  duplicidade; os consumidores devem ser idempotentes.
- Para consumidores assíncronos, recomenda-se uma subscription SNS para uma
  fila SQS com Dead Letter Queue.

## Segurança

- Restringir a política IAM ao tópico e às operações necessárias, principalmente
  `sns:Publish`.
- Não registrar tokens, credenciais ou dados sensíveis nos logs.
- Evitar incluir informações bancárias desnecessárias no payload da notificação.
- Usar IAM Roles em ambientes AWS e credenciais locais somente pelo provider
  chain padrão.

## Critérios de aceite

- O projeto possui dependência do AWS SDK SNS.
- Existe um bean configurado de `SnsClient` usando `aws.region`.
- O `SnsService` publica eventos no ARN configurado.
- O evento só é marcado como `PUBLISHED` após a publicação bem-sucedida.
- Uma subscription de e-mail confirmada recebe uma nova notificação.
- O projeto não contém chaves AWS, senhas ou tokens commitados.
- Falhas de publicação deixam o evento disponível para nova tentativa.
