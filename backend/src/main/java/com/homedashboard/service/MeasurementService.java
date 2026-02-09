package com.homedashboard.service;

import com.homedashboard.model.Room;
import com.homedashboard.model.Measurement;
import com.homedashboard.repository.MeasurementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MeasurementService {

    @Autowired
    private MeasurementRepository measurementRepository;

    public List<Measurement> list() {
        return measurementRepository.findAll();
    }

    public Measurement create(Measurement measurement) {
        return measurementRepository.save(measurement);
    }

    public Optional<Measurement> getLatest(Room room) {
        return measurementRepository.findTopByRoomOrderByTsDesc(room);
    }
}
