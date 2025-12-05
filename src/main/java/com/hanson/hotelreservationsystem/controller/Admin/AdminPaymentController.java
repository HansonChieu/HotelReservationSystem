package com.hanson.hotelreservationsystem.controller.Admin;

import com.hanson.hotelreservationsystem.model.Payment;
import com.hanson.hotelreservationsystem.model.Reservation;
import com.hanson.hotelreservationsystem.service.NavigationService;
import com.hanson.hotelreservationsystem.service.ReservationService;
import com.hanson.hotelreservationsystem.session.AdminSession;
import com.hanson.hotelreservationsystem.util.ActivityLogger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminPaymentController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(AdminPaymentController.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // ==================== UI Fields (Matched to FXML) ====================
    @FXML private Label confirmationLabel;
    @FXML private Label guestNameLabel;
    @FXML private Label datesLabel;
    @FXML private Label roomsLabel;

    // Balance Info
    @FXML private Label totalAmountLabel;
    @FXML private Label paidAmountLabel;
    @FXML private Label outstandingLabel;

    // Toggles
    @FXML private ToggleButton cashToggle;
    @FXML private ToggleButton cardToggle;
    @FXML private ToggleButton loyaltyToggle;
    @FXML private ToggleGroup paymentMethodGroup;

    // Inputs
    @FXML private TextField paymentAmountField;
    @FXML private Label errorLabel;
    @FXML private Button processButton;
    @FXML private ComboBox<String> paymentTypeCombo;
    @FXML private TextArea notesArea;

    // Card Section
    @FXML private VBox cardSection;
    @FXML private TextField cardNumberField;
    @FXML private TextField cardholderField;
    @FXML private TextField cardExpiryField;
    @FXML private PasswordField cardCvvField;
    @FXML private ComboBox<String> cardTypeCombo;

    // Loyalty Section
    @FXML private VBox loyaltySection;
    @FXML private Label availablePointsLabel;
    @FXML private Label pointsValueLabel;
    @FXML private Spinner<Integer> pointsSpinner;
    @FXML private Label pointsDiscountLabel;

    // Summary
    @FXML private Label summaryPaymentLabel;
    @FXML private Label summaryBalanceLabel;
    @FXML private Label remainingBalanceLabel;

    // ==================== Data ====================
    private Reservation currentReservation;
    private final NavigationService navigationService;
    private final ReservationService reservationService;
    private final AdminSession adminSession;
    private final ActivityLogger activityLogger;

    public AdminPaymentController() {
        this.navigationService = NavigationService.getInstance();
        this.reservationService = ReservationService.getInstance();
        this.adminSession = AdminSession.getInstance();
        this.activityLogger = ActivityLogger.getInstance();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Verify Session
        if (!adminSession.isLoggedIn()) {
            Platform.runLater(navigationService::goToAdminLogin);
            return;
        }

        currentReservation = adminSession.getCurrentReservation();
        if (currentReservation == null) {
            Platform.runLater(navigationService::goToAdminDashboard);
            return;
        }

        // 2. Setup UI
        setupDropdowns();
        loadReservationData();

        // Select Cash by default
        paymentMethodGroup.selectToggle(cashToggle);
        handlePaymentMethodChange(null);
    }

    private void setupDropdowns() {
        // Payment Types (Deposit, Partial, Settlement)
        paymentTypeCombo.getItems().addAll("Deposit", "Partial Payment", "Final Settlement", "Incidental");
        paymentTypeCombo.getSelectionModel().select("Partial Payment");

        // Card Types
        cardTypeCombo.getItems().addAll("Visa", "MasterCard", "Amex", "Debit");
    }

    private void loadReservationData() {
        // Header Info
        confirmationLabel.setText("Res #: " + currentReservation.getConfirmationNumber());
        if (currentReservation.getGuest() != null) {
            guestNameLabel.setText(currentReservation.getGuest().getFullName());
        }

        String dateStr = currentReservation.getCheckInDate().format(DATE_FORMAT) + " - " +
                currentReservation.getCheckOutDate().format(DATE_FORMAT);
        datesLabel.setText(dateStr + " (" + currentReservation.getNumberOfNights() + " nights)");

        roomsLabel.setText(currentReservation.getReservationRooms().size() + " Room(s)");

        // Financials
        // Ensure totals are fresh
        currentReservation.calculateTotal();
        BigDecimal total = currentReservation.getTotalAmount();
        BigDecimal paid = currentReservation.getAmountPaid();
        BigDecimal outstanding = currentReservation.getOutstandingBalance();

        totalAmountLabel.setText(formatCurrency(total));
        paidAmountLabel.setText(formatCurrency(paid));
        outstandingLabel.setText(formatCurrency(outstanding));

        // Init Summary Labels
        summaryBalanceLabel.setText(formatCurrency(outstanding));
        summaryPaymentLabel.setText("$0.00");
        remainingBalanceLabel.setText(formatCurrency(outstanding));
    }

    // ==================== Event Handlers ====================

    @FXML
    public void handlePaymentMethodChange(ActionEvent event) {
        // Hide all sections first
        cardSection.setVisible(false);
        cardSection.setManaged(false);
        loyaltySection.setVisible(false);
        loyaltySection.setManaged(false);

        Toggle selected = paymentMethodGroup.getSelectedToggle();
        if (selected == cardToggle) {
            cardSection.setVisible(true);
            cardSection.setManaged(true);
        } else if (selected == loyaltyToggle) {
            loyaltySection.setVisible(true);
            loyaltySection.setManaged(true);
            // TODO: Load loyalty points from LoyaltyService here if implementing loyalty
        }

        validateInput(); // Re-validate
    }

    @FXML
    public void handleAmountChange(KeyEvent event) {
        updatePaymentSummary();
        validateInput();
    }

    @FXML
    public void handlePayFullBalance(ActionEvent event) {
        BigDecimal outstanding = currentReservation.getOutstandingBalance();
        paymentAmountField.setText(outstanding.toString());
        updatePaymentSummary();
        validateInput();
    }

    private void updatePaymentSummary() {
        try {
            BigDecimal payment = new BigDecimal(paymentAmountField.getText());
            BigDecimal outstanding = currentReservation.getOutstandingBalance();
            BigDecimal remaining = outstanding.subtract(payment);

            summaryPaymentLabel.setText(formatCurrency(payment));
            remainingBalanceLabel.setText(formatCurrency(remaining));

            // Visual feedback for overpayment
            if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                remainingBalanceLabel.setStyle("-fx-text-fill: red;");
            } else {
                remainingBalanceLabel.setStyle(""); // Reset
            }

        } catch (NumberFormatException e) {
            summaryPaymentLabel.setText("$0.00");
            remainingBalanceLabel.setText(formatCurrency(currentReservation.getOutstandingBalance()));
        }
    }

    private void validateInput() {
        boolean isValid = true;
        errorLabel.setVisible(false);

        // 1. Validate Amount
        String amountText = paymentAmountField.getText();
        if (amountText == null || amountText.trim().isEmpty()) {
            isValid = false;
        } else {
            try {
                BigDecimal amount = new BigDecimal(amountText);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    errorLabel.setText("Amount must be positive");
                    errorLabel.setVisible(true);
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                errorLabel.setText("Invalid amount format");
                errorLabel.setVisible(true);
                isValid = false;
            }
        }

        // 2. Validate Card fields if Card is selected
        if (paymentMethodGroup.getSelectedToggle() == cardToggle) {
            if (cardNumberField.getText().isEmpty() || cardExpiryField.getText().isEmpty()) {
                isValid = false;
            }
        }

        processButton.setDisable(!isValid);
    }

    @FXML
    public void handleProcessPayment(ActionEvent event) {
        try {
            BigDecimal amount = new BigDecimal(paymentAmountField.getText());
            String method = "CASH";
            if (paymentMethodGroup.getSelectedToggle() == cardToggle) method = "CARD";
            if (paymentMethodGroup.getSelectedToggle() == loyaltyToggle) method = "LOYALTY";

            // Process via Service
            Payment payment = reservationService.processPayment(currentReservation, amount, method);

            // Log it
            activityLogger.logActivity(adminSession.getActorName(),
                    "PAYMENT", "RESERVATION",
                    currentReservation.getConfirmationNumber(),
                    "Processed payment of " + formatCurrency(amount) + " via " + method);

            // Show Success
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Payment Successful!", ButtonType.OK);
            alert.showAndWait();

            // *** REDIRECT LOGIC ***
            // Go back to details so admin can see the updated balance
            adminSession.setCurrentReservation(currentReservation);
            navigationService.goToAdminReservationDetails();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Payment failed", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Payment Failed: " + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        // Just go back to details without doing anything
        navigationService.goToAdminReservationDetails();
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("$%.2f", amount != null ? amount : BigDecimal.ZERO);
    }
}