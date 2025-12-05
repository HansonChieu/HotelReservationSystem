package com.hanson.hotelreservationsystem.model;

import com.hanson.hotelreservationsystem.model.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * Entity representing an administrator user.
 * Supports role-based access control (Admin and Manager roles).
 */
@Entity
@Table(name = "admins", indexes = {
        @Index(name = "idx_admin_username", columnList = "username"),
        @Index(name = "idx_admin_email", columnList = "email")
})
public class Admin extends BaseEntity {

    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 30, message = "Username must be between 4 and 30 characters")
    @Column(name = "username", nullable = false, unique = true, length = 30)
    private String username;

    @NotBlank(message = "Password is required")
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash; // BCrypt hashed password

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "subscribe_to_notifications")
    private boolean subscribeToNotifications = true; // For waitlist notifications

    // Constructors
    public Admin() {}

    public Admin(String username, String passwordHash, String firstName, String lastName, String email, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
    }

    // Business Methods

    /**
     * Gets the full name of the admin.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Checks if the account is currently locked.
     */
    public boolean isLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    /**
     * Records a successful login.
     */
    public void recordSuccessfulLogin() {
        this.lastLogin = LocalDateTime.now();
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    /**
     * Records a failed login attempt.
     * Locks account after 5 failed attempts for 15 minutes.
     */
    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(15);
        }
    }

    /**
     * Resets failed login attempts.
     */
    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    /**
     * Gets the maximum discount percentage this admin can apply.
     */
    public double getMaxDiscountRate() {
        return role.getMaxDiscountRate();
    }

    /**
     * Checks if admin can apply the given discount percentage.
     */
    public boolean canApplyDiscount(double discountPercentage) {
        return discountPercentage <= (role.getMaxDiscountRate() * 100);
    }

    /**
     * Checks if admin is a manager.
     */
    public boolean isManager() {
        return role == Role.MANAGER;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public boolean isSubscribeToNotifications() {
        return subscribeToNotifications;
    }

    public void setSubscribeToNotifications(boolean subscribeToNotifications) {
        this.subscribeToNotifications = subscribeToNotifications;
    }

    @Override
    public String toString() {
        return "Admin{" +
                "username='" + username + '\'' +
                ", name='" + getFullName() + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", active=" + active +
                '}';
    }
}
