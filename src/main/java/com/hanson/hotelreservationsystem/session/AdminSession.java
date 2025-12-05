package com.hanson.hotelreservationsystem.session;

import com.hanson.hotelreservationsystem.model.Admin;
import com.hanson.hotelreservationsystem.model.Guest;
import com.hanson.hotelreservationsystem.model.Reservation;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * Singleton class that holds the current admin session state.
 * Tracks the logged-in administrator and provides context for admin operations.
 *
 * Pattern: Singleton
 *
 * Responsibilities:
 * - Track currently logged-in admin
 * - Store current reservation being viewed/edited
 * - Provide session timing information
 * - Support role-based access control checks
 */
public class AdminSession {

    private static final Logger LOGGER = Logger.getLogger(AdminSession.class.getName());

    // ==================== Singleton Instance ====================
    private static AdminSession instance;

    // ==================== Session Properties ====================
    private final ObjectProperty<Admin> currentAdmin = new SimpleObjectProperty<>();
    private final ObjectProperty<Reservation> currentReservation = new SimpleObjectProperty<>();
    private final ObjectProperty<Guest> currentGuest = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> loginTime = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> lastActivityTime = new SimpleObjectProperty<>();

    // Session timeout in minutes (default: 30)
    private static final int SESSION_TIMEOUT_MINUTES = 30;

    // ==================== Constructor (Private for Singleton) ====================

    private AdminSession() {
        // Private constructor - use getInstance()
    }

    // ==================== Singleton Access ====================

    /**
     * Get the singleton instance of AdminSession.
     * Thread-safe implementation.
     *
     * @return The singleton AdminSession instance
     */
    public static synchronized AdminSession getInstance() {
        if (instance == null) {
            instance = new AdminSession();
        }
        return instance;
    }

    // ==================== Session Management ====================

    /**
     * Start a new admin session after successful login.
     *
     * @param admin The authenticated admin
     */
    public void startSession(Admin admin) {
        if (admin == null) {
            throw new IllegalArgumentException("Admin cannot be null");
        }

        currentAdmin.set(admin);
        loginTime.set(LocalDateTime.now());
        lastActivityTime.set(LocalDateTime.now());

        LOGGER.info("Admin session started for: " + admin.getUsername());
    }

    /**
     * End the current admin session (logout).
     */
    public void endSession() {
        String username = currentAdmin.get() != null ? currentAdmin.get().getUsername() : "Unknown";

        currentAdmin.set(null);
        currentReservation.set(null);
        currentGuest.set(null);
        loginTime.set(null);
        lastActivityTime.set(null);

        LOGGER.info("Admin session ended for: " + username);
    }

    /**
     * Update the last activity timestamp.
     * Should be called on each admin action.
     */
    public void updateActivity() {
        lastActivityTime.set(LocalDateTime.now());
    }

    /**
     * Check if the session has timed out.
     *
     * @return true if the session has exceeded the timeout period
     */
    public boolean isSessionTimedOut() {
        if (lastActivityTime.get() == null) {
            return true;
        }

        LocalDateTime timeout = lastActivityTime.get().plusMinutes(SESSION_TIMEOUT_MINUTES);
        return LocalDateTime.now().isAfter(timeout);
    }

    /**
     * Check if an admin is currently logged in.
     *
     * @return true if an admin is logged in
     */
    public boolean isLoggedIn() {
        return currentAdmin.get() != null && !isSessionTimedOut();
    }

    /**
     * Check if the current admin has manager role.
     *
     * @return true if the current admin is a manager
     */
    public boolean isManager() {
        return currentAdmin.get() != null && currentAdmin.get().isManager();
    }

    /**
     * Get the maximum discount rate the current admin can apply.
     *
     * @return Maximum discount rate (0.15 for Admin, 0.30 for Manager)
     */
    public double getMaxDiscountRate() {
        if (currentAdmin.get() != null) {
            return currentAdmin.get().getMaxDiscountRate();
        }
        return 0.0;
    }

    /**
     * Check if the current admin can apply a specific discount percentage.
     *
     * @param discountPercentage The discount percentage to check
     * @return true if the admin can apply this discount
     */
    public boolean canApplyDiscount(double discountPercentage) {
        if (currentAdmin.get() != null) {
            return currentAdmin.get().canApplyDiscount(discountPercentage);
        }
        return false;
    }

    // ==================== Context Management ====================

    /**
     * Set the current reservation being viewed/edited.
     *
     * @param reservation The reservation to set as current context
     */
    public void setCurrentReservation(Reservation reservation) {
        currentReservation.set(reservation);
        updateActivity();

        if (reservation != null) {
            LOGGER.fine("Current reservation set: " + reservation.getConfirmationNumber());
        }
    }

    /**
     * Clear the current reservation context.
     */
    public void clearCurrentReservation() {
        currentReservation.set(null);
    }

    /**
     * Set the current guest being viewed/edited.
     *
     * @param guest The guest to set as current context
     */
    public void setCurrentGuest(Guest guest) {
        currentGuest.set(guest);
        updateActivity();

        if (guest != null) {
            LOGGER.fine("Current guest set: " + guest.getFullName());
        }
    }

    /**
     * Clear the current guest context.
     */
    public void clearCurrentGuest() {
        currentGuest.set(null);
    }

    // ==================== Property Accessors ====================

    public ObjectProperty<Admin> currentAdminProperty() {
        return currentAdmin;
    }

    public Admin getCurrentAdmin() {
        return currentAdmin.get();
    }

    public ObjectProperty<Reservation> currentReservationProperty() {
        return currentReservation;
    }

    public Reservation getCurrentReservation() {
        return currentReservation.get();
    }

    public ObjectProperty<Guest> currentGuestProperty() {
        return currentGuest;
    }

    public Guest getCurrentGuest() {
        return currentGuest.get();
    }

    public ObjectProperty<LocalDateTime> loginTimeProperty() {
        return loginTime;
    }

    public LocalDateTime getLoginTime() {
        return loginTime.get();
    }

    public ObjectProperty<LocalDateTime> lastActivityTimeProperty() {
        return lastActivityTime;
    }

    public LocalDateTime getLastActivityTime() {
        return lastActivityTime.get();
    }

    // ==================== Utility Methods ====================

    /**
     * Get the current admin's username for logging purposes.
     *
     * @return The username or "System" if no admin is logged in
     */
    public String getActorName() {
        if (currentAdmin.get() != null) {
            return "Admin: " + currentAdmin.get().getUsername();
        }
        return "System";
    }

    /**
     * Get session duration in minutes.
     *
     * @return Session duration or 0 if not logged in
     */
    public long getSessionDurationMinutes() {
        if (loginTime.get() == null) {
            return 0;
        }
        return java.time.Duration.between(loginTime.get(), LocalDateTime.now()).toMinutes();
    }

    @Override
    public String toString() {
        return "AdminSession{" +
                "admin=" + (currentAdmin.get() != null ? currentAdmin.get().getUsername() : "none") +
                ", loggedIn=" + isLoggedIn() +
                ", sessionMinutes=" + getSessionDurationMinutes() +
                '}';
    }
}
