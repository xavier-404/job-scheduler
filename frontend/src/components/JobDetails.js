import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Paper,
  Grid,
  Divider,
  Chip,
  Box,
  CircularProgress,
  Alert,
  IconButton,
  Tooltip
} from '@mui/material';
import {
  Schedule as ScheduleIcon,
  Person as PersonIcon,
  CalendarToday as CalendarIcon,
  Refresh as RefreshIcon,
  ErrorOutline as ErrorIcon,
  CheckCircleOutline as SuccessIcon,
  AccessTime as TimeIcon,
  Close as CloseIcon
} from '@mui/icons-material';
import { formatInTimeZone } from 'date-fns-tz';
import { getJobById } from '../services/jobService';

// Helper function to get status color
const getStatusColor = (status) => {
  switch (status) {
    case 'SCHEDULED':
      return 'primary';
    case 'RUNNING':
      return 'warning';
    case 'COMPLETED_SUCCESS':
      return 'success';
    case 'COMPLETED_FAILURE':
      return 'error';
    default:
      return 'default';
  }
};

// Helper function to format time strings with timezone
const formatTimeWithTimezone = (timeString, timezone) => {
  if (!timeString) return 'Not scheduled';
  
  try {
    const date = new Date(timeString);
    return formatInTimeZone(date, timezone, "MMM d, yyyy 'at' h:mm a '('zzz')'");
  } catch (e) {
    console.error('Error formatting date:', e);
    return timeString;
  }
};

// Helper function to get a readable schedule description
const getScheduleDescription = (job) => {
  switch (job.scheduleType) {
    case 'IMMEDIATE':
      return 'Immediate execution';
    case 'ONE_TIME':
      return job.startTime
        ? `One time at ${formatTimeWithTimezone(job.startTime, job.timeZone)}`
        : 'One time';
    case 'RECURRING':
      if (job.cronExpression) {
        const cronParts = job.cronExpression.split(' ');
        if (cronParts.length >= 6) {
          const minute = cronParts[1];
          const hour = cronParts[2];

          let timeStr = '';
          if (hour.includes('/')) {
            const hourInterval = hour.split('/')[1];
            timeStr = `Every ${hourInterval} hour(s)`;
          } else {
            timeStr = `at ${hour}:${minute}`;
          }

          if (cronParts[5] !== '?' && cronParts[5] !== '*') {
            const days = cronParts[5].split(',');
            const dayMap = {
              '1': 'Monday', '2': 'Tuesday', '3': 'Wednesday',
              '4': 'Thursday', '5': 'Friday', '6': 'Saturday', '7': 'Sunday'
            };
            const dayNames = days.map(dayNum => dayMap[dayNum] || dayNum).join(', ');
            return `Recurring weekly on ${dayNames} ${timeStr}`;
          } else if (cronParts[3] !== '?' && cronParts[3] !== '*') {
            return `Recurring monthly on date(s) ${cronParts[3]} ${timeStr}`;
          } else {
            return `Recurring daily ${timeStr}`;
          }
        }
        return `Recurring (${job.cronExpression})`;
      }
      return 'Recurring';
    default:
      return `${job.scheduleType}`;
  }
};

/**
 * JobDetails component displays detailed information about a job in a dialog.
 * 
 * @param {Object} props - The component props
 * @param {boolean} props.open - Whether the dialog is open
 * @param {function} props.onClose - Function to call when dialog is closed
 * @param {string} props.jobId - The ID of the job to display details for
 */
