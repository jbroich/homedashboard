package com.homedashboard.service;

import com.homedashboard.model.ChartDataResponse;
import com.homedashboard.model.ChartRange;
import com.homedashboard.model.Room;
import com.homedashboard.repository.MeasurementRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeasurementServiceTest {

    @Test
    void usesExplicitToParameterAsEndOfRange() {
        MeasurementRepository measurementRepository = mock(MeasurementRepository.class);
        MeasurementService measurementService = new MeasurementService(measurementRepository);
        OffsetDateTime to = OffsetDateTime.parse("2026-04-30T23:59:59+02:00");
        OffsetDateTime from = to.minusHours(24);

        when(measurementRepository.findByRoomAndTsBetweenOrderByTsAsc(Room.OFFICE, from, to))
                .thenReturn(List.of());

        ChartDataResponse response = measurementService.getChartData(Room.OFFICE, ChartRange.DAY, to);

        assertThat(response.from()).isEqualTo(from);
        assertThat(response.to()).isEqualTo(to);
        assertThat(response.points()).isEmpty();
        verify(measurementRepository).findByRoomAndTsBetweenOrderByTsAsc(Room.OFFICE, from, to);
    }
}
