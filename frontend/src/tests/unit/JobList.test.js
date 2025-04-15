// Save as: frontend/src/tests/unit/JobList.test.js

import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import JobList from '../../components/JobList';
import { deleteJob, pauseJob, resumeJob } from '../../services/jobService';

// Mock the job service functions
jest.mock('../../services/jobService', () => ({
  deleteJob: jest.fn().mockResolvedValue({}),
  pauseJob: jest.fn().mockResolvedValue({}),
  resumeJob: jest.fn().mockResolvedValue({})
}));

// Mock toast notifications
jest.mock('react-toastify', () => ({
  toast: {
    success: jest.fn(),
    error: jest.fn()
  }
}));

describe('JobList Component', () => {
  const mockJobs = [
    {
      id: '123',
      clientId: 'CLIENT_1',
      scheduleType: 'ONE_TIME',
      startTime: '2025-04-15T14:30:00',
      nextFireTime: '2025-04-15T14:30:00',
      timeZone: 'UTC',
      status: 'SCHEDULED'
    },
    {
      id: '456',
      clientId: 'CLIENT_2',
      scheduleType: 'RECURRING',
      cronExpression: '0 0 * * * ?',
      timeZone: 'Asia/Kolkata',
      status: 'SCHEDULED'
    }
  ];
  
  const mockOnRefresh = jest.fn();
  
  beforeEach(() => {
    jest.clearAllMocks();
  });
  
  test('renders job list correctly', () => {
    render(<JobList jobs={mockJobs} loading={false} error={null} onRefresh={mockOnRefresh} />);
    
    expect(screen.getByText('CLIENT_1')).toBeInTheDocument();
    expect(screen.getByText('CLIENT_2')).toBeInTheDocument();
    expect(screen.getAllByText(/SCHEDULED/i).length).toBe(2);
  });
  
  test('shows loading indicator when loading', () => {
    render(<JobList jobs={[]} loading={true} error={null} onRefresh={mockOnRefresh} />);
    
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });
  
  test('shows error message when there is an error', () => {
    render(<JobList jobs={[]} loading={false} error="Failed to load jobs" onRefresh={mockOnRefresh} />);
    
    expect(screen.getByText(/Failed to load jobs/i)).toBeInTheDocument();
  });
  
  test('shows empty message when there are no jobs', () => {
    render(<JobList jobs={[]} loading={false} error={null} onRefresh={mockOnRefresh} />);
    
    expect(screen.getByText(/No jobs have been scheduled yet/i)).toBeInTheDocument();
  });
  
  test('calls delete function when delete button is clicked', async () => {
    render(<JobList jobs={mockJobs} loading={false} error={null} onRefresh={mockOnRefresh} />);
    
    const deleteButtons = screen.getAllByTitle('Delete Job');
    fireEvent.click(deleteButtons[0]);
    
    await waitFor(() => {
      expect(deleteJob).toHaveBeenCalledWith('123');
      expect(require('react-toastify').toast.success).toHaveBeenCalled();
      expect(mockOnRefresh).toHaveBeenCalled();
    });
  });
  
  test('calls pause function when pause button is clicked', async () => {
    render(<JobList jobs={mockJobs} loading={false} error={null} onRefresh={mockOnRefresh} />);
    
    const pauseButtons = screen.getAllByTitle('Pause Job');
    fireEvent.click(pauseButtons[0]);
    
    await waitFor(() => {
      expect(pauseJob).toHaveBeenCalledWith('123');
      expect(require('react-toastify').toast.success).toHaveBeenCalled();
      expect(mockOnRefresh).toHaveBeenCalled();
    });
  });
  
  test('calls resume function when resume button is clicked', async () => {
    render(<JobList jobs={mockJobs} loading={false} error={null} onRefresh={mockOnRefresh} />);
    
    const resumeButtons = screen.getAllByTitle('Resume Job');
    fireEvent.click(resumeButtons[0]);
    
    await waitFor(() => {
      expect(resumeJob).toHaveBeenCalledWith('123');
      expect(require('react-toastify').toast.success).toHaveBeenCalled();
      expect(mockOnRefresh).toHaveBeenCalled();
    });
  });
  
  test('formats time with timezone correctly', () => {
    render(<JobList jobs={mockJobs} loading={false} error={null} onRefresh={mockOnRefresh} />);
    
    // This test will rely on the implementation of formatTimeWithTimezone
    // Since we're directly rendering the component and not mocking the function
    const timeElements = screen.getAllByText(/Apr 15, 2025 at/i);
    expect(timeElements.length).toBeGreaterThan(0);
  });
});