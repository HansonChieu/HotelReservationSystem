package com.hanson.hotelreservationsystem.model;

import com.hanson.hotelreservationsystem.model.enums.RoomType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a waitlist entry when rooms are unavailable.
 * Used for Observer pattern notifications when availability changes.
 */
@Entity
@Table(name = "waitlist_entries", indexes = {
        @Index(name = "idx_waitlist_guest", columnList = "guest_id"),
        @Index(name = "idx_waitlist_room_type", columnList = "desired_room_type"),
        @Index(name = "idx_waitlist_dates", columnList = "desired_check_in, desired_check_out"),
        @Index(name = "idx_waitlist_status", columnList = "status")
})
public class WaitlistEntry extends BaseEntity {

    @NotNull(message = "Guest is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    @NotNull(message = "Desired room type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "desired_room_type", nullable = false, length = 20)
    private RoomType desiredRoomType;

    @NotNull(message = "Desired check-in date is required")
    @FutureOrPresent(message = "Check-in date must be today or in the future")
    @Column(name = "desired_check_in", nullable = false)
    private LocalDate desiredCheckIn;

    @NotNull(message = "Desired check-out date is required")
    @Future(message = "Check-out date must be in the future")
    @Column(name = "desired_check_out", nullable = false)
    private LocalDate desiredCheckOut;

    @NotNull(message = "Number of guests is required")
    @Min(value = 1, message = "At least one guest is required")
    @Column(name = "num_guests", nullable = false)
    private Integer numGuests;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "WAITING"; // WAITING, NOTIFIED, CONVERTED, EXPIRED, CANCELLED

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    @Column(name = "added_by", length = 50)
    private String addedBy; // Admin who added to waitlist

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    @Column(name = "reservation_id")
    private Long reservationId; // Link to created reservation if converted

    @Size(max = 200, message = "Notes cannot exceed 200 characters")
    @Column(name = "notes", length = 200)
    private String notes;

    @Column(name = "priority")
    private Integer priority = 0; // Higher = higher priority

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    // Constructors
    public WaitlistEntry() {
        this.addedAt = LocalDateTime.now();
    }

    public WaitlistEntry(Guest guest, RoomType desiredRoomType, LocalDate desiredCheckIn,
                         LocalDate desiredCheckOut, int numGuests) {
        this();
        this.guest = guest;
        this.desiredRoomType = desiredRoomType;
        this.desiredCheckIn = desiredCheckIn;
        this.desiredCheckOut = desiredCheckOut;
        this.numGuests = numGuests;
        this.contactPhone = guest.getPhone();
        this.contactEmail = guest.getEmail();
    }

    // Business Methods

    /**
     * Checks if this waitlist entry matches the given availability.
     */
    public boolean matchesAvailability(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        return this.desiredRoomType == roomType &&
                !this.desiredCheckIn.isBefore(checkIn) &&
                !this.desiredCheckOut.isAfter(checkOut);
    }

    /**
     * Checks if this waitlist entry overlaps with the given date range.
     */
    public boolean overlapsWithDateRange(LocalDate checkIn, LocalDate checkOut) {
        return !desiredCheckOut.isBefore(checkIn) && !desiredCheckIn.isAfter(checkOut);
    }

    /**
     * Marks the entry as notified.
     */
    public void markNotified() {
        this.status = "NOTIFIED";
        this.notifiedAt = LocalDateTime.now();
    }

    /**
     * Converts the waitlist entry to a reservation.
     */
    public void convertToReservation(Long reservationId) {
        this.status = "CONVERTED";
        this.convertedAt = LocalDateTime.now();
        this.reservationId = reservationId;
    }

    /**
     * Cancels the waitlist entry.
     */
    public void cancel() {
        this.status = "CANCELLED";
    }

    /**
     * Marks the entry as expired.
     */
    public void expire() {
        this.status = "EXPIRED";
    }

    /**
     * Checks if the entry is still active (waiting or notified).
     */
    public boolean isActive() {
        return "WAITING".equals(status) || "NOTIFIED".equals(status);
    }

    /**
     * Checks if the entry has expired (check-in date has passed).
     */
    public boolean hasExpired() {
        return LocalDate.now().isAfter(desiredCheckIn);
    }

    /**
     * Gets the number of nights requested.
     */
    public long getNumberOfNights() {
        return java.time.temporal.ChronoUnit.DAYS.between(desiredCheckIn, desiredCheckOut);
    }

    // Getters and Setters
    public Guest getGuest() {
        return guest;
    }

    public void setGuest(Guest guest) {
        this.guest = guest;
    }

    public RoomType getDesiredRoomType() {
        return desiredRoomType;
    }

    public void setDesiredRoomType(RoomType desiredRoomType) {
        this.desiredRoomType = desiredRoomType;
    }

    public LocalDate getDesiredCheckIn() {
        return desiredCheckIn;
    }

    public void setDesiredCheckIn(LocalDate desiredCheckIn) {
        this.desiredCheckIn = desiredCheckIn;
    }

    public LocalDate getDesiredCheckOut() {
        return desiredCheckOut;
    }

    public void setDesiredCheckOut(LocalDate desiredCheckOut) {
        this.desiredCheckOut = desiredCheckOut;
    }

    public Integer getNumGuests() {
        return numGuests;
    }

    public void setNumGuests(Integer numGuests) {
        this.numGuests = numGuests;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    public LocalDateTime getNotifiedAt() {
        return notifiedAt;
    }

    public void setNotifiedAt(LocalDateTime notifiedAt) {
        this.notifiedAt = notifiedAt;
    }

    public LocalDateTime getConvertedAt() {
        return convertedAt;
    }

    public void setConvertedAt(LocalDateTime convertedAt) {
        this.convertedAt = convertedAt;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    @Override
    public String toString() {
        return "WaitlistEntry{" +
                "guest=" + (guest != null ? guest.getFullName() : "null") +
                ", desiredRoomType=" + desiredRoomType +
                ", checkIn=" + desiredCheckIn +
                ", checkOut=" + desiredCheckOut +
                ", status='" + status + '\'' +
                '}';
    }
}
