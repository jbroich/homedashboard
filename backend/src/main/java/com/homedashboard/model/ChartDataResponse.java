package com.homedashboard.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public record ChartDataResponse(
        Room room,
        ChartRange range,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime from,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime to,
        ChartResolution resolution,
        List<ChartPoint> points) {

    public ChartDataResponse {
        Objects.requireNonNull(room, "room darf nicht null sein");
        Objects.requireNonNull(range, "range darf nicht null sein");
        Objects.requireNonNull(from, "from darf nicht null sein");
        Objects.requireNonNull(to, "to darf nicht null sein");
        Objects.requireNonNull(resolution, "resolution darf nicht null sein");
        points = List.copyOf(Objects.requireNonNull(points, "points darf nicht null sein"));
    }
}
