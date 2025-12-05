package com.hanson.hotelreservationsystem.model.enums;

/**
 * Enum representing sentiment tags for feedback categorization.
 */
public enum SentimentTag {
    POSITIVE("Positive"),
    NEUTRAL("Neutral"),
    NEGATIVE("Negative");

    private final String displayName;

    SentimentTag(String displayName) {
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
