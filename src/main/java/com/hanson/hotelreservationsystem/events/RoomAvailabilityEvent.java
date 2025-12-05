package com.hanson.hotelreservationsystem.events;

import com.hanson.hotelreservationsystem.model.enums.RoomType;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Event object representing a room availability change.
 *
 * This event is triggered when:
 * - A room becomes available after checkout
 * - A reservation is cancelled
 * - A room is taken out of service
 * - A room is restored to service
 *
 * Used with the Observer pattern to notify administrators and
 * process waitlist entries.
 */
public class RoomAvailabilityEvent {

    private final RoomType roomType;
    private final String roomNumber;
    private final LocalDate availableFrom;
    private final boolean isAvailable;
    private final LocalDateTime eventTimestamp;
    private final String eventSource;

    /**
     * Constructor for basic availability event.
     *
     * @param roomType The type of room
     * @param roomNumber The room number
     * @param availableFrom The date when the room becomes available
     * @param isAvailable true if the room is now available, false otherwise
     */
    public RoomAvailabilityEvent(RoomType roomType, String roomNumber,
                                 LocalDate availableFrom, boolean isAvailable) {
        this.roomType = roomType;
        this.roomNumber = roomNumber;
        this.availableFrom = availableFrom;
        this.isAvailable = isAvailable;
        this.eventTimestamp = LocalDateTime.now();
        this.eventSource = "SYSTEM";
    }

    /**
     * Constructor with event source.
     *
     * @param roomType The type of room
     * @param roomNumber The room number
     * @param availableFrom The date when the room becomes available
     * @param isAvailable true if the room is now available
     * @param eventSource The source of the event (e.g., "CHECKOUT", "CANCELLATION")
     */
    public RoomAvailabilityEvent(RoomType roomType, String roomNumber,
                                 LocalDate availableFrom, boolean isAvailable,
                                 String eventSource) {
        this.roomType = roomType;
        this.roomNumber = roomNumber;
        this.availableFrom = availableFrom;
        this.isAvailable = isAvailable;
        this.eventTimestamp = LocalDateTime.now();
        this.eventSource = eventSource;
    }

    // Getters

    public RoomType getRoomType() {
        return roomType;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public LocalDate getAvailableFrom() {
        return availableFrom;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public String getEventSource() {
        return eventSource;
    }

    /**
     * Get a human-readable description of the event.
     */
    public String getDescription() {
        if (isAvailable) {
            return String.format("Room %s (%s) is now available from %s",
                    roomNumber, roomType.getDisplayName(), availableFrom);
        } else {
            return String.format("Room %s (%s) is no longer available",
                    roomNumber, roomType.getDisplayName());
        }
    }

    @Override
    public String toString() {
        return "RoomAvailabilityEvent{" +
                "roomType=" + roomType +
                ", roomNumber='" + roomNumber + '\'' +
                ", availableFrom=" + availableFrom +
                ", isAvailable=" + isAvailable +
                ", eventTimestamp=" + eventTimestamp +
                ", eventSource='" + eventSource + '\'' +
                '}';
    }
}