# 🛡️ Insurance Claim Processing (TPA) System

A production-grade Insurance Third Party Administrator (TPA) system built with **Spring Boot 3.4+ (JDK 25)** and **React**.

## 🚀 Key Features

- **Smart Claim Timeline**: Real-time visual audit trail of the claim lifecycle.
- **AI-Powered Validation**: Automated document extraction and risk assessment using LLMs.
- **Razorpay Integration**: Production-grade payment settlement for approved claims.
- **Role-Based Access**: Specialized dashboards for Customers, Carriers, and Admin.
- **Dockerized Infrastructure**: One-click deployment with Postgres, Redis, and Kafka.

## 🏗️ System Architecture

```mermaid
graph TD
    subgraph Frontend
        Web[React + Vite]
    end
    
    subgraph Backend
        API[Spring Boot API]
        Auth[JWT Security]
        SM[State Machine]
    end
    
    subgraph Infrastructure
        DB[(PostgreSQL)]
        Cache[(Redis)]
        Broker[Kafka]
    end
    
    subgraph External
        AI[AI Service]
        RP[Razorpay]
    end
    
    Web --> API
    API --> Auth
    API --> SM
    API --> DB
    API --> Cache
    API --> Broker
    API --> AI
    API --> RP
```

## 📊 Data Model (ERD)

```mermaid
erDiagram
    USER {
        Long id
        String username
        String role
    }
    CLAIM {
        Long id
        String policyNumber
        Double amount
        String status
    }
    CLAIM_AUDIT {
        Long id
        String status
        String changedBy
        LocalDateTime timestamp
    }
    PAYMENT {
        Long id
        String razorpayOrderId
        Double amount
        String status
    }
    
    USER ||--o{ CLAIM : "manages/submits"
    CLAIM ||--o{ CLAIM_AUDIT : "audit trail"
    CLAIM ||--o{ PAYMENT : "settlement"
```

## 🔄 Claim Lifecycle Workflow

```mermaid
stateDiagram-v2
    [*] --> SUBMITTED : Customer Uploads
    SUBMITTED --> AI_VALIDATED : Auto Extraction
    AI_VALIDATED --> UNDER_REVIEW : Carrier Audit
    UNDER_REVIEW --> APPROVED : Final Verdict
    UNDER_REVIEW --> REJECTED : Fraud/Ineligible
    APPROVED --> PAYMENT_PENDING : Trigger Payout
    PAYMENT_PENDING --> SETTLED : Razorpay Success
    SETTLED --> [*]
```

## 🛠️ Tech Stack

- **Backend**: Spring Boot 3.4.x, Spring Security, Spring Data JPA, Spring AI.
- **Frontend**: React 18, Tailwind CSS, Framer Motion (for Timeline).
- **Middleware**: Redis (Caching), Kafka (Events), PostgreSQL (Main DB).
- **Integrations**: Razorpay (Payments), PDFBox (Doc Processing).

## 📦 Deployment (Docker)

Ensure you have your `.env` file configured with the required keys.

```bash
docker-compose up -d --build
```

Access the system at:
- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080/swagger-ui.html`

## 🧪 Testing

The system includes 10+ new production-grade test cases covering edge cases.

```bash
mvn clean test
```
