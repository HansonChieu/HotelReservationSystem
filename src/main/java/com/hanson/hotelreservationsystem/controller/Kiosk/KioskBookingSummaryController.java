package com.hanson.hotelreservationsystem.controller.Kiosk;

import com.hanson.hotelreservationsystem.model.Guest;
import com.hanson.hotelreservationsystem.model.Reservation;
import com.hanson.hotelreservationsystem.model.ReservationAddOn;
import com.hanson.hotelreservationsystem.model.ReservationRoom;
import com.hanson.hotelreservationsystem.model.enums.AddOnType;
import com.hanson.hotelreservationsystem.model.enums.ReservationStatus;
import com.hanson.hotelreservationsystem.model.enums.RoomType;
import com.hanson.hotelreservationsystem.service.NavigationService;
import com.hanson.hotelreservationsystem.service.ReservationService;
import com.hanson.hotelreservationsystem.session.BookingSession;
import com.hanson.hotelreservationsystem.session.BookingSession.RoomSelection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Kiosk Booking Summary Screen.
 *
 * Responsibilities:
 * - Display complete booking summary for review
 * - Allow editing of previous steps
 * - Handle booking confirmation
 * - Process the reservation creation
 *
 * This screen shows:
 * - Guest information
 * - Reservation details (dates, nights, guests)
 * - Selected rooms with pricing
 * - Add-on services
 * - Complete price breakdown with tax and loyalty effects
 */
