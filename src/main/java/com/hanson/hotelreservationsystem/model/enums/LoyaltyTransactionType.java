package com.hanson.hotelreservationsystem.model.enums;

/**
 * Enumeration of loyalty transaction types.
 * Used for tracking earning and redemption activity.
 */
public enum LoyaltyTransactionType {

    EARN("Points Earned", "Points earned from payment"),
    REDEEM("Points Redeemed", "Points redeemed for discount"),
    BONUS("Bonus Points", "Bonus points awarded"),
    ADJUSTMENT("Adjustment", "Manual balance adjustment"),
    EXPIRE("Points Expired", "Points expired due to inactivity"),
    REFUND("Points Refunded", "Points refunded due to cancellation");

    private final String displayName;
    private final String description;

    LoyaltyTransactionType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this transaction type adds points.
     */
    public boolean isCredit() {
        return this == EARN || this == BONUS || this == REFUND;
    }

    /**
     * Check if this transaction type removes points.
     */
    public boolean isDebit() {
        return this == REDEEM || this == EXPIRE;
    }

    @Override
    public String toString() {
        return displayName;
    }
}