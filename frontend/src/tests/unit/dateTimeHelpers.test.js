// Save as: frontend/src/tests/unit/dateTimeHelpers.test.js

import { 
    formatInTimezone, 
    isDateInPast, 
    formatForServer,
    getCurrentTimeInTimezone,
    createFutureDate,
    DEFAULT_TIMEZONE
  } from '../../utils/dateTimeHelpers';
  
  // Mock date-fns-tz to control output
  jest.mock('date-fns-tz', () => ({
    formatInTimeZone: jest.fn((date, tz, format) => `Mocked-${tz}-${format}`),
  }));
  
  describe('dateTimeHelpers', () => {
    let originalDate;
    
    beforeEach(() => {
      // Store the original Date constructor
      originalDate = global.Date;
      // Mock the current date to a fixed value
      const mockDate = new Date('2025-04-15T12:00:00Z');
      global.Date = jest.fn(() => mockDate);
      global.Date.now = jest.fn(() => mockDate.getTime());
      // Preserve other Date methods
      Object.setPrototypeOf(global.Date, originalDate);
    });
    
    afterEach(() => {
      // Restore the original Date constructor
      global.Date = originalDate;
    });
    
    describe('formatInTimezone', () => {
      it('should call formatInTimeZone with correct parameters', () => {
        const date = new Date();
        const result = formatInTimezone(date, 'UTC', 'yyyy-MM-dd');
        expect(result).toBe('Mocked-UTC-yyyy-MM-dd');
      });
      
      it('should use DEFAULT_TIMEZONE if none provided', () => {
        const date = new Date();
        const result = formatInTimezone(date);
        expect(result).toContain(DEFAULT_TIMEZONE);
      });
    });
    
    describe('formatForServer', () => {
      it('should format date correctly for server', () => {
        const date = new Date(2025, 3, 15, 14, 30, 0); // April 15, 2025, 14:30:00
        const result = formatForServer(date);
        expect(result).toBe('2025-04-15T14:30:00');
      });
      
      it('should handle single-digit values with padding', () => {
        const date = new Date(2025, 0, 5, 9, 5, 5); // Jan 5, 2025, 09:05:05
        const result = formatForServer(date);
        expect(result).toBe('2025-01-05T09:05:05');
      });
    });
    
    describe('isDateInPast', () => {
      it('should return true for past dates', () => {
        const pastDate = new Date(2025, 3, 14); // April 14, 2025
        expect(isDateInPast(pastDate)).toBe(true);
      });
      
      it('should return false for future dates', () => {
        const futureDate = new Date(2025, 3, 16); // April 16, 2025
        expect(isDateInPast(futureDate)).toBe(false);
      });
    });
    
    describe('createFutureDate', () => {
      it('should create a date in the future by the specified minutes', () => {
        const result = createFutureDate(30);
        const expectedTime = new Date('2025-04-15T12:30:00Z').getTime();
        expect(result.getTime()).toBe(expectedTime);
      });
      
      it('should use default 15 minutes if not specified', () => {
        const result = createFutureDate();
        const expectedTime = new Date('2025-04-15T12:15:00Z').getTime();
        expect(result.getTime()).toBe(expectedTime);
      });
    });
  });