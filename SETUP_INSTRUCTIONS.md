# Job Scheduler Application - Setup Instructions

This document provides detailed instructions on how to set up, build, run, and test the Job Scheduler application.

## Project Structure

The project consists of two main components:

1. **Backend**: A Java/Spring Boot application that handles job scheduling, database operations, and Kafka integration.
2. **Frontend**: A React application that provides a user interface for scheduling and monitoring jobs.

## Prerequisites

Before getting started, ensure you have the following installed on your system:

- Java 21 (for backend development)
- Node.js and npm (for frontend development)
- Docker and Docker Compose (for running the complete stack)
- Maven (for building the backend)

## Option 1: Running with Docker Compose (Recommended)

The easiest way to run the entire application stack is using Docker Compose.

### Step 1: Clone the Repository

```bash
git clone https://github.com/your-username/job-scheduler.git
cd job-scheduler
```

### Step 2: Build and Start the Services

```bash
docker-compose up --build
```

This command will:
- Build the backend and frontend Docker images
- Start YugabyteDB, Zookeeper, and Kafka
- Start the backend and frontend services
- Connect all services via a Docker network

### Step 3: Access the Application

Once all services are up and running, you can access:
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api/jobs
- YugabyteDB Admin UI: http://localhost:7000

### Step 4: Stopping the Services

```bash
docker-compose down
```

To remove all created volumes:

```bash
docker-compose down -v
```

## Option 2: Local Development Setup

For development purposes, you might want to run the components separately.

### Backend Setup

#### Step 1: Navigate to the Backend Directory

```bash
cd backend
```

#### Step 2: Build the Application

```bash
./mvnw clean package
```

or if you're using Maven directly:

```bash
mvn clean package
```

#### Step 3: Run the Backend

You'll need to have a local YugabyteDB and Kafka running, or modify the application.properties file to point to your existing instances.

```bash
./mvnw spring-boot:run
```

### Frontend Setup

#### Step 1: Navigate to the Frontend Directory

```bash
cd frontend
```

#### Step 2: Install Dependencies

```bash
npm install
```

#### Step 3: Start the Development Server

```bash
npm start
```

This will start the frontend on http://localhost:3000, and it will proxy API requests to the backend running on port 8080.

## Running Tests

### Backend Tests

The backend includes both unit tests and integration tests using TestContainers.

```bash
cd backend
./mvnw test
```

TestContainers will automatically start containerized instances of YugabyteDB and Kafka for the tests.

### Frontend Tests

```bash
cd frontend
npm test
```

## Interacting with the Application

### Creating a Job

1. Open the application at http://localhost:3000
2. Fill in the "Schedule New Job" form:
   - Enter a Client ID (e.g., CLIENT_ABC)
   - Select a Time Zone
   - Choose from immediate execution, one-time scheduling, or recurring scheduling
   - For recurring jobs, set up the recurrence pattern
3. Click "Schedule Job"

### Monitoring Jobs

The "Job Status Dashboard" section shows all scheduled jobs with their:
- Client ID
- Schedule details
- Next run time
- Current status

You can also perform actions like:
- Refreshing the job list
- Pausing/resuming jobs
- Deleting jobs

## Troubleshooting

### Database Connection Issues

If the backend cannot connect to YugabyteDB:
1. Ensure YugabyteDB is running: `docker ps | grep yugabyte`
2. Check the application.properties file for correct connection details
3. Verify network connectivity between the services

### Kafka Connection Issues

If jobs are stuck in the RUNNING state:
1. Check if Kafka is running: `docker ps | grep kafka`
2. Verify Kafka topic creation: `docker exec -it job-scheduler_kafka_1 kafka-topics --bootstrap-server localhost:9092 --list`
3. Check backend logs for Kafka-related errors

### Container Startup Order

Sometimes services may fail because they start before their dependencies. If this happens:
1. Stop all services: `docker-compose down`
2. Start YugabyteDB and Kafka first: `docker-compose up -d yugabytedb zookeeper kafka`
3. Wait for them to initialize (check healthchecks)
4. Start the rest of the stack: `docker-compose up -d`

## Development Tips

### Modifying the Backend

After making changes to the backend code:
1. Rebuild the application: `mvn clean package`
2. Restart the service: `docker-compose restart backend`

### Modifying the Frontend

If you're running the frontend in development mode with npm start, changes will automatically be applied with hot reloading.

If you're running via Docker:
1. Make your changes
2. Rebuild the frontend image: `docker-compose build frontend`
3. Restart the frontend container: `docker-compose up -d frontend`