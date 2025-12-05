package com.hanson.hotelreservationsystem.service;

import com.hanson.hotelreservationsystem.model.enums.RoomType;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * Represents a room suggestion for group bookings.
 * Moved to a standalone class to fix access issues.
 */
public class RoomSuggestion {
    private String name;
    private String description;
    private Map<RoomType, Integer> rooms = new EnumMap<>(RoomType.class);
    private BigDecimal totalPrice;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<RoomType, Integer> getRooms() {
        return rooms;
    }

    public void setRooms(Map<RoomType, Integer> rooms) {
        this.rooms = rooms;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public boolean isValid() {
        return rooms != null && !rooms.isEmpty() && totalPrice != null;
    }

    /**
     * Get a formatted summary of the room selection.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<RoomType, Integer> entry : rooms.entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(entry.getValue()).append("x ").append(entry.getKey().getDisplayName());
        }
        return sb.toString();
    }
}