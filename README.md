# 🔵 BlueTransfer

## Offline Digital Payment System Using Bluetooth Mesh Networking

BlueTransfer is a backend-focused distributed systems project that explores how secure digital payments could still be routed when internet connectivity is unavailable.

I built this project to understand the engineering challenges behind offline payments, secure communication, distributed systems, cryptography, and transaction processing. Rather than building another CRUD application, I wanted to design a system that simulates how encrypted payment packets could travel through nearby Bluetooth-enabled devices until one device regains internet connectivity and forwards the payment to the backend for settlement.

The project simulates the complete payment lifecycle—from creating an encrypted payment packet, routing it through a virtual Bluetooth mesh network, preventing duplicate settlements, and finally updating account balances inside the backend.

Although this is a proof-of-concept and not a production-ready payment system, the project implements several real engineering concepts used in modern financial systems, including hybrid encryption, idempotency, optimistic locking, replay protection, and layered backend architecture.

---

# 🚀 Motivation

Traditional digital payment systems depend entirely on internet connectivity.

But what happens if two users are underground, inside a tunnel, in a disaster zone, or somewhere with no network coverage?

BlueTransfer explores one possible approach:

- The sender creates an encrypted payment.
- Nearby devices relay the encrypted packet over Bluetooth.
- Eventually one device regains internet access.
- That device becomes a bridge node and uploads the packet to the backend.
- The backend securely decrypts, validates, and settles the transaction.

The goal of this project is not to replace existing payment systems, but to demonstrate the backend architecture and distributed systems concepts that such a system could require.

---

# ✨ Key Features

- Offline payment simulation using Bluetooth Mesh concepts
- Hybrid encryption using RSA-2048 and AES-GCM
- SHA-256 packet hashing
- Idempotent transaction settlement
- Replay attack prevention
- Optimistic locking with JPA
- Multi-hop packet routing simulation
- Bridge node architecture
- Spring Boot REST APIs
- Interactive web dashboard
- H2 in-memory database

---

# 🏗 System Architecture

```
          Sender Device
                 │
                 ▼
     PaymentInstruction
                 │
      Hybrid Encryption
      (RSA + AES-GCM)
                 │
                 ▼
          MeshPacket
                 │
        Bluetooth Mesh
(Device → Device → Device)
                 │
                 ▼
          Bridge Node
       (Internet Available)
                 │
                 ▼
      Spring Boot Backend
                 │
        Security Validation
                 │
                 ▼
      Settlement Service
                 │
                 ▼
          H2 Database
```

---

# 🔄 Payment Flow

## Step 1 — Payment Creation

The sender creates a payment containing:

- Sender VPA
- Receiver VPA
- Amount
- PIN Hash
- Timestamp
- Unique Nonce

The nonce guarantees that every payment is unique, even if the sender transfers the same amount multiple times.

---

## Step 2 — Hybrid Encryption

The payment instruction is encrypted before it ever enters the mesh network.

The project uses:

- RSA-2048 for key exchange
- AES-GCM for payload encryption
- SHA-256 for packet hashing

Intermediate devices never have access to the payment contents.

---

## Step 3 — Mesh Routing

Instead of requiring internet connectivity, encrypted packets travel between nearby devices.

Every relay device simply forwards ciphertext.

It cannot:

- Read the payment
- Modify the payment
- Change the amount
- Change the receiver

Eventually a bridge node gains internet connectivity and uploads the packet.

---

## Step 4 — Backend Validation

Once the backend receives the packet it performs multiple security checks:

- Compute SHA-256 packet hash
- Check idempotency
- Decrypt payload
- Verify timestamp freshness
- Validate payment
- Update account balances
- Record transaction

Only after all validations succeed does settlement occur.

---

# 🔐 Security Design

## Hybrid Encryption

BlueTransfer uses a hybrid encryption approach.

RSA is responsible for securely exchanging the AES session key.

AES-GCM encrypts the payment payload while also providing authentication to detect tampering.

This combination provides both security and performance.

---

## Idempotency

Duplicate packets are one of the biggest challenges in distributed systems.

If multiple bridge nodes upload the same payment simultaneously, the transaction must only settle once.

BlueTransfer solves this by:

- Computing SHA-256(ciphertext)
- Using the packet hash as the idempotency key
- Rejecting duplicate hashes before settlement

This guarantees that duplicate deliveries do not produce duplicate transactions.

---

## Replay Protection

Every payment contains:

- Timestamp
- Unique UUID Nonce

The backend rejects:

- Expired packets
- Previously processed packets

This prevents replay attacks.

---

## Optimistic Locking

Account balances use JPA's `@Version` annotation.

This protects against concurrent balance modifications and prevents lost updates when multiple transactions attempt to modify the same account simultaneously.

---

# 🧱 Project Structure

```text
src
└── main
    ├── java
    │   └── com.demo.upimesh
    │       ├── config
    │       ├── controller
    │       ├── crypto
    │       ├── model
    │       ├── service
    │       └── BluetransferApplication.java
    │
    └── resources
        ├── templates
        └── application.properties
```

---

# 💻 Technology Stack

## Backend

- Java 21
- Spring Boot
- Spring MVC
- Spring Data JPA
- Maven

## Database

- H2 Database

## Frontend

- HTML
- CSS
- JavaScript
- Thymeleaf

## Security

- RSA-2048
- AES-GCM
- SHA-256

---

# 🎯 Engineering Concepts Demonstrated

This project demonstrates practical understanding of:

- Distributed Systems
- Backend System Design
- REST API Development
- Cryptography
- Secure Communication
- Hybrid Encryption
- Concurrency
- Idempotency
- Optimistic Locking
- Transaction Processing
- Spring Boot Architecture

---

# ▶ Running the Project

Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/BlueTransfer.git
```

Navigate into the project

```bash
cd BlueTransfer
```

Run the application

```bash
./mvnw spring-boot:run
```

or on Windows

```bash
mvnw.cmd spring-boot:run
```

Open your browser

```
http://localhost:8080
```

---

# 🚧 Current Limitations

This project is an educational proof of concept.

Some production components are intentionally simplified.

Examples include:

- Simulated Bluetooth mesh instead of actual BLE communication
- H2 instead of PostgreSQL
- Local in-memory idempotency instead of Redis
- Server-side packet generation instead of Android devices
- Simplified authentication model

These choices allow the project to focus on backend architecture and distributed systems concepts.

---

# 📚 Learning Outcome

Building BlueTransfer helped me gain practical experience with topics that are difficult to understand through theory alone.

Some of the biggest takeaways include:

- Designing layered Spring Boot applications
- Applying cryptography in real workflows
- Understanding distributed system challenges
- Handling duplicate requests safely
- Managing concurrent database updates
- Building RESTful APIs
- Structuring backend services
- Thinking about system design from a production perspective

---

# 📄 License

This project was developed for educational and portfolio purposes to explore backend engineering, distributed systems, and secure payment architectures.
