package com.hanson.hotelreservationsystem.model;

import com.hanson.hotelreservationsystem.model.enums.ReservationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a hotel reservation.
 * Supports group bookings with multiple rooms and unified billing.
 */
@Entity
@Table(name = "reservations", indexes = {
        @Index(name = "idx_reservation_status", columnList = "status"),
        @Index(name = "idx_reservation_checkin", columnList = "check_in_date"),
        @Index(name = "idx_reservation_checkout", columnList = "check_out_date"),
        @Index(name = "idx_reservation_confirmation", columnList = "confirmation_number")
})
public class Reservation extends BaseEntity {

    @Column(name = "confirmation_number", unique = true, nullable = false, length = 20)
    private String confirmationNumber;

    @NotNull(message = "Guest is required")
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    @NotNull(message = "Check-in date is required")
    @FutureOrPresent(message = "Check-in date must be today or in the future")
    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    @Future(message = "Check-out date must be in the future")
    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @NotNull(message = "Number of adults is required")
    @Min(value = 1, message = "At least one adult is required")
    @Column(name = "num_adults", nullable = false)
    private Integer numAdults;

    @Min(value = 0, message = "Number of children cannot be negative")
    @Column(name = "num_children", nullable = false)
    private Integer numChildren = 0;

    @NotNull(message = "Reservation status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "actual_check_in")
    private LocalDateTime actualCheckIn;

    @Column(name = "actual_check_out")
    private LocalDateTime actualCheckOut;

    // Pricing fields
    @DecimalMin(value = "0.0", message = "Subtotal cannot be negative")
    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Tax cannot be negative")
    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Discount cannot be negative")
    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Discount percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "Discount percentage cannot exceed 100")
    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(name = "discount_applied_by", length = 50)
    private String discountAppliedBy;

    @DecimalMin(value = "0.0", message = "Add-ons total cannot be negative")
    @Column(name = "addons_total", precision = 10, scale = 2)
    private BigDecimal addOnsTotal = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Loyalty discount cannot be negative")
    @Column(name = "loyalty_discount", precision = 10, scale = 2)
    private BigDecimal loyaltyDiscount = BigDecimal.ZERO;

    @Column(name = "loyalty_points_used")
    private Integer loyaltyPointsUsed = 0;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "amount_paid", precision = 10, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Size(max = 500, message = "Special requests cannot exceed 500 characters")
    @Column(name = "special_requests", length = 500)
    private String specialRequests;

    @Column(name = "booked_via_kiosk")
    private boolean bookedViaKiosk = false;

    // Relationships
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ReservationRoom> reservationRooms = new ArrayList<>();

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ReservationAddOn> addOns = new ArrayList<>();

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payment> payments = new ArrayList<>();

