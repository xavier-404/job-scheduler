import React, { useState, useEffect } from 'react';
import {
  Grid,
  TextField,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormControlLabel,
  Checkbox,
  Box,
  CircularProgress,
  FormHelperText,
  Collapse,
  FormGroup,
  Typography,
  Divider,
  Alert
} from '@mui/material';
import {
  DEFAULT_TIMEZONE,  // Add this import
  formatInTimezone,
  formatForServer,
  createFutureDate,
  getCurrentTimeInTimezone,
  isDateInPast
} from '../utils/dateTimeHelpers';

import { TimePicker } from '@mui/x-date-pickers/TimePicker';
import { DateTimePicker } from '@mui/x-date-pickers/DateTimePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import ScheduleIcon from '@mui/icons-material/Schedule';
import { toast } from 'react-toastify';
import { createJob } from '../services/jobService';


// List of all available timezones, organized by region for better user experience
const TIMEZONE_GROUPS = [
  {
    region: 'UTC',
    timezones: ['UTC']
  },
  {
    region: 'North America',
    timezones: [
      'America/New_York',    // Eastern Time
      'America/Chicago',     // Central Time
      'America/Denver',      // Mountain Time
      'America/Los_Angeles', // Pacific Time
      'America/Anchorage',   // Alaska Time
      'America/Adak',        // Aleutian Time
      'Pacific/Honolulu',    // Hawaii Time
    ]
  },
  {
    region: 'South America',
    timezones: [
      'America/Sao_Paulo',   // Brazil Time
      'America/Argentina/Buenos_Aires', // Argentina Time
      'America/Santiago',    // Chile Time
      'America/Bogota'       // Colombia Time
    ]
  },
  {
    region: 'Europe',
    timezones: [
      'Europe/London',       // GMT/BST
      'Europe/Paris',        // Central European Time
      'Europe/Berlin',       // Central European Time
      'Europe/Moscow',       // Moscow Time
      'Europe/Athens',       // Eastern European Time
      'Europe/Istanbul'      // Turkey Time
    ]
  },
  {
    region: 'Asia',
    timezones: [
      'Asia/Dubai',          // Gulf Standard Time
      'Asia/Kolkata',        // Indian Standard Time
      'Asia/Tokyo',          // Japan Standard Time
      'Asia/Shanghai',       // China Standard Time
      'Asia/Singapore',      // Singapore Time
      'Asia/Seoul'           // Korea Standard Time
    ]
  },
  {
    region: 'Australia & Pacific',
    timezones: [
      'Australia/Sydney',    // Australian Eastern Time
      'Australia/Perth',     // Australian Western Time
      'Australia/Brisbane',  // Australian Eastern Time (No DST)
      'Pacific/Auckland',    // New Zealand Time
      'Pacific/Fiji'         // Fiji Time
    ]
  },
  {
    region: 'Africa',
    timezones: [
      'Africa/Cairo',        // Eastern European Time
      'Africa/Johannesburg', // South Africa Standard Time
      'Africa/Lagos',        // West Africa Time
      'Africa/Nairobi'       // East Africa Time
    ]
  }
];

// Get timezone names in a readable format
const formatTimezoneName = (timezone) => {
  const parts = timezone.split('/');
  let name = parts[parts.length - 1].replace(/_/g, ' ');

  // Special case for Buenos Aires and similar
  if (parts.length > 2) {
    name = parts[parts.length - 1].replace(/_/g, ' ');
  }

  try {
    const offset = new Date().toLocaleString('en-US', { timeZone: timezone, timeZoneName: 'short' }).split(' ').pop();
    return `${name} (${offset})`;
  } catch (e) {
    console.error('Error formatting timezone name:', e);
    return timezone;
  }
};

const DAYS_OF_WEEK = [
  { value: 1, label: 'Monday' },
  { value: 2, label: 'Tuesday' },
  { value: 3, label: 'Wednesday' },
  { value: 4, label: 'Thursday' },
  { value: 5, label: 'Friday' },
  { value: 6, label: 'Saturday' },
  { value: 7, label: 'Sunday' },
];

const DAYS_OF_MONTH = Array.from({ length: 31 }, (_, i) => ({ value: i + 1, label: `${i + 1}` }));

/**
 * Component for creating new jobs.
 * Includes comprehensive timezone handling and validation.
 * 
 * @param {Object} props - The component props
 * @param {Function} props.onJobAdded - Callback function called when a job is successfully added
 */
