import React, { useState, useEffect, useMemo } from 'react';
import {
  Container, 
  Typography, 
  Box, 
  CssBaseline, 
  Paper, 
  AppBar, 
  Toolbar,
  Switch,
  FormControlLabel,
  IconButton,
  useMediaQuery
} from '@mui/material';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { ToastContainer, toast } from 'react-toastify';
import Brightness4Icon from '@mui/icons-material/Brightness4';
import Brightness7Icon from '@mui/icons-material/Brightness7';
import 'react-toastify/dist/ReactToastify.css';

import JobForm from './components/JobForm';
import JobList from './components/JobList';
import { fetchJobs } from './services/jobService';

/**
 * Main application component.
 */
function App() {
  // Check if user prefers dark mode
  const prefersDarkMode = useMediaQuery('(prefers-color-scheme: dark)');
  
  // Initialize state from localStorage or system preference
  const [darkMode, setDarkMode] = useState(() => {
    const savedMode = localStorage.getItem('darkMode');
    return savedMode !== null ? JSON.parse(savedMode) : prefersDarkMode;
  });
  
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Create a theme instance based on the dark mode preference
  const theme = useMemo(() => 
    createTheme({
      palette: {
        mode: darkMode ? 'dark' : 'light',
        primary: {
          main: '#1976d2',
        },
        secondary: {
          main: darkMode ? '#f48fb1' : '#dc004e',
        },
        background: {
          default: darkMode ? '#303030' : '#f5f5f5',
          paper: darkMode ? '#424242' : '#fff',
        },
      },
    }),
    [darkMode]
  );

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

  // Save dark mode preference to localStorage when it changes
  useEffect(() => {
    localStorage.setItem('darkMode', JSON.stringify(darkMode));
  }, [darkMode]);

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

  /**
   * Toggles between light and dark mode.
   */
  const handleToggleDarkMode = () => {
    setDarkMode(prevMode => !prevMode);
  };

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <ToastContainer 
        position="top-right" 
        autoClose={5000}
        theme={darkMode ? 'dark' : 'light'} 
      />
      
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            Job Scheduler
          </Typography>
          <IconButton 
            color="inherit" 
            onClick={handleToggleDarkMode}
            sx={{ mr: 1 }}
            aria-label="toggle theme"
          >
            {darkMode ? <Brightness7Icon /> : <Brightness4Icon />}
          </IconButton>
          <FormControlLabel
            control={
              <Switch 
                checked={darkMode} 
                onChange={handleToggleDarkMode} 
                color="default" 
              />
            }
            label={darkMode ? "Dark" : "Light"}
          />
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