const JobDetails = ({ open, onClose, jobId }) => {
  const [job, setJob] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshing, setRefreshing] = useState(false);

  // Load job details when dialog opens or when jobId changes
  useEffect(() => {
    if (open && jobId) {
      loadJobDetails();
    }
  }, [open, jobId]);

  // Function to load job details from the API
  const loadJobDetails = async () => {
    if (!jobId) return;
    
    setLoading(true);
    setError(null);
    
    try {
      const data = await getJobById(jobId);
      setJob(data);
    } catch (err) {
      console.error('Error loading job details:', err);
      setError('Failed to load job details. Please try again.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  // Function to refresh job details
  const handleRefresh = () => {
    setRefreshing(true);
    loadJobDetails();
  };

  return (
    <Dialog 
      open={open} 
      onClose={onClose} 
      maxWidth="md" 
      fullWidth
      aria-labelledby="job-details-dialog-title"
    >
      <DialogTitle id="job-details-dialog-title">
        <Box display="flex" justifyContent="space-between" alignItems="center">
          <Typography variant="h6">Job Details</Typography>
          <Box>
            <Tooltip title="Refresh job details">
              <IconButton 
                color="primary" 
                onClick={handleRefresh} 
                disabled={loading || refreshing}
                size="small"
              >
                <RefreshIcon />
              </IconButton>
            </Tooltip>
            <IconButton
              onClick={onClose}
              size="small"
              aria-label="close"
            >
              <CloseIcon />
            </IconButton>
          </Box>
        </Box>
      </DialogTitle>

      <DialogContent dividers>
        {loading ? (
          <Box display="flex" justifyContent="center" alignItems="center" height="300px">
            <CircularProgress />
          </Box>
        ) : error ? (
          <Alert severity="error" action={
            <Button color="inherit" size="small" onClick={handleRefresh}>
              Retry
            </Button>
          }>
            {error}
          </Alert>
        ) : job ? (
          <Box>
            {/* Overview Section */}
            <Paper elevation={1} sx={{ p: 2, mb: 3 }}>
              <Grid container spacing={2}>
                <Grid item xs={12} md={7}>
                  <Typography variant="subtitle2" color="textSecondary">Client ID</Typography>
                  <Typography variant="h6" gutterBottom>
                    <Box display="flex" alignItems="center">
                      <PersonIcon fontSize="small" sx={{ mr: 1 }} />
                      {job.clientId}
                    </Box>
                  </Typography>
                </Grid>
                <Grid item xs={12} md={5}>
                  <Typography variant="subtitle2" color="textSecondary">Status</Typography>
                  <Chip
                    icon={job.status === 'COMPLETED_SUCCESS' ? <SuccessIcon /> : 
                          job.status === 'COMPLETED_FAILURE' ? <ErrorIcon /> : 
                          <ScheduleIcon />}
                    label={job.status.replace('_', ' ')}
                    color={getStatusColor(job.status)}
                    sx={{ fontWeight: 'bold' }}
                  />
                </Grid>
                <Grid item xs={12}>
                  <Divider sx={{ my: 1 }} />
                </Grid>
                <Grid item xs={12} md={7}>
                  <Typography variant="subtitle2" color="textSecondary">Schedule Type</Typography>
                  <Typography variant="body1" gutterBottom>
                    <Box display="flex" alignItems="center">
                      <CalendarIcon fontSize="small" sx={{ mr: 1 }} />
                      {getScheduleDescription(job)}
                    </Box>
                  </Typography>
                </Grid>
                <Grid item xs={12} md={5}>
                  <Typography variant="subtitle2" color="textSecondary">Timezone</Typography>
                  <Chip
                    icon={<TimeIcon />}
                    label={job.timeZone}
                    variant="outlined"
                    size="small"
                  />
                </Grid>
              </Grid>
            </Paper>

            {/* Timing Details */}
            <Paper elevation={1} sx={{ p: 2, mb: 3 }}>
              <Typography variant="h6" gutterBottom>Timing Details</Typography>
              <Grid container spacing={2}>
                {job.startTime && (
                  <Grid item xs={12} md={6}>
                    <Typography variant="subtitle2" color="textSecondary">Start Time</Typography>
                    <Typography variant="body2">{formatTimeWithTimezone(job.startTime, job.timeZone)}</Typography>
                  </Grid>
                )}
                
                {job.nextFireTime && (
                  <Grid item xs={12} md={6}>
                    <Typography variant="subtitle2" color="textSecondary">Next Execution</Typography>
                    <Typography variant="body2">{formatTimeWithTimezone(job.nextFireTime, job.timeZone)}</Typography>
                  </Grid>
                )}
                
                {job.cronExpression && (
                  <Grid item xs={12}>
                    <Typography variant="subtitle2" color="textSecondary">Cron Expression</Typography>
                    <Typography variant="body2" fontFamily="monospace" bgcolor="#f5f5f5" p={1} borderRadius="4px">
                      {job.cronExpression}
                    </Typography>
                  </Grid>
                )}
              </Grid>
            </Paper>

            {/* Creation and Update Info */}
            <Paper elevation={1} sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>Job History</Typography>
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle2" color="textSecondary">Created At</Typography>
                  <Typography variant="body2">{formatTimeWithTimezone(job.createdAt, job.timeZone)}</Typography>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle2" color="textSecondary">Last Updated</Typography>
                  <Typography variant="body2">{formatTimeWithTimezone(job.updatedAt, job.timeZone)}</Typography>
                </Grid>
                {job.error && (
                  <Grid item xs={12}>
                    <Alert severity="error">
                      <Typography variant="subtitle2">Error Details</Typography>
                      {job.error}
                    </Alert>
                  </Grid>
                )}
              </Grid>
            </Paper>
          </Box>
        ) : (
          <Alert severity="info">No job details available</Alert>
        )}
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose} color="primary">
          Close
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default JobDetails;