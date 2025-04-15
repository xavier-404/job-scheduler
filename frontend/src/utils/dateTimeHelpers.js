// frontend/src/utils/dateTimeHelpers.js

import { format, formatInTimeZone } from 'date-fns-tz';
import { zonedTimeToUtc, utcToZonedTime } from 'date-fns-tz';

/**
 * Default timezone for the application
 */
export const DEFAULT_TIMEZONE = 'Asia/Kolkata';

/**
 * Formats a date in the specified timezone
 * 
 * @param {Date} date - The date to format
 * @param {string} timezone - The timezone to format the date in
 * @param {string} formatStr - The format string (default: 'yyyy-MM-dd HH:mm:ss')
 * @returns {string} The formatted date string
 */
export const formatInTimezone = (date, timezone, formatStr = 'yyyy-MM-dd HH:mm:ss') => {
  if (!timezone) timezone = DEFAULT_TIMEZONE;
  console.log(`[TIMEZONE-DEBUG] formatInTimezone: Input date=${date}, timezone=${timezone}`);
  
  const formatted = formatInTimeZone(date, timezone, formatStr);
  console.log(`[TIMEZONE-DEBUG] formatInTimezone: Output=${formatted}`);
  return formatted;
};

/**
 * Formats a date for sending to the server in ISO format
 * This is critical for proper timezone handling
 * 
 * @param {Date} date - The date to format
 * @param {string} timezone - The timezone for interpreting this date
 * @returns {string} The formatted date string (YYYY-MM-DDTHH:MM:SS)
 */
export const formatForServer = (date, timezone) => {
  if (!timezone) timezone = DEFAULT_TIMEZONE;
  
  console.log(`[TIMEZONE-DEBUG] formatForServer: Input date=${date}, timezone=${timezone}`);
  console.log(`[TIMEZONE-DEBUG] formatForServer: Raw date year=${date.getFullYear()}, month=${date.getMonth()+1}, day=${date.getDate()}, hours=${date.getHours()}, minutes=${date.getMinutes()}`);
  
  // Store original date properties for debugging
  const originalISOString = date.toISOString();
  const originalTimestamp = date.getTime();
  
  // IMPORTANT: DON'T convert to specified timezone - the date picker already returns
  // a date object in the LOCAL timezone of the browser
  // Instead, we'll format it as an ISO string WITHOUT timezone information
  
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');
  
  // Format as ISO string WITHOUT timezone part
  const formattedDate = `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
  
  console.log(`[TIMEZONE-DEBUG] formatForServer: Output=${formattedDate}`);
  console.log(`[TIMEZONE-DEBUG] formatForServer: Original ISO=${originalISOString}, timestamp=${originalTimestamp}`);
  
  return formattedDate;
};

/**
 * Creates a date that's in the future
 * 
 * @param {number} minutesAhead - Minutes to add to the current time (default: 15)
 * @returns {Date} A Date object that's minutesAhead minutes in the future
 */
export const createFutureDate = (minutesAhead = 15) => {
  const now = new Date();
  console.log(`[TIMEZONE-DEBUG] createFutureDate: Current date=${now}, adding ${minutesAhead} minutes`);
  
  const futureDate = new Date(now.getTime() + minutesAhead * 60 * 1000);
  console.log(`[TIMEZONE-DEBUG] createFutureDate: Result=${futureDate}`);
  
  return futureDate;
};

/**
 * Gets the current time in the specified timezone
 * 
 * @param {string} timezone - The timezone to get the current time in
 * @returns {string} Current time in the specified timezone
 */
export const getCurrentTimeInTimezone = (timezone) => {
  if (!timezone) timezone = DEFAULT_TIMEZONE;
  
  const now = new Date();
  console.log(`[TIMEZONE-DEBUG] getCurrentTimeInTimezone: Current date=${now}, timezone=${timezone}`);
  
  const formatted = formatInTimeZone(now, timezone, 'yyyy-MM-dd HH:mm:ss');
  console.log(`[TIMEZONE-DEBUG] getCurrentTimeInTimezone: Output=${formatted}`);
  
  return formatted;
};

/**
 * Check if a date is in the past relative to the current time in the specified timezone
 * 
 * @param {Date} date - The date to check
 * @param {string} timezone - The timezone for comparison
 * @returns {boolean} True if the date is in the past
 */
export const isDateInPast = (date, timezone) => {
  if (!timezone) timezone = DEFAULT_TIMEZONE;
  
  console.log(`[TIMEZONE-DEBUG] isDateInPast: Input date=${date}, timezone=${timezone}`);
  
  // Get the browser's CURRENT date
  const now = new Date();
  
  // Create date objects from UTC timestamps to make a fair comparison
  const timestampNow = now.getTime();
  const timestampDate = date.getTime();
  
  console.log(`[TIMEZONE-DEBUG] isDateInPast: Now timestamp=${timestampNow}, Date timestamp=${timestampDate}`);
  
  // Simple timestamp comparison
  const isPast = timestampDate < timestampNow;
  console.log(`[TIMEZONE-DEBUG] isDateInPast: Result=${isPast}`);
  
  return isPast;
};