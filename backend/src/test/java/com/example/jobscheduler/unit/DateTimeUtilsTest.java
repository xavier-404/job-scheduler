// Save as: backend/src/test/java/com/example/jobscheduler/unit/DateTimeUtilsTest.java

package com.example.jobscheduler.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class DateTimeUtilsTest {

    @Test
    void shouldConvertBetweenTimezones() {
        // Given
        LocalDateTime dateTimeInIST = LocalDateTime.of(2025, 4, 15, 14, 30, 0);
        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        ZoneId utcZone = ZoneId.of("UTC");
        
        // When
        ZonedDateTime zonedDateTimeIST = dateTimeInIST.atZone(istZone);
        ZonedDateTime zonedDateTimeUTC = zonedDateTimeIST.withZoneSameInstant(utcZone);
        
        // Then
        assertEquals(9, zonedDateTimeUTC.getHour());
        assertEquals(0, zonedDateTimeUTC.getMinute());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"UTC", "America/New_York", "Europe/London", "Asia/Tokyo", "Australia/Sydney"})
    void shouldHandleVariousTimezones(String timezoneId) {
        // Given
        ZoneId zoneId = ZoneId.of(timezoneId);
        LocalDateTime now = LocalDateTime.now();
        
        // When
        ZonedDateTime zonedDateTime = now.atZone(zoneId);
        
        // Then
        assertEquals(zoneId, zonedDateTime.getZone());
        assertNotNull(zonedDateTime.toInstant());
    }
    
    @Test
    void shouldDetectPastDates() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime past = now.minusHours(1);
        LocalDateTime future = now.plusHours(1);
        ZoneId zoneId = ZoneId.of("UTC");
        
        // When & Then
        assertTrue(past.atZone(zoneId).isBefore(now.atZone(zoneId)));
        assertFalse(future.atZone(zoneId).isBefore(now.atZone(zoneId)));
    }
}