public class KioskBookingSummaryController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(KioskBookingSummaryController.class.getName());

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    // Guest Information
    @FXML private Label guestNameLabel;
    @FXML private Label guestEmailLabel;
    @FXML private Label guestPhoneLabel;
    @FXML private Label guestCountryLabel;

    // Reservation Details
    @FXML private Label checkInLabel;
    @FXML private Label checkOutLabel;
    @FXML private Label nightsLabel;
    @FXML private Label guestsLabel;

    // Rooms
    @FXML private VBox roomsListBox;

    // Add-on Services
    @FXML private VBox servicesListBox;

    // Pricing
    @FXML private Label roomChargesLabel;
    @FXML private Label roomBreakdownLabel;
    @FXML private Label servicesChargesLabel;
    @FXML private VBox servicesBreakdownBox;
    @FXML private Label subtotalLabel;
    @FXML private Label taxLabel;
    @FXML private HBox loyaltyDiscountRow;
    @FXML private Label loyaltyDiscountLabel;
    @FXML private Label totalLabel;

    // Buttons
    @FXML private Button confirmButton;
    @FXML private Button rulesButton;

    // Services
    private NavigationService navigationService;
    private BookingSession bookingSession;
    private ReservationService reservationService;

    /**
     * Default constructor for FXML loader.
     */
    public KioskBookingSummaryController() {
        this.navigationService = NavigationService.getInstance();
        this.bookingSession = BookingSession.getInstance();
        this.reservationService = ReservationService.getInstance();
    }

    /**
     * Constructor with dependency injection.
     */
    public KioskBookingSummaryController(NavigationService navigationService,
                                         BookingSession bookingSession,
                                         ReservationService reservationService) {
        this.navigationService = navigationService;
        this.bookingSession = bookingSession;
        this.reservationService = reservationService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("Initializing Kiosk Booking Summary Screen");

        populateGuestInfo();
        populateReservationDetails();
        populateRoomsList();
        populateServicesList();
        populatePricing();
    }

    /**
     * Populate guest information section.
     */
    private void populateGuestInfo() {
        if (guestNameLabel != null) {
            guestNameLabel.setText(bookingSession.getFullName());
        }
        if (guestEmailLabel != null) {
            guestEmailLabel.setText(bookingSession.getEmail());
        }
        if (guestPhoneLabel != null) {
            guestPhoneLabel.setText(bookingSession.getPhone());
        }
        if (guestCountryLabel != null) {
            guestCountryLabel.setText(bookingSession.getCountry());
        }
    }

    /**
     * Populate reservation details section.
     */
    private void populateReservationDetails() {
        if (checkInLabel != null && bookingSession.getCheckInDate() != null) {
            checkInLabel.setText(bookingSession.getCheckInDate().format(DATE_FORMATTER) + " (4:00 PM)");
        }
        if (checkOutLabel != null && bookingSession.getCheckOutDate() != null) {
            checkOutLabel.setText(bookingSession.getCheckOutDate().format(DATE_FORMATTER) + " (12:00 PM)");
        }
        if (nightsLabel != null) {
            long nights = bookingSession.getNights();
            nightsLabel.setText(nights + (nights == 1 ? " night" : " nights"));
        }
        if (guestsLabel != null) {
            int adults = bookingSession.getAdultCount();
            int children = bookingSession.getChildCount();
            StringBuilder guestText = new StringBuilder();
            guestText.append(adults).append(adults == 1 ? " Adult" : " Adults");
            if (children > 0) {
                guestText.append(", ").append(children).append(children == 1 ? " Child" : " Children");
            }
            guestsLabel.setText(guestText.toString());
        }
    }

    /**
     * Populate rooms list section.
     */
    private void populateRoomsList() {
        if (roomsListBox == null) return;

        roomsListBox.getChildren().clear();

        for (RoomSelection selection : bookingSession.getSelectedRooms()) {
            if (selection.getQuantity() > 0) {
                HBox roomRow = createRoomRow(selection);
                roomsListBox.getChildren().add(roomRow);
            }
        }
    }

    /**
     * Create a row for displaying a room selection.
     */
    private HBox createRoomRow(RoomSelection selection) {
        HBox row = new HBox(10);
        row.setStyle("-fx-alignment: CENTER-LEFT;");

        Label typeLabel = new Label(selection.getQuantity() + "x " + selection.getRoomType().getDisplayName());
        typeLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");

        Label priceLabel = new Label(String.format("$%.2f/night", selection.getPricePerNight()));
        priceLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333333;");

        row.getChildren().addAll(typeLabel, priceLabel);
        return row;
    }

    /**
     * Populate add-on services section.
     */
    private void populateServicesList() {
        if (servicesListBox == null) return;

        servicesListBox.getChildren().clear();

        boolean hasServices = false;

        if (bookingSession.isWifiSelected()) {
            servicesListBox.getChildren().add(createServiceLabel("✓ Premium Wi-Fi"));
            hasServices = true;
        }
        if (bookingSession.isBreakfastSelected()) {
            servicesListBox.getChildren().add(createServiceLabel("✓ Breakfast Buffet"));
            hasServices = true;
        }
        if (bookingSession.isParkingSelected()) {
            servicesListBox.getChildren().add(createServiceLabel("✓ Valet Parking"));
            hasServices = true;
        }
        if (bookingSession.isSpaSelected()) {
            servicesListBox.getChildren().add(createServiceLabel("✓ Spa Access"));
            hasServices = true;
        }

        if (!hasServices) {
            servicesListBox.getChildren().add(createServiceLabel("No add-on services selected"));
        }
    }

    /**
     * Create a label for a service item.
     */
    private Label createServiceLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
        return label;
    }

    /**
     * Populate pricing section.
     */
    private void populatePricing() {
        long nights = bookingSession.getNights();
        BigDecimal roomSubtotal = bookingSession.getRoomSubtotal();
        BigDecimal addOnsSubtotal = bookingSession.getAddOnsSubtotal();
        BigDecimal tax = bookingSession.getTaxAmount();
        BigDecimal loyaltyDiscount = bookingSession.getLoyaltyDiscount();
        BigDecimal total = bookingSession.getTotalAmount();

        // Room charges
        if (roomChargesLabel != null) {
            roomChargesLabel.setText(String.format("$%.2f", roomSubtotal));
        }

        // Room breakdown
        if (roomBreakdownLabel != null) {
            StringBuilder breakdown = new StringBuilder();
            for (RoomSelection selection : bookingSession.getSelectedRooms()) {
                if (selection.getQuantity() > 0) {
                    BigDecimal lineTotal = selection.getPricePerNight()
                            .multiply(BigDecimal.valueOf(selection.getQuantity()))
                            .multiply(BigDecimal.valueOf(nights));
                    breakdown.append(String.format("  • %dx %s: $%.2f × %d nights = $%.2f\n",
                            selection.getQuantity(),
                            selection.getRoomType().getDisplayName(),
                            selection.getPricePerNight(),
                            nights,
                            lineTotal));
                }
            }
            roomBreakdownLabel.setText(breakdown.toString().trim());
        }

        // Services charges
        if (servicesChargesLabel != null) {
            servicesChargesLabel.setText(String.format("$%.2f", addOnsSubtotal));
        }

        // Services breakdown
        if (servicesBreakdownBox != null) {
            servicesBreakdownBox.getChildren().clear();

            if (bookingSession.isWifiSelected()) {
                addServiceBreakdown("Wi-Fi", new BigDecimal("15.00"), nights, false);
            }
            if (bookingSession.isBreakfastSelected()) {
                addServiceBreakdown("Breakfast", new BigDecimal("25.00"), nights, false);
            }
            if (bookingSession.isParkingSelected()) {
                addServiceBreakdown("Parking", new BigDecimal("20.00"), nights, false);
            }
            if (bookingSession.isSpaSelected()) {
                addServiceBreakdown("Spa", new BigDecimal("50.00"), nights, true);
            }
        }

        // Subtotal
        if (subtotalLabel != null) {
            BigDecimal subtotal = roomSubtotal.add(addOnsSubtotal);
            subtotalLabel.setText(String.format("$%.2f", subtotal));
        }

        // Tax
        if (taxLabel != null) {
            taxLabel.setText(String.format("$%.2f", tax));
        }

        // Loyalty discount (only show if applicable)
        if (loyaltyDiscountRow != null && loyaltyDiscountLabel != null) {
            if (loyaltyDiscount != null && loyaltyDiscount.compareTo(BigDecimal.ZERO) > 0) {
                loyaltyDiscountRow.setVisible(true);
                loyaltyDiscountRow.setManaged(true);
                loyaltyDiscountLabel.setText(String.format("-$%.2f", loyaltyDiscount));
            } else {
                loyaltyDiscountRow.setVisible(false);
                loyaltyDiscountRow.setManaged(false);
            }
        }

        // Total
        if (totalLabel != null) {
            totalLabel.setText(String.format("$%.2f", total));
        }
    }

    /**
     * Add a service breakdown line to the services breakdown box.
     */
    private void addServiceBreakdown(String name, BigDecimal price, long nights, boolean oneTime) {
        Label label = new Label();
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #888888;");

        if (oneTime) {
            label.setText(String.format("  • %s: $%.2f", name, price));
        } else {
            BigDecimal total = price.multiply(BigDecimal.valueOf(nights));
            label.setText(String.format("  • %s: $%.2f × %d nights = $%.2f", name, price, nights, total));
        }

        servicesBreakdownBox.getChildren().add(label);
    }

    // ==================== Edit Handlers ====================

    @FXML
    public void handleEditGuest(ActionEvent event) {
        LOGGER.info("User clicked Edit Guest - returning to guest details");
        navigationService.goToGuestDetails();
    }

    @FXML
    public void handleEditDates(ActionEvent event) {
        LOGGER.info("User clicked Edit Dates - returning to date selection");
        navigationService.goToDateSelection();
    }

    @FXML
    public void handleEditRooms(ActionEvent event) {
        LOGGER.info("User clicked Edit Rooms - returning to room selection");
        navigationService.goToRoomSelection();
    }

    @FXML
    public void handleEditServices(ActionEvent event) {
        LOGGER.info("User clicked Edit Services - returning to add-on services");
        navigationService.goToAddOnServices();
    }

    // ==================== Navigation Handlers ====================

    @FXML
    public void handleBack(ActionEvent event) {
        LOGGER.info("User clicked Back - returning to add-on services");
        navigationService.goToAddOnServices();
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        LOGGER.info("User clicked Cancel Booking");

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancel Booking");
        alert.setHeaderText("Are you sure you want to cancel?");
        alert.setContentText("All entered information will be lost.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            LOGGER.info("Booking cancelled by user");
            bookingSession.reset();
            navigationService.goToWelcome();
        }
    }

    @FXML
    public void handleConfirm(ActionEvent event) {
        LOGGER.info("User clicked Confirm Booking");

        // Disable button to prevent double-click
        if (confirmButton != null) {
            confirmButton.setDisable(true);
            confirmButton.setText("Processing...");
        }

        try {
            // Create the reservation
            String confirmationNumber = createReservation();

            if (confirmationNumber != null) {
                bookingSession.setConfirmationNumber(confirmationNumber);
                Optional<Reservation> res = reservationService.findByConfirmationNumber(confirmationNumber);
                if (res.isPresent()) {
                    bookingSession.setReservationId(res.get().getId());
                    LOGGER.info("Stored Reservation ID " + res.get().getId() + " in session for feedback.");
                } else {
                    LOGGER.warning("Could not retrieve Reservation ID for confirmation: " + confirmationNumber);
                }
                LOGGER.info("Reservation confirmed: " + confirmationNumber);
                // Navigate to confirmation screen
                navigationService.goToConfirmation();
            } else {
                throw new RuntimeException("Failed to generate confirmation number");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create reservation", e);

            // Re-enable button
            if (confirmButton != null) {
                confirmButton.setDisable(false);
                confirmButton.setText("Confirm Booking");
            }

            // Show error
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Booking Error");
            alert.setHeaderText("Unable to complete your booking");
            alert.setContentText("An error occurred while processing your reservation. Please try again or contact the front desk for assistance.");
            alert.showAndWait();
        }
    }

    /**
     * Create the reservation in the system.
     * Returns the confirmation number on success.
     */
    private String createReservation() {
        if (reservationService == null) {
            reservationService = ReservationService.getInstance();
        }

        // The service handles everything: guest creation, room assignment,
        // add-ons, pricing, and persistence
        return reservationService.createReservation(bookingSession);
    }


    /**
     * Generate a confirmation number.
     */
    private String generateConfirmationNumber() {
        // Format: HRS-YYYY-XXXXXX where X is a random 6-digit number
        int year = java.time.Year.now().getValue();
        int random = (int) (Math.random() * 900000) + 100000;
        return String.format("HRS-%d-%06d", year, random);
    }

    @FXML
    public void handleViewTerms(ActionEvent event) {
        LOGGER.info("User clicked View Terms");
        showTermsAndConditions();
    }

    @FXML
    public void handleViewPrivacy(ActionEvent event) {
        LOGGER.info("User clicked Privacy Policy");
        showPrivacyPolicy();
    }

    @FXML
    public void handleShowRules(ActionEvent event) {
        LOGGER.info("User clicked Rules and Regulations");
        navigationService.showRulesAndRegulations();
    }

    /**
     * Show Terms and Conditions dialog.
     */
    private void showTermsAndConditions() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Terms and Conditions");
        alert.setHeaderText("ARC Hotel - Terms and Conditions");
        alert.setContentText(
                "1. Check-in time is 4:00 PM and check-out time is 12:00 PM.\n" +
                        "2. A valid credit card is required to guarantee your reservation.\n" +
                        "3. Cancellation: Free cancellation up to 48 hours before check-in.\n" +
                        "4. No-show charges may apply.\n" +
                        "5. Damages to hotel property will be charged to your account.\n" +
                        "6. Smoking is prohibited in all rooms.\n" +
                        "7. The hotel reserves the right to refuse service."
        );
        alert.showAndWait();
    }

    /**
     * Show Privacy Policy dialog.
     */
    private void showPrivacyPolicy() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Privacy Policy");
        alert.setHeaderText("ARC Hotel - Privacy Policy");
        alert.setContentText(
                "Your privacy is important to us. We collect personal information solely to:\n" +
                        "• Complete your reservation\n" +
                        "• Provide guest services\n" +
                        "• Process payments\n" +
                        "• Communicate about your stay\n\n" +
                        "We protect your data with secure systems and never share it with " +
                        "third parties except when required for booking, payment processing, " +
                        "or legal obligations."
        );
        alert.showAndWait();
    }

    // Setters for dependency injection
    public void setNavigationService(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    public void setBookingSession(BookingSession bookingSession) {
        this.bookingSession = bookingSession;
    }

    public void setReservationService(ReservationService reservationService) {
        this.reservationService = reservationService;
    }
}