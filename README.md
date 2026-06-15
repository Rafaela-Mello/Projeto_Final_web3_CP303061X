# Projeto Final Web3 — CP303061X

Sistema de autenticação por código enviado por e-mail, com cadastro de perfil (nome/cargo), dashboard protegido e comunicação assíncrona entre microsserviços via RabbitMQ.

## Arquitetura

O projeto é composto por **três serviços** independentes:

| Serviço | Tecnologia | Porta | Responsabilidade |
|---------|------------|-------|------------------|
| **ms_user** | Spring Boot | 8081 | Usuários, JWT, roles, geração/validação de código OTP, producer RabbitMQ |
| **ms_email** | Spring Boot | 8082 | Consumer RabbitMQ, envio real de e-mail (Gmail), persistência de envios |
| **frontend** | Node.js + Express | 3000 | Interface web: e-mail, código, cadastro e dashboard |

```
┌─────────────┐     HTTP      ┌──────────────┐     RabbitMQ     ┌──────────────┐
│  Frontend   │ ────────────► │  ms_user     │ ───────────────► │  ms_email    │
│  (Node.js)  │   :8081       │  (Spring)    │  fila email    │  (Spring)    │
│  :3000      │               │  :8081       │                │  :8082       │
└─────────────┘               └──────┬───────┘                └──────┬───────┘
                                     │                                 │
                                     ▼                                 ▼
                              ┌─────────────┐                  ┌─────────────┐
                              │   MySQL     │                  │   MySQL     │
                              │  ms_user    │                  │  ms_email   │
                              └─────────────┘                  └─────────────┘
                                                                    │
                                                                    ▼
                                                             ┌─────────────┐
                                                             │ Gmail SMTP  │
                                                             └─────────────┘
```

### Fluxo completo

1. Usuário informa o e-mail no frontend → `POST /auth/request-code`
2. **ms_user** gera código de 6 dígitos, armazena em cache (5 min) e publica mensagem na fila RabbitMQ
3. **ms_email** consome a fila, envia e-mail real via Gmail e persiste registro no banco `ms_email`
4. Usuário digita o código → `POST /auth/verify-code` → recebe JWT
5. Usuário preenche nome e cargo → `POST /users/update-profile`
6. Dashboard permite testar endpoints protegidos e consultar perfil

### Estrutura do repositório

```
Projeto_Final_web3_CP303061X/
├── ms_user/              # User Service (porta 8081)
├── ms_email/             # Email Service (porta 8082)
├── frontend/             # Frontend Node.js (porta 3000)
├── sql/
│   └── init-databases.sql
├── docs/
│   └── screenshots/      # Capturas de tela do fluxo
├── start.sh              # Script de inicialização (Linux/WSL)
├── iniciar.ps1           # Script de inicialização (Windows)
├── .gitignore
└── README.md
```

---

## Pré-requisitos

Antes de executar o projeto, instale e configure:

