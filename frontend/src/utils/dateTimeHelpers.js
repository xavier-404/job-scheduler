// frontend/src/utils/dateTimeHelpers.js

import { format, formatInTimeZone } from 'date-fns-tz';

/**
 * Default timezone for the application
 */
export const DEFAULT_TIMEZONE = 'Asia/Kolkata';

/**
 * Formats a date in the application's default timezone (IST)
 * 
 * @param {Date} date - The date to format
 * @param {string} formatStr - The format string (default: 'yyyy-MM-dd HH:mm:ss')
 * @returns {string} The formatted date string
 */
export const formatInIST = (date, formatStr = 'yyyy-MM-dd HH:mm:ss') => {
  return formatInTimeZone(date, DEFAULT_TIMEZONE, formatStr);
};

/**
 * Formats a date for sending to the server in ISO format
 * To be interpreted directly in the specified timezone
 * 
 * @param {Date} date - The date to format
 * @returns {string} The formatted date string (YYYY-MM-DDTHH:MM:SS)
 */
export const formatForServer = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');
  
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
};

/**
 * Creates a date that's in the future in IST
 * 
 * @param {number} minutesAhead - Minutes to add to the current time (default: 15)
 * @returns {Date} A Date object that's minutesAhead minutes in the future
 */
export const createFutureDate = (minutesAhead = 15) => {
  const now = new Date();
  return new Date(now.getTime() + minutesAhead * 60 * 1000);
};

/**
 * Gets the current time in IST as a formatted string
 * 
 * @returns {string} Current time in IST
 */
export const getCurrentTimeIST = () => {
  return formatInIST(new Date());
};