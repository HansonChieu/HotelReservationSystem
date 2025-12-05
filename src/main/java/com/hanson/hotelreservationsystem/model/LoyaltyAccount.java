package com.hanson.hotelreservationsystem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a guest's loyalty account.
 * Tracks points balance and provides earning/redemption functionality.
 */
@Entity
@Table(name = "loyalty_accounts", indexes = {
        @Index(name = "idx_loyalty_number", columnList = "loyalty_number"),
        @Index(name = "idx_loyalty_guest", columnList = "guest_id")
})
public class LoyaltyAccount extends BaseEntity {

    @Column(name = "loyalty_number", unique = true, nullable = false, length = 20)
    private String loyaltyNumber;

    @NotNull(message = "Guest is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false, unique = true)
    private Guest guest;

    @Min(value = 0, message = "Points balance cannot be negative")
    @Column(name = "points_balance", nullable = false)
    private Integer pointsBalance = 0;

    @Min(value = 0, message = "Lifetime points cannot be negative")
    @Column(name = "lifetime_points", nullable = false)
    private Integer lifetimePoints = 0; // Total points ever earned

    @Column(name = "tier", length = 20)
    private String tier = "BRONZE"; // BRONZE, SILVER, GOLD, PLATINUM

    @Column(name = "enrollment_date", nullable = false)
    private LocalDate enrollmentDate;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @Column(name = "is_active")
    private boolean active = true;

    // Relationships
    @OneToMany(mappedBy = "loyaltyAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LoyaltyTransaction> transactions = new ArrayList<>();

    // Constructors
    public LoyaltyAccount() {
        this.loyaltyNumber = generateLoyaltyNumber();
        this.enrollmentDate = LocalDate.now();
    }

    public LoyaltyAccount(Guest guest) {
        this();
        this.guest = guest;
    }

    // Business Methods

    /**
     * Generates a unique loyalty number.
     */
    private String generateLoyaltyNumber() {
        return "LOY" + System.currentTimeMillis() % 10000000 + (int)(Math.random() * 1000);
    }

    /**
     * Earns points based on payment amount.
     * Default: 1 point per dollar spent.
     * @param amount payment amount
     * @param earningRate points per dollar (configurable)
     * @return points earned
     */
    public int earnPoints(double amount, double earningRate) {
        int pointsEarned = (int) (amount * earningRate);
        this.pointsBalance += pointsEarned;
        this.lifetimePoints += pointsEarned;
        this.lastActivityDate = LocalDate.now();
        updateTier();
        return pointsEarned;
    }

    /**
     * Redeems points for discount.
     * @param points points to redeem
     * @param maxRedemptionCap maximum points that can be redeemed per reservation
     * @return actual points redeemed
     */
    public int redeemPoints(int points, int maxRedemptionCap) {
        int actualRedemption = Math.min(points, pointsBalance);
        actualRedemption = Math.min(actualRedemption, maxRedemptionCap);

        if (actualRedemption > 0) {
            this.pointsBalance -= actualRedemption;
            this.lastActivityDate = LocalDate.now();
        }

        return actualRedemption;
    }

    /**
     * Calculates discount amount from points.
     * Default: 100 points = $1 discount.
     * @param points points to convert
     * @param conversionRate points per dollar
     * @return discount amount in dollars
     */
    public double calculateDiscountFromPoints(int points, double conversionRate) {
        return points / conversionRate;
    }

    /**
     * Checks if the account has enough points.
     */
    public boolean hasEnoughPoints(int requiredPoints) {
        return pointsBalance >= requiredPoints;
    }

    /**
     * Updates the membership tier based on lifetime points.
     */
    private void updateTier() {
        if (lifetimePoints >= 50000) {
            this.tier = "PLATINUM";
        } else if (lifetimePoints >= 25000) {
            this.tier = "GOLD";
        } else if (lifetimePoints >= 10000) {
            this.tier = "SILVER";
        } else {
            this.tier = "BRONZE";
        }
    }

    /**
     * Gets tier benefits multiplier for earning bonus.
     */
    public double getTierBonusMultiplier() {
        return switch (tier) {
            case "PLATINUM" -> 2.0;
            case "GOLD" -> 1.5;
            case "SILVER" -> 1.25;
            default -> 1.0;
        };
    }

    /**
     * Adds a transaction to the account history.
     */
    public void addTransaction(LoyaltyTransaction transaction) {
        transactions.add(transaction);
        transaction.setLoyaltyAccount(this);
    }

    // Getters and Setters
    public String getLoyaltyNumber() {
        return loyaltyNumber;
    }

    public void setLoyaltyNumber(String loyaltyNumber) {
        this.loyaltyNumber = loyaltyNumber;
    }

    public Guest getGuest() {
        return guest;
    }

    public void setGuest(Guest guest) {
        this.guest = guest;
    }

    public Integer getPointsBalance() {
        return pointsBalance;
    }

    public void setPointsBalance(Integer pointsBalance) {
        this.pointsBalance = pointsBalance;
    }

    public Integer getLifetimePoints() {
        return lifetimePoints;
    }

    public void setLifetimePoints(Integer lifetimePoints) {
        this.lifetimePoints = lifetimePoints;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public LocalDate getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDate enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public LocalDate getLastActivityDate() {
        return lastActivityDate;
    }

    public void setLastActivityDate(LocalDate lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<LoyaltyTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<LoyaltyTransaction> transactions) {
        this.transactions = transactions;
    }

    @Override
    public String toString() {
        return "LoyaltyAccount{" +
                "loyaltyNumber='" + loyaltyNumber + '\'' +
                ", guest=" + (guest != null ? guest.getFullName() : "null") +
                ", pointsBalance=" + pointsBalance +
                ", tier='" + tier + '\'' +
                ", active=" + active +
                '}';
    }
}
