#!/bin/bash

echo "Starting Money Transfer Application..."
echo "Current directory: $(pwd)"
echo "Environment variables:"
echo "PORT: $PORT"
echo "SERVER_PORT: $SERVER_PORT"
echo "SPRING_PROFILES_ACTIVE: $SPRING_PROFILES_ACTIVE"

# Check if JAR exists
if [ -f "target/money-transfer-app-1.0.0.jar" ]; then
    echo "JAR file found"
    ls -la target/
else
    echo "JAR file not found!"
    ls -la target/ || echo "target directory not found"
    exit 1
fi

# Start the application
echo "Starting application..."
java -Dmanagement.metrics.binders.processor.enabled=false \
     -Dmanagement.metrics.binders.jvm.enabled=false \
     -Dmanagement.metrics.binders.system.enabled=false \
     -jar target/money-transfer-app-1.0.0.jar 