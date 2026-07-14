package com.autodealer.crm.shared.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;

public final class ApiDateTimeParser {

    public static final ZoneId API_ZONE = ZoneId.of("Asia/Shanghai");
    public static final DateTimeFormatter PROJECT_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<DateTimeFormatter> LOCAL_DATE_TIME_FORMATTERS = List.of(
            PROJECT_DATE_TIME_FORMATTER,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );

    private ApiDateTimeParser() {
    }

    public static LocalDateTime parseLocalDateTime(String value) {
        ZonedDateTime zonedDateTime = parseZonedDateTime(value);
        return zonedDateTime == null ? null : zonedDateTime.withZoneSameInstant(API_ZONE).toLocalDateTime();
    }

    public static Date parseDate(String value) {
        ZonedDateTime zonedDateTime = parseZonedDateTime(value);
        return zonedDateTime == null ? null : Date.from(zonedDateTime.toInstant());
    }

    private static ZonedDateTime parseZonedDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmedValue = value.trim();
        for (DateTimeFormatter formatter : LOCAL_DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(trimmedValue, formatter).atZone(API_ZONE);
            } catch (DateTimeParseException ignored) {
                // Try the next supported API format.
            }
        }
        try {
            return OffsetDateTime.parse(trimmedValue, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toZonedDateTime();
        } catch (DateTimeParseException ignored) {
            // Try zoned ISO date-time next.
        }
        try {
            return ZonedDateTime.parse(trimmedValue, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // Try date-only next.
        }
        try {
            return LocalDate.parse(trimmedValue, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(API_ZONE);
        } catch (DateTimeParseException ignored) {
            throw new DateTimeParseException(
                    "日期时间格式必须为 yyyy-MM-dd HH:mm:ss、yyyy-MM-dd 或 ISO-8601",
                    trimmedValue,
                    0);
        }
    }
}
