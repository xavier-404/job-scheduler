# Job Scheduler Application

A full-stack application for scheduling and managing jobs that fetch client-specific user data and publish it to Kafka.

## Overview

This application provides a robust solution for scheduling data processing jobs with flexible timing options. Users can schedule one-time or recurring jobs that retrieve user data associated with a specific client ID from a database and publish it to a Kafka topic for further processing.

## Features

- **Flexible Job Scheduling**:
  - Immediate execution
  - One-time scheduled execution
  - Recurring execution with various patterns (hourly, daily, weekly, monthly)
  
- **Comprehensive Timezone Support**:
  - Schedule jobs in any timezone
  - All dates and times displayed in the selected timezone
  - Default timezone set to IST (Asia/Kolkata)

- **Real-time Job Management**:
  - Pause/resume scheduled jobs
  - Delete jobs
  - Track job status (scheduled, running, completed, failed)
  - View next execution time

- **Robust Architecture**:
  - Spring Boot backend with Quartz scheduler
  - React frontend with Material UI
  - YugabyteDB for data storage
  - Kafka for message publishing
  - Docker Compose for easy deployment

## Technology Stack

### Backend
- Java 21
- Spring Boot 3.4.4
- Quartz Scheduler
- Spring Data JPA
- Spring Kafka
- PostgreSQL JDBC (for YugabyteDB)

### Frontend
- React 18
- Material UI 5
- Axios for API calls
- date-fns for timezone handling
- React-Toastify for notifications

### Database
- YugabyteDB (PostgreSQL compatible)

### Messaging
- Kafka (KRaft mode - no Zookeeper dependency)

### Deployment
- Docker
- Docker Compose

## Architecture

The application follows a microservices architecture with the following components:

1. **React Frontend**: Provides a user interface for scheduling and managing jobs
2. **Spring Boot Backend**: Handles job scheduling, database operations, and Kafka integration
3. **YugabyteDB**: Stores user data and job information
4. **Kafka**: Receives published user data for further processing

### Data Flow
1. User schedules a job through the frontend
2. Backend schedules the job with Quartz and stores it in YugabyteDB
3. At the scheduled time, Quartz triggers the job
4. The job fetches users for the specific client ID from YugabyteDB
5. Each user record is published to a Kafka topic
6. Job status is updated in the database

## Getting Started

To run the application, you need Docker and Docker Compose installed. For setup instructions, see [SETUP_INSTRUCTIONS.md](SETUP_INSTRUCTIONS.md).

Quick start:
```bash
git clone <repository-url>
cd job-scheduler
docker-compose up
```

## Repository Structure

```
job-scheduler/
├── backend/                # Spring Boot application
│   ├── src/                # Source code
│   ├── pom.xml             # Maven configuration
│   └── Dockerfile          # Backend Docker configuration
├── frontend/               # React application
│   ├── public/             # Static files
│   ├── src/                # Source code
│   ├── package.json        # NPM dependencies
│   └── Dockerfile          # Frontend Docker configuration
├── docker-compose.yml      # Docker Compose configuration
├── README.md               # Project overview (this file)
├── SETUP_INSTRUCTIONS.md   # Detailed setup instructions
└── TESTING_GUIDE.md        # Testing guides and scenarios
```

## Testing

For comprehensive testing guides, see [TESTING_GUIDE.md](TESTING_GUIDE.md).

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Acknowledgments

- Spring Boot and Quartz for robust job scheduling
- React and Material UI for the responsive frontend
- YugabyteDB for distributed data storage
- Kafka for reliable message processing