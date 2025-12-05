package com.hanson.hotelreservationsystem.events;

/**
 * Observer interface for room availability changes.
 *
 * Implementations of this interface can subscribe to receive notifications
 * when room availability changes (e.g., after checkout or cancellation).
 *
 * This is part of the Observer pattern implementation as required by the project.
 * Used to notify administrators when rooms become available for waitlist processing.
 */
public interface RoomAvailabilityObserver {

    /**
     * Called when room availability changes.
     *
     * @param event The availability change event containing details
     */
    void onAvailabilityChange(RoomAvailabilityEvent event);

    /**
     * Get a unique identifier for this observer.
     * Used for managing subscriptions.
     *
     * @return Observer identifier
     */
    default String getObserverId() {
        return this.getClass().getSimpleName() + "@" + System.identityHashCode(this);
    }

    /**
     * Check if this observer is interested in a specific room type.
     * Default implementation returns true (interested in all types).
     *
     * @param event The availability event
     * @return true if this observer should receive the notification
     */
    default boolean isInterestedIn(RoomAvailabilityEvent event) {
        return true;
    }
}