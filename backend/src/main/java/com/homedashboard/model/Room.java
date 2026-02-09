package com.homedashboard.model;

import java.util.Optional;

public enum Room {
    LIVINGROOM,
    BEDROOM,
    OFFICE,
    KITCHEN,
    TOILET;

    public static Optional<Room> from(String value) {
        if (value == null) return Optional.empty();

        String normalized = value.trim().toUpperCase();
        if (normalized.isEmpty()) return Optional.empty();

        try {
            return Optional.of(Room.valueOf(normalized));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
