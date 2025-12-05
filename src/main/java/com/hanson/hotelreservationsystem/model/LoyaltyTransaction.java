package com.hanson.hotelreservationsystem.model;

import com.hanson.hotelreservationsystem.model.enums.LoyaltyTransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * Entity representing a loyalty points transaction.
 * Provides audit trail for all point earning and redemption activities.
 */
@Entity
@Table(name = "loyalty_transactions", indexes = {
        @Index(name = "idx_loytrans_account", columnList = "loyalty_account_id"),
        @Index(name = "idx_loytrans_date", columnList = "transaction_date"),
        @Index(name = "idx_loytrans_type", columnList = "transaction_type")
})
public class LoyaltyTransaction extends BaseEntity {

    @NotNull(message = "Loyalty account is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loyalty_account_id", nullable = false)
    private LoyaltyAccount loyaltyAccount;

    @NotNull(message = "Transaction type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private LoyaltyTransactionType transactionType;

    @NotNull(message = "Points amount is required")
    @Column(name = "points", nullable = false)
    private Integer points; // Positive for earned, negative for redeemed/expired

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @NotNull(message = "Transaction date is required")
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "reservation_id")
    private Long reservationId; // Link to related reservation if applicable

    @Size(max = 200, message = "Description cannot exceed 200 characters")
    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "processed_by", length = 50)
    private String processedBy; // Admin who processed manual adjustments

    // Constructors
    public LoyaltyTransaction() {
        this.transactionDate = LocalDateTime.now();
    }

    public LoyaltyTransaction(LoyaltyAccount account, LoyaltyTransactionType type, int points, String description) {
        this();
        this.loyaltyAccount = account;
        this.transactionType = type;
        this.points = points;
        this.description = description;
        this.balanceAfter = account.getPointsBalance();
    }

    // Factory Methods

    /**
     * Creates an EARNED transaction.
     */
    public static LoyaltyTransaction createEarnedTransaction(LoyaltyAccount account, int points, Long reservationId) {
        LoyaltyTransaction tx = new LoyaltyTransaction();
        tx.setLoyaltyAccount(account);
        tx.setTransactionType(LoyaltyTransactionType.EARN);
        tx.setPoints(points);
        tx.setReservationId(reservationId);
        tx.setBalanceAfter(account.getPointsBalance());
        tx.setDescription("Points earned from reservation #" + reservationId);
        return tx;
    }

    /**
     * Creates a REDEEMED transaction.
     */
    public static LoyaltyTransaction createRedeemedTransaction(LoyaltyAccount account, int points, Long reservationId) {
        LoyaltyTransaction tx = new LoyaltyTransaction();
        tx.setLoyaltyAccount(account);
        tx.setTransactionType(LoyaltyTransactionType.REDEEM);
        tx.setPoints(-Math.abs(points)); // Always negative for redemption
        tx.setReservationId(reservationId);
        tx.setBalanceAfter(account.getPointsBalance());
        tx.setDescription("Points redeemed for reservation #" + reservationId);
        return tx;
    }

    /**
     * Creates a BONUS transaction.
     */
    public static LoyaltyTransaction createBonusTransaction(LoyaltyAccount account, int points, String reason, String processedBy) {
        LoyaltyTransaction tx = new LoyaltyTransaction();
        tx.setLoyaltyAccount(account);
        tx.setTransactionType(LoyaltyTransactionType.BONUS);
        tx.setPoints(points);
        tx.setProcessedBy(processedBy);
        tx.setBalanceAfter(account.getPointsBalance());
        tx.setDescription("Bonus points: " + reason);
        return tx;
    }

    /**
     * Creates an ADJUSTMENT transaction.
     */
    public static LoyaltyTransaction createAdjustmentTransaction(LoyaltyAccount account, int points, String reason, String processedBy) {
        LoyaltyTransaction tx = new LoyaltyTransaction();
        tx.setLoyaltyAccount(account);
        tx.setTransactionType(LoyaltyTransactionType.ADJUSTMENT);
        tx.setPoints(points);
        tx.setProcessedBy(processedBy);
        tx.setBalanceAfter(account.getPointsBalance());
        tx.setDescription("Manual adjustment: " + reason);
        return tx;
    }

    // Business Methods

    /**
     * Checks if this is a credit (adding points) transaction.
     */
    public boolean isCredit() {
        return points > 0;
    }

    /**
     * Checks if this is a debit (removing points) transaction.
     */
    public boolean isDebit() {
        return points < 0;
    }

    /**
     * Gets the absolute value of points.
     */
    public int getAbsolutePoints() {
        return Math.abs(points);
    }

    // Getters and Setters
    public LoyaltyAccount getLoyaltyAccount() {
        return loyaltyAccount;
    }

    public void setLoyaltyAccount(LoyaltyAccount loyaltyAccount) {
        this.loyaltyAccount = loyaltyAccount;
    }

    public LoyaltyTransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(LoyaltyTransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Integer getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Integer balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    @Override
    public String toString() {
        return "LoyaltyTransaction{" +
                "transactionType=" + transactionType +
                ", points=" + points +
                ", balanceAfter=" + balanceAfter +
                ", transactionDate=" + transactionDate +
                ", description='" + description + '\'' +
                '}';
    }
}
