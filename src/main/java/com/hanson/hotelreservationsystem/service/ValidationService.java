package com.hanson.hotelreservationsystem.service;

import com.hanson.hotelreservationsystem.model.Guest;
import com.hanson.hotelreservationsystem.model.Reservation;
import com.hanson.hotelreservationsystem.model.enums.RoomType;
import com.hanson.hotelreservationsystem.model.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Service for validating user input across the hotel reservation system.
 *
 * Responsibilities:
 * - Validate guest names, phone numbers, and email addresses
 * - Validate date ranges with minimums and check for overlaps
 * - Validate occupancy distribution across group bookings
 * - Validate payment amounts and prevent negative balances
 * - Validate discounts within configured caps
 * - Validate feedback ratings and cap comment length
 *
 * This service provides both individual field validation and comprehensive
 * object validation with clear, actionable error messages.
 *
 * Pattern: Singleton (accessed via getInstance())
 */
public class ValidationService {

    private static final Logger LOGGER = Logger.getLogger(ValidationService.class.getName());

    // Singleton instance
    private static ValidationService instance;

    // ==================== Validation Patterns ====================

    // Name: letters, spaces, hyphens, apostrophes, 2-50 characters
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[a-zA-Z\\s'-]{2,50}$");

    // Email: standard email format
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Phone: digits, spaces, parentheses, plus, hyphen, 10-20 characters
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[\\d\\s()+-]{10,20}$");

    // Postal code: alphanumeric, spaces, hyphen, 3-10 characters
    private static final Pattern POSTAL_PATTERN =
            Pattern.compile("^[A-Za-z0-9\\s-]{3,10}$");

    // ID/Passport: alphanumeric, 5-20 characters
    private static final Pattern ID_PATTERN =
            Pattern.compile("^[A-Za-z0-9]{5,20}$");

    // ==================== Configuration Constants ====================

    // Date validation
    private static final int MIN_NIGHTS = 1;
    private static final int MAX_ADVANCE_BOOKING_DAYS = 365;
    private static final int MAX_STAY_NIGHTS = 30;

    // Occupancy limits per room type
    private static final int SINGLE_ROOM_MAX_OCCUPANCY = 2;
    private static final int DOUBLE_ROOM_MAX_OCCUPANCY = 4;
    private static final int DELUXE_ROOM_MAX_OCCUPANCY = 2;
    private static final int PENTHOUSE_MAX_OCCUPANCY = 2;

    // Discount limits by role
    private static final BigDecimal ADMIN_MAX_DISCOUNT_PERCENT = new BigDecimal("15");
    private static final BigDecimal MANAGER_MAX_DISCOUNT_PERCENT = new BigDecimal("30");

    // Feedback constraints
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int MAX_COMMENT_LENGTH = 1000;

    /**
     * Private constructor for Singleton pattern.
     */
    private ValidationService() {
    }

    /**
     * Get the singleton instance.
     */
    public static synchronized ValidationService getInstance() {
        if (instance == null) {
            instance = new ValidationService();
        }
        return instance;
    }

    // ==================== Guest Validation Methods ====================

    /**
     * Validate a complete guest object.
     *
     * @param guest The guest to validate
     * @return ValidationResult containing any errors
     */
    public ValidationResult validateGuest(Guest guest) {
        ValidationResult result = new ValidationResult();

        if (guest == null) {
            result.addError("guest", "Guest information is required");
            return result;
        }

        // Required fields
        result.merge(validateFirstName(guest.getFirstName()));
        result.merge(validateLastName(guest.getLastName()));
        result.merge(validateEmail(guest.getEmail()));
        result.merge(validatePhone(guest.getPhone()));
        result.merge(validateCountry(guest.getCountry()));
        result.merge(validateAddress(guest.getAddress()));
        result.merge(validateCity(guest.getCity()));
        result.merge(validateState(guest.getStateProvince()));
        result.merge(validatePostalCode(guest.getPostalCode()));

        // Optional fields (validate format if provided)
        if (guest.getIdNumber() != null && !guest.getIdNumber().trim().isEmpty()) {
            result.merge(validateIdNumber(guest.getIdNumber()));
        }

        return result;
    }

    /**
     * Validate first name.
     */
    public ValidationResult validateFirstName(String firstName) {
        ValidationResult result = new ValidationResult();

        if (firstName == null || firstName.trim().isEmpty()) {
            result.addError("firstName", "First name is required");
            return result;
        }

        if (!NAME_PATTERN.matcher(firstName.trim()).matches()) {
            result.addError("firstName",
                    "First name must be 2-50 characters and contain only letters, spaces, hyphens, or apostrophes");
        }

        return result;
    }

    /**
     * Validate last name.
     */
    public ValidationResult validateLastName(String lastName) {
        ValidationResult result = new ValidationResult();

        if (lastName == null || lastName.trim().isEmpty()) {
            result.addError("lastName", "Last name is required");
            return result;
        }

        if (!NAME_PATTERN.matcher(lastName.trim()).matches()) {
            result.addError("lastName",
                    "Last name must be 2-50 characters and contain only letters, spaces, hyphens, or apostrophes");
        }

        return result;
    }

    /**
     * Validate email address.
     */
    public ValidationResult validateEmail(String email) {
        ValidationResult result = new ValidationResult();

        if (email == null || email.trim().isEmpty()) {
            result.addError("email", "Email address is required");
            return result;
        }

        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            result.addError("email", "Please enter a valid email address (e.g., name@example.com)");
        }

        return result;
    }

