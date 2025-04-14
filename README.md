# Job Scheduler Application

A full-stack application for scheduling jobs to fetch client-specific data and publish it to Kafka.

## Overview

This application allows users to schedule jobs that fetch data associated with a specific `clientID` from a database and publish it to a Kafka topic. The application consists of a React frontend and a Java/Spring Boot backend, with YugabyteDB for data storage and Kafka for message publishing.

## Architecture

- **Frontend**: React SPA using Material UI components
- **Backend**: Java 21 / Spring Boot 3.4.4 with Quartz Scheduler
- **Database**: YugabyteDB for user data and job information
- **Messaging**: Kafka for asynchronous data publishing

## Features

- Schedule one-time or recurring jobs (hourly, daily, weekly, monthly)
- Execute jobs immediately
- Track job status (scheduled, running, completed, failed)
- View all scheduled jobs and their statuses

## Database Schema

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

CREATE INDEX idx_client_id ON users(client_id);
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

## Design Decisions

1. **Kafka Topic Naming Strategy**: We use a single topic named `user-data` for all published user data. Each message includes the `clientID` as metadata, allowing consumers to filter messages by client if needed.

2. **Data Serialization Format**: User data is serialized as JSON. Each user record is sent as a separate Kafka message to allow for easier processing and avoid message size limitations.

3. **Error Handling Approach**: 
   - Failed job executions are marked with a COMPLETED_FAILURE status
   - Detailed error information is logged
   - Retries are implemented for Kafka publishing failures

4. **Application Flow**:
   - When a job is scheduled, it's stored in YugabyteDB and registered with Quartz
   - At the scheduled time, Quartz triggers the job execution
   - The job fetches all users with the specified clientID
   - Each user record is published to Kafka
   - The job status is updated based on the outcome

## Setup Instructions

### Prerequisites

- Docker and Docker Compose
- Java 21 (for local development)
- Node.js and npm (for local development)

### Running with Docker Compose

1. Clone the repository:
   ```
   git clone https://github.com/your-username/job-scheduler.git
   cd job-scheduler
   ```

2. Start all services using Docker Compose:
   ```
   docker-compose up
   ```

3. Access the application at http://localhost:3000

### Local Development Setup

#### Backend

1. Navigate to the backend directory:
   ```
   cd backend
   ```

2. Build the application:
   ```
   ./mvnw clean package -DskipTests
   ```

3. Run the application:
   ```
   ./mvnw spring-boot:run
   ```

#### Frontend

1. Navigate to the frontend directory:
   ```
   cd frontend
   ```

2. Install dependencies:
   ```
   npm install
   ```

3. Start the development server:
   ```
   npm start
   ```

### Running Tests

To run the backend tests (which use TestContainers for integration testing):

```
cd backend
./mvnw test
```

Note: TestContainers requires Docker to be running on your machine.

## API Endpoints

### Jobs API

- `POST /api/jobs` - Create a new job
- `GET /api/jobs` - Get all jobs
- `GET /api/jobs/{id}` - Get a specific job
- `DELETE /api/jobs/{id}` - Delete a job
- `PATCH /api/jobs/{id}/pause` - Pause a job
- `PATCH /api/jobs/{id}/resume` - Resume a job

## Docker Services

The docker-compose setup includes the following services:

- `backend` - Spring Boot application
- `frontend` - React application served by Nginx
- `kafka` - Kafka message broker
- `zookeeper` - Required by Kafka
- `yugabytedb` - YugabyteDB database

## Assumptions

1. The `user` table in YugabyteDB already exists with the schema described above
2. Jobs should be persisted and survive application restarts
3. Each user record is published as a separate Kafka message rather than batching them