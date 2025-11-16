# Development Commands - TMS SEP490 Backend

## Environment Setup (Windows)
```bash
# Set Java 21 (adjust path as needed)
export JAVA_HOME="/c/Users/YourUsername/.jdks/openjdk-21.0.1"
export PATH="$JAVA_HOME/bin:$PATH"

# Verify setup
java -version
mvn -version
```

## Build & Run
```bash
# Clean and compile
mvn clean compile

# Run application (port 8080)
mvn spring-boot:run

# Build JAR (skip tests)
mvn clean package -DskipTests

# Run JAR
java -jar target/tms-sep490-be-0.0.1-SNAPSHOT.jar
```

## Testing Commands
```bash
# Run all tests with coverage
mvn clean verify

# Run unit tests only
mvn test

# Run specific test class
mvn test -Dtest=CenterServiceImplTest

# Run specific test method
mvn test -Dtest=CenterServiceImplTest#shouldFindCenterById

# Run tests in parallel (faster)
mvn -T 1C clean verify

# View coverage report after mvn verify
# Open: target/site/jacoco/index.html
```

## Database Setup (Docker)
```bash
# Start PostgreSQL container
docker run --name tms-postgres -e POSTGRES_PASSWORD=979712 -p 5432:5432 -d postgres:16

# Create database
docker exec -it tms-postgres psql -U postgres -c "CREATE DATABASE tms;"

# Load schema (PowerShell)
Get-Content "src/main/resources/schema.sql" | docker exec -i tms-postgres psql -U postgres -d tms

# Load seed data
Get-Content "src/main/resources/seed-data.sql" | docker exec -i tms-postgres psql -U postgres -d tms
```

## API Documentation
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI Spec: http://localhost:8080/v3/api-docs

## Git Commands
```bash
git status
git add .
git commit -m "message"
git push origin branch-name
git pull origin branch-name
git checkout -b new-branch
```

## Maven Wrapper (Alternative)
```bash
# Use if JAVA_HOME setup is problematic
./mvnw clean compile
./mvnw spring-boot:run
./mvnw test -Dtest=ClassName
```

## Windows Specific
```bash
# List files
dir /s /b
Get-ChildItem -Recurse

# Find text in files (PowerShell)
Select-String -Path "*.java" -Pattern "searchTerm"

# Check port usage
netstat -ano | findstr :8080
```
