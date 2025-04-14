import React, { useState, useEffect } from 'react';
import { Container, Typography, Box, CssBaseline, Paper, AppBar, Toolbar } from '@mui/material';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

import JobForm from './components/JobForm';
import JobList from './components/JobList';
import { fetchJobs } from './services/jobService';

// Create a theme
const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
  },
});

/**
 * Main application component.
 */
function App() {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Load jobs when the component mounts
  useEffect(() => {
    loadJobs();
    
    // Set up polling for job updates every 10 seconds
    const intervalId = setInterval(() => {
      loadJobs(false); // Silent reload (no loading indicator)
    }, 10000);
    
    // Clean up the interval when the component unmounts
    return () => clearInterval(intervalId);
  }, []);

  /**
   * Loads jobs from the API.
   * @param {boolean} showLoading - Whether to show a loading indicator
   */
  const loadJobs = async (showLoading = true) => {
    if (showLoading) {
      setLoading(true);
    }
    
    try {
      const data = await fetchJobs();
      setJobs(data);
      setError(null);
    } catch (err) {
      console.error('Error loading jobs:', err);
      setError('Failed to load jobs. Please try again later.');
      toast.error('Failed to load jobs. Please try again later.');
    } finally {
      if (showLoading) {
        setLoading(false);
      }
    }
  };

  /**
   * Handles when a job is added by reloading the job list.
   */
  const handleJobAdded = () => {
    toast.success('Job scheduled successfully!');
    loadJobs();
  };

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <ToastContainer position="top-right" autoClose={5000} />
      
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            Job Scheduler
          </Typography>
        </Toolbar>
      </AppBar>
      
      <Container maxWidth="lg">
        <Box sx={{ my: 4 }}>
          <Paper elevation={3} sx={{ p: 3, mb: 4 }}>
            <Typography variant="h4" component="h1" gutterBottom>
              Schedule New Job
            </Typography>
            <JobForm onJobAdded={handleJobAdded} />
          </Paper>
          
          <Paper elevation={3} sx={{ p: 3 }}>
            <Typography variant="h4" component="h2" gutterBottom>
              Job Status Dashboard
            </Typography>
            <JobList jobs={jobs} loading={loading} error={error} onRefresh={loadJobs} />
          </Paper>
        </Box>
      </Container>
    </ThemeProvider>
  );
}

export default App;