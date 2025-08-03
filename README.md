# Money Transfer Application

A robust Spring Boot application that demonstrates proper handling of concurrency, atomicity, and deadlock prevention in financial systems. **Now with automatic versioning and Railway deployment!**

## 🎯 Why This Matters for Financial Applications

Financial applications must handle multiple users transferring money simultaneously without data corruption. This application shows how to prevent common issues:

### 🔒 **Concurrency Problems Solved**

1. **Race Conditions**: When two transfers happen at the same time
   - **Problem**: Both transfers read the same balance, leading to incorrect final balance
   - **Solution**: In-memory locking with `ReentrantLock` prevents simultaneous access

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
# Health check (no auth required)
curl http://localhost:8080/ping

# Transfer money (requires authentication)
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" \
  -d '{
    "fromAccountNumber": "ACC001234567890",
    "toAccountNumber": "ACC002345678901", 
    "amount": 100.00,
    "currency": "USD",
    "description": "Test transfer"
  }'

# Check transfer status
curl -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" \
  http://localhost:8080/api/v1/transfers/{transferId}
```

## 🔧 How Concurrency is Handled

### 1. **In-Memory Locking** - Preventing Race Conditions

**The Problem**: Imagine two people trying to withdraw money from the same account at exactly the same time. Both see the balance is $1000, both try to withdraw $800. Without locking, both might succeed, leaving the account with -$600 instead of $200!

**The Solution**: We use `ReentrantLock` to ensure only one transfer can access an account at a time.

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
void testBasicTransfer() {
    // Tests basic transfer functionality
    TransferResponse response = transferService.processTransfer(request);
    assertEquals("COMPLETED", response.getStatus().name());
}

@Test
void testMultipleTransfers() {
    // Tests multiple sequential transfers
    for (int i = 0; i < 3; i++) {
        TransferResponse response = transferService.processTransfer(request);
        assertEquals("COMPLETED", response.getStatus().name());
    }
}
```

## 📊 API Endpoints

| Method | Endpoint | Auth Required | Description |
|--------|----------|---------------|-------------|
| GET | `/ping` | ❌ | Health check |
| GET | `/` | ❌ | Root endpoint with status |
| GET | `/actuator/health` | ❌ | Detailed health check |
| POST | `/api/v1/transfers` | ✅ | Create a new transfer |
| GET | `/api/v1/transfers/{id}` | ✅ | Get transfer details |

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Controller    │    │     Service     │    │   Repository    │
│                 │    │                 │    │                 │
│ • REST API      │───▶│ • Business Logic│───▶│ • Data Access   │
│ • Validation    │    │ • In-Memory     │    │ • Locking       │
│ • Security      │    │   Locking       │    │ • Transactions  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🔍 Key Features

- ✅ **In-Memory Locking**: Prevents race conditions with `ReentrantLock`
- ✅ **Deadlock Prevention**: Ordered locking strategy
- ✅ **Atomic Transactions**: All-or-nothing transfer operations
- ✅ **Optimistic Locking**: Version-based concurrency control
- ✅ **Spring Security**: Protected API endpoints with Basic Auth
- ✅ **Comprehensive Testing**: Concurrency stress tests
- ✅ **Error Handling**: Proper rollback on failures
- ✅ **Railway Deployment**: Live production deployment
- ✅ **Auto Versioning**: Automatic releases on every push

## 🚀 Live Demo

**This application is deployed and running on Railway!**

### **Live API URL**: https://money-transfer-app-production-9d8e.up.railway.app

### Test the Live API

```bash
# Health check (no authentication required)
curl https://money-transfer-app-production-9d8e.up.railway.app/ping

# Root endpoint with profile info
curl https://money-transfer-app-production-9d8e.up.railway.app/

# Transfer money (requires authentication)
curl -X POST https://money-transfer-app-production-9d8e.up.railway.app/api/v1/transfers \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" \
  -d '{
    "fromAccountNumber": "ACC001234567890",
    "toAccountNumber": "ACC002345678901", 
    "amount": 100.00,
    "currency": "USD",
    "description": "Test transfer"
  }'

# Check transfer status
curl -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" \
  https://money-transfer-app-production-9d8e.up.railway.app/api/v1/transfers/{transferId}
```

