# Projeto Final Web3 — CP303061X

Microsserviços **User Service** e **Email Service** (Etapa 1), conforme o enunciado *Etapa 1 – Base de Segurança e Estrutura dos Microsserviços*.

Baseado no projeto de referência `web3_projeto_final_CP3031187` (Spring Security + JWT e estrutura RabbitMQ/Mail).

## Estrutura

```
Projeto_Final_web3_CP303061X/
├── ms_user/          # porta 8081 — JWT, roles, MySQL
├── ms_email/         # porta 8082 — estrutura base (sem consumer RabbitMQ)
├── sql/
│   └── init-databases.sql
└── README.md
```

## Pré-requisitos

- Java 17+ (projeto configurado com Java 21)
- Maven 3.9+
- MySQL em execução
- Postman ou similar (opcional)

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

No `ms_email`, ajuste também `spring.mail.username` e `spring.mail.password` quando for usar envio de e-mail (Etapa 2+).

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

## 4. Testes (User Service)

**Criar usuário** — `POST http://localhost:8081/users`

```json
{
  "email": "cliente@email.com",
  "password": "123456",
  "role": "ROLE_CUSTOMER"
}
```

**Login** — `POST http://localhost:8081/users/login`

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