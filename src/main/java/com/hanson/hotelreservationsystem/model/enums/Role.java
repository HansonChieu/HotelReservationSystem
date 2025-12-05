package com.hanson.hotelreservationsystem.model.enums;

/**
 * Enum representing administrator roles with different permission levels.
 * Admin can apply up to 15% discount, Manager can apply up to 30%.
 */
public enum Role {
    ADMIN("Administrator", 0.15),
    MANAGER("Manager", 0.30);

    private final String displayName;
    private final double maxDiscountRate;

    Role(String displayName, double maxDiscountRate) {
        this.displayName = displayName;
        this.maxDiscountRate = maxDiscountRate;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getMaxDiscountRate() {
        return maxDiscountRate;
    }

    /**
     * Returns the maximum discount percentage this role can apply.
     * @return discount percentage (e.g., 15 for 15%)
     */
    public int getMaxDiscountPercentage() {
        return (int) (maxDiscountRate * 100);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
