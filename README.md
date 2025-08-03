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
- Java 17+
- Redis (for distributed locking)

### Run Locally
```bash
# Start Redis
docker run -d -p 6379:6379 redis:latest

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

### 1. Distributed Locking
```java
// Lock both accounts before transfer
RLock fromAccountLock = redissonClient.getLock("account:" + fromAccountNumber);
RLock toAccountLock = redissonClient.getLock("account:" + toAccountNumber);

// Acquire locks with timeout
boolean firstLockAcquired = firstLock.tryLock(lockTimeout, TimeUnit.SECONDS);
```

### 2. Deadlock Prevention
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

### 3. Atomic Transactions
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

### 4. Optimistic Locking
```java
@Entity
public class Account {
    @Version
    private Long version; // Prevents concurrent modifications
    
    public synchronized boolean debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            return false;
        }
        balance = balance.subtract(amount);
        return true;
    }
}
```

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

**Ready for Deployment!** 

### **Deploy to Railway (Free)**

1. **Sign up**: Go to [railway.app](https://railway.app) and sign up with GitHub
2. **Connect repo**: Select "Deploy from GitHub repo" → `shihabcsedu09/money-transfer-app`
3. **Configure**: Add environment variables:
   ```
   SPRING_PROFILES_ACTIVE=prod
   SERVER_PORT=8080
   SPRING_CACHE_TYPE=simple
   ```
4. **Deploy**: Railway will automatically build and deploy

### **Deploy to Render (Alternative)**

1. **Sign up**: Go to [render.com](https://render.com) and sign up with GitHub
2. **Connect repo**: Select "New Web Service" → Connect your GitHub repo
3. **Configure**: Use the `render.yaml` file in this repo
4. **Deploy**: Render will automatically deploy

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

### Test the Live API

```bash
# Transfer money
curl -X POST https://money-transfer-app-production.up.railway.app/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountNumber": "ACC001234567890",
    "toAccountNumber": "ACC002345678901", 
    "amount": 100.00,
    "currency": "USD",
    "description": "Test transfer"
  }'

# Check transfer status
curl https://money-transfer-app-production.up.railway.app/api/v1/transfers/{transferId}

# Health check
curl https://money-transfer-app-production.up.railway.app/api/v1/transfers/health
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

## 📝 License

MIT License 