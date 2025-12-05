package com.hanson.hotelreservationsystem.model.enums;

/**
 * Enum representing the current status of a room.
 */
public enum RoomStatus {
    AVAILABLE("Available"),
    OCCUPIED("Occupied"),
    RESERVED("Reserved"),
    MAINTENANCE("Under Maintenance"),
    CLEANING("Being Cleaned");

    private final String displayName;

    RoomStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
