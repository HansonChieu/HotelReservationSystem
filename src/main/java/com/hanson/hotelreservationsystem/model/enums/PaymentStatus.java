package com.hanson.hotelreservationsystem.model.enums;

/**
 * Enum representing the status of a payment transaction.
 */
public enum PaymentStatus {
    PENDING("Pending", "Payment is being processed"),
    COMPLETED("Completed", "Payment was successful"),
    FAILED("Failed", "Payment was declined or failed"),
    CANCELLED("Cancelled", "Payment was cancelled"),
    REFUNDED("Refunded", "Payment was fully refunded"),
    PARTIALLY_REFUNDED("Partially Refunded", "Payment was partially refunded");

    private final String displayName;
    private final String description;

    PaymentStatus(String displayName, String description) {
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
     * Check if this status represents a successful payment.
     */
    public boolean isSuccessful() {
        return this == COMPLETED || this == PARTIALLY_REFUNDED;
    }

    /**
     * Check if this status allows refund.
     */
    public boolean canRefund() {
        return this == COMPLETED || this == PARTIALLY_REFUNDED;
    }

    /**
     * Check if this is a terminal status (no further changes expected).
     */
    public boolean isTerminal() {
        return this == FAILED || this == CANCELLED || this == REFUNDED;
    }

    @Override
    public String toString() {
        return displayName;
    }
}