    @OneToOne(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Feedback feedback;

    // Constructors
    public Reservation() {
        this.confirmationNumber = generateConfirmationNumber();
    }

    public Reservation(Guest guest, LocalDate checkInDate, LocalDate checkOutDate, int numAdults) {
        this();
        this.guest = guest;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.numAdults = numAdults;
    }

    // Business Methods

    /**
     * Generates a unique confirmation number.
     */
    private String generateConfirmationNumber() {
        return "RES" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    /**
     * Calculates the number of nights for this reservation.
     */
    public long getNumberOfNights() {
        return ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    /**
     * Gets the total number of guests (adults + children).
     */
    public int getTotalGuests() {
        return numAdults + (numChildren != null ? numChildren : 0);
    }

    /**
     * Calculates the outstanding balance.
     */
    public BigDecimal getOutstandingBalance() {
        return totalAmount.subtract(amountPaid);
    }

    /**
     * Checks if the reservation is fully paid.
     */
    public boolean isFullyPaid() {
        return amountPaid.compareTo(totalAmount) >= 0;
    }

    /**
     * Checks if checkout is allowed (must be fully paid).
     */
    public boolean canCheckout() {
        return status == ReservationStatus.CHECKED_IN && isFullyPaid();
    }

    /**
     * Adds a room to this reservation.
     */
    public void addRoom(ReservationRoom reservationRoom) {
        reservationRooms.add(reservationRoom);
        reservationRoom.setReservation(this);
    }

    /**
     * Removes a room from this reservation.
     */
    public void removeRoom(ReservationRoom reservationRoom) {
        reservationRooms.remove(reservationRoom);
        reservationRoom.setReservation(null);
    }

    /**
     * Adds an add-on service to this reservation.
     */
    public void addAddOn(ReservationAddOn addOn) {
        addOns.add(addOn);
        addOn.setReservation(this);
    }

    /**
     * Removes an add-on service from this reservation.
     */
    public void removeAddOn(ReservationAddOn addOn) {
        addOns.remove(addOn);
        addOn.setReservation(null);
    }

    /**
     * Records a payment for this reservation.
     */
    public void addPayment(Payment payment) {
        payments.add(payment);
        payment.setReservation(this);
        recalculateAmountPaid();
    }

    /**
     * Recalculates the total amount paid from all payments.
     */
    public void recalculateAmountPaid() {
        this.amountPaid = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates and updates the total amount.
     */
    public void calculateTotal() {
        // Calculate room subtotal
        BigDecimal roomSubtotal = reservationRooms.stream()
                .map(rr -> rr.getRoomPrice().multiply(BigDecimal.valueOf(getNumberOfNights())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate add-ons total
        BigDecimal addOnsSum = addOns.stream()
                .map(ReservationAddOn::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.subtotal = roomSubtotal;
        this.addOnsTotal = addOnsSum;

        // Apply percentage discount
        BigDecimal afterDiscount = subtotal.add(addOnsTotal);
        if (discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            this.discountAmount = afterDiscount.multiply(discountPercentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            afterDiscount = afterDiscount.subtract(discountAmount);
        }

        // Apply loyalty discount
        afterDiscount = afterDiscount.subtract(loyaltyDiscount);

        // Calculate tax (13% HST example)
        this.taxAmount = afterDiscount.multiply(BigDecimal.valueOf(0.13))
                .setScale(2, RoundingMode.HALF_UP);

        this.totalAmount = afterDiscount.add(taxAmount);
    }

    /**
     * Confirms the reservation.
     */
    public void confirm() {
        if (status == ReservationStatus.PENDING) {
            this.status = ReservationStatus.CONFIRMED;
        }
    }

    /**
     * Performs check-in.
     */
    public void checkIn() {
        if (status == ReservationStatus.CONFIRMED || status == ReservationStatus.PENDING) {
            this.status = ReservationStatus.CHECKED_IN;
            this.actualCheckIn = LocalDateTime.now();
            // Mark all rooms as occupied
            reservationRooms.forEach(rr -> rr.getRoom().markOccupied());
        }
    }

    /**
     * Performs check-out.
     */
    public void checkOut() {
        if (canCheckout()) {
            this.status = ReservationStatus.CHECKED_OUT;
            this.actualCheckOut = LocalDateTime.now();
            // Mark all rooms for cleaning
            reservationRooms.forEach(rr -> rr.getRoom().markForCleaning());
        }
    }

    /**
     * Checks if the reservation has been checked out.
     */
    public boolean isCheckedOut() {
        return status == ReservationStatus.CHECKED_OUT;
    }

    /**
     * Cancels the reservation.
     */
    public void cancel() {
        if (status != ReservationStatus.CHECKED_IN && status != ReservationStatus.CHECKED_OUT) {
            this.status = ReservationStatus.CANCELLED;
            // Release all rooms
            reservationRooms.forEach(rr -> rr.getRoom().markAvailable());
        }
    }

    // Getters and Setters
    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public void setConfirmationNumber(String confirmationNumber) {
        this.confirmationNumber = confirmationNumber;
    }

    public Guest getGuest() {
        return guest;
    }

    public void setGuest(Guest guest) {
        this.guest = guest;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public Integer getNumAdults() {
        return numAdults;
    }

    public void setNumAdults(Integer numAdults) {
        this.numAdults = numAdults;
    }

    public Integer getNumChildren() {
        return numChildren;
    }

    public void setNumChildren(Integer numChildren) {
        this.numChildren = numChildren;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public LocalDateTime getActualCheckIn() {
        return actualCheckIn;
    }

    public void setActualCheckIn(LocalDateTime actualCheckIn) {
        this.actualCheckIn = actualCheckIn;
    }

    public LocalDateTime getActualCheckOut() {
        return actualCheckOut;
    }

    public void setActualCheckOut(LocalDateTime actualCheckOut) {
        this.actualCheckOut = actualCheckOut;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(BigDecimal discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public String getDiscountAppliedBy() {
        return discountAppliedBy;
    }

    public void setDiscountAppliedBy(String discountAppliedBy) {
        this.discountAppliedBy = discountAppliedBy;
    }

    public BigDecimal getAddOnsTotal() {
        return addOnsTotal;
    }

    public void setAddOnsTotal(BigDecimal addOnsTotal) {
        this.addOnsTotal = addOnsTotal;
    }

    public BigDecimal getLoyaltyDiscount() {
        return loyaltyDiscount;
    }

    public void setLoyaltyDiscount(BigDecimal loyaltyDiscount) {
        this.loyaltyDiscount = loyaltyDiscount;
    }

    public Integer getLoyaltyPointsUsed() {
        return loyaltyPointsUsed;
    }

    public void setLoyaltyPointsUsed(Integer loyaltyPointsUsed) {
        this.loyaltyPointsUsed = loyaltyPointsUsed;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }

    public boolean isBookedViaKiosk() {
        return bookedViaKiosk;
    }

    public void setBookedViaKiosk(boolean bookedViaKiosk) {
        this.bookedViaKiosk = bookedViaKiosk;
    }

    public List<ReservationRoom> getReservationRooms() {
        return reservationRooms;
    }

    public void setReservationRooms(List<ReservationRoom> reservationRooms) {
        this.reservationRooms = reservationRooms;
    }

    public List<ReservationAddOn> getAddOns() {
        return addOns;
    }

    public void setAddOns(List<ReservationAddOn> addOns) {
        this.addOns = addOns;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public Feedback getFeedback() {
        return feedback;
    }

    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "confirmationNumber='" + confirmationNumber + '\'' +
                ", guest=" + (guest != null ? guest.getFullName() : "null") +
                ", checkIn=" + checkInDate +
                ", checkOut=" + checkOutDate +
                ", status=" + status +
                ", total=" + totalAmount +
                ", balance=" + getOutstandingBalance() +
                '}';
    }
}
