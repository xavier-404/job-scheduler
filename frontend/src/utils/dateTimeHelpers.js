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

  console.log(`[TIMEZONE-DEBUG] Formatting date ${date.toISOString()} in timezone ${timezone}`);
  try {
    const formatted = formatInTimeZone(date, timezone, formatStr);
    console.log(`[TIMEZONE-DEBUG] Result: ${formatted}`);
    return formatted;
  } catch (e) {
    console.error(`[TIMEZONE-DEBUG] Error formatting in timezone: ${e.message}`);
    return date.toString();
  }
};

/**
 * Formats a date for sending to the server
 * 
 * IMPORTANT: We send the selected time as a timezone-naive ISO string,
 * along with the timezone information separately. The backend will interpret
 * this time in the context of the specified timezone.
 */
export const formatForServer = (date) => {
  // Extract date components directly from the Date object (local browser time)
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');

  // Format as ISO string WITHOUT timezone part
  const dateString = `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
  console.log(`[TIMEZONE-DEBUG] Formatting for server: ${date} -> ${dateString}`);
  return dateString;
};

/**
 * Creates a date that's in the future by the specified minutes
 * 
 * @param {number} minutesAhead - Minutes to add to the current time (default: 15)
 * @returns {Date} A Date object that's minutesAhead minutes in the future
 */
export const createFutureDate = (minutesAhead = 15) => {
  const now = new Date();
  console.log(`[TIMEZONE-DEBUG] Creating future date: ${minutesAhead} minutes from now (${now})`);
  return new Date(now.getTime() + minutesAhead * 60 * 1000);
};

/**
 * Gets the current time in the specified timezone as a formatted string
 * 
 * @param {string} timezone - The timezone to get the current time in
 * @returns {string} Current time in the specified timezone
 */
export const getCurrentTimeInTimezone = (timezone) => {
  if (!timezone) timezone = DEFAULT_TIMEZONE;
  const now = new Date();
  console.log(`[TIMEZONE-DEBUG] Getting current time in ${timezone} (from ${now})`);
  return formatInTimeZone(now, timezone, 'yyyy-MM-dd HH:mm:ss');
};

/**
 * Check if a date is in the past relative to the current time in the specified timezone
 * 
 * @param {Date} date - The date to check
 * @param {string} timezone - The timezone to use for comparison
 * @returns {boolean} True if the date is in the past in the specified timezone
 */
export const isDateInPast = (date, timezone) => {
  if (!timezone) timezone = DEFAULT_TIMEZONE;

  // Get current time in UTC
  const nowUtc = new Date();

  // Convert browser's timezone date to an ISO string without timezone info
  // to get a timezone-neutral representation
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');

  // Create timestamp strings for comparison
  const selectedTimeStr = `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;

  // The key is to convert both times to the same timezone for comparison
  const nowInTz = formatInTimeZone(nowUtc, timezone, 'yyyy-MM-dd HH:mm:ss');

  console.log(`[TIMEZONE-DEBUG] Past time check: selected (${selectedTimeStr}) vs current in ${timezone} (${nowInTz})`);

  // Parse the times in the same format for accurate comparison
  const selectedTimeParts = selectedTimeStr.split(/[-T:]/);
  const nowParts = nowInTz.split(/[-:\s]/);

  // Create Date objects with consistent timezone handling
  const selectedDate = new Date(Date.UTC(
    parseInt(selectedTimeParts[0]),   // year
    parseInt(selectedTimeParts[1]) - 1, // month (0-indexed)
    parseInt(selectedTimeParts[2]),   // day
    parseInt(selectedTimeParts[3]),   // hour
    parseInt(selectedTimeParts[4]),   // minute
    parseInt(selectedTimeParts[5])    // second
  ));

  const nowDate = new Date(Date.UTC(
    parseInt(nowParts[0]),   // year
    parseInt(nowParts[1]) - 1, // month (0-indexed)
    parseInt(nowParts[2]),   // day
    parseInt(nowParts[3]),   // hour
    parseInt(nowParts[4]),   // minute
    parseInt(nowParts[5])    // second
  ));

  const isPast = selectedDate < nowDate;
  console.log(`[TIMEZONE-DEBUG] Is past: ${isPast}`);

  return isPast;
};
/**
 * Convert a date from one timezone to another
 * 
 * @param {Date} date - The date to convert
 * @param {string} fromTimezone - Source timezone
 * @param {string} toTimezone - Target timezone
 * @returns {Date} The converted date
 */
export const convertTimezone = (date, fromTimezone, toTimezone) => {
  if (!fromTimezone) fromTimezone = DEFAULT_TIMEZONE;
  if (!toTimezone) toTimezone = DEFAULT_TIMEZONE;

  // First convert to UTC
  const utcDate = zonedTimeToUtc(date, fromTimezone);
  // Then convert to target timezone
  const convertedDate = utcToZonedTime(utcDate, toTimezone);

  console.log(`[TIMEZONE-DEBUG] Converting date ${date} from ${fromTimezone} to ${toTimezone}: ${convertedDate}`);
  return convertedDate;
};