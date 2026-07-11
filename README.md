# BlueTransfer - Offline Digital Payment System Using Bluetooth Mesh

A Spring Boot backend that demonstrates **mesh-routed deferred digital payments** over a simulated Bluetooth Mesh network.

Imagine you're in a basement with zero connectivity. You send your friend ₹500. Your phone encrypts the payment, broadcasts it to nearby devices, and the packet hops from device to device until one eventually regains internet connectivity. That device acts as a bridge node and securely uploads the encrypted packet to the backend, where it is decrypted, deduplicated, validated, and settled.

This repository contains the backend services responsible for encryption, packet processing, idempotent settlement, and transaction management, along with a software simulator that models the Bluetooth Mesh network so the complete workflow can be demonstrated on a single machine without requiring physical Bluetooth hardware.

---

## Table of Contents

1. [Key Engineering Highlights](#key-engineering-highlights)
2. [Transaction Processing Pipeline](#transaction-processing-pipeline)
3. [Architecture](#architecture)
4. [Quick Start](#quick-start)
5. [Engineering Challenges](#engineering-challenges)
6. [File-by-file Walkthrough](#file-by-file-walkthrough)
7. [API Reference](#api-reference)
8. [Tests](#tests)
9. [Production Considerations](#production-considerations)
10. [Current Limitations](#current-limitations)
---

## Key Engineering Highlights

BlueTransfer demonstrates several core concepts in distributed systems, secure transaction processing, and backend engineering:

- **Secure Offline Transactions** — Payment packets are encrypted using hybrid RSA-OAEP and AES-256-GCM encryption before entering the mesh network, ensuring intermediary devices cannot read or modify transaction data.

- **Reliable Multi-Hop Routing** — Transactions propagate across simulated Bluetooth Mesh devices until they reach an internet-enabled bridge node capable of synchronizing with the backend.

- **Idempotent Settlement** — Duplicate packet deliveries are detected using SHA-256 packet hashes and processed exactly once through an atomic idempotency mechanism.

- **Replay Attack Protection** — Every payment contains a timestamp and unique nonce, allowing expired or replayed packets to be rejected before settlement.

- **Concurrent Transaction Processing** — Thread-safe processing and transactional settlement ensure consistency even when multiple bridge nodes submit identical packets simultaneously.

---

## Transaction Processing Pipeline

Every payment follows the workflow below before being successfully settled.

```text
Sender Device
      │
      ▼
Create Payment Instruction
      │
      ▼
Encrypt using RSA + AES-GCM
      │
      ▼
Broadcast through Bluetooth Mesh
      │
      ▼
Bridge Node Receives Packet
      │
      ▼
Upload to Spring Boot Backend
      │
      ▼
SHA-256 Hash Generation
      │
      ▼
Idempotency Verification
      │
      ▼
Decrypt & Validate
      │
      ▼
Transaction Settlement
      │
      ▼
Ledger Update
```

### Payment Creation

A payment request is converted into a secure transaction packet containing sender information, recipient details, transaction amount, timestamp, and a unique nonce before being encrypted using hybrid cryptography.


### Mesh Propagation

Encrypted packets propagate through nearby virtual Bluetooth Mesh devices using multi-hop forwarding until they reach an internet-enabled bridge node.


### Backend Processing

Once uploaded to the backend, every packet passes through several validation stages:

- SHA-256 packet hashing
- Duplicate detection through idempotency verification
- Hybrid decryption
- Timestamp freshness validation
- Transaction settlement
- Ledger persistence


### Transaction Settlement

Validated payments are processed atomically within a database transaction, ensuring account balances and transaction records remain consistent even under concurrent requests.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         SENDER PHONE (offline)                          │
│  PaymentInstruction { sender, receiver, amount, pinHash, nonce, time }  │
│              │                                                          │
│              ▼ encrypt with server's RSA public key                     │
│   MeshPacket { packetId, ttl, createdAt, ciphertext }                   │
└──────────────────────────────────────┬──────────────────────────────────┘
                                       │ Bluetooth gossip
                                       ▼
        ┌─────────┐  hop   ┌─────────┐  hop   ┌─────────┐
        │stranger1│ ─────▶ │stranger2│ ─────▶ │ bridge  │ ◀── walks outside
        └─────────┘        └─────────┘        └────┬────┘     gets 4G
                                                   │
                                                   ▼ HTTPS POST
┌─────────────────────────────────────────────────────────────────────────┐
│                     SPRING BOOT BACKEND (this project)                  │
│                                                                         │
│  /api/bridge/ingest                                                     │
│       │                                                                 │
│       ▼                                                                 │
│  [1] hash ciphertext (SHA-256)                                          │
│       │                                                                 │
│       ▼                                                                 │
│  [2] IdempotencyService.claim(hash)  ◀── atomic putIfAbsent (≈ Redis    │
│       │                                  SETNX). Duplicates rejected    │
│       │                                  here, before any work.         │
│       ▼                                                                 │
│  [3] HybridCryptoService.decrypt(ciphertext)                            │
│       │       (RSA-OAEP unwraps AES key, AES-GCM decrypts payload       │
│       │        AND verifies the auth tag — tampering = exception)       │
│       ▼                                                                 │
│  [4] Freshness check: signedAt within last 24h                          │
│       │                                                                 │
│       ▼                                                                 │
│  [5] SettlementService.settle()                                         │
│       @Transactional: debit sender, credit receiver, write ledger       │
│       @Version on Account = optimistic locking (defense in depth)       │
└─────────────────────────────────────────────────────────────────────────┘
```

---
## Quick Start

### Prerequisites

- Java 17+
- Git
- Maven *(or Maven Wrapper)*

### Installation

```bash
git clone https://github.com/saoirseros/BlueTransfer.git
cd BlueTransfer
```

### Start the Application

```bash
./mvnw spring-boot:run
```

> **Windows:** Use `mvnw.cmd spring-boot:run` instead.

### Open

```
http://localhost:8080
```

### Run Tests

```bash
./mvnw test
```

> **Windows:** Use `mvnw.cmd test`.
---

## Engineering Challenges

### Secure Communication Across Untrusted Devices

Payment packets may traverse multiple intermediary devices before reaching the backend. Since these devices cannot be trusted, transaction data must remain confidential and tamper-proof throughout its journey.

**Solution**

BlueTransfer uses hybrid encryption combining **RSA-OAEP** and **AES-256-GCM**.

- AES-256-GCM encrypts the transaction payload efficiently while providing authenticated encryption.
- The AES session key is encrypted using the backend's RSA public key.
- Only the backend possesses the corresponding private key, ensuring intermediary devices cannot decrypt or modify transaction data.

This approach provides confidentiality, integrity, and protection against packet tampering while remaining suitable for larger payloads than RSA alone.


### Duplicate Transaction Prevention

In a mesh network, the same payment packet may reach the backend through multiple bridge nodes simultaneously. Without proper safeguards, duplicate submissions could result in multiple settlements for a single transaction.

**Solution**

Every encrypted payment is assigned a unique SHA-256 hash before processing.

The backend uses an atomic idempotency mechanism to claim each packet before settlement.

- The first request successfully claims the transaction.
- Subsequent duplicate submissions are immediately rejected.
- Database-level uniqueness constraints provide an additional layer of protection against accidental duplicate settlements.

This guarantees that every payment is processed exactly once, even under concurrent submissions.


### Replay Attack Protection

Captured network packets should never be reusable by malicious actors.

**Solution**

Each encrypted payment includes:

- A cryptographically secure unique nonce.
- A signed timestamp indicating when the payment was created.

During processing, the backend verifies packet freshness and rejects expired transactions. Since both the timestamp and nonce are protected by authenticated encryption, they cannot be modified without invalidating the encrypted payload.

This prevents replay attacks while allowing legitimate repeated payments to be processed independently.

---

## File-by-file walkthrough

```
upi-offline-mesh/
├── pom.xml                                  Maven build, Spring Boot 3.3, Java 17
├── mvnw, mvnw.cmd                           Maven wrapper (no install needed)
├── README.md                                this file
└── src/main/
    ├── resources/
    │   ├── application.properties           H2 in-memory DB, port 8080, TTLs
    │   └── templates/dashboard.html         The interactive demo UI
    └── java/com/demo/upimesh/
        ├── UpiMeshApplication.java          Spring Boot main class
        │
        ├── model/                           ── Domain layer
        │   ├── Account.java                 JPA entity. @Version = optimistic lock
        │   ├── AccountRepository.java       Spring Data JPA
        │   ├── Transaction.java             Settled-tx ledger. unique idx on packetHash
        │   ├── TransactionRepository.java   Spring Data JPA
        │   ├── MeshPacket.java              Wire format. Outer fields readable, ciphertext opaque
        │   └── PaymentInstruction.java      Decrypted payload (sender/receiver/amount/nonce/time)
        │
        ├── crypto/                          ── Cryptography layer
        │   ├── ServerKeyHolder.java         Generates RSA-2048 keypair on startup
        │   └── HybridCryptoService.java     RSA-OAEP + AES-256-GCM encrypt/decrypt + ciphertext hash
        │
        ├── service/                         ── Business logic
        │   ├── DemoService.java             Seeds accounts, simulates a sender phone
        │   ├── VirtualDevice.java           One simulated phone in the mesh
        │   ├── MeshSimulatorService.java    Gossip protocol across virtual devices
        │   ├── IdempotencyService.java      ConcurrentHashMap = JVM-local Redis SETNX
        │   ├── SettlementService.java       @Transactional debit + credit + ledger insert
        │   └── BridgeIngestionService.java  THE pipeline: hash → claim → decrypt → freshness → settle
        │
        ├── controller/                      ── HTTP layer
        │   ├── ApiController.java           All REST endpoints
        │   └── DashboardController.java     Serves the dashboard HTML at /
        │
        └── config/
            └── AppConfig.java               @EnableScheduling for cache eviction

src/test/java/com/demo/upimesh/
└── IdempotencyConcurrencyTest.java          The 3-bridges-at-once test + tamper test
```

---

## API reference

| Method | Path | What it does |
|---|---|---|
| GET | `/` | Dashboard HTML |
| GET | `/api/server-key` | Server's RSA public key (base64) |
| GET | `/api/accounts` | All accounts and balances |
| GET | `/api/transactions` | Last 20 transactions |
| GET | `/api/mesh/state` | Current state of every virtual device |
| POST | `/api/demo/send` | Simulate sender phone — encrypt + inject packet |
| POST | `/api/mesh/gossip` | Run one round of gossip across the mesh |
| POST | `/api/mesh/flush` | Bridges with internet upload to backend (parallel) |
| POST | `/api/mesh/reset` | Clear mesh + idempotency cache |
| POST | `/api/bridge/ingest` | **The production endpoint.** Real bridges POST here |
| GET | `/h2-console` | Browse the in-memory database |

H2 console login: JDBC URL `jdbc:h2:mem:upimesh`, username `sa`, no password.

### Request format for `/api/bridge/ingest`

```http
POST /api/bridge/ingest
Content-Type: application/json
X-Bridge-Node-Id: phone-bridge-42
X-Hop-Count: 3

{
  "packetId": "550e8400-e29b-41d4-a716-446655440000",
  "ttl": 2,
  "createdAt": 1730000000000,
  "ciphertext": "base64-encoded-RSA-and-AES-blob"
}
```

Response:
```json
{
  "outcome": "SETTLED",                     // or "DUPLICATE_DROPPED" or "INVALID"
  "packetHash": "a3f8c9...",
  "reason": null,                            // populated on INVALID
  "transactionId": 42                        // populated on SETTLED
}
```

---

## Tests

Run all tests:
```
mvnw.cmd test
```

The three included tests:

- **`encryptDecryptRoundTrip`** — sanity-check that hybrid encryption is symmetric.
- **`tamperedCiphertextIsRejected`** — flip a byte in the ciphertext, verify that `BridgeIngestionService` returns `INVALID` instead of crashing or settling.
- **`singlePacketDeliveredByThreeBridgesSettlesExactlyOnce`** — the headline test. Three threads, one packet, simultaneous delivery. Asserts exactly one `SETTLED`, two `DUPLICATE_DROPPED`, and that the sender's balance changed by exactly the amount once.

---

## Production Considerations

BlueTransfer is designed as a proof of concept to demonstrate secure offline transaction processing over a simulated Bluetooth Mesh network. A production deployment would replace several components with enterprise-grade infrastructure.

| Current Implementation | Production Alternative |
| :--------------------- | :--------------------- |
| H2 Database | PostgreSQL / MySQL |
| ConcurrentHashMap Idempotency | Redis |
| Simulated Bluetooth Mesh | Bluetooth Low Energy (BLE) Mesh |
| Locally Generated RSA Keys | Hardware Security Module (AWS KMS / HashiCorp Vault) |
| Local Spring Boot Backend | Distributed backend services |
| Simulated Accounts | Bank-integrated KYC accounts |
| Console Logging | Centralized logging and monitoring |
---

## Current Limitations

BlueTransfer is designed as a proof-of-concept to explore secure offline payment processing over a simulated Bluetooth Mesh network. While the core transaction pipeline demonstrates the underlying concepts, several challenges remain before such a system could support real-world deployment.

- **Offline Balance Verification** — Without internet connectivity, recipients cannot verify a sender's account balance before accepting a payment. Final settlement occurs only after the transaction reaches the backend.

- **Offline Double Spending** — Since account balances cannot be synchronized in real time, a malicious user could initiate multiple offline transactions before backend settlement. Preventing this requires pre-funded wallets or trusted secure hardware.

- **Simulated Mesh Network** — The project models Bluetooth Mesh communication in software rather than using real Bluetooth Low Energy (BLE) devices. Real-world deployment would introduce additional challenges around device discovery, connectivity, and power management.

- **Privacy & Infrastructure Considerations** — While transaction contents remain encrypted throughout transmission, metadata such as packet forwarding and device participation would require additional privacy protections and regulatory considerations in a production environment.
