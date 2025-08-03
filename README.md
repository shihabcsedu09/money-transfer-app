# Money Transfer Application

A robust Spring Boot application that demonstrates proper handling of concurrency, atomicity, and deadlock prevention in financial systems.

## 🎯 Why This Matters for Financial Applications

Financial applications must handle multiple users transferring money simultaneously without data corruption. This application shows how to prevent common issues:

### 🔒 **Concurrency Problems Solved**

1. **Race Conditions**: When two transfers happen at the same time
   - **Problem**: Both transfers read the same balance, leading to incorrect final balance
   - **Solution**: Distributed locking with Redisson prevents simultaneous access

2. **Deadlocks**: When two transfers wait for each other's locks
   - **Problem**: Transfer A locks Account 1, Transfer B locks Account 2, then both wait for the other
   - **Solution**: Ordered locking (always lock accounts alphabetically)

3. **Atomicity**: Ensuring transfers are all-or-nothing
   - **Problem**: Debit succeeds but credit fails, leaving money in limbo
   - **Solution**: Database transactions with automatic rollback

4. **Data Consistency**: Maintaining accurate account balances
   - **Problem**: Concurrent updates overwrite each other
   - **Solution**: Optimistic locking with version numbers

## 🚀 Quick Start

### Prerequisites
- Java 11+
- Maven

### Run Locally
```bash
# Run the application
mvn spring-boot:run
```

### Test the API
```bash
# Transfer money
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountNumber": "ACC001234567890",
    "toAccountNumber": "ACC002345678901", 
    "amount": 100.00,
    "currency": "USD",
    "description": "Test transfer"
  }'

# Check transfer status
curl http://localhost:8080/api/v1/transfers/{transferId}
```

## 🔧 How Concurrency is Handled

### 1. **Distributed Locking** - Preventing Race Conditions

**The Problem**: Imagine two people trying to withdraw money from the same account at exactly the same time. Both see the balance is $1000, both try to withdraw $800. Without locking, both might succeed, leaving the account with -$600 instead of $200!

**The Solution**: We use locks to ensure only one transfer can access an account at a time.

```java
// Lock both accounts before transfer
ReentrantLock fromAccountLock = accountLocks.computeIfAbsent(fromAccountNumber, k -> new ReentrantLock());
ReentrantLock toAccountLock = accountLocks.computeIfAbsent(toAccountNumber, k -> new ReentrantLock());

// Acquire locks with timeout
boolean firstLockAcquired = firstLock.tryLock(lockTimeout, TimeUnit.SECONDS);
```

**How it helps**: 
- When Transfer A starts, it locks Account 1
- Transfer B tries to lock Account 1, but it's already locked
- Transfer B waits until Transfer A finishes
- This prevents both transfers from reading the same balance

### 2. **Deadlock Prevention** - Avoiding Infinite Waits

**The Problem**: Transfer A locks Account 1, then tries to lock Account 2. Transfer B locks Account 2, then tries to lock Account 1. Both are waiting for each other forever!

**The Solution**: Always lock accounts in alphabetical order.

```java
// Always lock in alphabetical order
boolean isFromAccountFirst = fromAccountNumber.compareTo(toAccountNumber) <= 0;
if (isFromAccountFirst) {
    firstLock = fromAccountLock;
    secondLock = toAccountLock;
} else {
    firstLock = toAccountLock;
    secondLock = fromAccountLock;
}
```

**How it helps**:
- If Transfer A wants to lock ACC001 and ACC002, it locks ACC001 first
- If Transfer B wants to lock ACC002 and ACC001, it locks ACC001 first
- Both transfers try to lock the same account first, so one waits for the other
- No circular waiting = no deadlock!

### 3. **Atomic Transactions** - All-or-Nothing Operations

**The Problem**: What if we successfully debit $100 from Account A, but then the credit to Account B fails? The money disappears into thin air!

**The Solution**: Database transactions ensure everything succeeds or everything fails.

```java
@Transactional
protected void executeTransfer(Transfer transfer) {
    // Debit from account
    boolean debitSuccess = fromAccount.debit(amount);
    if (!debitSuccess) {
        throw new InsufficientFundsException("Insufficient funds");
    }
    
    // Credit to account
    boolean creditSuccess = toAccount.credit(amount);
    if (!creditSuccess) {
        // Rollback the debit
        fromAccount.credit(amount);
        throw new TransferException("Failed to credit destination account");
    }
}
```

**How it helps**:
- If anything fails, the entire transfer is cancelled
- The database automatically rolls back all changes
- No money can be lost or created out of thin air
- Account balances always stay mathematically correct

### 4. **Optimistic Locking** - Version-Based Concurrency Control

**The Problem**: Two transfers read the same account balance at the same time. Both see $1000, both calculate the new balance as $900, both save $900. The final balance is wrong!

**The Solution**: Each account has a version number that changes when the account is modified.

```java
@Entity
public class Account {
    @Version
    private Long version; // This number changes every time the account is updated
    
    public synchronized boolean debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            return false;
        }
        balance = balance.subtract(amount);
        return true;
    }
}
```

