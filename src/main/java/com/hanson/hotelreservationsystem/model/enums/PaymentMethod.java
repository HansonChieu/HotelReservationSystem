package com.hanson.hotelreservationsystem.model.enums;

/**
 * Enum representing accepted payment methods.
 */
public enum PaymentMethod {
    CASH("Cash"),
    CARD("Credit/Debit Card"),
    LOYALTY_POINTS("Loyalty Points");

    private final String displayName;

    PaymentMethod(String displayName) {
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
