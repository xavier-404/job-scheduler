// Save as: frontend/src/tests/integration/App.test.js

import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import App from '../../App';
import axios from 'axios';

// Mock axios to simulate API calls
jest.mock('axios');

describe('App Integration Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    
    // Mock successful job fetch
    axios.get.mockResolvedValue({
      data: [
        {
          id: '123',
          clientId: 'CLIENT_1',
          scheduleType: 'ONE_TIME',
          startTime: '2025-04-15T14:30:00',
          nextFireTime: '2025-04-15T14:30:00',
          timeZone: 'UTC',
          status: 'SCHEDULED'
        }
      ]
    });
    
    // Mock successful job creation
    axios.post.mockResolvedValue({
      data: {
        id: '456',
        clientId: 'NEW_CLIENT',
        scheduleType: 'ONE_TIME',
        startTime: '2025-04-15T15:00:00',
        nextFireTime: '2025-04-15T15:00:00',
        timeZone: 'UTC',
        status: 'SCHEDULED'
      }
    });
  });
  
  test('full job scheduling workflow', async () => {
    render(<App />);
    
    // 1. Initially loads and displays jobs
    await waitFor(() => {
      expect(axios.get).toHaveBeenCalledWith('/api/jobs');
      expect(screen.getByText('CLIENT_1')).toBeInTheDocument();
    });
    
    // 2. Fill out the job form
    const clientIdInput = screen.getByLabelText(/client id/i);
    fireEvent.change(clientIdInput, { target: { value: 'NEW_CLIENT' } });
    
    // 3. Select timezone from dropdown
    const timezoneSelect = screen.getByLabelText(/time zone/i);
    fireEvent.change(timezoneSelect, { target: { value: 'UTC' } });
    
    // 4. Schedule the job
    const scheduleButton = screen.getByText(/schedule job/i);
    fireEvent.click(scheduleButton);
    
    // 5. Verify job is created
    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith('/api/jobs', expect.objectContaining({
        clientId: 'NEW_CLIENT',
        timeZone: 'UTC'
      }));
    });
    
    // 6. Verify jobs are refreshed
    await waitFor(() => {
      // This should be called at least twice - once on initial load and once after job creation
      expect(axios.get).toHaveBeenCalledTimes(2);
    });
  });
  
  test('handles error scenarios gracefully', async () => {
    // Mock an API error
    axios.get.mockRejectedValueOnce(new Error('Failed to fetch jobs'));
    
    render(<App />);
    
    // Verify error state
    await waitFor(() => {
      expect(axios.get).toHaveBeenCalledWith('/api/jobs');
      expect(screen.getByText(/failed to load jobs/i)).toBeInTheDocument();
    });
    
    // Test refresh ability
    const refreshButton = screen.getByText(/refresh/i);
    
    // Reset mock to succeed on refresh
    axios.get.mockResolvedValueOnce({
      data: [
        {
          id: '123',
          clientId: 'CLIENT_1',
          scheduleType: 'ONE_TIME',
          timeZone: 'UTC',
          status: 'SCHEDULED'
        }
      ]
    });
    
    fireEvent.click(refreshButton);
    
    // Verify refresh works
    await waitFor(() => {
      expect(axios.get).toHaveBeenCalledTimes(2);
      expect(screen.getByText('CLIENT_1')).toBeInTheDocument();
    });
  });
});