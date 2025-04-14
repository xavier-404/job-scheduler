package com.example.jobscheduler.repository;

import com.example.jobscheduler.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for User entities.
 * Provides database operations for the User entity.
 * Extends JpaRepository to inherit basic CRUD operations and query methods.
 */
@Repository // Marks this interface as a Spring Data repository.
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Custom query method to find all users associated with a specific client ID.
     * Spring Data JPA automatically implements this method based on its name.
     *
     * @param clientId the client ID to search for (cannot be null)
     * @return a list of User entities associated with the given client ID
     */
    List<User> findByClientId(String clientId);
}