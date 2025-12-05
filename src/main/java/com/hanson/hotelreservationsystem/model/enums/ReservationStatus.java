package com.hanson.hotelreservationsystem.model.enums;

/**
 * Enum representing the lifecycle status of a reservation.
 */
public enum ReservationStatus {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    CHECKED_IN("Checked In"),
    CHECKED_OUT("Checked Out"),
    CANCELLED("Cancelled"),
    NO_SHOW("No Show");

    private final String displayName;

    ReservationStatus(String displayName) {
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
