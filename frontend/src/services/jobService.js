import axios from 'axios';

const API_URL = '/api/jobs';

/**
 * Fetches all jobs from the API.
 * @returns {Promise<Array>} A promise that resolves to an array of jobs
 */
export const fetchJobs = async () => {
  try {
    const response = await axios.get(API_URL);
    return response.data;
  } catch (error) {
    console.error('Error fetching jobs:', error);
    throw error;
  }
};

/**
 * Fetches a single job by its ID.
 * @param {string} id - The ID of the job to fetch
 * @returns {Promise<Object>} A promise that resolves to the job data
 */
export const getJobById = async (id) => {
  try {
    const response = await axios.get(`${API_URL}/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error fetching job ${id}:`, error);
    throw error;
  }
};

/**
 * Creates a new job.
 * @param {Object} jobData - The job data to create
 * @returns {Promise<Object>} A promise that resolves to the created job
 */
export const createJob = async (jobData) => {
  try {
    const response = await axios.post(API_URL, jobData);
    return response.data;
  } catch (error) {
    console.error('Error creating job:', error);
    throw error;
  }
};

/**
 * Deletes a job by ID.
 * @param {string} id - The ID of the job to delete
 * @returns {Promise<void>} A promise that resolves when the job is deleted
 */
export const deleteJob = async (id) => {
  try {
    await axios.delete(`${API_URL}/${id}`);
  } catch (error) {
    console.error(`Error deleting job ${id}:`, error);
    throw error;
  }
};

/**
 * Pauses a job by ID.
 * @param {string} id - The ID of the job to pause
 * @returns {Promise<Object>} A promise that resolves to the updated job
 */
export const pauseJob = async (id) => {
  try {
    const response = await axios.patch(`${API_URL}/${id}/pause`);
    return response.data;
  } catch (error) {
    console.error(`Error pausing job ${id}:`, error);
    throw error;
  }
};

/**
 * Resumes a job by ID.
 * @param {string} id - The ID of the job to resume
 * @returns {Promise<Object>} A promise that resolves to the updated job
 */
export const resumeJob = async (id) => {
  try {
    const response = await axios.patch(`${API_URL}/${id}/resume`);
    return response.data;
  } catch (error) {
    console.error(`Error resuming job ${id}:`, error);
    throw error;
  }
};