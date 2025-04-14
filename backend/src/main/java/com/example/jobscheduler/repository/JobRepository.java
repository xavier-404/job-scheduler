package com.example.jobscheduler.repository;

import com.example.jobscheduler.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Job entities.
 * Provides database operations for the Job entity.
 * Extends JpaRepository to inherit basic CRUD operations and query methods.
 */
@Repository // Marks this interface as a Spring Data repository.
public interface JobRepository extends JpaRepository<Job, UUID> {

    /**
     * Custom query method to find all jobs associated with a specific client ID.
     * Spring Data JPA automatically implements this method based on its name.
     *
     * @param clientId the client ID to search for (cannot be null)
     * @return a list of Job entities associated with the given client ID
     */
    List<Job> findByClientId(String clientId);

    /**
     * Custom query method to find all jobs with a specific status.
     * Spring Data JPA automatically implements this method based on its name.
     *
     * @param status the status to search for (cannot be null)
     * @return a list of Job entities with the given status
     */
    List<Job> findByStatus(Job.JobStatus status);
}