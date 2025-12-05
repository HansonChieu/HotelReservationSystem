package com.hanson.hotelreservationsystem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * Entity representing an administrative activity log entry.
 * Provides audit trail for all admin actions in the system.
 */
@Entity
@Table(name = "activity_logs", indexes = {
        @Index(name = "idx_actlog_timestamp", columnList = "timestamp"),
        @Index(name = "idx_actlog_actor", columnList = "actor"),
        @Index(name = "idx_actlog_action", columnList = "action"),
        @Index(name = "idx_actlog_entity", columnList = "entity_type, entity_id")
})
public class ActivityLog extends BaseEntity {

    @NotNull(message = "Timestamp is required")
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @NotBlank(message = "Actor is required")
    @Size(max = 50, message = "Actor cannot exceed 50 characters")
    @Column(name = "actor", nullable = false, length = 50)
    private String actor; // Username of the admin

    @NotBlank(message = "Action is required")
    @Size(max = 50, message = "Action cannot exceed 50 characters")
    @Column(name = "action", nullable = false, length = 50)
    private String action; // LOGIN, CREATE_RESERVATION, CHECKOUT, etc.

    @Size(max = 50, message = "Entity type cannot exceed 50 characters")
    @Column(name = "entity_type", length = 50)
    private String entityType; // RESERVATION, GUEST, PAYMENT, etc.

    @Column(name = "entity_id")
    private Long entityId; // ID of the affected entity

    @Size(max = 500, message = "Message cannot exceed 500 characters")
    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "success")
    private boolean success = true;

    @Size(max = 200, message = "Error message cannot exceed 200 characters")
    @Column(name = "error_message", length = 200)
    private String errorMessage;

    // Additional context
    @Column(name = "old_value", length = 500)
    private String oldValue;

    @Column(name = "new_value", length = 500)
    private String newValue;

    // Constructors
    public ActivityLog() {
        this.timestamp = LocalDateTime.now();
    }

    public ActivityLog(String actor, String action) {
        this();
        this.actor = actor;
        this.action = action;
    }

    public ActivityLog(String actor, String action, String entityType, Long entityId, String message) {
        this(actor, action);
        this.entityType = entityType;
        this.entityId = entityId;
        this.message = message;
    }

    // Factory Methods

    /**
     * Creates a login success log entry.
     */
    public static ActivityLog loginSuccess(String username, String ipAddress) {
        ActivityLog log = new ActivityLog(username, "LOGIN");
        log.setEntityType("ADMIN");
        log.setMessage("Successful login");
        log.setIpAddress(ipAddress);
        return log;
    }

    /**
     * Creates a login failure log entry.
     */
    public static ActivityLog loginFailure(String username, String ipAddress, String reason) {
        ActivityLog log = new ActivityLog(username != null ? username : "UNKNOWN", "LOGIN_FAILED");
        log.setEntityType("ADMIN");
        log.setMessage("Failed login attempt: " + reason);
        log.setIpAddress(ipAddress);
        log.setSuccess(false);
        log.setErrorMessage(reason);
        return log;
    }

    /**
     * Creates a reservation created log entry.
     */
    public static ActivityLog reservationCreated(String actor, Long reservationId, String confirmationNumber) {
        ActivityLog log = new ActivityLog(actor, "CREATE_RESERVATION");
        log.setEntityType("RESERVATION");
        log.setEntityId(reservationId);
        log.setMessage("Reservation created: " + confirmationNumber);
        return log;
    }

    /**
     * Creates a reservation modified log entry.
     */
    public static ActivityLog reservationModified(String actor, Long reservationId, String changes) {
        ActivityLog log = new ActivityLog(actor, "MODIFY_RESERVATION");
        log.setEntityType("RESERVATION");
        log.setEntityId(reservationId);
        log.setMessage("Reservation modified: " + changes);
        return log;
    }

    /**
     * Creates a checkout log entry.
     */
    public static ActivityLog checkout(String actor, Long reservationId, String confirmationNumber) {
        ActivityLog log = new ActivityLog(actor, "CHECKOUT");
        log.setEntityType("RESERVATION");
        log.setEntityId(reservationId);
        log.setMessage("Guest checked out: " + confirmationNumber);
        return log;
    }

    /**
     * Creates a cancellation log entry.
     */
    public static ActivityLog cancellation(String actor, Long reservationId, String confirmationNumber) {
        ActivityLog log = new ActivityLog(actor, "CANCEL_RESERVATION");
        log.setEntityType("RESERVATION");
        log.setEntityId(reservationId);
        log.setMessage("Reservation cancelled: " + confirmationNumber);
        return log;
    }

    /**
     * Creates a payment log entry.
     */
    public static ActivityLog paymentProcessed(String actor, Long paymentId, Long reservationId, String amount) {
        ActivityLog log = new ActivityLog(actor, "PROCESS_PAYMENT");
        log.setEntityType("PAYMENT");
        log.setEntityId(paymentId);
        log.setMessage("Payment processed: $" + amount + " for reservation #" + reservationId);
        return log;
    }

    /**
     * Creates a refund log entry.
     */
    public static ActivityLog refundProcessed(String actor, Long paymentId, Long reservationId, String amount) {
        ActivityLog log = new ActivityLog(actor, "PROCESS_REFUND");
        log.setEntityType("PAYMENT");
        log.setEntityId(paymentId);
        log.setMessage("Refund processed: $" + amount + " for reservation #" + reservationId);
        return log;
    }

    /**
     * Creates a discount applied log entry.
     */
    public static ActivityLog discountApplied(String actor, Long reservationId, String discountInfo) {
        ActivityLog log = new ActivityLog(actor, "APPLY_DISCOUNT");
        log.setEntityType("RESERVATION");
        log.setEntityId(reservationId);
        log.setMessage("Discount applied: " + discountInfo);
        return log;
    }

    /**
     * Creates a search log entry.
     */
    public static ActivityLog searchPerformed(String actor, String searchType, String criteria) {
        ActivityLog log = new ActivityLog(actor, "SEARCH");
        log.setEntityType(searchType);
        log.setMessage("Search performed: " + criteria);
        return log;
    }

    /**
     * Creates an error log entry.
     */
    public static ActivityLog error(String actor, String action, String errorMessage) {
        ActivityLog log = new ActivityLog(actor, action);
        log.setSuccess(false);
        log.setErrorMessage(errorMessage);
        log.setMessage("Error: " + errorMessage);
        return log;
    }

    // Getters and Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    @Override
    public String toString() {
        return "ActivityLog{" +
                "timestamp=" + timestamp +
                ", actor='" + actor + '\'' +
                ", action='" + action + '\'' +
                ", entityType='" + entityType + '\'' +
                ", entityId=" + entityId +
                ", message='" + message + '\'' +
                ", success=" + success +
                '}';
    }
}
