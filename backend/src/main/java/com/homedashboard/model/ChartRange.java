package com.homedashboard.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;

public enum ChartRange {
    DAY("day", Duration.ofHours(24), ChartResolution.RAW),
    WEEK("week", Duration.ofDays(7), ChartResolution.HOURLY),
    MONTH("month", Duration.ofDays(30), ChartResolution.DAILY);

    private final String value;
    private final Duration duration;
    private final ChartResolution resolution;

    ChartRange(String value, Duration duration, ChartResolution resolution) {
        this.value = value;
        this.duration = duration;
        this.resolution = resolution;
    }

    public OffsetDateTime calculateFrom(OffsetDateTime to) {
        return to.minus(duration);
    }

    public ChartResolution getResolution() {
        return resolution;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static Optional<ChartRange> from(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ChartRange range : values()) {
            if (range.value.equals(normalized)) {
                return Optional.of(range);
            }
        }

        return Optional.empty();
    }
}
