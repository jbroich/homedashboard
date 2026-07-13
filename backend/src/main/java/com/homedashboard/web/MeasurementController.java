package com.homedashboard.web;

import com.homedashboard.model.ChartDataResponse;
import com.homedashboard.model.ChartRange;
import com.homedashboard.model.Measurement;
import com.homedashboard.model.Room;
import com.homedashboard.service.MeasurementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@RestController
@RequestMapping("api/measurements")
public class MeasurementController {

    private final MeasurementService measurementService;

    public MeasurementController(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    @GetMapping("/{room}/latest")
    public ResponseEntity<Measurement> getLatest(@PathVariable String room) {
        return Room.from(room)
                .map(r -> measurementService.getLatest(r)
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build()))
                .orElse(ResponseEntity.badRequest().build());
    }

    @GetMapping("/{room}/chart/{range}")
    public ResponseEntity<ChartDataResponse> getChartData(
            @PathVariable String room,
            @PathVariable String range,
            @RequestParam(required = false) String to) {
        Optional<Room> parsedRoom = Room.from(room);
        Optional<ChartRange> parsedRange = ChartRange.from(range);

        if (parsedRoom.isEmpty() || parsedRange.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (to == null) {
            return ResponseEntity.ok(measurementService.getChartData(
                    parsedRoom.get(),
                    parsedRange.get()));
        }

        try {
            OffsetDateTime parsedTo = OffsetDateTime.parse(to);
            return ResponseEntity.ok(measurementService.getChartData(
                    parsedRoom.get(),
                    parsedRange.get(),
                    parsedTo));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
