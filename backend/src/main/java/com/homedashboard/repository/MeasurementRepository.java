package com.homedashboard.repository;

import com.homedashboard.model.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    List<Measurement> findByRoom(String room);

    Optional<Measurement> findFirstByRoomOrderByTsDesc(String room);
}
