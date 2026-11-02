package com.homedashboard.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.homedashboard.model.Measurement;
import com.homedashboard.model.Room;
import com.homedashboard.repository.projection.AverageMeasurementProjection;

@Testcontainers
@DataJpaTest(properties = "spring.datasource.hikari.connection-init-sql=SET TIME ZONE 'UTC'")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MeasurementRepositoryTest {

    @Autowired
    private MeasurementRepository measurementRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15");

    @Test
    void savesAndLoadsAllMeasurementFields() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-09-14T12:30:00Z");
        Measurement measurement = measurement(Room.BEDROOM, timestamp, 19.5, 50.0, 100);

        Long id = measurementRepository.saveAndFlush(measurement).getId();
        entityManager.clear();

        Measurement loaded = measurementRepository.findById(id).orElseThrow();

        assertThat(loaded.getId()).isEqualTo(id);
        assertThat(loaded.getRoom()).isEqualTo(Room.BEDROOM);
        assertThat(loaded.getTemperature()).isEqualTo(19.5);
        assertThat(loaded.getHumidity()).isEqualTo(50.0);
        assertThat(loaded.getBatteryPercentage()).isEqualTo(100);
        assertThat(loaded.getTs()).isEqualTo(timestamp);
    }

    @Test
    void returnsLatestMeasurementForRequestedRoom() {
        persist(Room.BEDROOM, "2026-09-14T10:00:00Z", 19.0, 48.0);
        persist(Room.BEDROOM, "2026-09-14T11:00:00Z", 20.0, 50.0);
        persist(Room.OFFICE, "2026-09-14T12:00:00Z", 25.0, 60.0);
        entityManager.flush();

        Measurement latest = measurementRepository
                .findTopByRoomOrderByTsDesc(Room.BEDROOM)
                .orElseThrow();

        assertThat(latest.getRoom()).isEqualTo(Room.BEDROOM);
        assertThat(latest.getTs()).isEqualTo(OffsetDateTime.parse("2026-09-14T11:00:00Z"));
        assertThat(latest.getTemperature()).isEqualTo(20.0);
    }

    @Test
    void returnsEmptyWhenRoomHasNoMeasurements() {
        persist(Room.OFFICE, "2026-09-14T12:00:00Z", 25.0, 60.0);
        entityManager.flush();

        assertThat(measurementRepository.findTopByRoomOrderByTsDesc(Room.BEDROOM))
                .isEmpty();
    }

    @Test
    void returnsMeasurementsInsideRangeInAscendingOrder() {
        OffsetDateTime from = OffsetDateTime.parse("2026-09-14T10:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-14T12:00:00Z");

        persist(Room.BEDROOM, "2026-09-14T09:59:59Z", 9.0, 40.0);
        persist(Room.BEDROOM, "2026-09-14T10:00:00Z", 10.0, 41.0);
        persist(Room.BEDROOM, "2026-09-14T11:00:00Z", 11.0, 42.0);
        persist(Room.BEDROOM, "2026-09-14T12:00:00Z", 12.0, 43.0);
        persist(Room.BEDROOM, "2026-09-14T12:00:01Z", 13.0, 44.0);
        persist(Room.OFFICE, "2026-09-14T10:30:00Z", 99.0, 99.0);
        entityManager.flush();

        List<Measurement> result = measurementRepository
                .findByRoomAndTsBetweenOrderByTsAsc(Room.BEDROOM, from, to);

        assertThat(result)
                .extracting(Measurement::getTs)
                .containsExactly(
                        OffsetDateTime.parse("2026-09-14T10:00:00Z"),
                        OffsetDateTime.parse("2026-09-14T11:00:00Z"),
                        OffsetDateTime.parse("2026-09-14T12:00:00Z"));
        assertThat(result)
                .extracting(Measurement::getTemperature)
                .containsExactly(10.0, 11.0, 12.0);
    }

    @Test
    void calculatesHourlyAveragesAndFiltersByRoomAndRange() {
        OffsetDateTime from = OffsetDateTime.parse("2026-09-14T10:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-14T11:59:59Z");

        persist(Room.BEDROOM, "2026-09-14T09:59:59Z", 10.0, 10.0);
        persist(Room.BEDROOM, "2026-09-14T10:10:00Z", 20.0, 40.0);
        persist(Room.BEDROOM, "2026-09-14T10:40:00Z", 22.0, 60.0);
        persist(Room.BEDROOM, "2026-09-14T11:15:00Z", 24.0, 50.0);
        persist(Room.OFFICE, "2026-09-14T10:20:00Z", 100.0, 100.0);
        persist(Room.BEDROOM, "2026-09-14T12:00:00Z", 30.0, 70.0);
        entityManager.flush();

        List<AverageMeasurementProjection> result = measurementRepository
                .loadHourlyAverageProjections(Room.BEDROOM.name(), from, to);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getBucket()).isEqualTo(OffsetDateTime.parse("2026-09-14T10:00:00Z").toInstant());
        assertThat(result.get(0).getAvgTemp()).isEqualTo(21.0);
        assertThat(result.get(0).getAvgHumidity()).isEqualTo(50.0);
        assertThat(result.get(1).getBucket()).isEqualTo(OffsetDateTime.parse("2026-09-14T11:00:00Z").toInstant());
        assertThat(result.get(1).getAvgTemp()).isEqualTo(24.0);
        assertThat(result.get(1).getAvgHumidity()).isEqualTo(50.0);
    }

    @Test
    void hourlyAveragesIgnoreMissingSensorValues() {
        OffsetDateTime from = OffsetDateTime.parse("2026-09-14T10:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-14T10:59:59Z");

        persist(Room.BEDROOM, "2026-09-14T10:10:00Z", 20.0, null);
        persist(Room.BEDROOM, "2026-09-14T10:20:00Z", null, 60.0);
        entityManager.flush();

        List<AverageMeasurementProjection> result = measurementRepository
                .loadHourlyAverageProjections(Room.BEDROOM.name(), from, to);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getAvgTemp()).isEqualTo(20.0);
        assertThat(result.getFirst().getAvgHumidity()).isEqualTo(60.0);
    }

    @Test
    void calculatesDailyAveragesAndFiltersByRoomAndRange() {
        OffsetDateTime from = OffsetDateTime.parse("2026-09-10T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-11T23:59:59Z");

        persist(Room.BEDROOM, "2026-09-09T23:59:59Z", 10.0, 10.0);
        persist(Room.BEDROOM, "2026-09-10T08:00:00Z", 18.0, 40.0);
        persist(Room.BEDROOM, "2026-09-10T20:00:00Z", 22.0, 60.0);
        persist(Room.BEDROOM, "2026-09-11T12:00:00Z", 24.0, 55.0);
        persist(Room.OFFICE, "2026-09-10T10:00:00Z", 100.0, 100.0);
        persist(Room.BEDROOM, "2026-09-12T00:00:00Z", 30.0, 70.0);
        entityManager.flush();

        List<AverageMeasurementProjection> result = measurementRepository
                .loadDailyAverageProjections(Room.BEDROOM.name(), from, to);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getBucket()).isEqualTo(startOfDay("2026-09-10"));
        assertThat(result.get(0).getAvgTemp()).isEqualTo(20.0);
        assertThat(result.get(0).getAvgHumidity()).isEqualTo(50.0);
        assertThat(result.get(1).getBucket()).isEqualTo(startOfDay("2026-09-11"));
        assertThat(result.get(1).getAvgTemp()).isEqualTo(24.0);
        assertThat(result.get(1).getAvgHumidity()).isEqualTo(55.0);
    }

    private Instant startOfDay(String date) {
        return LocalDate.parse(date)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
    }

    private Measurement persist(
            Room room,
            String timestamp,
            Double temperature,
            Double humidity) {
        Measurement measurement = measurement(
                room,
                OffsetDateTime.parse(timestamp),
                temperature,
                humidity,
                90);
        entityManager.persist(measurement);
        return measurement;
    }

    private Measurement measurement(
            Room room,
            OffsetDateTime timestamp,
            Double temperature,
            Double humidity,
            Integer batteryPercentage) {
        Measurement measurement = new Measurement();
        measurement.setRoom(room);
        measurement.setTs(timestamp);
        measurement.setTemperature(temperature);
        measurement.setHumidity(humidity);
        measurement.setBatteryPercentage(batteryPercentage);

        return measurement;
    }
}
