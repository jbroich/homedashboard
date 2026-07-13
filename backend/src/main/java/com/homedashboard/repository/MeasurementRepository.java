package com.homedashboard.repository;

import com.homedashboard.model.Measurement;
import com.homedashboard.model.Room;
import com.homedashboard.repository.projection.AverageMeasurementProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    List<Measurement> findByRoom(String room);

    Optional<Measurement> findTopByRoomOrderByTsDesc(Room room);

    List<Measurement> findByRoomAndTsBetweenOrderByTsAsc(
            Room room,
            OffsetDateTime from,
            OffsetDateTime to);

    @Query(value = """
            select date_trunc('hour', ts) as bucket,
                AVG(temperature_c) as avgTemp, AVG(humidity_percent) as avgHumidity
                from measurement
                    where room = ?1
                        and ts >= ?2
                        and ts <= ?3
                group by bucket
                order by bucket""", nativeQuery = true)
    List<AverageMeasurementProjection> loadHourlyAverageProjections(
            String room,
            OffsetDateTime from,
            OffsetDateTime to);

    @Query(value = """
            select date_trunc('day', ts) as bucket,
                AVG(temperature_c) as avgTemp, AVG(humidity_percent) as avgHumidity
                from measurement
                    where room = ?1
                        and ts >= ?2
                        and ts <= ?3
                group by bucket
                order by bucket""", nativeQuery = true)
    List<AverageMeasurementProjection> loadDailyAverageProjections(
            String room,
            OffsetDateTime from,
            OffsetDateTime to);
}