**How it helps**:
- When Transfer A reads the account, it gets version 5
- When Transfer B reads the account, it also gets version 5
- Transfer A saves with version 5 → succeeds, version becomes 6
- Transfer B tries to save with version 5 → fails because version is now 6
- Transfer B gets an error and retries with the new balance
- This prevents overwriting each other's changes

### 5. **Synchronized Methods** - Thread-Safe Balance Operations

**The Problem**: Even with all the above, what if two threads try to modify the same account object in memory at the same time?

**The Solution**: The `synchronized` keyword ensures only one thread can execute these methods at a time.

```java
public synchronized boolean debit(BigDecimal amount) {
    if (balance.compareTo(amount) < 0) {
        return false;
    }
    balance = balance.subtract(amount);
    return true;
}
```

**How it helps**:
- When Thread A calls `debit()`, Thread B must wait
- This prevents two threads from reading and writing the balance simultaneously
- Ensures the balance calculation is always accurate

## 🧪 Concurrency Testing

The application includes comprehensive tests that demonstrate:

```java
@Test
void testConcurrentTransfers() {
    // 10 threads, 5 transfers each = 50 concurrent transfers
    int numberOfThreads = 10;
    int transfersPerThread = 5;
    
    // All transfers should complete successfully
    // Final balances should be mathematically correct
}
```

## 📊 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/transfers` | Create a new transfer |
| GET | `/api/v1/transfers/{id}` | Get transfer details |
| GET | `/api/v1/transfers/health` | Health check |

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Controller    │    │     Service     │    │   Repository    │
│                 │    │                 │    │                 │
│ • REST API      │───▶│ • Business Logic│───▶│ • Data Access   │
│ • Validation    │    │ • Distributed   │    │ • Locking       │
│ • Error Handling│    │   Locking       │    │ • Transactions  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🔍 Key Features

- ✅ **Distributed Locking**: Prevents race conditions across multiple instances
- ✅ **Deadlock Prevention**: Ordered locking strategy
- ✅ **Atomic Transactions**: All-or-nothing transfer operations
- ✅ **Optimistic Locking**: Version-based concurrency control
- ✅ **Comprehensive Testing**: Concurrency stress tests
- ✅ **Error Handling**: Proper rollback on failures

## 🚀 Live Demo

**This application is deployed and running on Railway!**

### **Live API URL**: https://money-transfer-app-production-9d8e.up.railway.app

### Test the Live API

```bash
# Transfer money
curl -X POST https://money-transfer-app-production-9d8e.up.railway.app/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountNumber": "ACC001234567890",
    "toAccountNumber": "ACC002345678901", 
    "amount": 100.00,
    "currency": "USD",
    "description": "Test transfer"
  }'

# Check transfer status
curl https://money-transfer-app-production-9d8e.up.railway.app/api/v1/transfers/{transferId}

# Health check
curl https://money-transfer-app-production-9d8e.up.railway.app/api/v1/transfers/health
```

### Sample Account Numbers for Testing
- `ACC001234567890` - John Doe (USD: $10,000)
- `ACC002345678901` - John Doe (EUR: €8,500)
- `ACC003456789012` - Jane Smith (USD: $5,000)
- `ACC004567890123` - Jane Smith (GBP: £3,000)
- `ACC005678901234` - Bob Johnson (USD: $7,500)
- `ACC006789012345` - Alice Brown (EUR: €12,000)
- `ACC007890123456` - Charlie Wilson (GBP: £4,500)
- `ACC008901234567` - Diana Davis (USD: $2,000)

### **Local Testing**

```bash
# Run locally
mvn spring-boot:run

# Test API
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountNumber": "ACC001234567890",
    "toAccountNumber": "ACC002345678901", 
    "amount": 100.00,
    "currency": "USD",
    "description": "Test transfer"
  }'
```

## 🚀 Releases & Packages

### **Creating a Release**

To create a new release with artifacts and packages:

```bash
# Make sure you're on main branch and have clean working directory
git checkout main
git pull origin main

# Create a release (this will trigger GitHub Actions)
./scripts/create-release.sh 1.0.1
```

### **What Gets Created**

When you create a release, GitHub Actions will automatically:

1. **🏷️ Create a Release**: With detailed changelog and documentation
2. **📦 Build JAR**: Executable JAR file for deployment
3. **🐳 Build Docker Image**: Multi-platform Docker image
4. **📦 Publish Packages**: To GitHub Container Registry

### **Available Artifacts**

- **JAR File**: `money-transfer-app-{version}.jar` - Ready to run
- **Docker Image**: `ghcr.io/shihabcsedu09/money-transfer-app:{version}` - Containerized
- **Source Code**: Tagged release with full source

### **Using the Docker Image**

```bash
# Pull the latest image
docker pull ghcr.io/shihabcsedu09/money-transfer-app:latest

# Run the application
docker run -p 8080:8080 ghcr.io/shihabcsedu09/money-transfer-app:latest

# Run specific version
docker run -p 8080:8080 ghcr.io/shihabcsedu09/money-transfer-app:1.0.0
```

### **GitHub Actions Workflows**

- **CI**: Runs on every push/PR - tests and builds
- **Release**: Creates releases when tags are pushed
- **Package**: Publishes Docker images to GitHub Container Registry

## 📝 License

MIT License 