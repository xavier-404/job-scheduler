package com.example.jobscheduler.controller;

import com.example.jobscheduler.dto.JobRequest;
import com.example.jobscheduler.dto.JobResponse;
import com.example.jobscheduler.model.Job;
import com.example.jobscheduler.service.JobSchedulerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for the JobController.
 */
@WebMvcTest(JobController.class)
public class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobSchedulerService jobSchedulerService;

    /**
     * Tests creating a job.
     */
    @Test
    public void testCreateJob() throws Exception {
        // Arrange
        UUID jobId = UUID.randomUUID();
        
        JobRequest jobRequest = JobRequest.builder()
                .clientId("CLIENT_ABC")
                .scheduleType(Job.ScheduleType.ONE_TIME)
                .startTime(LocalDateTime.now().plusHours(1))
                .timeZone("UTC")
                .build();
        
        JobResponse jobResponse = JobResponse.builder()
                .id(jobId)
                .clientId("CLIENT_ABC")
                .scheduleType(Job.ScheduleType.ONE_TIME)
                .startTime(jobRequest.getStartTime())
                .nextFireTime(jobRequest.getStartTime())
                .status(Job.JobStatus.SCHEDULED)
                .timeZone("UTC")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(jobSchedulerService.createJob(any(JobRequest.class))).thenReturn(jobResponse);
        
        // Act & Assert
        mockMvc.perform(post("/api/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(jobRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(jobId.toString()))
                .andExpect(jsonPath("$.clientId").value("CLIENT_ABC"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    /**
     * Tests getting all jobs.
     */
    @Test
    public void testGetAllJobs() throws Exception {
        // Arrange
        UUID jobId1 = UUID.randomUUID();
        UUID jobId2 = UUID.randomUUID();
        
        JobResponse job1 = JobResponse.builder()
                .id(jobId1)
                .clientId("CLIENT_ABC")
                .scheduleType(Job.ScheduleType.ONE_TIME)
                .status(Job.JobStatus.SCHEDULED)
                .timeZone("UTC")
                .build();
        
        JobResponse job2 = JobResponse.builder()
                .id(jobId2)
                .clientId("CLIENT_XYZ")
                .scheduleType(Job.ScheduleType.RECURRING)
                .status(Job.JobStatus.SCHEDULED)
                .timeZone("UTC")
                .build();
        
        when(jobSchedulerService.getAllJobs()).thenReturn(List.of(job1, job2));
        
        // Act & Assert
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(jobId1.toString()))
                .andExpect(jsonPath("$[0].clientId").value("CLIENT_ABC"))
                .andExpect(jsonPath("$[1].id").value(jobId2.toString()))
                .andExpect(jsonPath("$[1].clientId").value("CLIENT_XYZ"));
    }

    /**
     * Tests getting a job by ID.
     */
    @Test
    public void testGetJobById() throws Exception {
        // Arrange
        UUID jobId = UUID.randomUUID();
        
        JobResponse jobResponse = JobResponse.builder()
                .id(jobId)
                .clientId("CLIENT_ABC")
                .scheduleType(Job.ScheduleType.ONE_TIME)
                .status(Job.JobStatus.SCHEDULED)
                .timeZone("UTC")
                .build();
        
        when(jobSchedulerService.getJobById(jobId)).thenReturn(jobResponse);
        
        // Act & Assert
        mockMvc.perform(get("/api/jobs/{id}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId.toString()))
                .andExpect(jsonPath("$.clientId").value("CLIENT_ABC"));
    }

    /**
     * Tests deleting a job.
     */
    @Test
    public void testDeleteJob() throws Exception {
        // Arrange
        UUID jobId = UUID.randomUUID();
        
        // Act & Assert
        mockMvc.perform(delete("/api/jobs/{id}", jobId))
                .andExpect(status().isNoContent());
    }
}