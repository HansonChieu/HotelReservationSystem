package com.hanson.hotelreservationsystem.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for dynamic pricing.
 *
 * Supports:
 * - Weekday/weekend multipliers
 * - Seasonal pricing periods with custom multipliers
 * - Tax rate configuration
 */
public class PricingConfig {

    // Multiplier for weekday rates (default: 1.0 = no change)
    private BigDecimal weekdayMultiplier;

    // Multiplier for weekend rates (Friday-Sunday, default: 1.2 = 20% higher)
    private BigDecimal weekendMultiplier;

    // Multiplier for seasonal/peak periods (default: 1.5 = 50% higher)
    private BigDecimal seasonalMultiplier;

    // List of seasonal pricing periods
    private List<SeasonalPeriod> seasonalPeriods;

    // Tax rate (default: 0.13 = 13%)
    private BigDecimal taxRate;

    /**
     * Default constructor with standard values.
     */
    public PricingConfig() {
        this.weekdayMultiplier = BigDecimal.ONE;
        this.weekendMultiplier = new BigDecimal("1.20");
        this.seasonalMultiplier = new BigDecimal("1.50");
        this.seasonalPeriods = new ArrayList<>();
        this.taxRate = new BigDecimal("0.13");
    }

    /**
     * Constructor with custom multipliers.
     */
    public PricingConfig(BigDecimal weekdayMultiplier, BigDecimal weekendMultiplier,
                         BigDecimal seasonalMultiplier, BigDecimal taxRate) {
        this.weekdayMultiplier = weekdayMultiplier;
        this.weekendMultiplier = weekendMultiplier;
        this.seasonalMultiplier = seasonalMultiplier;
        this.seasonalPeriods = new ArrayList<>();
        this.taxRate = taxRate;
    }

    /**
     * Add a seasonal pricing period.
     *
     * @param name Period name (e.g., "Summer Peak", "Holiday Season")
     * @param startDate Start date of the period
     * @param endDate End date of the period
     * @param multiplier Price multiplier for this period
     */
    public void addSeasonalPeriod(String name, LocalDate startDate,
                                  LocalDate endDate, BigDecimal multiplier) {
        seasonalPeriods.add(new SeasonalPeriod(name, startDate, endDate, multiplier));
    }

    // Getters and Setters

    public BigDecimal getWeekdayMultiplier() {
        return weekdayMultiplier;
    }

    public void setWeekdayMultiplier(BigDecimal weekdayMultiplier) {
        this.weekdayMultiplier = weekdayMultiplier;
    }

    public BigDecimal getWeekendMultiplier() {
        return weekendMultiplier;
    }

    public void setWeekendMultiplier(BigDecimal weekendMultiplier) {
        this.weekendMultiplier = weekendMultiplier;
    }

    public BigDecimal getSeasonalMultiplier() {
        return seasonalMultiplier;
    }

    public void setSeasonalMultiplier(BigDecimal seasonalMultiplier) {
        this.seasonalMultiplier = seasonalMultiplier;
    }

    public List<SeasonalPeriod> getSeasonalPeriods() {
        return seasonalPeriods;
    }

    public void setSeasonalPeriods(List<SeasonalPeriod> seasonalPeriods) {
        this.seasonalPeriods = seasonalPeriods;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    /**
     * Represents a seasonal pricing period.
     */
    public static class SeasonalPeriod {
        private String name;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal multiplier;

        public SeasonalPeriod() {
        }

        public SeasonalPeriod(String name, LocalDate startDate,
                              LocalDate endDate, BigDecimal multiplier) {
            this.name = name;
            this.startDate = startDate;
            this.endDate = endDate;
            this.multiplier = multiplier;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        public BigDecimal getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(BigDecimal multiplier) {
            this.multiplier = multiplier;
        }

        /**
         * Check if a date falls within this period.
         */
        public boolean contains(LocalDate date) {
            return !date.isBefore(startDate) && !date.isAfter(endDate);
        }
    }
}