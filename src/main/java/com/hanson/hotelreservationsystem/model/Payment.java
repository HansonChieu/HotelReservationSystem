package com.hanson.hotelreservationsystem.model;

import com.hanson.hotelreservationsystem.model.enums.PaymentStatus;
import com.hanson.hotelreservationsystem.model.enums.PaymentMethod;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a payment for a reservation.
 * Tracks all payment transactions including method, amount, and status.
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_reservation", columnList = "reservation_id"),
        @Index(name = "idx_payment_date", columnList = "payment_date"),
        @Index(name = "idx_payment_status", columnList = "status")
})
public class Payment extends BaseEntity {

    @NotNull(message = "Reservation is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be at least $0.01")
    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    @Column(name = "payment_method", length = 30, nullable = false)
    private String paymentMethod;

    @NotNull(message = "Payment date is required")
    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @NotNull(message = "Payment status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    @Column(name = "card_type", length = 20)
    private String cardType;

    @Size(max = 200, message = "Notes cannot exceed 200 characters")
    @Column(name = "notes", length = 200)
    private String notes;

    @Column(name = "processed_by", length = 50)
    private String processedBy;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_date")
    private LocalDateTime refundDate;

    @Column(name = "refund_reason", length = 200)
    private String refundReason;

    // Constructors
    public Payment() {
        this.paymentDate = LocalDateTime.now();
        this.status = PaymentStatus.PENDING;
    }

    public Payment(BigDecimal amount, String paymentMethod) {
        this();
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }

    public Payment(Reservation reservation, BigDecimal amount, String paymentMethod) {
        this(amount, paymentMethod);
        this.reservation = reservation;
    }

    // Business Methods

    /**
     * Mark payment as completed.
     */
    public void markCompleted() {
        this.status = PaymentStatus.COMPLETED;
    }

    /**
     * Mark payment as failed.
     */
    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.notes = reason;
    }

    /**
     * Process a refund.
     */
    public void processRefund(BigDecimal refundAmount, String reason) {
        if (refundAmount.compareTo(this.amount) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed payment amount");
        }
        this.refundAmount = refundAmount;
        this.refundDate = LocalDateTime.now();
        this.refundReason = reason;

        if (refundAmount.compareTo(this.amount) == 0) {
            this.status = PaymentStatus.REFUNDED;
        } else {
            this.status = PaymentStatus.PARTIALLY_REFUNDED;
        }
    }

    /**
     * Check if payment is successful.
     */
    public boolean isSuccessful() {
        return status == PaymentStatus.COMPLETED;
    }

    /**
     * Check if payment can be refunded.
     */
    public boolean canRefund() {
        return status == PaymentStatus.COMPLETED &&
                (refundAmount == null || refundAmount.compareTo(amount) < 0);
    }

    /**
     * Get remaining refundable amount.
     */
    public BigDecimal getRefundableAmount() {
        if (refundAmount == null) {
            return amount;
        }
        return amount.subtract(refundAmount);
    }

    // Getters and Setters

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCardLastFour() {
        return cardLastFour;
    }

    public void setCardLastFour(String cardLastFour) {
        this.cardLastFour = cardLastFour;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public LocalDateTime getRefundDate() {
        return refundDate;
    }

    public void setRefundDate(LocalDateTime refundDate) {
        this.refundDate = refundDate;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    @Override
    public String toString() {
        return "Payment{" +
                "amount=" + amount +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", paymentDate=" + paymentDate +
                ", status=" + status +
                ", transactionId='" + transactionId + '\'' +
                '}';
    }
}