package com.homedashboard.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ChartResolution {
    RAW("raw"),
    HOURLY("hourly"),
    DAILY("daily");

    private final String value;

    ChartResolution(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
