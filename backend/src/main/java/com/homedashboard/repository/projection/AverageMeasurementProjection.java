package com.homedashboard.repository.projection;

import java.time.Instant;

public interface AverageMeasurementProjection {
    Instant getBucket();

    Double getAvgTemp();

    Double getAvgHumidity();
}
