# Money Transfer Application

A robust, production-ready money transfer application built with Spring Boot that handles concurrency, atomicity, and deadlock prevention for financial operations.

## 🚀 Features

### Core Functionality
- **Money Transfers**: Secure transfer of funds between accounts
- **Multi-Currency Support**: Support for USD, EUR, GBP, JPY, CAD, AUD, CHF, CNY, INR, BRL
- **Account Management**: Create and manage user accounts with balances
- **Transfer History**: Complete audit trail of all transfers

### Concurrency & Safety Features
- **Distributed Locking**: Uses Redisson for distributed locking to prevent race conditions
- **Atomic Transactions**: All transfer operations are atomic with proper rollback
- **Deadlock Prevention**: Ordered locking strategy prevents deadlocks
- **Optimistic Locking**: Version-based concurrency control for entities
- **Thread-Safe Operations**: Synchronized balance operations

### Monitoring & Observability
- **Actuator Endpoints**: Health checks, metrics, and monitoring
- **Prometheus Integration**: Metrics export for monitoring
- **Comprehensive Logging**: Detailed logging for debugging and audit
- **Performance Metrics**: Transfer operation timing and statistics

### Security & Validation
- **Input Validation**: Comprehensive request validation
- **Error Handling**: Global exception handling with proper HTTP status codes
- **Audit Trail**: Complete audit trail for all operations
- **Data Integrity**: Proper constraints and validation

## 🏗️ Architecture

### Technology Stack
- **Spring Boot 3.2.0**: Core framework
- **Spring Data JPA**: Data access layer
- **H2 Database**: In-memory database for development
- **PostgreSQL**: Production database support
- **Redisson**: Distributed locking and caching
- **Spring Security**: Security framework
- **Micrometer**: Metrics and monitoring
- **Maven**: Build tool

### Key Components

#### Domain Layer
- `Account`: Represents user accounts with balance and status
- `Transfer`: Represents money transfers with status tracking
- `Currency`: Supported currencies enumeration
- `AccountStatus`: Account status enumeration
- `TransferStatus`: Transfer status enumeration

#### Service Layer
- `TransferService`: Core business logic with distributed locking
- Atomic operations with proper error handling
- Deadlock prevention through ordered locking

#### Repository Layer
- `AccountRepository`: Account data access with pessimistic locking
- `TransferRepository`: Transfer data access with custom queries

#### Controller Layer
- `TransferController`: REST API endpoints
- `GlobalExceptionHandler`: Global error handling

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Redis (for distributed locking)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd money-transfer-app
   ```

2. **Start Redis** (required for distributed locking)
   ```bash
   # Using Docker
   docker run -d -p 6379:6379 redis:latest
   
   # Or install Redis locally
   brew install redis  # macOS
   sudo apt-get install redis-server  # Ubuntu
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application**
   - Application: http://localhost:8080
   - H2 Console: http://localhost:8080/h2-console
   - Actuator: http://localhost:8080/actuator

### API Endpoints

#### Transfer Money
```bash
POST /api/v1/transfers
Content-Type: application/json

{
  "fromAccountNumber": "ACC001234567890",
  "toAccountNumber": "ACC002345678901",
  "amount": 100.00,
  "currency": "USD",
  "description": "Payment for services"
}
```

#### Get Transfer Details
```bash
GET /api/v1/transfers/{transferId}
```

#### Health Check
```bash
GET /api/v1/transfers/health
```

## 🧪 Testing

### Run Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=TransferServiceConcurrencyTest

# Run with coverage
mvn test jacoco:report
```

### Concurrency Tests
The application includes comprehensive concurrency tests that demonstrate:
- Thread safety of transfer operations
- Atomicity of transactions
- Deadlock prevention
- Proper handling of concurrent transfers
- Insufficient funds scenarios

## 🔧 Configuration

### Application Properties
Key configuration options in `application.yml`:

```yaml
app:
  transfer:
    max-amount: 1000000.00      # Maximum transfer amount
    min-amount: 0.01           # Minimum transfer amount
    lock-timeout: 30           # Distributed lock timeout (seconds)
    retry-attempts: 3          # Number of retry attempts
    concurrent-transfers-limit: 100  # Concurrent transfer limit
```

### Database Configuration
- **Development**: H2 in-memory database
- **Production**: PostgreSQL with connection pooling
- **Migrations**: Automatic schema generation

### Redis Configuration
- **Host**: localhost:6379 (configurable)
- **Timeout**: 2000ms
- **Connection Pool**: 8 max connections

## 📊 Monitoring

### Actuator Endpoints
- `/actuator/health`: Application health
- `/actuator/metrics`: Application metrics
- `/actuator/prometheus`: Prometheus metrics export

### Key Metrics
- Transfer operation timing
- Success/failure rates
- Concurrent transfer counts
- Lock acquisition times

## 🔒 Security Features

### Concurrency Control
1. **Distributed Locking**: Prevents race conditions across multiple instances
2. **Optimistic Locking**: Version-based concurrency control
3. **Pessimistic Locking**: Database-level locking for critical operations
4. **Ordered Locking**: Deadlock prevention through consistent lock ordering

### Data Integrity
1. **Atomic Transactions**: All-or-nothing transfer operations
2. **Rollback Mechanisms**: Automatic rollback on failures
3. **Validation**: Comprehensive input validation
4. **Audit Trail**: Complete operation history

## 🚨 Error Handling

### Exception Types
- `TransferException`: General transfer errors
- `InsufficientFundsException`: Insufficient funds errors
- `ValidationException`: Input validation errors

### HTTP Status Codes
- `200 OK`: Successful operation
- `201 Created`: Transfer created successfully
- `400 Bad Request`: Validation errors
- `409 Conflict`: Insufficient funds
- `500 Internal Server Error`: Server errors

## 📈 Performance Considerations

### Concurrency Handling
- **Distributed Locks**: Prevents race conditions
- **Connection Pooling**: Efficient database connections
- **Caching**: Redis-based caching for frequently accessed data
- **Async Processing**: Non-blocking operations where possible

### Scalability
- **Horizontal Scaling**: Stateless design supports multiple instances
- **Database Sharding**: Support for database partitioning
- **Load Balancing**: Ready for load balancer deployment

## 🔧 Development

### Project Structure
```
src/
├── main/
│   ├── java/com/moneytransfer/
│   │   ├── controller/     # REST controllers
│   │   ├── service/        # Business logic
│   │   ├── repository/     # Data access
│   │   ├── domain/         # Entity classes
│   │   ├── dto/           # Data transfer objects
│   │   ├── exception/      # Custom exceptions
│   │   └── config/        # Configuration classes
│   └── resources/
│       └── application.yml # Application configuration
└── test/
    ├── java/              # Test classes
    └── resources/         # Test configuration
```

### Adding New Features
1. Create domain entities with proper JPA annotations
2. Add repository interfaces with custom queries
3. Implement service layer with business logic
4. Create DTOs for API requests/responses
5. Add controller endpoints
6. Write comprehensive tests

## 🚀 Deployment

### Docker Deployment
```bash
# Build Docker image
docker build -t money-transfer-app .

# Run with Redis
docker-compose up -d
```

### Production Considerations
1. **Database**: Use PostgreSQL in production
2. **Redis**: Configure Redis cluster for high availability
3. **Monitoring**: Set up Prometheus and Grafana
4. **Logging**: Configure centralized logging
5. **Security**: Enable HTTPS and authentication

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## 📞 Support

For questions and support, please open an issue in the GitHub repository. 