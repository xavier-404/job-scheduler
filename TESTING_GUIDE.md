# Job Scheduler Application - Testing Guide

This guide provides instructions for testing the Job Scheduler application, including unit tests, integration tests, and manual testing scenarios.

## Automated Testing

The application includes comprehensive automated tests for both the backend and frontend.

### Backend Tests

The backend uses JUnit 5, Mockito, and TestContainers for automated testing.

#### Running Backend Tests

Navigate to the backend directory and execute:

```bash
# Using Maven Wrapper
./mvnw test

# Or using Maven directly
mvn test
```

This will run all tests, including:
- Unit tests for individual components
- Integration tests using TestContainers (requires Docker)

#### Key Test Categories

1. **Unit Tests**:
   - Controller tests (`*ControllerTest.java`)
   - Service tests (`*ServiceTest.java`)
   - Repository tests (`*RepositoryTest.java`)

2. **Integration Tests**:
   - Job scheduling tests (`JobSchedulerIntegrationTest.java`)
   - Kafka integration tests (`KafkaKRaftIntegrationTest.java`)
   - End-to-end tests that verify API functionality

3. **Specific Test Cases**:
   - Timezone handling tests (`TimezoneHandlingTest.java`)
   - Asynchronous job handling tests (`AsyncJobServiceTest.java`)

### Frontend Tests

The frontend uses Jest and React Testing Library for automated testing.

#### Running Frontend Tests

Navigate to the frontend directory and execute:

```bash
npm test
```

To run tests with coverage report:

```bash
npm test -- --coverage
```

## Manual Testing Scenarios

Here are key scenarios to test manually through the user interface:

### Scenario 1: Scheduling an Immediate Job

1. Open the application at [http://localhost:3000](http://localhost:3000)
2. Enter a client ID (e.g., "CLIENT_ABC")
3. Check the "Execute Immediately" checkbox
4. Click "Schedule Job"
5. Verify:
   - Success notification appears
   - Job appears in the job list with "SCHEDULED" status
   - Status changes to "COMPLETED_SUCCESS" shortly after (refresh if needed)

### Scenario 2: Scheduling a One-Time Job

1. Enter a client ID (e.g., "CLIENT_XYZ")
2. Uncheck "Execute Immediately" if checked
3. Ensure "One Time" is selected in Schedule Type
4. Set a future date and time (at least 5 minutes in the future)
5. Click "Schedule Job"
6. Verify:
   - Success notification appears
   - Job appears in the job list with "SCHEDULED" status
   - "Next Run" shows the scheduled date and time
   - If you wait until the scheduled time, status should change to "COMPLETED_SUCCESS"

### Scenario 3: Scheduling a Recurring Job

1. Enter a client ID (e.g., "CLIENT_123")
2. Select "Recurring" in Schedule Type
3. Set execution time
4. Choose a recurrence pattern:
   - Hourly: Set an hourly interval (e.g., 1 hour)
   - Weekly: Select days of the week 
   - Monthly: Select days of the month
5. Click "Schedule Job"
6. Verify:
   - Success notification appears
   - Job appears in the job list with "SCHEDULED" status
   - "Next Run" shows the next execution time
   - Schedule description matches your selection

### Scenario 4: Testing Job Management

1. Schedule several jobs using the methods above
2. Test job management functionality:
   - Click the Pause button (pause icon) for a job
   - Verify the job doesn't execute at the scheduled time
   - Click the Resume button (play icon) for the job
   - Verify the job resumes its schedule
   - Click the Delete button (trash icon) for a job
   - Verify the job is removed from the list

### Scenario 5: Testing Timezone Handling

1. Schedule a one-time job
2. Select a different timezone (e.g., "America/New_York")
3. Set a specific time in that timezone
4. Click "Schedule Job"
5. Verify:
   - The job appears with the correct timezone
   - The "Next Run" time is displayed appropriately
   - The job executes at the correct time (in the specified timezone)

### Scenario 6: Error Handling

1. Test validation errors:
   - Try to schedule a job without a client ID
   - Try to schedule a one-time job in the past
   - Verify appropriate error messages are displayed
2. Test API errors:
   - Stop the backend server 
   - Try to schedule a job
   - Verify proper error handling and messaging

### Scenario 7: Testing with Real Data

If sample data is loaded in the database:

1. Schedule a job for "CLIENT_ABC" (has users in the database)
2. Verify the job completes successfully
3. Check for published messages:
   - Visit [http://localhost:8080/api/test/kafka-messages](http://localhost:8080/api/test/kafka-messages)
   - Verify that user records have been published to Kafka

## End-to-End (E2E) Testing

For manual end-to-end testing that verifies the complete workflow:

1. Start all services with Docker Compose
2. Schedule various job types through the UI
3. Verify jobs execute as expected
4. Check the Kafka messages endpoint to confirm user data publication
5. Verify the data is published according to the schedule

## Performance Testing

For basic performance testing:

1. Schedule multiple jobs with different schedules
2. Monitor system performance:
   - Backend CPU and memory usage with Docker: `docker stats`
   - Database performance: Check YugabyteDB metrics at [http://localhost:7001](http://localhost:7001)
   - Kafka performance: Monitor with appropriate Kafka tools

## Security Testing

Basic security checks:

1. Input validation: 
   - Try entering scripts or SQL injection in the client ID
   - Verify the application properly validates and sanitizes inputs
2. API access:
   - Try accessing API endpoints directly
   - Verify proper error handling for invalid inputs

## Test Data

For testing purposes, the database is initialized with sample data:

**Users:**
- CLIENT_ABC: 2 users
- CLIENT_XYZ: 2 users  
- CLIENT_123: 1 user

You can use these client IDs to test jobs that should find existing users.

## Troubleshooting Test Issues

### Backend Test Failures

1. TestContainers issues:
   - Ensure Docker is running
   - Check Docker has sufficient resources
   - Remove any conflicting containers: `docker rm -f $(docker ps -aq)`

2. Database connection issues:
   - Check that no other tests are running
   - Ensure no port conflicts on 5433

### Frontend Test Failures

1. Check the correct version of Node.js is being used
2. Update npm dependencies: `npm install`
3. Clear the test cache: `npm test -- --clearCache`

## Continuous Integration Testing

The application is configured for CI testing with:

- GitHub Actions workflows
- TestContainers for integration testing
- Jest for frontend testing

When setting up CI, ensure:
1. Docker is available in the CI environment
2. Sufficient resources are allocated for TestContainers
3. Timeouts are set appropriately for integration tests