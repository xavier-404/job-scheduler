import React from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Chip,
  Button,
  Typography,
  Box,
  CircularProgress,
  Alert,
  Tooltip
} from '@mui/material';
import { formatInTimeZone } from 'date-fns-tz';
import {
  Refresh as RefreshIcon,
  Delete as DeleteIcon,
  Pause as PauseIcon,
  PlayArrow as PlayArrowIcon,
  AccessTime as AccessTimeIcon 
} from '@mui/icons-material';
import { toast } from 'react-toastify';
import { deleteJob, pauseJob, resumeJob } from '../services/jobService';

/**
 * Returns a color based on the job status.
 * 
 * @param {string} status - The job status
 * @returns {string} The color
 */
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

/**
 * Formats time with timezone information.
 * 
 * @param {string} timeString - The time string to format
 * @param {string} timezone - The timezone
 * @returns {string} The formatted time string with timezone
 */
const formatTimeWithTimezone = (timeString, timezone) => {
  if (!timeString) return 'Not scheduled';
  
  try {
    // Parse the ISO date string
    const date = new Date(timeString);
    
    // Format the date in the specific timezone using date-fns-tz
    // Make sure the timezone is explicitly displayed
    return formatInTimeZone(date, timezone, "MMM d, yyyy 'at' h:mm a '('zzz')'");
  } catch (e) {
    console.error('Error formatting date:', e);
    return timeString;
  }
};

/**
 * Gets the current time in the specified timezone.
 * 
 * @param {string} timezone - The timezone
 * @returns {string} The current time in the specified timezone
 */
const getCurrentTimeInTimezone = (timezone) => {
  try {
    const now = new Date();
    return formatInTimeZone(now, timezone, "MMM d, yyyy 'at' h:mm a '('zzz')'");
  } catch (e) {
    console.error('Error getting current time in timezone:', e);
    return 'Invalid timezone';
  }
};

/**
 * Returns a user-friendly schedule description.
 * 
 * @param {Object} job - The job object
 * @returns {string} The schedule description
 */
const getScheduleDescription = (job) => {
  switch (job.scheduleType) {
    case 'IMMEDIATE':
      return 'Immediate execution';
    case 'ONE_TIME':
      return job.startTime
        ? `One time at ${formatInTimeZone(new Date(job.startTime), job.timeZone, "MMM d, yyyy 'at' h:mm a")}`
        : 'One time';
    case 'RECURRING':
      // Extract time information from the cron expression if available
      if (job.cronExpression) {
        const cronParts = job.cronExpression.split(' ');
        // Cron format: second minute hour day month day-of-week
        // If we have at least 6 parts and it's a standard format
        if (cronParts.length >= 6) {
          const minute = cronParts[1];
          const hour = cronParts[2];
          
          // Create a descriptive string based on the cron pattern
          let timeStr = '';
          if (hour.includes('/')) {
            // Hourly pattern
            const hourInterval = hour.split('/')[1];
            timeStr = `Every ${hourInterval} hour(s)`;
          } else {
            // Regular time pattern (e.g., "At 14:30")
            timeStr = `at ${hour}:${minute}`;
          }
          
          // Check for day of week or day of month patterns
          if (cronParts[5] !== '?' && cronParts[5] !== '*') {
            // Weekly pattern
            const days = cronParts[5].split(',');
            const dayNames = days.map(dayNum => {
              const dayMap = {
                '1': 'Monday', '2': 'Tuesday', '3': 'Wednesday', 
                '4': 'Thursday', '5': 'Friday', '6': 'Saturday', '7': 'Sunday'
              };
              return dayMap[dayNum] || dayNum;
            }).join(', ');
            return `Recurring weekly on ${dayNames} ${timeStr}`;
          } else if (cronParts[3] !== '?' && cronParts[3] !== '*') {
            // Monthly pattern
            return `Recurring monthly on date(s) ${cronParts[3]} ${timeStr}`;
          } else {
            // Daily pattern
            return `Recurring daily ${timeStr}`;
          }
        }
        return `Recurring (${job.cronExpression})`;
      }
      return `Recurring`;
    default:
      return `${job.scheduleType}`;
  }
};

/**
 * Component that displays a list of jobs in a table.
 * 
 * @param {Object} props - The component props
 * @param {Array} props.jobs - The list of jobs to display
 * @param {boolean} props.loading - Whether the jobs are loading
 * @param {string} props.error - Error message if any
 * @param {Function} props.onRefresh - Callback function to refresh the job list
 */
