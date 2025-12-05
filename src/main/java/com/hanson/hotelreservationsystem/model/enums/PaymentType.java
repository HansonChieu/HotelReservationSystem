package com.hanson.hotelreservationsystem.model.enums;

/**
 * Enum representing the type of payment transaction.
 */
public enum PaymentType {
    DEPOSIT("Deposit"),
    PARTIAL("Partial Payment"),
    FULL("Full Payment"),
    REFUND("Refund");

    private final String displayName;

    PaymentType(String displayName) {
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
