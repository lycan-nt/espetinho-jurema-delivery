# Delivere Açaí – Sistema de comandas

Sistema de venda de açaí com **comandas** por cliente, mesa ou comanda.  
Lance o peso (kg) e o preço por kilo; o valor total é calculado automaticamente.

- **Backend:** Java 17 + Spring Boot 3 + JPA (H2 em memória)
- **Frontend:** Angular 18 + SCSS

## Pré-requisitos

- **Backend:** JDK 17+, Maven 3.8+
- **Frontend:** Node.js 18+, npm

## Como rodar

### 1. Backend (porta 8080)

Não é necessário ter Maven instalado: use o **Maven Wrapper** (já incluso no projeto):

```bash
cd backend
./mvnw spring-boot:run
```

Se preferir usar Maven instalado na máquina:

```bash
cd backend
mvn spring-boot:run
```

- API: `http://localhost:8080/api/comandas`
- H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:acaidb`, user: `sa`, senha em branco)

### 2. Frontend (porta 4200)

```bash
cd frontend
npm install
npm start
```

Acesse: **http://localhost:4200**

## Funcionalidades

- **Nova comanda:** tipo (Cliente / Mesa / Comanda), identificador, peso (kg), preço por kilo → cálculo automático do total e gravação.
- **Lista de comandas:** ver todas ou só abertas, fechar comanda.

## Estrutura

```
delivere-acai/
├── backend/          # Spring Boot
│   └── src/main/java/br/com/delivere/acai/
│       ├── AcaiApplication.java
│       ├── comanda/   # Comanda, TipoComanda, Repository, Service, Controller
│       └── config/    # CORS
├── frontend/         # Angular 18
│   └── src/app/
│       ├── pages/comanda/         # Tela nova comanda
│       ├── pages/lista-comandas/  # Tela listagem
│       ├── services/comanda.service.ts
│       └── models/comanda.model.ts
└── README.md
```

## API (resumo)

| Método | URL | Descrição |
|--------|-----|-----------|
| POST | `/api/comandas` | Criar comanda (body: tipo, identificador, pesoKg, precoPorKilo) |
| GET | `/api/comandas` | Listar (query `?abertas=true` opcional) |
| GET | `/api/comandas/{id}` | Buscar por ID |
| PUT | `/api/comandas/{id}` | Atualizar peso/preço |
| PATCH | `/api/comandas/{id}/fechar` | Fechar comanda |
