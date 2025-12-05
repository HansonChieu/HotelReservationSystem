package com.hanson.hotelreservationsystem.config;

import java.math.BigDecimal;

/**
 * Configuration for the loyalty program.
 *
 * Allows configuring:
 * - Points earning rate (points per dollar spent)
 * - Points redemption value (dollar value per point)
 * - Maximum points that can be redeemed per reservation
 * - Welcome bonus points for new members
 */
public class LoyaltyConfig {

    // Points earned per dollar spent (default: 1 point per $1)
    private BigDecimal earningRate;

    // Dollar value per point when redeeming (default: $0.01 per point)
    private BigDecimal redemptionValue;

    // Maximum points that can be redeemed in a single reservation
    private int maxRedemptionPoints;

    // Bonus points awarded when joining the program
    private int welcomeBonus;

    // Minimum points required for redemption
    private int minRedemptionPoints;

    // Points expiration in months (0 = never expires)
    private int pointsExpirationMonths;

    /**
     * Default constructor with standard values.
     */
    public LoyaltyConfig() {
        this.earningRate = BigDecimal.ONE;
        this.redemptionValue = new BigDecimal("0.01");
        this.maxRedemptionPoints = 10000;
        this.welcomeBonus = 100;
        this.minRedemptionPoints = 100;
        this.pointsExpirationMonths = 0; // Never expires
    }

    /**
     * Constructor with all parameters.
     */
    public LoyaltyConfig(BigDecimal earningRate, BigDecimal redemptionValue,
                         int maxRedemptionPoints, int welcomeBonus) {
        this.earningRate = earningRate;
        this.redemptionValue = redemptionValue;
        this.maxRedemptionPoints = maxRedemptionPoints;
        this.welcomeBonus = welcomeBonus;
        this.minRedemptionPoints = 100;
        this.pointsExpirationMonths = 0;
    }

    // Getters and Setters

    public BigDecimal getEarningRate() {
        return earningRate;
    }

    public void setEarningRate(BigDecimal earningRate) {
        this.earningRate = earningRate;
    }

    public BigDecimal getRedemptionValue() {
        return redemptionValue;
    }

    public void setRedemptionValue(BigDecimal redemptionValue) {
        this.redemptionValue = redemptionValue;
    }

    public int getMaxRedemptionPoints() {
        return maxRedemptionPoints;
    }

    public void setMaxRedemptionPoints(int maxRedemptionPoints) {
        this.maxRedemptionPoints = maxRedemptionPoints;
    }

    public int getWelcomeBonus() {
        return welcomeBonus;
    }

    public void setWelcomeBonus(int welcomeBonus) {
        this.welcomeBonus = welcomeBonus;
    }

    public int getMinRedemptionPoints() {
        return minRedemptionPoints;
    }

    public void setMinRedemptionPoints(int minRedemptionPoints) {
        this.minRedemptionPoints = minRedemptionPoints;
    }

    public int getPointsExpirationMonths() {
        return pointsExpirationMonths;
    }

    public void setPointsExpirationMonths(int pointsExpirationMonths) {
        this.pointsExpirationMonths = pointsExpirationMonths;
    }
}