package com.enterprise.framework.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Date/time and string utility helpers for test data generation and validation.
 */
public final class DateUtils {

    private DateUtils() { }

    public static final DateTimeFormatter DEFAULT_DATE      = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    public static final DateTimeFormatter DEFAULT_DATETIME  = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
    public static final DateTimeFormatter ISO_DATE          = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter REPORT_TIMESTAMP  = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /** Returns today in MM/dd/yyyy. */
    public static String today() {
        return LocalDate.now().format(DEFAULT_DATE);
    }

    /** Returns today in a custom format. */
    public static String today(DateTimeFormatter formatter) {
        return LocalDate.now().format(formatter);
    }

    /** Returns today plus N days. */
    public static String futureDateByDays(int days) {
        return LocalDate.now().plusDays(days).format(DEFAULT_DATE);
    }

    /** Returns today minus N days. */
    public static String pastDateByDays(int days) {
        return LocalDate.now().minusDays(days).format(DEFAULT_DATE);
    }

    /** Returns the current date/time as a timestamp string for report naming. */
    public static String reportTimestamp() {
        return LocalDateTime.now().format(REPORT_TIMESTAMP);
    }

    /** Parses a date string using the given formatter. */
    public static LocalDate parse(String dateStr, DateTimeFormatter formatter) {
        return LocalDate.parse(dateStr, formatter);
    }

    /** Returns a UUID-based unique string (useful for test data uniqueness). */
    public static String uniqueId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    /** Returns a unique email address for test account creation. */
    public static String uniqueEmail(String prefix) {
        return prefix + "_" + uniqueId() + "@test.qa.com";
    }

    /** Returns a unique username. */
    public static String uniqueUsername(String prefix) {
        return prefix + "_" + uniqueId();
    }
}
