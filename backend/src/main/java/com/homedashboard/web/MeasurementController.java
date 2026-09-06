package com.homedashboard.web;

import com.homedashboard.exception.MeasurementNotFoundException;
import com.homedashboard.exception.NoSuchRangeException;
import com.homedashboard.exception.NoSuchRoomException;
import com.homedashboard.model.ChartDataResponse;
import com.homedashboard.model.ChartRange;
import com.homedashboard.model.Measurement;
import com.homedashboard.model.Room;
import com.homedashboard.service.MeasurementService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
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
        Room parsedRoom = Room.from(room)
                .orElseThrow(
                        () -> new NoSuchRoomException(
                                "No such room: " + room,
                                "NO_SUCH_ROOM",
                                HttpStatus.BAD_REQUEST));

        return measurementService.getLatest(parsedRoom)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new MeasurementNotFoundException(
                        "No measurement found for room: " + room,
                        "NO_LATEST_MEASUREMENT",
                        HttpStatus.NOT_FOUND));
    }

    @GetMapping("/{room}/chart/{range}")
    public ResponseEntity<ChartDataResponse> getChartData(
            @PathVariable String room,
            @PathVariable String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        Optional<Room> parsedRoom = Room.from(room);
        Optional<ChartRange> parsedRange = ChartRange.from(range);

        if (parsedRoom.isEmpty()) {
            throw new NoSuchRoomException(
                    "No such room: " + room,
                    "NO_SUCH_ROOM",
                    HttpStatus.BAD_REQUEST);
        }

        if (parsedRange.isEmpty()) {
            throw new NoSuchRangeException(
                    "No such range: " + range,
                    "NO_SUCH_RANGE",
                    HttpStatus.BAD_REQUEST);
        }

        if (to == null) {
            return ResponseEntity.ok(measurementService.getChartData(
                    parsedRoom.get(),
                    parsedRange.get()));
        }

        return ResponseEntity.ok(measurementService.getChartData(
                parsedRoom.get(),
                parsedRange.get(),
                to));
    }
}
