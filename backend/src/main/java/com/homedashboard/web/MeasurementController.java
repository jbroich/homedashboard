package com.homedashboard.web;

import com.homedashboard.model.Room;
import com.homedashboard.model.Measurement;
import com.homedashboard.service.MeasurementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/rooms")
public class MeasurementController {

    private final MeasurementService measurementService;

    public MeasurementController(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }
    
    @PostMapping("/create")
    public Measurement createMeasurement(@RequestBody Measurement measurement) {
        return measurementService.create(measurement);
    }
    
    @GetMapping("/{room}/latest")
    public ResponseEntity<Measurement> getLatest(@PathVariable String room) {
        return Room.from(room)
                .map(r -> measurementService.getLatest(r)
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build()))
                .orElse(ResponseEntity.badRequest().build());
    }

}