### Expected API Response

When you run the transfer command, you should get a response like this:

```json
{
  "transferId": "transfer_1234567890",
  "status": "COMPLETED",
  "fromAccountNumber": "ACC001234567890",
  "toAccountNumber": "ACC002345678901",
  "amount": 100.00,
  "currency": "USD",
  "description": "Test transfer",
  "timestamp": "2025-08-03T17:46:00.000Z"
}
```

### Authentication
- **Username**: `admin`
- **Password**: `admin123`
- **Basic Auth Header**: `Authorization: Basic YWRtaW46YWRtaW4xMjM=`

### Troubleshooting

If you get a 401 Unauthorized error:
1. Make sure you're using the correct authentication header
2. Check that the application is running in production mode (profile should be "prod")
3. Verify the accounts exist in the database

If you get a 404 Not Found error:
1. Check that the endpoint URL is correct
2. Ensure the application is deployed and running
3. Try the health check endpoint first: `curl https://money-transfer-app-production-9d8e.up.railway.app/ping`

If you get no response:
1. Add the `-v` flag to curl for verbose output: `curl -v ...`
2. Check if the application is still deploying
3. Wait a few minutes and try again

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
  -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" \
  -d '{
    "fromAccountNumber": "ACC001234567890",
    "toAccountNumber": "ACC002345678901", 
    "amount": 100.00,
    "currency": "USD",
    "description": "Test transfer"
  }'
```

## 🚀 Automatic Versioning & Releases

### **How It Works**

Every push to the `main` branch automatically triggers:

1. **🏷️ Version Detection**: Determines the next version number
2. **📦 Build Process**: Compiles and tests the application
3. **🏷️ Tag Creation**: Creates a Git tag (v1.0.0, v1.0.1, etc.)
4. **📦 Release Creation**: Creates a GitHub Release with artifacts
5. **🐳 Package Publishing**: Publishes Docker image to GitHub Container Registry

### **Version Progression**

```bash
# First push → v1.0.0
# Second push → v1.0.1
# Third push → v1.0.2
# And so on...
```

### **What Gets Created Automatically**

- **📦 JAR File**: `money-transfer-app-v1.0.0.jar` - Ready to run
- **🐳 Docker Image**: `ghcr.io/shihabcsedu09/money-transfer-app:v1.0.0` - Containerized
- **📋 GitHub Release**: With detailed changelog and documentation
- **🏷️ Git Tag**: Versioned tag for tracking

### **Using the Docker Image**

```bash
# Pull the latest image
docker pull ghcr.io/shihabcsedu09/money-transfer-app:latest

# Run the application
docker run -p 8080:8080 ghcr.io/shihabcsedu09/money-transfer-app:latest

# Run specific version
docker run -p 8080:8080 ghcr.io/shihabcsedu09/money-transfer-app:v1.0.0
```

### **GitHub Actions Workflows**

- **CI**: Runs on every push/PR - tests and builds
- **Auto Release**: Creates releases automatically on every push to main
- **Auto Package**: Publishes Docker images to GitHub Container Registry

## 🔧 Deployment

### **Railway Deployment**

The application is automatically deployed to Railway with:

- **Health Checks**: `/ping` endpoint for monitoring
- **Production Profile**: Optimized for containerized environment
- **Metrics Disabled**: Prevents container-related issues
- **Security**: Protected endpoints with authentication

### **Environment Variables**

```bash
SPRING_PROFILES_ACTIVE=prod
PORT=8080
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info
MANAGEMENT_METRICS_ENABLE_JVM=false
MANAGEMENT_METRICS_ENABLE_PROCESSOR=false
MANAGEMENT_METRICS_ENABLE_SYSTEM=false
MANAGEMENT_METRICS_ENABLE_TOMCAT=false
```

## 📝 License

MIT License 