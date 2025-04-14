package com.example.jobscheduler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a user in the system.
 * Users are associated with clients via the clientId field.
 */
@Entity // Marks this class as a JPA entity mapped to a database table.
@Table(name = "users") // Specifies the table name in the database.
@Data // Lombok annotation to generate getters, setters, toString, equals, and hashCode methods.
@Builder // Lombok annotation to implement the builder pattern for this class.
@NoArgsConstructor // Lombok annotation to generate a no-argument constructor.
@AllArgsConstructor // Lombok annotation to generate an all-argument constructor.
public class User {

    @Id // Marks this field as the primary key of the entity.
    @GeneratedValue(strategy = GenerationType.AUTO) // Specifies that the ID will be generated automatically.
    private UUID id; // Unique identifier for the user.

    @Column(name = "client_id", nullable = false) // Maps this field to the "client_id" column in the database. It cannot be null.
    private String clientId; // Identifier for the client associated with the user.

    @Column(nullable = false) // Maps this field to a database column. It cannot be null.
    private String name; // Name of the user.

    @Column // Maps this field to a database column.
    private String email; // Email address of the user.

    @Column // Maps this field to a database column.
    private String address; // Address of the user.

    @Column // Maps this field to a database column.
    private String phone; // Phone number of the user.

    @CreationTimestamp // Automatically sets the timestamp when the entity is created.
    @Column(name = "created_at", updatable = false) // Maps this field to the "created_at" column. It cannot be updated after creation.
    private LocalDateTime createdAt; // Timestamp of when the user was created.

    @UpdateTimestamp // Automatically updates the timestamp when the entity is updated.
    @Column(name = "updated_at") // Maps this field to the "updated_at" column.
    private LocalDateTime updatedAt; // Timestamp of the last update to the user.
}