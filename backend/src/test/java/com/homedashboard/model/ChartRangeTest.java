package com.homedashboard.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ChartRangeTest {

    private static final OffsetDateTime TO =
            OffsetDateTime.parse("2026-06-20T12:00:00+02:00");

    @Test
    void definesRollingWindowsAndResolutions() {
        assertThat(ChartRange.DAY.calculateFrom(TO)).isEqualTo(TO.minusHours(24));
        assertThat(ChartRange.DAY.getResolution()).isEqualTo(ChartResolution.RAW);

        assertThat(ChartRange.WEEK.calculateFrom(TO)).isEqualTo(TO.minusDays(7));
        assertThat(ChartRange.WEEK.getResolution()).isEqualTo(ChartResolution.HOURLY);

        assertThat(ChartRange.MONTH.calculateFrom(TO)).isEqualTo(TO.minusDays(30));
        assertThat(ChartRange.MONTH.getResolution()).isEqualTo(ChartResolution.DAILY);
    }

    @Test
    void parsesSupportedValuesCaseInsensitively() {
        assertThat(ChartRange.from(" DAY ")).contains(ChartRange.DAY);
        assertThat(ChartRange.from("week")).contains(ChartRange.WEEK);
        assertThat(ChartRange.from("Month")).contains(ChartRange.MONTH);
    }

    @Test
    void rejectsUnsupportedValues() {
        assertThat(ChartRange.from(null)).isEmpty();
        assertThat(ChartRange.from("")).isEmpty();
        assertThat(ChartRange.from("year")).isEmpty();
    }
}
