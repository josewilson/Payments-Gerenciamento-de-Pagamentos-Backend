<h1 align="center">Payments API</h1>

<p align="center">
  API REST desenvolvida em Java com Spring Boot para gerenciamento de pagamentos, com persistência em PostgreSQL,
  auditoria em MongoDB, integração com API externa de cotação e documentação com Swagger/OpenAPI.
</p>

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java%2021-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
  <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white">
  <img alt="MongoDB" src="https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white">
  <img alt="Maven" src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white">
  <img alt="Swagger" src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black">
  <img alt="JUnit 5" src="https://img.shields.io/badge/JUnit%205-25A162?style=for-the-badge&logo=junit5&logoColor=white">
  <img alt="Postman" src="https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white">
</p>

---

## Objetivo do projeto

Este projeto foi desenvolvido com foco em prática de back-end e composição de portfólio, simulando uma aplicação real de mercado para gerenciamento de pagamentos.

A API permite:

- criar pagamentos
- listar pagamentos com paginação
- filtrar pagamentos por status
- buscar pagamento por id
- atualizar o status de um pagamento
- consultar auditoria das ações realizadas

---

## Tecnologias utilizadas

<p>
  <img alt="Java" src="https://img.shields.io/badge/Java%2021-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
  <img alt="Spring Web" src="https://img.shields.io/badge/Spring%20Web-6DB33F?style=for-the-badge&logo=spring&logoColor=white">
  <img alt="Spring Data JPA" src="https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white">
  <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white">
  <img alt="MongoDB" src="https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white">
  <img alt="Maven" src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white">
  <img alt="Lombok" src="https://img.shields.io/badge/Lombok-BC4521?style=for-the-badge&logo=lombok&logoColor=white">
  <img alt="Swagger" src="https://img.shields.io/badge/Swagger%20%2F%20OpenAPI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black">
  <img alt="JUnit 5" src="https://img.shields.io/badge/JUnit%205-25A162?style=for-the-badge&logo=junit5&logoColor=white">
  <img alt="Mockito" src="https://img.shields.io/badge/Mockito-25A162?style=for-the-badge&logo=java&logoColor=white">
  <img alt="Postman" src="https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white">
</p>

---

## Funcionalidades

- Cadastro de pagamentos
- Atualização de status do pagamento
- Paginação na listagem
- Filtro por status
- Registro de auditoria no MongoDB
- Integração com API externa de cotação
- Testes unitários e de controller
- Documentação automática da API com Swagger

---

## Estrutura do projeto

```text
src/main/java/com/josewilson/payments
├── client
├── config
├── controller
├── domain
├── dto
├── exception
├── mapper
├── repository
└── service
```

### Responsabilidade dos pacotes

- `controller`: endpoints da API
- `service`: regras de negócio
- `repository`: acesso ao banco de dados
- `domain`: entidades e enums
- `dto`: objetos de entrada e saída
- `mapper`: conversão entre entidade e DTO
- `exception`: tratamento global de erros
- `client`: integração com serviço externo
- `config`: configurações gerais do projeto

---

## Arquitetura resumida

A aplicação utiliza:

- PostgreSQL para armazenamento principal dos pagamentos
- MongoDB para armazenar auditorias das operações
- API externa para obter cotação em BRL
- Spring Boot para expor endpoints REST

Fluxo principal:

1. o cliente envia uma requisição para criar pagamento
2. a API valida os dados recebidos
3. o pagamento é salvo no PostgreSQL
4. a auditoria é registrada no MongoDB
5. a API consulta a cotação da moeda
6. a resposta é retornada ao cliente

---

## Como executar o projeto localmente

### Pré-requisitos

- Java 21
- Maven
- PostgreSQL
- MongoDB
- IntelliJ IDEA ou outra IDE Java

### Configuração do banco PostgreSQL

Crie um banco com o nome:

```sql
CREATE DATABASE paymentsdb;
```

### Configuração do application.yml

