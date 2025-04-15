// Save as: frontend/src/tests/unit/JobForm.test.js

import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import JobForm from '../../components/JobForm';
import { createJob } from '../../services/jobService';
import * as dateTimeHelpers from '../../utils/dateTimeHelpers';

// Mock the job service
jest.mock('../../services/jobService');

// Mock toast notifications
jest.mock('react-toastify', () => ({
  toast: {
    success: jest.fn(),
    error: jest.fn()
  }
}));

// Mock date-time helpers
jest.mock('../../utils/dateTimeHelpers', () => ({
  formatInTimezone: jest.fn(() => 'Formatted Date'),
  formatForServer: jest.fn(() => '2025-04-15T14:30:00'),
  createFutureDate: jest.fn(() => new Date('2025-04-15T12:30:00Z')),
  getCurrentTimeInTimezone: jest.fn(() => 'Current Time'),
  isDateInPast: jest.fn(() => false),
  DEFAULT_TIMEZONE: 'Asia/Kolkata'
}));

describe('JobForm Component', () => {
  const mockOnJobAdded = jest.fn();
  
  beforeEach(() => {
    jest.clearAllMocks();
    createJob.mockResolvedValue({ id: '123', status: 'SCHEDULED' });
  });
  
  test('renders form with all required fields', () => {
    render(<JobForm onJobAdded={mockOnJobAdded} />);
    
    expect(screen.getByLabelText(/client id/i)).toBeInTheDocument();
    expect(screen.getByText(/Time Zone/i)).toBeInTheDocument();
    expect(screen.getByText(/Execute Immediately/i)).toBeInTheDocument();
    expect(screen.getByText(/Schedule Job/i)).toBeInTheDocument();
  });
  
  test('validates required fields', async () => {
    render(<JobForm onJobAdded={mockOnJobAdded} />);
    
    // Submit with empty form
    fireEvent.click(screen.getByText(/Schedule Job/i));
    
    await waitFor(() => {
      expect(screen.getByText(/Client ID is required/i)).toBeInTheDocument();
    });
    
    expect(createJob).not.toHaveBeenCalled();
  });
  
  test('submits form with correct one-time job data', async () => {
    render(<JobForm onJobAdded={mockOnJobAdded} />);
    
    // Fill the form
    fireEvent.change(screen.getByLabelText(/client id/i), { 
      target: { value: 'TEST_CLIENT' } 
    });
    
    // Ensure not immediate execution
    const immediateCheckbox = screen.getByLabelText(/Execute Immediately/i);
    if (immediateCheckbox.checked) {
      fireEvent.click(immediateCheckbox);
    }
    
    // Submit the form
    fireEvent.click(screen.getByText(/Schedule Job/i));
    
    await waitFor(() => {
      expect(createJob).toHaveBeenCalledWith(
        expect.objectContaining({
          clientId: 'TEST_CLIENT',
          scheduleType: 'ONE_TIME',
          timeZone: expect.any(String),
          startTime: expect.any(String)
        })
      );
      
      expect(mockOnJobAdded).toHaveBeenCalled();
    });
  });
  
  test('submits form with immediate execution', async () => {
    render(<JobForm onJobAdded={mockOnJobAdded} />);
    
    // Fill the form
    fireEvent.change(screen.getByLabelText(/client id/i), { 
      target: { value: 'TEST_CLIENT' } 
    });
    
    // Set to immediate execution
    fireEvent.click(screen.getByLabelText(/Execute Immediately/i));
    
    // Submit the form
    fireEvent.click(screen.getByText(/Schedule Job/i));
    
    await waitFor(() => {
      expect(createJob).toHaveBeenCalledWith(
        expect.objectContaining({
          clientId: 'TEST_CLIENT',
          scheduleType: 'IMMEDIATE',
          timeZone: expect.any(String)
        })
      );
      
      expect(mockOnJobAdded).toHaveBeenCalled();
    });
  });
  
  test('shows error notification when job creation fails', async () => {
    // Mock a failure
    createJob.mockRejectedValue({ 
      response: { data: { message: 'Server error' } } 
    });
    
    render(<JobForm onJobAdded={mockOnJobAdded} />);
    
    // Fill the form
    fireEvent.change(screen.getByLabelText(/client id/i), { 
      target: { value: 'TEST_CLIENT' } 
    });
    
    // Submit the form
    fireEvent.click(screen.getByText(/Schedule Job/i));
    
    await waitFor(() => {
      expect(createJob).toHaveBeenCalled();
      // Toast error should be called
      expect(require('react-toastify').toast.error).toHaveBeenCalledWith(
        expect.stringContaining('Server error')
      );
      
      expect(mockOnJobAdded).not.toHaveBeenCalled();
    });
  });
});