| Requisito | Versão mínima | Observação |
|-----------|---------------|------------|
| **JDK** | 17+ (projeto usa Java 21) | Necessário para os microsserviços Spring Boot |
| **Maven** | 3.9+ | Incluso via `./mvnw` em cada serviço |
| **Node.js** | 18+ | Necessário para o frontend |
| **MySQL** | 8+ | Dois bancos: `ms_user` e `ms_email` |
| **CloudAMQP** | Conta gratuita | [cloudamqp.com](https://www.cloudamqp.com/) — fila RabbitMQ |
| **Gmail** | Conta com senha de app | [Senha de aplicativo Google](https://myaccount.google.com/apppasswords) |

Ferramentas úteis para testes: **Postman** ou navegador.

---

## Configuração

### 1. Bancos MySQL

Crie os dois bancos de dados:

```sql
CREATE DATABASE ms_user;
CREATE DATABASE ms_email;
```

Ou execute o script:

```bash
mysql -u root -p < sql/init-databases.sql
```

### 2. User Service — `ms_user/src/main/resources/application.properties`

| Propriedade | Descrição |
|-------------|-----------|
| `spring.datasource.url` | URL JDBC do banco `ms_user` |
| `spring.datasource.username` | Usuário MySQL (ex.: `root`) |
| `spring.datasource.password` | Senha do MySQL |
| `spring.rabbitmq.addresses` | URL AMQP do CloudAMQP |
| `broker.queue.email.name` | Nome da fila (ex.: `default.email`) |

Exemplo:

```properties
spring.datasource.password=SUA_SENHA_MYSQL
spring.rabbitmq.addresses=amqps://usuario:senha@beaver.rmq.cloudamqp.com/vhost
broker.queue.email.name=default.email
```

### 3. Email Service — `ms_email/src/main/resources/application.properties`

| Propriedade | Descrição |
|-------------|-----------|
| `spring.datasource.url` | URL JDBC do banco `ms_email` |
| `spring.datasource.username` | Usuário MySQL |
| `spring.datasource.password` | Senha do MySQL |
| `spring.rabbitmq.addresses` | URL AMQP do CloudAMQP (mesma fila do ms_user) |
| `spring.mail.username` | E-mail Gmail remetente |
| `spring.mail.password` | Senha de aplicativo do Gmail |
| `broker.queue.email.name` | Nome da fila (mesmo valor do ms_user) |

Exemplo:

```properties
spring.datasource.password=SUA_SENHA_MYSQL
spring.rabbitmq.addresses=amqps://usuario:senha@beaver.rmq.cloudamqp.com/vhost
spring.mail.username=seuemail@gmail.com
spring.mail.password=SUA_SENHA_DE_APLICATIVO_GMAIL
broker.queue.email.name=default.email
```

### 4. Frontend — variável de ambiente (opcional)

Por padrão o frontend aponta para `http://127.0.0.1:8081`. Para alterar:

**Linux / WSL:**
```bash
export USER_SERVICE_URL=http://127.0.0.1:8081
```

**Windows (PowerShell):**
```powershell
$env:USER_SERVICE_URL = "http://127.0.0.1:8081"
```

---

## Executar os serviços

### Opção 1 — Script automático (recomendado)

Abre um terminal separado para cada serviço.

**Linux / WSL:**
```bash
chmod +x start.sh
./start.sh
```

**Windows (PowerShell):**
```powershell
.\iniciar.ps1
```

### Opção 2 — Manual (um terminal por serviço)

**Terminal 1 — User Service (8081):**
```bash
cd ms_user
./mvnw spring-boot:run
```

**Terminal 2 — Email Service (8082):**
```bash
cd ms_email
./mvnw spring-boot:run
```

**Terminal 3 — Frontend (3000):**
```bash
cd frontend
npm install
npm start
```

Acesse: **http://localhost:3000**

---

## Testes — fluxo integrado (Frontend)

1. Suba MySQL, **ms_user**, **ms_email** e **frontend**
2. Acesse `http://localhost:3000`
3. Informe um e-mail real e clique em **Enviar código**
4. Verifique a caixa de entrada do Gmail — o e-mail deve conter o código de 6 dígitos
5. Digite o código na tela de verificação
6. Se válido, você é redirecionado para `/register`
7. Preencha **nome** e escolha o **cargo**
8. Após salvar, você vai para o `/dashboard`
9. No dashboard:
   - **Testar endpoint protegido** → `GET /api/protected`
   - **Meu perfil** → `GET /api/me`
   - **Sair** → limpa sessão e volta ao início

> Para o botão "Testar endpoint protegido" retornar sucesso, escolha o cargo **Cliente** (`ROLE_CUSTOMER`).

### Verificar persistência do e-mail

Consulte a tabela `emails` no banco `ms_email` — registros com status `SENT` ou `ERROR`.

---

## Testes — API (Postman)

**Solicitar código** — `POST http://localhost:8081/auth/request-code`
```json
{ "email": "seuemail@gmail.com" }
```

**Verificar código** — `POST http://localhost:8081/auth/verify-code`
```json
{ "email": "seuemail@gmail.com", "code": "123456" }
```

**Atualizar perfil** — `POST http://localhost:8081/users/update-profile`
```
Authorization: Bearer <token>
```
```json
{ "name": "Maria Silva", "role": "ROLE_CUSTOMER" }
```

**Perfil** — `GET http://localhost:8081/users/me` (com Bearer token)

**Endpoint protegido** — `GET http://localhost:8081/users/test/customer` (com Bearer token e role CUSTOMER)

---

## Capturas de tela

Salve as imagens em `docs/screenshots/` e referencie abaixo:

| # | Etapa | Arquivo |
|---|-------|---------|
| 1 | Tela inicial (e-mail) | `docs/screenshots/01-tela-email.png` |
| 2 | Tela de verificação de código | `docs/screenshots/02-verificacao-codigo.png` |
| 3 | Tela de cadastro (nome/cargo) | `docs/screenshots/03-cadastro-perfil.png` |
| 4 | Dashboard com perfil e endpoint protegido | `docs/screenshots/04-dashboard.png` |
| 5 | E-mail recebido com o código | `docs/screenshots/05-email-codigo.png` |

<!-- Descomente após adicionar as imagens:
![Tela inicial](docs/screenshots/01-tela-email.png)
![Verificação de código](docs/screenshots/02-verificacao-codigo.png)
![Cadastro de perfil](docs/screenshots/03-cadastro-perfil.png)
![Dashboard](docs/screenshots/04-dashboard.png)
![E-mail com código](docs/screenshots/05-email-codigo.png)
-->

---

## Endpoints principais

| Serviço | Método | Rota | Descrição |
|---------|--------|------|-----------|
| ms_user | POST | `/auth/request-code` | Solicita código por e-mail |
| ms_user | POST | `/auth/verify-code` | Valida código e retorna JWT |
| ms_user | POST | `/users/update-profile` | Atualiza nome e cargo (auth) |
| ms_user | GET | `/users/me` | Retorna perfil do usuário (auth) |
| ms_user | GET | `/users/test/customer` | Endpoint protegido (CUSTOMER) |
| frontend | GET | `/api/protected` | Proxy para endpoint protegido |
| frontend | GET | `/api/me` | Proxy para perfil do usuário |

---

## Entrega

Para a entrega da Semana 4, crie a tag:

```bash
git tag entrega4
git push origin entrega4
```