const JobForm = ({ onJobAdded }) => {
  // Get browser's timezone as default or use IST as fallback
  const getUserTimezone = () => {
    try {
      // Always return "Asia/Kolkata" as the default timezone
      return "Asia/Kolkata";
    } catch (e) {
      console.warn('Could not set default timezone:', e);
      return "Asia/Kolkata";
    }
  };

  // Then modify the useState initialization to use the helper functions:
  const [formData, setFormData] = useState({
    clientId: '',
    scheduleType: 'ONE_TIME',
    startTime: createFutureDate(30), // 30 minutes from now
    timeZone: DEFAULT_TIMEZONE,
    immediateExecution: false,
    cronExpression: '',
    daysOfWeek: [],
    daysOfMonth: [],
    hourlyInterval: 1,
    recurringTime: new Date(new Date().setHours(12, 0, 0, 0)), // Default to noon
  });


  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState({});
  const [currentTime, setCurrentTime] = useState(new Date());
  const [showTimezoneInfo, setShowTimezoneInfo] = useState(false);

  // Update current time every minute
  useEffect(() => {
    const timerId = setInterval(() => {
      setCurrentTime(new Date());
    }, 60000);

    return () => clearInterval(timerId);
  }, []);

  // Get formatted current time in selected timezone
  // Replace getCurrentTimeInSelectedTimezone with:
  const getCurrentTimeInSelectedTimezone = () => {
    return getCurrentTimeInTimezone(formData.timeZone);
  };

  /**
   * Handles changes to form inputs.
   * 
   * @param {Object} e - The event object
   */
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });

    // Clear any error for this field
    if (errors[name]) {
      setErrors({ ...errors, [name]: null });
    }
  };

  /**
   * Handles changes to the startTime date/time picker.
   * 
   * @param {Date} date - The selected date
   */
  const handleDateChange = (date) => {
    setFormData({ ...formData, startTime: date });

    // Clear any error for this field
    if (errors.startTime) {
      setErrors({ ...errors, startTime: null });
    }
  };

  /**
   * Handles changes to the recurring time picker.
   * 
   * @param {Date} time - The selected time
   */
  const handleRecurringTimeChange = (time) => {
    setFormData({ ...formData, recurringTime: time });
  };

  /**
   * Handles changes to the immediate execution checkbox.
   * 
   * @param {Object} e - The event object
   */
  const handleImmediateExecutionChange = (e) => {
    setFormData({ ...formData, immediateExecution: e.target.checked });
  };

  /**
   * Handles changes to the days of week checkboxes.
   * 
   * @param {number} day - The day value
   */
  const handleDayOfWeekChange = (day) => {
    const daysOfWeek = formData.daysOfWeek.includes(day)
      ? formData.daysOfWeek.filter(d => d !== day)
      : [...formData.daysOfWeek, day];

    setFormData({ ...formData, daysOfWeek });
  };

  /**
   * Handles changes to the days of month checkboxes.
   * 
   * @param {number} day - The day value
   */
  const handleDayOfMonthChange = (day) => {
    const daysOfMonth = formData.daysOfMonth.includes(day)
      ? formData.daysOfMonth.filter(d => d !== day)
      : [...formData.daysOfMonth, day];

    setFormData({ ...formData, daysOfMonth });
  };

  /**
   * Validates the form and returns any errors.
   * 
   * @returns {Object} The validation errors
   */
  const validateForm = () => {
    const newErrors = {};
  
    if (!formData.clientId.trim()) {
      newErrors.clientId = 'Client ID is required';
    }
  
    if (formData.scheduleType === 'ONE_TIME' && !formData.immediateExecution && !formData.startTime) {
      newErrors.startTime = 'Start time is required for one-time jobs';
    }
  
    // Validate that startTime is in the future for ONE_TIME jobs
    // This is critical - we need to check if the time is in the past in the SELECTED timezone
    if (formData.scheduleType === 'ONE_TIME' && !formData.immediateExecution && formData.startTime) {
      console.log(`[TIMEZONE-DEBUG] Validating date ${formData.startTime} in timezone ${formData.timeZone}`);
      
      if (isDateInPast(formData.startTime, formData.timeZone)) {
        newErrors.startTime = `Start time must be in the future in ${formData.timeZone} timezone`;
      }
    }
    if (formData.scheduleType === 'RECURRING') {
      if (formData.daysOfWeek.length === 0 && formData.daysOfMonth.length === 0 && !formData.hourlyInterval) {
        newErrors.recurrence = 'Please select a recurrence pattern';
      }
    }

    return newErrors;
  };

  /**
   * Handles form submission.
   * 
   * @param {Object} e - The event object
   */
  const handleSubmit = async (e) => {
    e.preventDefault();

    // Validate form
    const validationErrors = validateForm();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    console.log(`[TIMEZONE-DEBUG] Submitting job with selected time: ${formData.startTime} in timezone ${formData.timeZone}`);

    setLoading(true);

    try {
      // Prepare job request
      const jobRequest = {
        clientId: formData.clientId,
        scheduleType: formData.immediateExecution ? 'IMMEDIATE' : formData.scheduleType,
        timeZone: formData.timeZone // Always send the selected timezone
      };

      if (formData.scheduleType === 'ONE_TIME' && !formData.immediateExecution) {
        // Format the date as an ISO string WITHOUT timezone info
        // The backend will interpret this in the context of the specified timezone
        const dateTimeString = formatForServer(formData.startTime);
        jobRequest.startTime = dateTimeString;

        console.log(`[TIMEZONE-DEBUG] Scheduling job for ${dateTimeString} in ${formData.timeZone}`);
      }
      else if (formData.scheduleType === 'RECURRING') {
        // Add recurrence fields
        if (formData.daysOfWeek.length > 0) {
          jobRequest.daysOfWeek = formData.daysOfWeek;
        } else if (formData.daysOfMonth.length > 0) {
          jobRequest.daysOfMonth = formData.daysOfMonth;
        } else if (formData.hourlyInterval) {
          jobRequest.hourlyInterval = parseInt(formData.hourlyInterval);
        }

        // Add the execution time for recurring jobs
        if (formData.recurringTime) {
          const hours = formData.recurringTime.getHours();
          const minutes = formData.recurringTime.getMinutes();
          jobRequest.recurringTimeHour = hours;
          jobRequest.recurringTimeMinute = minutes;
        }
      }

      // Create the job
      await createJob(jobRequest);

      // Reset form
      setFormData({
        clientId: '',
        scheduleType: 'ONE_TIME',
        startTime: createFutureDate(30),
        timeZone: formData.timeZone, // Preserve selected timezone
        immediateExecution: false,
        cronExpression: '',
        daysOfWeek: [],
        daysOfMonth: [],
        hourlyInterval: 1,
        recurringTime: new Date(new Date().setHours(12, 0, 0, 0))
      });

      // Notify parent component
      onJobAdded();
    } catch (error) {
      console.error('Error creating job:', error);

      if (error.response && error.response.data) {
        if (error.response.data.details) {
          // Set field-specific errors
          setErrors(error.response.data.details);
        } else {
          // Show a general error message
          toast.error(error.response.data.message || 'Failed to create job. Please try again.');
        }
      } else {
        toast.error('Failed to create job. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };
  return (
    <form onSubmit={handleSubmit}>
      <Grid container spacing={3}>

        {/* Current Time Indicator */}
        <Grid item xs={12}>
          <Alert
            severity="info"
            icon={<ScheduleIcon />}
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
            Current time in {formData.timeZone}: <strong>{getCurrentTimeInSelectedTimezone()}</strong>
          </Alert>
          <Collapse in={showTimezoneInfo}>
            <Box sx={{ mt: 2, p: 2, bgcolor: '#f5f5f5', borderRadius: 1 }}>
              <Typography variant="body2">
                <strong>Note about Timezones:</strong> All scheduled jobs will run according to the selected timezone,
                regardless of the server's location. Make sure to select the appropriate timezone for your scheduling needs.
              </Typography>
            </Box>
          </Collapse>
        </Grid>

        {/* Client ID */}
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            label="Client ID"
            name="clientId"
            value={formData.clientId}
            onChange={handleChange}
            error={!!errors.clientId}
            helperText={errors.clientId}
            disabled={loading}
            required
          />
        </Grid>

        {/* Time Zone */}
        <Grid item xs={12} md={6}>
          <FormControl fullWidth>
            <InputLabel>Time Zone</InputLabel>
            <Select
              name="timeZone"
              value={formData.timeZone}
              onChange={handleChange}
              label="Time Zone"
              disabled={loading}
            >
              {TIMEZONE_GROUPS.map((group) => [
                <MenuItem key={group.region} disabled divider sx={{ opacity: 0.7, fontWeight: 'bold' }}>
                  {group.region}
                </MenuItem>,
                ...group.timezones.map((timezone) => (
                  <MenuItem key={timezone} value={timezone}>
                    {formatTimezoneName(timezone)}
                  </MenuItem>
                ))
              ])}
            </Select>
            <FormHelperText>
              All job schedules will be based on this timezone
            </FormHelperText>
          </FormControl>
        </Grid>

        {/* Immediate Execution */}
        <Grid item xs={12}>
          <FormControlLabel
            control={
              <Checkbox
                checked={formData.immediateExecution}
                onChange={handleImmediateExecutionChange}
                disabled={loading}
                color="primary"
              />
            }
            label="Execute Immediately"
          />
        </Grid>

        {/* Schedule Type (only if not immediate) */}
        {!formData.immediateExecution && (
          <Grid item xs={12} md={6}>
            <FormControl fullWidth>
              <InputLabel>Schedule Type</InputLabel>
              <Select
                name="scheduleType"
                value={formData.scheduleType}
                onChange={handleChange}
                label="Schedule Type"
                disabled={loading}
              >
                <MenuItem value="ONE_TIME">One Time</MenuItem>
                <MenuItem value="RECURRING">Recurring</MenuItem>
              </Select>
            </FormControl>
          </Grid>
        )}

        {/* One-Time Schedule Options */}
        {!formData.immediateExecution && formData.scheduleType === 'ONE_TIME' && (
          <Grid item xs={12} md={6}>
            <LocalizationProvider dateAdapter={AdapterDateFns}>
              <DateTimePicker
                label="Start Time"
                value={formData.startTime}
                onChange={handleDateChange}
                slotProps={{
                  textField: {
                    fullWidth: true,
                    error: !!errors.startTime,
                    helperText: errors.startTime || `Will execute at this time in ${formData.timeZone}`
                  }
                }}
                disabled={loading}
              />
            </LocalizationProvider>
          </Grid>
        )}

        {/* Recurring Schedule Options */}
        {!formData.immediateExecution && formData.scheduleType === 'RECURRING' && (
          <>
            <Grid item xs={12}>
              <Divider />
              <Typography variant="subtitle1" sx={{ mt: 2, mb: 1 }}>
                Recurrence Pattern
              </Typography>
            </Grid>

            {/* Add this new Grid item for time selection */}
            <Grid item xs={12}>
              <Typography variant="subtitle2" sx={{ mb: 1 }}>
                Execution Time:
              </Typography>
              <LocalizationProvider dateAdapter={AdapterDateFns}>
                <TimePicker
                  label="Time to execute"
                  value={formData.recurringTime}
                  onChange={handleRecurringTimeChange}
                  slotProps={{
                    textField: {
                      fullWidth: true,
                      helperText: `Time in ${formData.timeZone}`
                    }
                  }}
                  disabled={loading}
                />
              </LocalizationProvider>
              <FormHelperText>
                The time when recurring jobs will execute (in the selected time zone)
              </FormHelperText>
            </Grid>

            {/* Hourly */}
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Hourly Interval"
                name="hourlyInterval"
                type="number"
                InputProps={{ inputProps: { min: 1, max: 24 } }}
                value={formData.hourlyInterval}
                onChange={handleChange}
                disabled={loading}
              />
              <FormHelperText>Run every X hours</FormHelperText>
            </Grid>

            {/* Weekly (days of week) */}
            <Grid item xs={12} md={4}>
              <Typography variant="body2" sx={{ mb: 1 }}>
                Weekly on:
              </Typography>
              <FormGroup>
                {DAYS_OF_WEEK.map((day) => (
                  <FormControlLabel
                    key={day.value}
                    control={
                      <Checkbox
                        checked={formData.daysOfWeek.includes(day.value)}
                        onChange={() => handleDayOfWeekChange(day.value)}
                        disabled={loading}
                      />
                    }
                    label={day.label}
                  />
                ))}
              </FormGroup>
            </Grid>

            {/* Monthly (days of month) */}
            <Grid item xs={12} md={4}>
              <Typography variant="body2" sx={{ mb: 1 }}>
                Monthly on day:
              </Typography>
              <Box sx={{ maxHeight: 200, overflow: 'auto', border: '1px solid #ccc', p: 1 }}>
                <Grid container>
                  {DAYS_OF_MONTH.map((day) => (
                    <Grid item xs={3} key={day.value}>
                      <FormControlLabel
                        control={
                          <Checkbox
                            checked={formData.daysOfMonth.includes(day.value)}
                            onChange={() => handleDayOfMonthChange(day.value)}
                            disabled={loading}
                            size="small"
                          />
                        }
                        label={day.label}
                      />
                    </Grid>
                  ))}
                </Grid>
              </Box>
            </Grid>

            {errors.recurrence && (
              <Grid item xs={12}>
                <FormHelperText error>{errors.recurrence}</FormHelperText>
              </Grid>
            )}
          </>
        )}

        {/* Submit Button */}
        <Grid item xs={12}>
          <Button
            type="submit"
            variant="contained"
            color="primary"
            disabled={loading}
            startIcon={loading && <CircularProgress size={20} />}
          >
            {loading ? 'Scheduling...' : 'Schedule Job'}
          </Button>
        </Grid>
      </Grid>
    </form>
  );
};

export default JobForm;