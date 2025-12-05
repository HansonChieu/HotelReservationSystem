package com.hanson.hotelreservationsystem.model.enums;

import java.math.BigDecimal;

public enum AddOnType {
    WIFI("Wi-Fi", new BigDecimal("15.00"), PricingModel.PER_NIGHT),
    BREAKFAST("Breakfast", new BigDecimal("25.00"), PricingModel.PER_PERSON_PER_NIGHT),
    PARKING("Parking", new BigDecimal("20.00"), PricingModel.PER_NIGHT),
    SPA("Spa Package", new BigDecimal("75.00"), PricingModel.PER_PERSON);

    private final String displayName;
    private final BigDecimal basePrice;
    private final PricingModel pricingModel;

    AddOnType(String displayName, BigDecimal basePrice, PricingModel pricingModel) {
        this.displayName = displayName;
        this.basePrice = basePrice;
        this.pricingModel = pricingModel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public PricingModel getPricingModel() {
        return pricingModel;
    }

    public enum PricingModel {
        PER_NIGHT,           // Wi-Fi, Parking
        PER_PERSON,          // Spa (one-time)
        PER_PERSON_PER_NIGHT // Breakfast
    }
}