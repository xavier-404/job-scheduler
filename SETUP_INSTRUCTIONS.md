# Job Scheduler Application - Setup Instructions

This document provides detailed instructions on how to set up, build, run, and deploy the Job Scheduler application.

## Prerequisites

Before getting started, ensure you have the following installed on your system:

- Docker (version 20.10.0 or later)
- Docker Compose (version 2.0.0 or later)

For local development, you'll also need:

- Java 21 JDK
- Maven 3.8+
- Node.js 18.x and npm

## Option 1: Running with Docker Compose (Recommended)

The easiest way to run the entire application stack is using Docker Compose.

### Step 1: Clone the Repository

```bash
git clone <repository-url>
cd job-scheduler
```

### Step 2: Configure Environment Variables (Optional)

The application comes with sensible defaults, but you can customize the configuration by creating a `.env` file in the root directory:

```bash
# Example .env file
POSTGRES_USER=yugabyte
POSTGRES_PASSWORD=yugabyte
POSTGRES_DB=yugabyte
KAFKA_TOPIC=user-data
DEFAULT_TIMEZONE=Asia/Kolkata
```

### Step 3: Start the Services

```bash
docker-compose up -d
```

This command will:
- Build the backend and frontend Docker images
- Start YugabyteDB, Kafka, the backend, and the frontend services
- Connect all services via a Docker network

The first startup might take a few minutes as it downloads the necessary Docker images and builds the application.

### Step 4: Verify the Services

Once all services are up and running, you can verify they're working correctly:

- Frontend: [http://localhost:3000](http://localhost:3000)
- Backend API: [http://localhost:8080/api/jobs](http://localhost:8080/api/jobs)
- YugabyteDB Admin UI: [http://localhost:7001](http://localhost:7001)

### Step 5: View Logs (Optional)

To view logs for a specific service:

```bash
docker-compose logs -f <service_name>
```

Where `<service_name>` can be one of: `backend`, `frontend`, `yugabytedb`, or `kafka`.

### Step 6: Stop the Services

When you're done, you can stop and remove the containers:

```bash
docker-compose down
```

To remove the volumes as well (this will delete all data):

```bash
docker-compose down -v
```

## Option 2: Local Development Setup

For development purposes, you might want to run the components separately.

### Database and Kafka Setup

Start YugabyteDB and Kafka using Docker Compose but exclude the application services:

```bash
docker-compose up -d yugabytedb kafka
```

### Backend Setup

#### Step 1: Navigate to the Backend Directory

```bash
cd backend
```

#### Step 2: Configure the Application Properties

Edit `src/main/resources/application.properties` to point to your local YugabyteDB and Kafka instances:

```properties
# Database configuration
spring.datasource.url=jdbc:postgresql://localhost:5433/yugabyte
spring.datasource.username=yugabyte
spring.datasource.password=yugabyte

# Kafka configuration
spring.kafka.bootstrap-servers=localhost:9092
```

#### Step 3: Build and Run the Backend

```bash
# Using Maven Wrapper
./mvnw clean spring-boot:run

# Or using Maven directly
mvn clean spring-boot:run
```

The backend service will start on port 8080.

### Frontend Setup

#### Step 1: Navigate to the Frontend Directory

```bash
cd frontend
```

#### Step 2: Install Dependencies

```bash
npm install
```

#### Step 3: Configure the Proxy (Optional)

If your backend is running on a different port or host, edit the `proxy` field in `package.json`:

```json
"proxy": "http://localhost:8080"
```

#### Step 4: Start the Development Server

```bash
npm start
```

The frontend development server will start on port 3000 with hot-reloading enabled.

## Database Schema Initialization

The application automatically initializes the database schema on startup. For reference, the main tables are:

### User Table
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    address VARCHAR(500),
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Job Table
```sql
CREATE TABLE jobs (
    id UUID PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL,
    schedule_type VARCHAR(20) NOT NULL, -- IMMEDIATE, ONE_TIME, RECURRING
    cron_expression VARCHAR(100),
    time_zone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    start_time TIMESTAMP,
    next_fire_time TIMESTAMP,
    status VARCHAR(20) NOT NULL, -- SCHEDULED, RUNNING, COMPLETED_SUCCESS, COMPLETED_FAILURE
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Production Deployment Considerations

For production deployments, consider the following:

1. **Security**:
   - Use a proper secrets management solution instead of environment variables
   - Enable HTTPS for all services
   - Implement proper authentication and authorization

2. **Scalability**:
   - Increase the replicas for Kafka
   - Use a clustered YugabyteDB setup
   - Set up multiple backend instances behind a load balancer

3. **Monitoring**:
   - Configure metrics collection (Prometheus)
   - Set up dashboards (Grafana)
   - Implement centralized logging (ELK stack)

4. **Backup and Recovery**:
   - Configure regular database backups
   - Implement a disaster recovery plan

## Troubleshooting

### Database Connection Issues

If the backend cannot connect to YugabyteDB:
1. Check that YugabyteDB is running: `docker ps | grep yugabyte`
2. Verify the connection details in `application.properties`
3. Check if YugabyteDB is accessible: `psql -h localhost -p 5433 -U yugabyte -d yugabyte`

### Kafka Connection Issues

If jobs are stuck in the RUNNING state:
1. Check if Kafka is running: `docker ps | grep kafka`
2. Verify Kafka topic creation: `docker exec -it job-scheduler_kafka_1 kafka-topics.sh --bootstrap-server localhost:9092 --list`
3. Check the backend logs for Kafka-related errors

### Frontend Connection Issues

If the frontend cannot connect to the backend:
1. Verify that the backend is running: `curl http://localhost:8080/api/jobs`
2. Check the proxy configuration in `package.json`
3. Check for CORS-related errors in the browser console

### Container Startup Order Issues

Sometimes services may fail because they start before their dependencies. If this happens:
1. Stop all services: `docker-compose down`
2. Start the database first: `docker-compose up -d yugabytedb`
3. Wait for it to initialize (about 30 seconds)
4. Start Kafka: `docker-compose up -d kafka`
5. Start the application services: `docker-compose up -d backend frontend`