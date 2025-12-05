package com.hanson.hotelreservationsystem.model.enums;

/**
 * Enum representing different room types available in the hotel.
 * Each room type has specific occupancy limits and base pricing.
 */
public enum RoomType {
    SINGLE("Single Room", 2, 100.00),
    DOUBLE("Double Room", 4, 150.00),
    DELUXE("Deluxe Room", 2, 250.00),
    PENTHOUSE("Penthouse Suite", 2, 500.00);

    private final String displayName;
    private final int maxOccupancy;
    private final double basePrice;

    RoomType(String displayName, int maxOccupancy, double basePrice) {
        this.displayName = displayName;
        this.maxOccupancy = maxOccupancy;
        this.basePrice = basePrice;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxOccupancy() {
        return maxOccupancy;
    }

    public double getBasePrice() {
        return basePrice;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
