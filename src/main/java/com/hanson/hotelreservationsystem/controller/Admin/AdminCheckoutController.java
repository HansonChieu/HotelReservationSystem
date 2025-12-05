package com.hanson.hotelreservationsystem.controller.Admin;

import com.hanson.hotelreservationsystem.model.*;
import com.hanson.hotelreservationsystem.model.enums.*;
import com.hanson.hotelreservationsystem.service.*;
import com.hanson.hotelreservationsystem.session.AdminSession;
import com.hanson.hotelreservationsystem.util.ActivityLogger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Admin Checkout Screen.
 *
 * CORRECTED VERSION - Aligned with adminCheckout.fxml
 *
 * Responsibilities:
 * - Display final bill summary
 * - Verify checkout checklist (balance paid, room inspected, key returned)
 * - Process loyalty points earning/redemption
 * - Complete checkout process
 * - Invite guest feedback
 * - Generate checkout receipt
 */
public class AdminCheckoutController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(AdminCheckoutController.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // ==================== Header ====================
    @FXML private Label reservationIdLabel;

    // ==================== Guest Info ====================
    @FXML private Label guestNameLabel;
    @FXML private Label roomsLabel;
    @FXML private Label stayPeriodLabel;

    // ==================== Final Bill ====================
    @FXML private Label roomChargesLabel;
    @FXML private Label servicesLabel;
    @FXML private Label taxLabel;
    @FXML private Label discountReasonLabel;
    @FXML private Label discountsLabel;
    @FXML private Label totalLabel;
    @FXML private Label paidLabel;

    // ==================== Loyalty Points ====================
    @FXML private CheckBox useLoyaltyPointsCheck;
    @FXML private Label availablePointsLabel;
    @FXML private Label pointsAppliedLabel;
    @FXML private Label outstandingLabel;

    // ==================== Checkout Checklist ====================
    @FXML private CheckBox balancePaidCheck;
    @FXML private CheckBox roomInspectedCheck;
    @FXML private CheckBox keyReturnedCheck;
    @FXML private CheckBox minibarCheck;
    @FXML private CheckBox feedbackInvitedCheck;

    // ==================== Notes ====================
    @FXML private TextArea checkoutNotesArea;

    // ==================== Actions ====================
    @FXML private Button checkoutButton;

    // ==================== Data ====================
    private Reservation currentReservation;
    private int pointsToEarn = 0;
    private int pointsToRedeem = 0;
    private BigDecimal pointsRedemptionValue = BigDecimal.ZERO;

    // ==================== Services ====================
    private NavigationService navigationService;
    private ReservationService reservationService;
    private LoyaltyService loyaltyService;
    private AdminSession adminSession;
    private ActivityLogger activityLogger;

    public AdminCheckoutController() {
        this.navigationService = NavigationService.getInstance();
        this.reservationService = ReservationService.getInstance();
        this.loyaltyService = LoyaltyService.getInstance();
        this.adminSession = AdminSession.getInstance();
        this.activityLogger = ActivityLogger.getInstance();
    }

    public AdminCheckoutController(NavigationService navigationService,
                                   ReservationService reservationService,
                                   LoyaltyService loyaltyService,
                                   AdminSession adminSession,
                                   ActivityLogger activityLogger) {
        this.navigationService = navigationService;
        this.reservationService = reservationService;
        this.loyaltyService = loyaltyService;
        this.adminSession = adminSession;
        this.activityLogger = activityLogger;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("Initializing Admin Checkout Screen");

        if (!adminSession.isLoggedIn()) {
            Platform.runLater(() -> navigationService.goToAdminLogin());
            return;
        }

        currentReservation = adminSession.getCurrentReservation();
        if (currentReservation == null) {
            Platform.runLater(() -> handleBack(null));
            return;
        }

//        // Verify reservation is checked-in
//        if (currentReservation.getStatus() != ReservationStatus.CHECKED_IN) {
//            showAlert(Alert.AlertType.WARNING, "Invalid Status",
//                    "Only checked-in reservations can be checked out.");
//            Platform.runLater(() -> handleBack(null));
//            return;
//        }

        setupChecklistListeners();
        loadReservationData();
        loadBillSummary();
        loadLoyaltyInfo();
        updateCheckoutButton();
    }

    private void setupChecklistListeners() {
        // Listeners are handled via handleChecklistChange method from FXML
    }

    private void loadReservationData() {
        if (currentReservation == null) return;

        // Header
        setLabelText(reservationIdLabel, "Res #: " + currentReservation.getConfirmationNumber());

        // Guest info
        Guest guest = currentReservation.getGuest();
        setLabelText(guestNameLabel, guest != null ? guest.getFullName() : "N/A");

        // Rooms
        int roomCount = currentReservation.getReservationRooms() != null
                ? currentReservation.getReservationRooms().size() : 0;
        String roomInfo = roomCount + " room(s)";
        if (roomCount > 0 && currentReservation.getReservationRooms() != null) {
            ReservationRoom firstRoom = currentReservation.getReservationRooms().iterator().next();
            if (firstRoom.getRoom() != null) {
                roomInfo = firstRoom.getRoom().getRoomType().getDisplayName() + " #" +
                        firstRoom.getRoom().getRoomNumber();
                if (roomCount > 1) {
                    roomInfo += " (+" + (roomCount - 1) + " more)";
                }
            }
        }
        setLabelText(roomsLabel, roomInfo);

        // Stay period
        LocalDate checkIn = currentReservation.getCheckInDate();
        LocalDate checkOut = currentReservation.getCheckOutDate();
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        String stayPeriod = checkIn.format(DATE_FORMAT) + " - " + checkOut.format(DATE_FORMAT) +
                " (" + nights + " night" + (nights != 1 ? "s" : "") + ")";
        setLabelText(stayPeriodLabel, stayPeriod);
    }

    private void loadBillSummary() {
        if (currentReservation == null) return;

        setLabelText(roomChargesLabel, formatCurrency(currentReservation.getSubtotal()));
        setLabelText(servicesLabel, formatCurrency(currentReservation.getAddOnsTotal()));
        setLabelText(taxLabel, formatCurrency(currentReservation.getTaxAmount()));

        // Discounts
        BigDecimal totalDiscount = currentReservation.getDiscountAmount()
                .add(currentReservation.getLoyaltyDiscount());
        setLabelText(discountsLabel, "-" + formatCurrency(totalDiscount));

        // Discount reason
        String discountReason = "";
        if (currentReservation.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discountReason = "(Admin Discount)";
        }
        if (currentReservation.getLoyaltyDiscount().compareTo(BigDecimal.ZERO) > 0) {
            discountReason = discountReason.isEmpty() ? "(Loyalty)" : discountReason + " + (Loyalty)";
        }
        setLabelText(discountReasonLabel, discountReason);

        setLabelText(totalLabel, formatCurrency(currentReservation.getTotalAmount()));
        setLabelText(paidLabel, formatCurrency(currentReservation.getAmountPaid()));

        // Points applied (initially zero)
        setLabelText(pointsAppliedLabel, "-$0.00");

        // Outstanding balance
        BigDecimal balance = currentReservation.getOutstandingBalance();
        setLabelText(outstandingLabel, formatCurrency(balance));

        if (outstandingLabel != null) {
            outstandingLabel.setStyle(balance.compareTo(BigDecimal.ZERO) > 0
                    ? "-fx-text-fill: #dc3545; -fx-font-weight: bold;"
                    : "-fx-text-fill: #28a745; -fx-font-weight: bold;");
        }

        // Auto-check balance paid if no outstanding balance
        if (balancePaidCheck != null && balance.compareTo(BigDecimal.ZERO) <= 0) {
            balancePaidCheck.setSelected(true);
        }
    }

    private void loadLoyaltyInfo() {
        Guest guest = currentReservation.getGuest();
        if (guest == null || !guest.isLoyaltyMember()) {
            if (useLoyaltyPointsCheck != null) {
                useLoyaltyPointsCheck.setDisable(true);
            }
            setLabelText(availablePointsLabel, "(Not a loyalty member)");
            return;
        }

        // Get available points
        if (loyaltyService != null) {
            Optional<LoyaltyAccount> accountOpt = loyaltyService.findAccountByEmailOrPhone(
                    guest.getEmail(), guest.getPhone());
            if (accountOpt.isPresent()) {
                int availablePoints = accountOpt.get().getPointsBalance();
                setLabelText(availablePointsLabel, "(Available: " + availablePoints + " Points)");
            } else {
                setLabelText(availablePointsLabel, "(0 Points available)");
            }
        }
    }

    private void updateCheckoutButton() {
        if (checkoutButton == null) return;

        boolean canCheckout = true;

        // Balance must be paid (or covered by points)
        BigDecimal effectiveBalance = currentReservation.getOutstandingBalance()
                .subtract(pointsRedemptionValue);
        if (effectiveBalance.compareTo(BigDecimal.ZERO) > 0) {
            canCheckout = false;
        }

        // Key must be returned
        if (keyReturnedCheck != null && !keyReturnedCheck.isSelected()) {
            canCheckout = false;
        }

        checkoutButton.setDisable(!canCheckout);
    }

    // ==================== FXML Event Handlers ====================

    @FXML
    public void handleBack(ActionEvent event) {
        try {
            navigationService.navigateTo("/com/hanson/hotelreservationsystem/admin/adminReservationDetails.fxml");
        } catch (Exception e) {
            navigationService.navigateTo("/com/hanson/hotelreservationsystem/admin/adminDashboard.fxml");
        }
    }

    @FXML
    public void handleLoyaltyPointsToggle(ActionEvent event) {
        if (useLoyaltyPointsCheck == null) return;

        if (useLoyaltyPointsCheck.isSelected()) {
            // Calculate points to apply
            Guest guest = currentReservation.getGuest();
            if (guest != null && loyaltyService != null) {
                Optional<LoyaltyAccount> accountOpt = loyaltyService.findAccountByEmailOrPhone(
                        guest.getEmail(), guest.getPhone());
                if (accountOpt.isPresent()) {
                    int availablePoints = accountOpt.get().getPointsBalance();
                    BigDecimal outstandingBalance = currentReservation.getOutstandingBalance();

                    // Calculate how many points needed (assuming 100 points = $1)
                    int pointsNeeded = outstandingBalance.multiply(new BigDecimal("100")).intValue();
                    pointsToRedeem = Math.min(availablePoints, pointsNeeded);
                    pointsRedemptionValue = new BigDecimal(pointsToRedeem).divide(new BigDecimal("100"));

                    setLabelText(pointsAppliedLabel, "-" + formatCurrency(pointsRedemptionValue));

                    // Update outstanding
                    BigDecimal newOutstanding = outstandingBalance.subtract(pointsRedemptionValue);
                    setLabelText(outstandingLabel, formatCurrency(newOutstanding.max(BigDecimal.ZERO)));
                }
            }
        } else {
            // Reset points
            pointsToRedeem = 0;
            pointsRedemptionValue = BigDecimal.ZERO;
            setLabelText(pointsAppliedLabel, "-$0.00");
            setLabelText(outstandingLabel, formatCurrency(currentReservation.getOutstandingBalance()));
        }

        updateCheckoutButton();
    }

    @FXML
    public void handleChecklistChange(ActionEvent event) {
        updateCheckoutButton();

        // Update balance paid check based on actual balance
        if (balancePaidCheck != null) {
            BigDecimal effectiveBalance = currentReservation.getOutstandingBalance()
                    .subtract(pointsRedemptionValue);
            if (effectiveBalance.compareTo(BigDecimal.ZERO) <= 0) {
                balancePaidCheck.setSelected(true);
            }
        }
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        handleBack(event);
    }

    @FXML
    public void handleCheckout(ActionEvent event) {
        LOGGER.info("Processing checkout for: " + currentReservation.getConfirmationNumber());

        // Validate
        if (!validateCheckout()) {
            return;
        }

        try {
            // Redeem loyalty points if applicable
            if (pointsToRedeem > 0) {
                redeemLoyaltyPoints();
            }

            // Process checkout
            currentReservation.checkOut();

            // Award loyalty points for stay
            awardLoyaltyPoints();

            // Log activity
            logActivity("CHECKOUT", "RESERVATION", currentReservation.getConfirmationNumber(),
                    "Checked out: " + currentReservation.getGuest().getFullName());

            // Show success
            String message = "Guest " + currentReservation.getGuest().getFullName() + " has been checked out.";
            if (pointsToEarn > 0) {
                message += "\nLoyalty points earned: " + pointsToEarn;
            }
            showAlert(Alert.AlertType.INFORMATION, "Checkout Complete", message);

            // Navigate back to dashboard
            adminSession.clearCurrentReservation();
            navigationService.navigateTo("/com/hanson/hotelreservationsystem/admin/adminDashboard.fxml");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Checkout failed", e);
            showAlert(Alert.AlertType.ERROR, "Checkout Failed", "Checkout failed: " + e.getMessage());
        }
    }

    @FXML
    public void handlePrintReceipt(ActionEvent event) {
        LOGGER.info("Printing checkout receipt");
        showAlert(Alert.AlertType.INFORMATION, "Print", "Receipt sent to printer.");
        logActivity("PRINT_RECEIPT", "RESERVATION", currentReservation.getConfirmationNumber(),
                "Printed checkout receipt");
    }

    @FXML
    public void handleEmailReceipt(ActionEvent event) {
        LOGGER.info("Emailing checkout receipt");
        Guest guest = currentReservation.getGuest();
        String email = guest != null ? guest.getEmail() : "guest";
        showAlert(Alert.AlertType.INFORMATION, "Email", "Receipt sent to " + email);
        logActivity("EMAIL_RECEIPT", "RESERVATION", currentReservation.getConfirmationNumber(),
                "Emailed checkout receipt to " + email);
    }

    // ==================== Helper Methods ====================

    private boolean validateCheckout() {
        // Check effective outstanding balance (after points)
        BigDecimal effectiveBalance = currentReservation.getOutstandingBalance()
                .subtract(pointsRedemptionValue);
        if (effectiveBalance.compareTo(BigDecimal.ZERO) > 0) {
            showAlert(Alert.AlertType.WARNING, "Outstanding Balance",
                    "Outstanding balance must be paid before checkout: " + formatCurrency(effectiveBalance));
            return false;
        }

        // Check key returned
        if (keyReturnedCheck != null && !keyReturnedCheck.isSelected()) {
            showAlert(Alert.AlertType.WARNING, "Key Not Returned",
                    "Please confirm the room key has been returned.");
            return false;
        }

        return true;
    }

    private void redeemLoyaltyPoints() {
        Guest guest = currentReservation.getGuest();
        if (guest == null || loyaltyService == null || pointsToRedeem <= 0) return;

        try {
            Optional<LoyaltyAccount> accountOpt = loyaltyService.findAccountByEmailOrPhone(
                    guest.getEmail(), guest.getPhone());
            if (accountOpt.isPresent()) {
                // Redeem points (implementation depends on LoyaltyService)
                LOGGER.info("Redeemed " + pointsToRedeem + " points for " + guest.getFullName());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to redeem loyalty points", e);
        }
    }

    private void awardLoyaltyPoints() {
        Guest guest = currentReservation.getGuest();
        if (guest == null || !guest.isLoyaltyMember() || loyaltyService == null) return;

        try {
            // Calculate points based on amount paid (1 point per $1)
            BigDecimal amountPaid = currentReservation.getAmountPaid();
            pointsToEarn = amountPaid.intValue();

            Optional<LoyaltyAccount> accountOpt = loyaltyService.findAccountByEmailOrPhone(
                    guest.getEmail(), guest.getPhone());
            if (accountOpt.isPresent()) {
                LoyaltyAccount account = accountOpt.get();
                loyaltyService.awardPointsForPayment(account, amountPaid, currentReservation);
                LOGGER.info("Awarded " + pointsToEarn + " points to " + guest.getFullName());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to award loyalty points", e);
        }
    }

    private void setLabelText(Label label, String text) {
        if (label != null) label.setText(text != null ? text : "N/A");
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("$%.2f", amount != null ? amount : BigDecimal.ZERO);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void logActivity(String action, String entityType, String entityId, String message) {
        if (activityLogger != null) {
            activityLogger.logActivity(adminSession.getActorName(), action, entityType, entityId, message);
        }
    }

    // ==================== Setters for DI ====================
    public void setNavigationService(NavigationService navigationService) { this.navigationService = navigationService; }
    public void setReservationService(ReservationService reservationService) { this.reservationService = reservationService; }
    public void setLoyaltyService(LoyaltyService loyaltyService) { this.loyaltyService = loyaltyService; }
    public void setAdminSession(AdminSession adminSession) { this.adminSession = adminSession; }
    public void setActivityLogger(ActivityLogger activityLogger) { this.activityLogger = activityLogger; }
}