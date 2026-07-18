package com.homedashboard.service;

import com.homedashboard.model.ChartDataResponse;
import com.homedashboard.model.ChartPoint;
import com.homedashboard.model.ChartRange;
import com.homedashboard.model.Measurement;
import com.homedashboard.model.Room;
import com.homedashboard.repository.MeasurementRepository;
import com.homedashboard.repository.projection.AverageMeasurementProjection;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MeasurementServiceTest {

    MeasurementRepository measurementRepository = mock(MeasurementRepository.class);
    MeasurementService measurementService = new MeasurementService(measurementRepository);

    @Test
    void listsMeasurments() {
        Measurement measurement = new Measurement();
        measurement.setBatteryPercentage(100);
        measurement.setHumidity(50.0);
        measurement.setRoom(Room.BEDROOM);
        measurement.setTemperature(19.5);
        measurement.setTs(OffsetDateTime.now(ZoneOffset.UTC));

        when(measurementRepository.findAll()).thenReturn(List.of(measurement));

        List<Measurement> result = measurementService.list();
        assertThat(result).containsExactly(measurement);
        verify(measurementRepository).findAll();
    }

    @Test
    void savesMeasurement() {
        Measurement measurement = new Measurement();
        measurement.setBatteryPercentage(100);
        measurement.setHumidity(50.0);
        measurement.setRoom(Room.BEDROOM);
        measurement.setTemperature(19.5);
        measurement.setTs(OffsetDateTime.now(ZoneOffset.UTC));

        when(measurementRepository.save(measurement)).thenReturn(measurement);

        Measurement result = measurementService.create(measurement);

        assertThat(result).isSameAs(measurement);
        verify(measurementRepository).save(measurement);
    }

    @Test
    void saveRejectsNullMeasurement() {
        assertThatThrownBy(() -> measurementService.create(null))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(measurementRepository);
    }

    @Test
    void loadsLatestMeasurement() {
        Measurement measurement = new Measurement();
        measurement.setBatteryPercentage(100);
        measurement.setHumidity(50.0);
        measurement.setRoom(Room.BEDROOM);
        measurement.setTemperature(19.5);
        OffsetDateTime ts = OffsetDateTime.now(ZoneOffset.UTC);
        measurement.setTs(ts);

        when(measurementRepository.findTopByRoomOrderByTsDesc(Room.BEDROOM))
                .thenReturn(Optional.of(measurement));

        Optional<Measurement> resultOpt = measurementService.getLatest(Room.BEDROOM);
        Measurement result = resultOpt.get();

        assertThat(result.getHumidity()).isEqualTo(50.0);
        assertThat(result.getTemperature()).isEqualTo(19.5);
        assertThat(result.getTs()).isEqualTo(ts);
        assertThat(result.getRoom()).isEqualTo(Room.BEDROOM);
        verify(measurementRepository).findTopByRoomOrderByTsDesc(Room.BEDROOM);
    }

    @Test
    void rejtectsNullRoom() {
        assertThatThrownBy(
                () -> measurementService.getChartData(null, ChartRange.MONTH, OffsetDateTime.now(ZoneOffset.UTC)))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(measurementRepository);
    }

    @Test
    void rejtectsNullRange() {
        assertThatThrownBy(
                () -> measurementService.getChartData(Room.BEDROOM, null, OffsetDateTime.now(ZoneOffset.UTC)))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(measurementRepository);
    }

    @Test
    void rejtectsNullTo() {
        assertThatThrownBy(
                () -> measurementService.getChartData(Room.BEDROOM, ChartRange.MONTH, null))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(measurementRepository);
    }

    @Test
    void loadsChartDataWithRangeDay() {
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime from = ChartRange.DAY.calculateFrom(to);

        Measurement measurement = new Measurement();
        measurement.setRoom(Room.BEDROOM);
        measurement.setTs(to);
        measurement.setTemperature(22.5);
        measurement.setHumidity(50.3);

        Measurement earlierMeasurement = new Measurement();
        earlierMeasurement.setRoom(Room.BEDROOM);
        earlierMeasurement.setTs(to.minusHours(1));
        earlierMeasurement.setTemperature(21.0);
        earlierMeasurement.setHumidity(48.0);

        when(measurementRepository.findByRoomAndTsBetweenOrderByTsAsc(Room.BEDROOM, from, to))
                .thenReturn(List.of(earlierMeasurement, measurement));

        ChartDataResponse response = measurementService.getChartData(Room.BEDROOM, ChartRange.DAY, to);
        assertThat(response.room()).isEqualTo(Room.BEDROOM);
        assertThat(response.to()).isEqualTo(to);
        assertThat(response.from()).isEqualTo(from);
        assertThat(response.range()).isEqualTo(ChartRange.DAY);

        ChartPoint earlierPoint = new ChartPoint(
                earlierMeasurement.getTs(), earlierMeasurement.getTemperature(), earlierMeasurement.getHumidity());
        ChartPoint point = new ChartPoint(
                measurement.getTs(), measurement.getTemperature(), measurement.getHumidity());
        assertThat(response.points()).containsExactly(earlierPoint, point);
        verify(measurementRepository).findByRoomAndTsBetweenOrderByTsAsc(Room.BEDROOM, from, to);
    }

    @Test
    void loadsChartDataWithRangeWeek() {
        OffsetDateTime to = OffsetDateTime.parse("2026-04-30T12:00:00Z");
        OffsetDateTime from = ChartRange.WEEK.calculateFrom(to);

        AverageMeasurementProjection projection = mock(AverageMeasurementProjection.class);
        when(projection.getBucket()).thenReturn(Instant.parse("2026-04-30T10:00:00Z"));
        when(projection.getAvgTemp()).thenReturn(21.5);
        when(projection.getAvgHumidity()).thenReturn(50.4);

        AverageMeasurementProjection laterProjection = mock(AverageMeasurementProjection.class);
        when(laterProjection.getBucket()).thenReturn(Instant.parse("2026-04-30T11:00:00Z"));
        when(laterProjection.getAvgTemp()).thenReturn(22.0);
        when(laterProjection.getAvgHumidity()).thenReturn(51.0);

        when(measurementRepository.loadHourlyAverageProjections(Room.BEDROOM.name(), from, to))
                .thenReturn(List.of(projection, laterProjection));

        ChartDataResponse response = measurementService.getChartData(Room.BEDROOM, ChartRange.WEEK, to);
        assertThat(response.room()).isEqualTo(Room.BEDROOM);
        assertThat(response.to()).isEqualTo(to);
        assertThat(response.from()).isEqualTo(from);
        assertThat(response.range()).isEqualTo(ChartRange.WEEK);

        ChartPoint point = new ChartPoint(
                projection.getBucket().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                projection.getAvgTemp(),
                projection.getAvgHumidity());
        ChartPoint laterPoint = new ChartPoint(
                laterProjection.getBucket().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                laterProjection.getAvgTemp(),
                laterProjection.getAvgHumidity());
        assertThat(response.points()).containsExactly(point, laterPoint);
        verify(measurementRepository).loadHourlyAverageProjections(Room.BEDROOM.name(), from, to);
    }

    @Test
    void loadsChartDataWithRangeMonth() {
        OffsetDateTime to = OffsetDateTime.parse("2026-04-30T12:00:00Z");
        OffsetDateTime from = ChartRange.MONTH.calculateFrom(to);

        AverageMeasurementProjection projection = mock(AverageMeasurementProjection.class);
        when(projection.getBucket()).thenReturn(Instant.parse("2026-04-20T00:00:00Z"));
        when(projection.getAvgTemp()).thenReturn(21.5);
        when(projection.getAvgHumidity()).thenReturn(50.4);

        AverageMeasurementProjection laterProjection = mock(AverageMeasurementProjection.class);
        when(laterProjection.getBucket()).thenReturn(Instant.parse("2026-04-21T00:00:00Z"));
        when(laterProjection.getAvgTemp()).thenReturn(22.0);
        when(laterProjection.getAvgHumidity()).thenReturn(51.0);

        when(measurementRepository.loadDailyAverageProjections(Room.BEDROOM.name(), from, to))
                .thenReturn(List.of(projection, laterProjection));

        ChartDataResponse response = measurementService.getChartData(Room.BEDROOM, ChartRange.MONTH, to);
        assertThat(response.room()).isEqualTo(Room.BEDROOM);
        assertThat(response.to()).isEqualTo(to);
        assertThat(response.from()).isEqualTo(from);
        assertThat(response.range()).isEqualTo(ChartRange.MONTH);

        ChartPoint point = new ChartPoint(
                projection.getBucket().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                projection.getAvgTemp(),
                projection.getAvgHumidity());
        ChartPoint laterPoint = new ChartPoint(
                laterProjection.getBucket().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                laterProjection.getAvgTemp(),
                laterProjection.getAvgHumidity());
        assertThat(response.points()).containsExactly(point, laterPoint);
        verify(measurementRepository).loadDailyAverageProjections(Room.BEDROOM.name(), from, to);
    }

    @Test
    void usesExplicitToParameterAsEndOfRange() {
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
