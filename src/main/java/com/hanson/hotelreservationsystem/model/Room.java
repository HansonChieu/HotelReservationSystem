package com.hanson.hotelreservationsystem.model;

import com.hanson.hotelreservationsystem.model.enums.RoomStatus;
import com.hanson.hotelreservationsystem.model.enums.RoomType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a hotel room.
 * Contains room details, type, status, and pricing information.
 */
@Entity
@Table(name = "rooms", indexes = {
        @Index(name = "idx_room_number", columnList = "room_number"),
        @Index(name = "idx_room_type", columnList = "room_type"),
        @Index(name = "idx_room_status", columnList = "status")
})
public class Room extends BaseEntity {

    @NotBlank(message = "Room number is required")
    @Column(name = "room_number", nullable = false, unique = true, length = 10)
    private String roomNumber;

    @NotNull(message = "Room type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 20)
    private RoomType roomType;

    @NotNull(message = "Room status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RoomStatus status = RoomStatus.AVAILABLE;

    @NotNull(message = "Floor number is required")
    @Min(value = 1, message = "Floor must be at least 1")
    @Max(value = 100, message = "Floor cannot exceed 100")
    @Column(name = "floor", nullable = false)
    private Integer floor;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "has_view")
    private boolean hasView = false;

    @Column(name = "is_smoking")
    private boolean smoking = false;

    @Column(name = "is_accessible")
    private boolean accessible = false;

    @DecimalMin(value = "0.0", message = "Base price cannot be negative")
    @Column(name = "base_price_override")
    private Double basePriceOverride; // Override the enum's base price if needed

    // Relationships
    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    private List<ReservationRoom> reservationRooms = new ArrayList<>();

    // Constructors
    public Room() {}

    public Room(String roomNumber, RoomType roomType, Integer floor) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.floor = floor;
        this.status = RoomStatus.AVAILABLE;
    }

    // Business Methods

    /**
     * Gets the effective base price for this room.
     * Uses override price if set, otherwise uses the room type's base price.
     */
    public double getBasePrice() {
        return basePriceOverride != null ? basePriceOverride : roomType.getBasePrice();
    }

    /**
     * Gets the maximum occupancy for this room based on its type.
     */
    public int getMaxOccupancy() {
        return roomType.getMaxOccupancy();
    }

    /**
     * Checks if the room can accommodate the given number of guests.
     */
    public boolean canAccommodate(int numberOfGuests) {
        return numberOfGuests > 0 && numberOfGuests <= getMaxOccupancy();
    }

    /**
     * Checks if the room is available for booking.
     */
    public boolean isAvailable() {
        return status == RoomStatus.AVAILABLE;
    }

    /**
     * Marks the room as occupied.
     */
    public void markOccupied() {
        this.status = RoomStatus.OCCUPIED;
    }

    /**
     * Marks the room as reserved.
     */
    public void markReserved() {
        this.status = RoomStatus.RESERVED;
    }

    /**
     * Marks the room as available.
     */
    public void markAvailable() {
        this.status = RoomStatus.AVAILABLE;
    }

    /**
     * Marks the room for cleaning.
     */
    public void markForCleaning() {
        this.status = RoomStatus.CLEANING;
    }

    /**
     * Marks the room under maintenance.
     */
    public void markUnderMaintenance() {
        this.status = RoomStatus.MAINTENANCE;
    }

    // Getters and Setters
    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isHasView() {
        return hasView;
    }

    public void setHasView(boolean hasView) {
        this.hasView = hasView;
    }

    public boolean isSmoking() {
        return smoking;
    }

    public void setSmoking(boolean smoking) {
        this.smoking = smoking;
    }

    public boolean isAccessible() {
        return accessible;
    }

    public void setAccessible(boolean accessible) {
        this.accessible = accessible;
    }

    public Double getBasePriceOverride() {
        return basePriceOverride;
    }

    public void setBasePriceOverride(Double basePriceOverride) {
        this.basePriceOverride = basePriceOverride;
    }

    public List<ReservationRoom> getReservationRooms() {
        return reservationRooms;
    }

    public void setReservationRooms(List<ReservationRoom> reservationRooms) {
        this.reservationRooms = reservationRooms;
    }

    @Override
    public String toString() {
        return "Room{" +
                "roomNumber='" + roomNumber + '\'' +
                ", roomType=" + roomType +
                ", status=" + status +
                ", floor=" + floor +
                ", basePrice=" + getBasePrice() +
                '}';
    }


}
