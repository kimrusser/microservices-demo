# Event-Driven Microservices with Spring Boot & Kafka

A complete microservices architecture demonstrating event-driven communication using Spring Boot 3.3, Apache Kafka, and PostgreSQL.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Technologies](#technologies)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Event Flow](#event-flow)
- [Testing](#testing)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)
- [Project Structure](#project-structure)

---

## 🎯 Overview

This project implements a complete e-commerce order processing system using microservices architecture with event-driven communication via Apache Kafka. It demonstrates the **Saga Pattern** for distributed transactions across three independent services.

### Services

1. **Order Service** (Port 8081) - Manages orders and orchestrates the order lifecycle
2. **Payment Service** (Port 8082) - Processes payments and handles payment logic
3. **Inventory Service** (Port 8083) - Manages product inventory and reservations

---

## 🏗️ Architecture

```
┌─────────────────┐
│   Order Service │ (8081)
│   PostgreSQL    │ (5432)
└────────┬────────┘
         │
         ├─── REST ──────► Inventory Service (Reserve Stock)
         │
         ├─── Kafka ─────► order-created event
         │                        │
         │                        ▼
         │              ┌─────────────────┐
         │              │ Payment Service │ (8082)
         │              │   PostgreSQL    │ (5433)
         │              └────────┬────────┘
         │                       │
         │                       │ Kafka: payment-processed event
         │                       │
         ▼                       ▼
┌─────────────────────────────────────────┐
│        Inventory Service (8083)         │
│           PostgreSQL (5434)             │
└────────────────┬────────────────────────┘
                 │
                 │ Kafka: inventory-updated event
                 │
                 ▼
         ┌──────────────┐
         │Order Service │ (Status: COMPLETED)
         └──────────────┘
```

### Event Flow

```
1. POST /api/orders
   └─► Order Service creates order (PENDING)
       └─► REST call to Inventory Service (reserve stock)
           └─► Publishes order-created event to Kafka
               └─► Payment Service processes payment
                   └─► Publishes payment-processed event
                       └─► Order Service updates (PAYMENT_COMPLETED)
                       └─► Inventory Service confirms reservation
                           └─► Publishes inventory-updated event
                               └─► Order Service updates (COMPLETED) ✅
```

---

## 🛠️ Technologies

### Core Technologies

- **Java 21** - Latest LTS version with Records, Pattern Matching, Streams
- **Spring Boot 3.3.6** - Application framework
- **Spring Data JPA** - Database access with Hibernate
- **Apache Kafka 7.5.0** - Event streaming platform
- **PostgreSQL 15** - Relational database (3 separate instances)
- **Docker Compose** - Container orchestration

### Java 21 Features Used

- **Records** - Immutable DTOs and Events
- **Pattern Matching for Switch** - Cleaner control flow
- **Stream API** - Functional data processing
- **var** - Type inference for cleaner code

### Spring Ecosystem

- **Spring Web** - REST API
- **Spring Kafka** - Kafka integration
- **Spring Validation** - Request validation
- **Lombok** - Boilerplate reduction
- **Spring DevTools** - Development productivity

---

## ✨ Features

### Functional Features

- ✅ Order creation and management
- ✅ Automated payment processing
- ✅ Inventory reservation and management
- ✅ Event-driven saga pattern for distributed transactions
- ✅ Order status tracking through lifecycle
- ✅ Payment success/failure handling
- ✅ Inventory confirmation/release based on payment result

### Technical Features

- ✅ **Microservices Architecture** - Independent, scalable services
- ✅ **Event-Driven Communication** - Asynchronous messaging via Kafka
- ✅ **Saga Pattern** - Distributed transaction management
- ✅ **Database Per Service** - Data isolation
- ✅ **Optimistic Locking** - Concurrency control (@Version)
- ✅ **Pessimistic Locking** - Race condition prevention
- ✅ **Idempotency** - Safe retry handling
- ✅ **Transaction Management** - ACID properties with @Transactional
- ✅ **Exception Handling** - Clean error responses
- ✅ **Bean Validation** - Request validation

---

## 📦 Prerequisites

### Required Software

- **Java 21** - [Download](https://adoptium.net/)
- **Maven 3.8+** - [Download](https://maven.apache.org/download.cgi)
- **Docker Desktop** - [Download](https://www.docker.com/products/docker-desktop)
- **Postman** (optional) - [Download](https://www.postman.com/downloads/)

### System Requirements

- **RAM:** Minimum 8GB (16GB recommended)
- **CPU:** 2+ cores
- **Disk:** 5GB free space
- **OS:** Windows 10/11, macOS 10.15+, Linux

---

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone <your-repo-url>
cd microservices-demo
```

### 2. Start Infrastructure (Docker)

```bash
# Start all Docker containers
docker-compose up -d

# Wait 60 seconds for Kafka to fully start
sleep 60

# Verify all containers are running
docker ps
```

**Expected containers:**
- `zookeeper` (port 2181)
- `kafka` (port 9092)
- `kafka-ui` (port 8090)
- `postgres-order` (port 5432)
- `postgres-payment` (port 5433)
- `postgres-inventory` (port 5434)

### 3. Create Kafka Topics

```bash
# Create order-created topic
docker exec -it kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic order-created \
  --partitions 3 \
  --replication-factor 1

# Create payment-processed topic
docker exec -it kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic payment-processed \
  --partitions 3 \
  --replication-factor 1

# Create inventory-updated topic
docker exec -it kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic inventory-updated \
  --partitions 3 \
  --replication-factor 1

# Verify topics were created
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

### 4. Start Microservices

**Terminal 1: Order Service**
```bash
cd order-service
./mvnw spring-boot:run
```

**Terminal 2: Payment Service**
```bash
cd payment-service
./mvnw spring-boot:run
```

**Terminal 3: Inventory Service**
```bash
cd inventory-service
./mvnw spring-boot:run
```

**Wait for all services to show:** `Started [Service]Application in X seconds`

### 5. Initialize Inventory Data

```bash
# Add Gaming Laptop
curl -X POST http://localhost:8083/api/inventory/items \
  -H "Content-Type: application/json" \
  -d '{"productId":"prod-001","productName":"Gaming Laptop","initialQuantity":10}'

# Add Gaming Mouse
curl -X POST http://localhost:8083/api/inventory/items \
  -H "Content-Type: application/json" \
  -d '{"productId":"prod-002","productName":"Gaming Mouse","initialQuantity":50}'

# Add Mechanical Keyboard
curl -X POST http://localhost:8083/api/inventory/items \
  -H "Content-Type: application/json" \
  -d '{"productId":"prod-003","productName":"Mechanical Keyboard","initialQuantity":25}'
```

### 6. Test the System

**Create an order:**

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust-001",
    "items": [
      {
        "productId": "prod-001",
        "productName": "Gaming Laptop",
        "quantity": 2,
        "unitPrice": 999.99
      }
    ]
  }'
```

**Wait 5 seconds, then check order status:**

```bash
curl http://localhost:8081/api/orders/{order-id}
```

**Expected:** `"status": "COMPLETED"`

---

## 📚 API Documentation

### Order Service (Port 8081)

#### Create Order
```http
POST /api/orders
Content-Type: application/json

{
  "customerId": "cust-001",
  "items": [
    {
      "productId": "prod-001",
      "productName": "Gaming Laptop",
      "quantity": 2,
      "unitPrice": 999.99
    }
  ]
}
```

**Response:** `201 Created`
```json
{
  "id": "uuid",
  "customerId": "cust-001",
  "status": "PENDING",
  "totalAmount": 1999.98,
  "items": [...],
  "createdAt": "2026-04-03T...",
  "updatedAt": null
}
```

#### Get Order by ID
```http
GET /api/orders/{orderId}
```

**Response:** `200 OK`

#### Get All Orders
```http
GET /api/orders
```

#### Get Orders by Customer
```http
GET /api/orders/customer/{customerId}
```

#### Cancel Order
```http
PATCH /api/orders/{orderId}/cancel
```

**Response:** `200 OK`

---

### Payment Service (Port 8082)

#### Get Payment by Order ID
```http
GET /api/payments/order/{orderId}
```

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "orderId": "order-uuid",
  "customerId": "cust-001",
  "amount": 1999.98,
  "status": "COMPLETED",
  "paymentMethod": "AUTO",
  "transactionId": "TXN-XXXXXXXX",
  "failureReason": null,
  "createdAt": "2026-04-03T...",
  "processedAt": "2026-04-03T..."
}
```

#### Get Payment by ID
```http
GET /api/payments/{paymentId}
```

#### Get Payments by Customer
```http
GET /api/payments/customer/{customerId}
```

#### Get All Payments
```http
GET /api/payments
```

---

### Inventory Service (Port 8083)

#### Add Inventory Item
```http
POST /api/inventory/items
Content-Type: application/json

{
  "productId": "prod-001",
  "productName": "Gaming Laptop",
  "initialQuantity": 10
}
```

**Response:** `201 Created`

#### Reserve Stock (Manual)
```http
POST /api/inventory/reserve
Content-Type: application/json

{
  "productId": "prod-001",
  "quantity": 2,
  "orderId": "order-uuid"
}
```

#### Add Stock to Existing Item
```http
PATCH /api/inventory/items/{productId}/add-stock?quantity=5
```

#### Get Inventory Item
```http
GET /api/inventory/items/{productId}
```

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "productId": "prod-001",
  "productName": "Gaming Laptop",
  "availableQuantity": 8,
  "reservedQuantity": 0,
  "createdAt": "2026-04-03T...",
  "updatedAt": "2026-04-03T..."
}
```

#### Get All Inventory Items
```http
GET /api/inventory/items
```

---

## 🔄 Event Flow

### Order Status Lifecycle

```
PENDING
   │
   ├─► (Payment Processing)
   │
   ├─► PAYMENT_COMPLETED ───► (Payment Success)
   │   or
   └─► PAYMENT_FAILED ───────► (Payment Failure, amount > $10,000)
       │
       └─► COMPLETED ────────► (Inventory Confirmed)
           or
       └─► INVENTORY_FAILED ─► (Insufficient Stock)
```

### Kafka Events

#### order-created
```json
{
  "orderId": "uuid",
  "customerId": "cust-001",
  "totalAmount": 1999.98,
  "items": [
    {
      "productId": "prod-001",
      "productName": "Gaming Laptop",
      "quantity": 2,
      "unitPrice": 999.99
    }
  ],
  "createdAt": "2026-04-03T..."
}
```

#### payment-processed
```json
{
  "orderId": "uuid",
  "paymentId": "payment-uuid",
  "success": true,
  "message": "Payment processed successfully",
  "processedAt": "2026-04-03T..."
}
```

#### inventory-updated
```json
{
  "orderId": "uuid",
  "success": true,
  "message": "Inventory confirmed successfully",
  "updatedAt": "2026-04-03T..."
}
```

---

## 🧪 Testing

### Manual Testing with Postman

1. Import the Postman collection (coming soon)
2. Run requests in order:
   - Add inventory items
   - Create order
   - Check order status
   - Verify inventory decreased
   - Check payment record

### Test Scenarios

#### ✅ Success Scenario
- Order amount: ≤ $10,000
- Inventory: Available
- **Expected:** Order status = `COMPLETED`

#### ❌ Payment Failure Scenario
- Order amount: > $10,000
- **Expected:** Order status = `PAYMENT_FAILED`
- Inventory unchanged

#### ❌ Insufficient Stock Scenario
- Order quantity > available stock
- **Expected:** Order creation fails with 404

---

## 📊 Monitoring

### Kafka UI
- **URL:** http://localhost:8090
- **Features:** View topics, messages, consumer groups

### Adminer (Database UI)
- **URL:** http://localhost:8080
- **Databases:**
  - Order: `postgres-order:5432` (orderuser/orderpass/orderdb)
  - Payment: `postgres-payment:5433` (paymentuser/paymentpass/paymentdb)
  - Inventory: `postgres-inventory:5434` (inventoryuser/inventorypass/inventorydb)

### Service Logs
```bash
# Order Service logs
cd order-service
./mvnw spring-boot:run

# Payment Service logs
cd payment-service
./mvnw spring-boot:run

# Inventory Service logs
cd inventory-service
./mvnw spring-boot:run
```

---

## 🔧 Troubleshooting

### Kafka Not Starting

**Error:** `Timed out waiting for connection to Zookeeper`

**Solution:**
```bash
cd microservices-demo
docker-compose stop zookeeper kafka
docker-compose rm -f zookeeper kafka
docker-compose up -d zookeeper
sleep 30
docker-compose up -d kafka
sleep 60
```

### Topics Not Created

**Error:** `Topic order-created not present in metadata`

**Solution:**
```bash
docker exec -it kafka kafka-topics --create --bootstrap-server localhost:9092 --topic order-created --partitions 3 --replication-factor 1
docker exec -it kafka kafka-topics --create --bootstrap-server localhost:9092 --topic payment-processed --partitions 3 --replication-factor 1
docker exec -it kafka kafka-topics --create --bootstrap-server localhost:9092 --topic inventory-updated --partitions 3 --replication-factor 1
```

### Service Won't Start

**Error:** `Port 8081 already in use`

**Solution:**
```bash
# Find process using the port
lsof -i :8081  # Mac/Linux
netstat -ano | findstr :8081  # Windows

# Kill the process
kill -9 <PID>
```

### Database Connection Issues

**Error:** `Connection to localhost:5432 refused`

**Solution:**
```bash
docker ps  # Check if postgres containers are running
docker-compose up -d postgres-order postgres-payment postgres-inventory
```

### Order Status Stuck at PENDING

**Possible causes:**
1. Kafka topics not created
2. Event deserialization error
3. Service not consuming events

**Debug:**
1. Check Kafka UI for messages
2. Check service logs for errors
3. Verify all services are running

---

## 📁 Project Structure

```
microservices-demo/
├── docker-compose.yml
├── README.md
│
├── order-service/
│   ├── src/main/java/com/demo/
│   │   ├── OrderServiceApplication.java
│   │   ├── config/
│   │   │   ├── JacksonConfig.java
│   │   │   ├── KafkaConsumerConfig.java
│   │   │   ├── KafkaProducerConfig.java
│   │   │   ├── KafkaTopicConfig.java
│   │   │   └── RestTemplateConfig.java
│   │   ├── controller/
│   │   │   └── OrderController.java
│   │   ├── dto/
│   │   │   ├── CreateOrderRequest.java
│   │   │   ├── OrderItemRequest.java
│   │   │   ├── OrderResponse.java
│   │   │   └── OrderItemResponse.java
│   │   ├── entity/
│   │   │   ├── Order.java
│   │   │   ├── OrderItem.java
│   │   │   └── OrderStatus.java
│   │   ├── event/
│   │   │   ├── OrderCreatedEvent.java
│   │   │   ├── OrderItemEvent.java
│   │   │   ├── PaymentProcessedEvent.java
│   │   │   └── InventoryUpdatedEvent.java
│   │   ├── kafka/
│   │   │   ├── OrderEventProducer.java
│   │   │   └── OrderEventConsumer.java
│   │   ├── repository/
│   │   │   └── OrderRepository.java
│   │   └── service/
│   │       └── OrderService.java
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
│
├── payment-service/
│   ├── src/main/java/com/demo/
│   │   ├── PaymentServiceApplication.java
│   │   ├── config/
│   │   │   ├── JacksonConfig.java
│   │   │   ├── KafkaConsumerConfig.java
│   │   │   └── KafkaProducerConfig.java
│   │   ├── controller/
│   │   │   └── PaymentController.java
│   │   ├── dto/
│   │   │   ├── ProcessPaymentRequest.java
│   │   │   └── PaymentResponse.java
│   │   ├── entity/
│   │   │   ├── Payment.java
│   │   │   └── PaymentStatus.java
│   │   ├── event/
│   │   │   ├── OrderCreatedEvent.java
│   │   │   ├── OrderItemEvent.java
│   │   │   └── PaymentProcessedEvent.java
│   │   ├── kafka/
│   │   │   ├── PaymentEventProducer.java
│   │   │   └── PaymentEventConsumer.java
│   │   ├── repository/
│   │   │   └── PaymentRepository.java
│   │   └── service/
│   │       └── PaymentService.java
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
│
└── inventory-service/
    ├── src/main/java/com/demo/
    │   ├── InventoryServiceApplication.java
    │   ├── config/
    │   │   ├── JacksonConfig.java
    │   │   ├── KafkaConsumerConfig.java
    │   │   └── KafkaProducerConfig.java
    │   ├── controller/
    │   │   └── InventoryController.java
    │   ├── dto/
    │   │   ├── CreateInventoryItemRequest.java
    │   │   ├── InventoryItemResponse.java
    │   │   └── ReserveStockRequest.java
    │   ├── entity/
    │   │   ├── InventoryItem.java
    │   │   ├── Reservation.java
    │   │   └── ReservationStatus.java
    │   ├── event/
    │   │   ├── PaymentProcessedEvent.java
    │   │   └── InventoryUpdatedEvent.java
    │   ├── kafka/
    │   │   ├── InventoryEventProducer.java
    │   │   └── InventoryEventConsumer.java
    │   ├── repository/
    │   │   ├── InventoryItemRepository.java
    │   │   └── ReservationRepository.java
    │   └── service/
    │       └── InventoryService.java
    ├── src/main/resources/
    │   └── application.yml
    └── pom.xml
```

---

## 🎓 Learning Resources

### Spring Boot
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)

### Apache Kafka
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring for Apache Kafka](https://docs.spring.io/spring-kafka/reference/html/)

### Microservices Patterns
- [Microservices.io](https://microservices.io/)
- [Saga Pattern](https://microservices.io/patterns/data/saga.html)

### Java 21
- [Java 21 Features](https://openjdk.org/projects/jdk/21/)
- [Records](https://docs.oracle.com/en/java/javase/21/language/records.html)

---


## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 🙏 Acknowledgments

- Spring Boot Team for the excellent framework
- Apache Kafka for the robust event streaming platform
- Confluent for Kafka Docker images
- PostgreSQL community