    /**
     * Validate phone number.
     */
    public ValidationResult validatePhone(String phone) {
        ValidationResult result = new ValidationResult();

        if (phone == null || phone.trim().isEmpty()) {
            result.addError("phone", "Phone number is required");
            return result;
        }

        if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
            result.addError("phone",
                    "Please enter a valid phone number (10-20 digits, may include spaces, dashes, or parentheses)");
        }

        return result;
    }

    /**
     * Validate country.
     */
    public ValidationResult validateCountry(String country) {
        ValidationResult result = new ValidationResult();

        if (country == null || country.trim().isEmpty()) {
            result.addError("country", "Country is required");
        }

        return result;
    }

    /**
     * Validate address.
     */
    public ValidationResult validateAddress(String address) {
        ValidationResult result = new ValidationResult();

        if (address == null || address.trim().isEmpty()) {
            result.addError("address", "Address is required");
        } else if (address.trim().length() < 5) {
            result.addError("address", "Please enter a complete address");
        }

        return result;
    }

    /**
     * Validate city.
     */
    public ValidationResult validateCity(String city) {
        ValidationResult result = new ValidationResult();

        if (city == null || city.trim().isEmpty()) {
            result.addError("city", "City is required");
        }

        return result;
    }

    /**
     * Validate state/province.
     */
    public ValidationResult validateState(String state) {
        ValidationResult result = new ValidationResult();

        if (state == null || state.trim().isEmpty()) {
            result.addError("state", "State/Province is required");
        }

        return result;
    }

    /**
     * Validate postal code.
     */
    public ValidationResult validatePostalCode(String postalCode) {
        ValidationResult result = new ValidationResult();

        if (postalCode == null || postalCode.trim().isEmpty()) {
            result.addError("postalCode", "Postal code is required");
            return result;
        }

        if (!POSTAL_PATTERN.matcher(postalCode.trim()).matches()) {
            result.addError("postalCode", "Please enter a valid postal code");
        }

        return result;
    }

    /**
     * Validate ID/Passport number (optional field).
     */
    public ValidationResult validateIdNumber(String idNumber) {
        ValidationResult result = new ValidationResult();

        if (idNumber != null && !idNumber.trim().isEmpty()) {
            if (!ID_PATTERN.matcher(idNumber.trim()).matches()) {
                result.addError("idNumber",
                        "ID/Passport number must be 5-20 alphanumeric characters");
            }
        }

        return result;
    }

    // ==================== Date Validation Methods ====================

    /**
     * Validate check-in and check-out dates.
     *
     * @param checkIn The check-in date
     * @param checkOut The check-out date
     * @return ValidationResult containing any errors
     */
    public ValidationResult validateDateRange(LocalDate checkIn, LocalDate checkOut) {
        ValidationResult result = new ValidationResult();
        LocalDate today = LocalDate.now();

        // Validate check-in date
        if (checkIn == null) {
            result.addError("checkInDate", "Check-in date is required");
        } else {
            if (checkIn.isBefore(today)) {
                result.addError("checkInDate", "Check-in date cannot be in the past");
            }
            if (checkIn.isAfter(today.plusDays(MAX_ADVANCE_BOOKING_DAYS))) {
                result.addError("checkInDate",
                        "Check-in date cannot be more than " + MAX_ADVANCE_BOOKING_DAYS + " days in advance");
            }
        }

        // Validate check-out date
        if (checkOut == null) {
            result.addError("checkOutDate", "Check-out date is required");
        } else if (checkIn != null) {
            if (!checkOut.isAfter(checkIn)) {
                result.addError("checkOutDate", "Check-out date must be after check-in date");
            } else {
                long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
                if (nights < MIN_NIGHTS) {
                    result.addError("dateRange", "Minimum stay is " + MIN_NIGHTS + " night(s)");
                }
                if (nights > MAX_STAY_NIGHTS) {
                    result.addError("dateRange",
                            "Maximum stay is " + MAX_STAY_NIGHTS + " nights. Please contact us for extended stays.");
                }
            }
        }

        return result;
    }

    /**
     * Check for date range overlap with existing reservations.
     *
     * @param checkIn The check-in date
     * @param checkOut The check-out date
     * @param existingReservations List of existing reservations to check against
     * @param excludeReservationId Reservation ID to exclude (for modifications)
     * @return true if there is an overlap
     */
    public boolean hasDateOverlap(LocalDate checkIn, LocalDate checkOut,
                                  List<Reservation> existingReservations, Long excludeReservationId) {
        if (checkIn == null || checkOut == null || existingReservations == null) {
            return false;
        }

        for (Reservation existing : existingReservations) {
            // Skip the reservation being modified
            if (excludeReservationId != null && excludeReservationId.equals(existing.getId())) {
                continue;
            }

            // Check for overlap: new booking starts before existing ends AND new booking ends after existing starts
            if (checkIn.isBefore(existing.getCheckOutDate()) &&
                    checkOut.isAfter(existing.getCheckInDate())) {
                return true;
            }
        }

        return false;
    }

    // ==================== Occupancy Validation Methods ====================

    /**
     * Get the maximum occupancy for a room type.
     *
     * @param roomType The room type
     * @return Maximum number of guests allowed
     */
    public int getMaxOccupancy(RoomType roomType) {
        if (roomType == null) {
            return 0;
        }

        switch (roomType) {
            case SINGLE:
                return SINGLE_ROOM_MAX_OCCUPANCY;
            case DOUBLE:
                return DOUBLE_ROOM_MAX_OCCUPANCY;
            case DELUXE:
                return DELUXE_ROOM_MAX_OCCUPANCY;
            case PENTHOUSE:
                return PENTHOUSE_MAX_OCCUPANCY;
            default:
                return SINGLE_ROOM_MAX_OCCUPANCY;
        }
    }

    /**
     * Validate occupancy for a single room.
     *
     * @param roomType The room type
     * @param adults Number of adults
     * @param children Number of children
     * @return ValidationResult containing any errors
     */
    public ValidationResult validateRoomOccupancy(RoomType roomType, int adults, int children) {
        ValidationResult result = new ValidationResult();

        if (roomType == null) {
            result.addError("roomType", "Room type is required");
            return result;
        }

        if (adults < 1) {
            result.addError("adults", "At least one adult is required per booking");
            return result;
        }

        if (children < 0) {
            result.addError("children", "Number of children cannot be negative");
            return result;
        }

        int totalGuests = adults + children;
        int maxOccupancy = getMaxOccupancy(roomType);

        if (totalGuests > maxOccupancy) {
            result.addError("occupancy",
                    String.format("%s room allows maximum %d guests. You have %d guests selected.",
                            roomType.getDisplayName(), maxOccupancy, totalGuests));
        }

        return result;
    }

    /**
     * Validate occupancy distribution across a group booking.
     *
     * @param roomSelections Map of room type to quantity
     * @param totalAdults Total number of adults
     * @param totalChildren Total number of children
     * @return ValidationResult containing any errors
     */
    public ValidationResult validateGroupOccupancy(Map<RoomType, Integer> roomSelections,
                                                   int totalAdults, int totalChildren) {
        ValidationResult result = new ValidationResult();

        if (roomSelections == null || roomSelections.isEmpty()) {
            result.addError("rooms", "At least one room must be selected");
            return result;
        }

        if (totalAdults < 1) {
            result.addError("adults", "At least one adult is required per booking");
            return result;
        }

        int totalGuests = totalAdults + totalChildren;
        int totalCapacity = 0;

        // Calculate total capacity
        for (Map.Entry<RoomType, Integer> entry : roomSelections.entrySet()) {
            RoomType roomType = entry.getKey();
            int quantity = entry.getValue();

            if (quantity < 0) {
                result.addError("rooms", "Room quantity cannot be negative");
                return result;
            }

            totalCapacity += getMaxOccupancy(roomType) * quantity;
        }

        // Validate total capacity vs total guests
        if (totalGuests > totalCapacity) {
            result.addError("occupancy",
                    String.format("Selected rooms can accommodate %d guests, but you have %d guests. " +
                            "Please select additional rooms.", totalCapacity, totalGuests));
        }

        // Warn if significantly under-utilizing capacity (optional)
        if (totalCapacity > totalGuests * 2) {
            result.addWarning("occupancy",
                    "You have selected more rooms than typically needed for your group size.");
        }

        return result;
    }

    /**
     * Validate that a single adult can book.
     *
     * @param adults Number of adults
     * @return ValidationResult containing any errors
     */
    public ValidationResult validateSinglePersonBooking(int adults) {
        ValidationResult result = new ValidationResult();

        if (adults < 1) {
            result.addError("adults", "At least one adult is required");
        }

        return result;
    }

    // ==================== Payment Validation Methods ====================

    /**
     * Validate a payment amount.
     *
     * @param amount The payment amount
     * @param outstandingBalance The current outstanding balance
     * @param isRefund Whether this is a refund transaction
     * @return ValidationResult containing any errors
     */
    public ValidationResult validatePayment(BigDecimal amount, BigDecimal outstandingBalance,
                                            boolean isRefund) {
        ValidationResult result = new ValidationResult();

        if (amount == null) {
            result.addError("amount", "Payment amount is required");
            return result;
        }

        if (!isRefund && amount.compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("amount", "Payment amount must be greater than zero");
            return result;
        }

        if (isRefund && amount.compareTo(BigDecimal.ZERO) >= 0) {
            result.addError("amount", "Refund amount must be negative");
            return result;
        }

        // Check for overpayment
        if (!isRefund && outstandingBalance != null && amount.compareTo(outstandingBalance) > 0) {
            result.addWarning("amount",
                    String.format("Payment amount ($%.2f) exceeds outstanding balance ($%.2f). " +
                            "A credit will be applied.", amount, outstandingBalance));
        }

        return result;
    }

    /**
     * Validate that checkout is allowed (no outstanding balance).
     *
     * @param outstandingBalance The current outstanding balance
     * @return ValidationResult containing any errors
     */
    public ValidationResult validateCheckoutBalance(BigDecimal outstandingBalance) {
        ValidationResult result = new ValidationResult();

        if (outstandingBalance != null && outstandingBalance.compareTo(BigDecimal.ZERO) > 0) {
            result.addError("balance",
                    String.format("Cannot checkout with outstanding balance of $%.2f. " +
                            "Please settle all payments first.", outstandingBalance));
        }

        return result;
    }

    // ==================== Discount Validation Methods ====================

    /**
     * Validate a discount percentage based on user role.
     *
     * @param discountPercent The discount percentage to apply
     * @param userRole The role of the user applying the discount
     * @return ValidationResult containing any errors
     */
    public ValidationResult validateDiscount(BigDecimal discountPercent, Role userRole) {
        ValidationResult result = new ValidationResult();

        if (discountPercent == null) {
            result.addError("discount", "Discount amount is required");
            return result;
        }

        if (discountPercent.compareTo(BigDecimal.ZERO) < 0) {
            result.addError("discount", "Discount cannot be negative");
            return result;
        }

        if (discountPercent.compareTo(new BigDecimal("100")) > 0) {
            result.addError("discount", "Discount cannot exceed 100%");
            return result;
        }

        // Check role-based limits
        BigDecimal maxDiscount = getMaxDiscountForRole(userRole);
        if (discountPercent.compareTo(maxDiscount) > 0) {
            result.addError("discount",
                    String.format("Your role allows a maximum discount of %.0f%%. " +
                            "Please contact a manager for higher discounts.", maxDiscount));
        }

        return result;
    }

    /**
     * Get the maximum discount percentage for a role.
     *
     * @param role The user role
     * @return Maximum discount percentage
     */
    public BigDecimal getMaxDiscountForRole(Role role) {
        if (role == null) {
            return BigDecimal.ZERO;
        }

        switch (role) {
            case MANAGER:
                return MANAGER_MAX_DISCOUNT_PERCENT;
            case ADMIN:
                return ADMIN_MAX_DISCOUNT_PERCENT;
            default:
                return BigDecimal.ZERO;
        }
    }

    /**
     * Validate a discount amount (absolute value).
     *
     * @param discountAmount The discount amount
     * @param totalAmount The total amount before discount
     * @param maxDiscountPercent Maximum allowed discount percentage
     * @return ValidationResult containing any errors
     */
    public ValidationResult validateDiscountAmount(BigDecimal discountAmount,
                                                   BigDecimal totalAmount,
                                                   BigDecimal maxDiscountPercent) {
        ValidationResult result = new ValidationResult();

        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return result; // No discount applied, valid
        }

        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("discount", "Cannot apply discount to zero or negative total");
            return result;
        }

        // Calculate actual percentage
        BigDecimal actualPercent = discountAmount.multiply(new BigDecimal("100"))
                .divide(totalAmount, 2, BigDecimal.ROUND_HALF_UP);

        if (actualPercent.compareTo(maxDiscountPercent) > 0) {
            result.addError("discount",
                    String.format("Discount of $%.2f (%.1f%%) exceeds maximum allowed (%.0f%%)",
                            discountAmount, actualPercent, maxDiscountPercent));
        }

        return result;
    }

    // ==================== Feedback Validation Methods ====================

    /**
     * Validate feedback submission.
     *
     * @param rating The star rating (1-5)
     * @param comment The feedback comment
     * @return ValidationResult containing any errors
     */
    public ValidationResult validateFeedback(int rating, String comment) {
        ValidationResult result = new ValidationResult();

        // Validate rating
        if (rating < MIN_RATING || rating > MAX_RATING) {
            result.addError("rating",
                    String.format("Rating must be between %d and %d stars", MIN_RATING, MAX_RATING));
        }

        // Validate comment length
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            result.addError("comment",
                    String.format("Comment cannot exceed %d characters (current: %d)",
                            MAX_COMMENT_LENGTH, comment.length()));
        }

        return result;
    }

    /**
     * Validate that feedback submission is allowed (reservation checked out and paid).
     *
     * @param reservation The reservation
     * @return ValidationResult containing any errors
     */
    public ValidationResult validateFeedbackEligibility(Reservation reservation) {
        ValidationResult result = new ValidationResult();

        if (reservation == null) {
            result.addError("reservation", "Reservation not found");
            return result;
        }

        if (!reservation.isCheckedOut()) {
            result.addError("feedback",
                    "Feedback can only be submitted after checkout");
        }

        if (reservation.getOutstandingBalance() != null &&
                reservation.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0) {
            result.addError("feedback",
                    "Feedback can only be submitted after all balances are settled");
        }

        return result;
    }

    // ==================== Utility Methods ====================

    /**
     * Check if a string is null or empty.
     */
    public boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Check if a string matches a pattern.
     */
    public boolean matches(String value, Pattern pattern) {
        return value != null && pattern.matcher(value.trim()).matches();
    }

    // ==================== Inner Classes ====================

    /**
     * Result of a validation operation.
     */
    public static class ValidationResult {
        private final List<ValidationError> errors = new ArrayList<>();
        private final List<ValidationWarning> warnings = new ArrayList<>();

        public void addError(String field, String message) {
            errors.add(new ValidationError(field, message));
            LOGGER.warning("Validation error for " + field + ": " + message);
        }

        public void addWarning(String field, String message) {
            warnings.add(new ValidationWarning(field, message));
            LOGGER.info("Validation warning for " + field + ": " + message);
        }

        public void merge(ValidationResult other) {
            if (other != null) {
                this.errors.addAll(other.errors);
                this.warnings.addAll(other.warnings);
            }
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public List<ValidationError> getErrors() {
            return new ArrayList<>(errors);
        }

        public List<ValidationWarning> getWarnings() {
            return new ArrayList<>(warnings);
        }

        public String getFirstErrorMessage() {
            return errors.isEmpty() ? null : errors.get(0).getMessage();
        }

        public String getAllErrorMessages() {
            if (errors.isEmpty()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (ValidationError error : errors) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append("• ").append(error.getMessage());
            }
            return sb.toString();
        }

        public String getErrorMessageForField(String field) {
            for (ValidationError error : errors) {
                if (error.getField().equals(field)) {
                    return error.getMessage();
                }
            }
            return null;
        }
    }

    /**
     * A single validation error.
     */
    public static class ValidationError {
        private final String field;
        private final String message;

        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * A single validation warning.
     */
    public static class ValidationWarning {
        private final String field;
        private final String message;

        public ValidationWarning(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }
    }
}