const JobList = ({ jobs, loading, error, onRefresh }) => {
  const [deleteLoading, setDeleteLoading] = React.useState(null);
  const [actionLoading, setActionLoading] = React.useState(null);
  const [showTimezoneInfo, setShowTimezoneInfo] = React.useState(false);

  /**
   * Handles job deletion.
   * 
   * @param {string} id - The job ID
   * @param {string} clientId - The client ID
   */
  const handleDelete = async (id, clientId) => {
    try {
      setDeleteLoading(id);
      await deleteJob(id);
      toast.success(`Job for client ${clientId} deleted successfully`);
      onRefresh();
    } catch (error) {
      console.error(`Error deleting job ${id}:`, error);
      toast.error('Failed to delete job. Please try again.');
    } finally {
      setDeleteLoading(null);
    }
  };

  /**
   * Handles pausing a job.
   * 
   * @param {string} id - The job ID
   */
  const handlePause = async (id) => {
    try {
      setActionLoading(id);
      await pauseJob(id);
      toast.success('Job paused successfully');
      onRefresh();
    } catch (error) {
      console.error(`Error pausing job ${id}:`, error);
      toast.error('Failed to pause job. Please try again.');
    } finally {
      setActionLoading(null);
    }
  };

  /**
   * Handles resuming a job.
   * 
   * @param {string} id - The job ID
   */
  const handleResume = async (id) => {
    try {
      setActionLoading(id);
      await resumeJob(id);
      toast.success('Job resumed successfully');
      onRefresh();
    } catch (error) {
      console.error(`Error resuming job ${id}:`, error);
      toast.error('Failed to resume job. Please try again.');
    } finally {
      setActionLoading(null);
    }
  };

  // If there's an error, display it
  if (error) {
    return (
      <Alert severity="error" sx={{ mb: 2 }}>
        {error}
        <Button
          size="small"
          startIcon={<RefreshIcon />}
          onClick={onRefresh}
          sx={{ ml: 2 }}
        >
          Retry
        </Button>
      </Alert>
    );
  }

  // If loading and no jobs yet, show loading indicator
  if (loading && (!jobs || jobs.length === 0)) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
        <CircularProgress />
      </Box>
    );
  }

  // If no jobs, show empty state
  if (!jobs || jobs.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', p: 3 }}>
        <Typography variant="body1" color="text.secondary">
          No jobs have been scheduled yet.
        </Typography>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={onRefresh}
          sx={{ mt: 2 }}
        >
          Refresh
        </Button>
      </Box>
    );
  }

  return (
    <>
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={onRefresh}
          disabled={loading}
        >
          {loading ? 'Refreshing...' : 'Refresh'}
        </Button>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Client ID</TableCell>
              <TableCell>Schedule</TableCell>
              <TableCell>Next Run</TableCell>
              <TableCell>Timezone</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {jobs.map((job) => (
              <TableRow key={job.id}>
                <TableCell>{job.clientId}</TableCell>
                <TableCell>{getScheduleDescription(job)}</TableCell>
                <TableCell>
                  {job.nextFireTime 
                    ? formatTimeWithTimezone(job.nextFireTime, job.timeZone)
                    : job.scheduleType === 'RECURRING' 
                      ? `Based on cron: ${job.cronExpression}`
                      : 'Not scheduled'}
                </TableCell>
                <TableCell>
                  <Tooltip title="All times are specific to this timezone">
                    <Chip 
                      icon={<AccessTimeIcon />} 
                      label={job.timeZone} 
                      variant="outlined" 
                      size="small"
                    />
                  </Tooltip>
                  {showTimezoneInfo && (
                    <Alert
                      severity="info"
                      icon={<AccessTimeIcon />}
                      action={
                        <Button
                          color="inherit"
                          size="small"
                          onClick={() => setShowTimezoneInfo(!showTimezoneInfo)}
                        >
                          {showTimezoneInfo ? 'Hide Info' : 'More Info'}
                        </Button>
                      }
                    >
                      Current time in selected timezone: <strong>{getCurrentTimeInTimezone(job.timeZone)}</strong>
                    </Alert>
                  )}
                </TableCell>
                <TableCell>
                  <Chip
                    label={job.status.replace('_', ' ')}
                    color={getStatusColor(job.status)}
                    size="small"
                  />
                </TableCell>
                <TableCell>
                  {actionLoading === job.id ? (
                    <CircularProgress size={24} />
                  ) : (
                    <>
                      <IconButton
                        color="default"
                        onClick={() => handlePause(job.id)}
                        title="Pause Job"
                      >
                        <PauseIcon />
                      </IconButton>
                      <IconButton
                        color="primary"
                        onClick={() => handleResume(job.id)}
                        title="Resume Job"
                      >
                        <PlayArrowIcon />
                      </IconButton>
                    </>
                  )}
                  {deleteLoading === job.id ? (
                    <CircularProgress size={24} />
                  ) : (
                    <IconButton
                      color="error"
                      onClick={() => handleDelete(job.id, job.clientId)}
                      title="Delete Job"
                    >
                      <DeleteIcon />
                    </IconButton>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
};

export default JobList;