No arquivo `src/main/resources/application.yml`, configure conforme seu ambiente:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/paymentsdb
    username: postgres
    password: sua_senha
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  data:
    mongodb:
      uri: mongodb://localhost:27017/payments_audit_db

server:
  port: 8080
```

### Executando a aplicação

Pelo terminal:

```bash
mvn spring-boot:run
```

Ou execute a classe principal na IDE:

```java
PaymentsApplication
```

---

## Documentação da API

Após iniciar a aplicação, acesse:

### Swagger UI

```text
http://localhost:8080/swagger-ui/index.html
```

### OpenAPI JSON

```text
http://localhost:8080/v3/api-docs
```

---

## Endpoints principais

### Criar pagamento
**POST** `/api/payments`

### Listar pagamentos com paginação
**GET** `/api/payments?page=0&size=10`

### Filtrar pagamentos por status
**GET** `/api/payments?status=APPROVED&page=0&size=10`

### Buscar pagamento por id
**GET** `/api/payments/{id}`

### Atualizar status do pagamento
**PATCH** `/api/payments/{id}/status`

### Consultar auditoria do pagamento
**GET** `/api/payments/{id}/audit`

---

## Exemplo de criação de pagamento

### Requisição

```json
{
  "customerName": "Francisco Almeida",
  "customerEmail": "francisco@email.com",
  "amount": 199.90,
  "currency": "BRL",
  "externalReference": "PEDIDO-123"
}
```

### Resposta esperada

```json
{
  "id": 1,
  "customerName": "Francisco Almeida",
  "customerEmail": "francisco@email.com",
  "amount": 199.90,
  "currency": "BRL",
  "status": "PENDING",
  "externalReference": "PEDIDO-123",
  "createdAt": "2026-03-23T21:30:00",
  "updatedAt": "2026-03-23T21:30:00",
  "exchangeRateToBrl": 1.0
}
```

---

## Exemplo de atualização de status

### Requisição

```json
{
  "status": "APPROVED"
}
```

### Resposta esperada

```json
{
  "id": 1,
  "customerName": "Francisco Almeida",
  "customerEmail": "francisco@email.com",
  "amount": 199.90,
  "currency": "BRL",
  "status": "APPROVED",
  "externalReference": "PEDIDO-123",
  "createdAt": "2026-03-23T21:30:00",
  "updatedAt": "2026-03-23T21:35:00",
  "exchangeRateToBrl": 1.0
}
```

---

## Como testar no Postman

Crie uma collection chamada:

```text
Payments API
```

Sugestão de requisições:

- Create Payment
- List Payments
- Get Payment By Id
- Update Payment Status
- Filter Payments By Status
- Get Payment Audit

### Variáveis recomendadas

- `baseUrl` = `http://localhost:8080`
- `paymentId` = id retornado no cadastro

### Exemplo de uso de variável

```text
{{baseUrl}}/api/payments
```

```text
{{baseUrl}}/api/payments/{{paymentId}}
```

```text
{{baseUrl}}/api/payments/{{paymentId}}/status
```

### Script para salvar o id automaticamente no Postman

Na aba **Tests** da requisição `Create Payment`, use:

```javascript
const response = pm.response.json();
pm.collectionVariables.set("paymentId", response.id);
```

---

## Testes automatizados

O projeto possui testes para:

- camada de serviço
- camada de controller

Principais tecnologias de teste:

- JUnit 5
- Mockito
- Spring Boot Test

Para executar os testes:

```bash
mvn test
```

---

## Melhorias futuras

- autenticação e autorização com Spring Security
- Docker Compose para subir toda a stack
- uso de MapStruct para mapeamento
- tratamento mais robusto para falhas da API externa
- padronização de respostas com objeto de erro
- logs estruturados
- pipeline CI/CD

---

## Autor

<p align="center">
  Desenvolvido por <strong>José Wilson Alves de Souza</strong> para fins de estudo, prática em back-end Java e portfólio profissional.
</p>
