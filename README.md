# Projeto Final Web3 — CP303061X

Microsserviços **User Service**, **Email Service** e **Frontend Node.js** (Etapas 1–3).

Baseado no projeto de referência `microsservicos_CP303061X` (RabbitMQ) e `web3_projeto_final_CP3031187` (Spring Security + JWT).

## Estrutura

```
Projeto_Final_web3_CP303061X/
├── ms_user/          # porta 8081 — JWT, roles, request/verify code
├── ms_email/         # porta 8082 — consumer RabbitMQ, envio Gmail, persistência
├── frontend/         # porta 3000 — telas de e-mail e código
├── sql/
│   └── init-databases.sql
└── README.md
```

## Pré-requisitos

- Java 17+ (projeto configurado com Java 21)
- Maven 3.9+
- Node.js 18+
- MySQL em execução
- RabbitMQ (CloudAMQP ou local)
- Conta Gmail com senha de aplicativo (para envio real de e-mail)
- Postman ou navegador (para testes)

## 1. Bancos MySQL

```sql
CREATE DATABASE ms_user;
CREATE DATABASE ms_email;
```

Ou execute: `mysql -u root -p < sql/init-databases.sql`

## 2. Configuração

Em cada `application.properties`, substitua `SUA_SENHA` pela senha do MySQL:

- `ms_user/src/main/resources/application.properties`
- `ms_email/src/main/resources/application.properties`

No `ms_email`, configure também:
- `spring.rabbitmq.addresses` (credenciais do RabbitMQ)
- `spring.mail.username` e `spring.mail.password` (senha de aplicativo do Gmail)

## 3. Executar os serviços

**User Service (8081):**

```bash
cd ms_user
./mvnw spring-boot:run
```

**Email Service (8082):**

```bash
cd ms_email
./mvnw spring-boot:run
```

**Frontend (3000):**

```bash
cd frontend
npm install
npm start
```

## 4. Testes (User Service)

**Criar usuário** — `POST http://localhost:8081/users`

```json
{
  "email": "cliente@email.com",
  "password": "123456",
  "role": "ROLE_CUSTOMER"
}
```

**Login** — `POST http://localhost:8081/auth/login`

```json
{
  "email": "cliente@email.com",
  "password": "123456"
}
```

Resposta: `{ "token": "..." }`

**Rota protegida por role** — `GET http://localhost:8081/users/test/customer`

Header: `Authorization: Bearer <token>`

- Sem token ou token inválido: erro / 403 conforme configuração
- Com token válido e role `ROLE_CUSTOMER`: `200`

**Perfil** — `GET http://localhost:8081/users/me` (com Bearer token)

## 5. Testes (Etapa 3 — fluxo integrado)

### Solicitar código por e-mail

`POST http://localhost:8081/auth/request-code`

```json
{
  "email": "seuemail@gmail.com"
}
```

O User Service gera um código de 6 dígitos, salva em cache (5 min) e publica mensagem na fila RabbitMQ.

### Verificar código

`POST http://localhost:8081/auth/verify-code`

```json
{
  "email": "seuemail@gmail.com",
  "code": "123456"
}
```

Resposta de sucesso: `{ "token": "..." }`  
Resposta de erro: `{ "message": "Código inválido ou expirado" }`

### Frontend completo

1. Suba MySQL, User Service, Email Service e Frontend.
2. Acesse `http://localhost:3000`.
3. Informe um e-mail real e clique em **Enviar código**.
4. Verifique a caixa de entrada do Gmail — o e-mail deve conter o código de 6 dígitos.
5. Na tela de verificação, digite o código.
6. Se válido, o token JWT é salvo em `sessionStorage` e você é redirecionado para `/dashboard`.
7. Código inválido ou expirado exibe mensagem de erro.

### Verificar persistência do e-mail

Consulte a tabela `emails` no banco `ms_email` — registros devem aparecer com status `SENT` ou `ERROR`.