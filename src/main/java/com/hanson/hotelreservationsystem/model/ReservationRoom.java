package com.hanson.hotelreservationsystem.model;

import com.hanson.hotelreservationsystem.model.enums.RoomType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Junction entity linking reservations to rooms.
 * Supports group bookings where one reservation can have multiple rooms.
 */
@Entity
@Table(name = "reservation_rooms", indexes = {
        @Index(name = "idx_resroom_reservation", columnList = "reservation_id"),
        @Index(name = "idx_resroom_room", columnList = "room_id")
})
public class ReservationRoom extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type")
    private RoomType roomType;

    @NotNull(message = "Reservation is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @NotNull(message = "Room is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @NotNull(message = "Number of guests is required")
    @Min(value = 1, message = "At least one guest is required")
    @Column(name = "num_guests", nullable = false)
    private Integer numGuests;

    @NotNull(message = "Room price is required")
    @DecimalMin(value = "0.0", message = "Room price cannot be negative")
    @Column(name = "room_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal roomPrice;

    @Column(name = "price_multiplier", precision = 5, scale = 2)
    private BigDecimal priceMultiplier = BigDecimal.ONE; // For dynamic pricing

    // Constructors
    public ReservationRoom() {}

    public ReservationRoom(Reservation reservation, Room room, int numGuests) {
        this.reservation = reservation;
        this.room = room;
        this.numGuests = numGuests;
        this.roomPrice = BigDecimal.valueOf(room.getBasePrice());
    }

    public ReservationRoom(Reservation reservation, Room room, int numGuests, BigDecimal roomPrice) {
        this.reservation = reservation;
        this.room = room;
        this.numGuests = numGuests;
        this.roomPrice = roomPrice;
    }

    // Business Methods

    /**
     * Validates that the number of guests doesn't exceed room capacity.
     */
    public boolean isValidOccupancy() {
        return room != null && room.canAccommodate(numGuests);
    }

    /**
     * Gets the effective room price with any multiplier applied.
     */
    public BigDecimal getEffectivePrice() {
        return roomPrice.multiply(priceMultiplier);
    }

    /**
     * Calculates total price for this room for all nights.
     */
    public BigDecimal calculateTotalPrice(long nights) {
        return getEffectivePrice().multiply(BigDecimal.valueOf(nights));
    }

    // Getters and Setters
    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Integer getNumGuests() {
        return numGuests;
    }

    public void setNumGuests(Integer numGuests) {
        this.numGuests = numGuests;
    }

    public BigDecimal getRoomPrice() {
        return roomPrice;
    }

    public void setRoomPrice(BigDecimal roomPrice) {
        this.roomPrice = roomPrice;
    }

    public BigDecimal getPriceMultiplier() {
        return priceMultiplier;
    }

    public void setPriceMultiplier(BigDecimal priceMultiplier) {
        this.priceMultiplier = priceMultiplier;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    @Override
    public String toString() {
        return "ReservationRoom{" +
                "reservationId=" + (reservation != null ? reservation.getId() : "null") +
                ", room=" + (room != null ? room.getRoomNumber() : "null") +
                ", numGuests=" + numGuests +
                ", roomPrice=" + roomPrice +
                '}';
    }
}
