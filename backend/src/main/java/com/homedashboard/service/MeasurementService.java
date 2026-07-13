package com.homedashboard.service;

import com.homedashboard.model.ChartDataResponse;
import com.homedashboard.model.ChartPoint;
import com.homedashboard.model.ChartRange;
import com.homedashboard.model.Measurement;
import com.homedashboard.model.Room;
import com.homedashboard.repository.MeasurementRepository;
import com.homedashboard.repository.projection.AverageMeasurementProjection;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class MeasurementService {

    private final MeasurementRepository measurementRepository;

    public MeasurementService(MeasurementRepository measurementRepository) {
        this.measurementRepository = measurementRepository;
    }

    public List<Measurement> list() {
        return measurementRepository.findAll();
    }

    public Measurement create(Measurement measurement) {
        Objects.requireNonNull(measurement, "measurement darf nicht null sein");
        return measurementRepository.save(measurement);
    }

    public Optional<Measurement> getLatest(Room room) {
        return measurementRepository.findTopByRoomOrderByTsDesc(room);
    }

    public ChartDataResponse getChartData(Room room, ChartRange range) {
        ZoneId zone = ZoneId.systemDefault();
        return getChartData(room, range, OffsetDateTime.now(zone));
    }

    public ChartDataResponse getChartData(Room room, ChartRange range, OffsetDateTime to) {
        Objects.requireNonNull(room, "room darf nicht null sein");
        Objects.requireNonNull(range, "range darf nicht null sein");
        Objects.requireNonNull(to, "to darf nicht null sein");

        ZoneId zone = ZoneId.systemDefault();
        OffsetDateTime from = range.calculateFrom(to);

        List<ChartPoint> chartPoints = switch (range) {
            case DAY -> loadRawPoints(room, from, to);
            case WEEK -> loadAveragePoints(
                    measurementRepository.loadHourlyAverageProjections(room.name(), from, to),
                    zone);
            case MONTH -> loadAveragePoints(
                    measurementRepository.loadDailyAverageProjections(room.name(), from, to),
                    zone);
        };

        return new ChartDataResponse(
                room,
                range,
                from,
                to,
                range.getResolution(),
                chartPoints);
    }

    private List<ChartPoint> loadRawPoints(Room room, OffsetDateTime from, OffsetDateTime to) {
        return measurementRepository
                .findByRoomAndTsBetweenOrderByTsAsc(room, from, to)
                .stream()
                .map(measurement -> new ChartPoint(
                        measurement.getTs(),
                        measurement.getTemperature(),
                        measurement.getHumidity()))
                .toList();
    }

    private List<ChartPoint> loadAveragePoints(
            List<AverageMeasurementProjection> averages,
            ZoneId zone) {
        return averages.stream()
                .map(average -> new ChartPoint(
                        average.getBucket().atZone(zone).toOffsetDateTime(),
                        average.getAvgTemp(),
                        average.getAvgHumidity()))
                .toList();
    }
}
