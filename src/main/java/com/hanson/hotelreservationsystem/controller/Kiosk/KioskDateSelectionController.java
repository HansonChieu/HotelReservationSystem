package com.hanson.hotelreservationsystem.controller.Kiosk;

import com.hanson.hotelreservationsystem.service.NavigationService;
import com.hanson.hotelreservationsystem.service.RoomService;
import com.hanson.hotelreservationsystem.session.BookingSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.Callback;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Controller for the Kiosk Date Selection Screen (Step 2 of 5).
 *
 * Responsibilities:
 * - Collect check-in and check-out dates
 * - Validate date selections (check-in >= today, check-out > check-in)
 * - Check room availability for selected dates
 * - Calculate and display number of nights
 *
 * Business Rules:
 * - Check-in time: 4:00 PM
 * - Check-out time: 12:00 PM
 * - Minimum stay: 1 night
 * - Weekend rates may apply (Friday-Sunday)
 */
public class KioskDateSelectionController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(KioskDateSelectionController.class.getName());

    // Date format for display
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    // Minimum stay in nights
    private static final int MIN_NIGHTS = 1;

    // Maximum advance booking in days
    private static final int MAX_ADVANCE_BOOKING_DAYS = 365;

    @FXML
    private DatePicker checkInDatePicker;

    @FXML
    private DatePicker checkOutDatePicker;

    @FXML
    private Label nightsLabel;

    @FXML
    private Label errorLabel;

    @FXML
    private Label checkInErrorLabel;

    @FXML
    private Label checkOutErrorLabel;

    @FXML
    private Button backButton;

    @FXML
    private Button checkAvailabilityButton;

    // Services
    private NavigationService navigationService;
    private BookingSession bookingSession;
    private RoomService roomService; // Optional - for availability checking

    /**
     * Default constructor for FXML loader.
     */
    public KioskDateSelectionController() {
        this.navigationService = NavigationService.getInstance();
        this.bookingSession = BookingSession.getInstance();
    }

    /**
     * Constructor with dependency injection.
     */
    public KioskDateSelectionController(NavigationService navigationService,
                                        BookingSession bookingSession,
                                        RoomService roomService) {
        this.navigationService = navigationService;
        this.bookingSession = bookingSession;
        this.roomService = roomService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("Initializing Kiosk Date Selection Screen (Step 2)");

        setupDatePickers();
        loadExistingValues();
        updateNightsDisplay();
    }

    /**
     * Configure the date pickers with validation and restrictions.
     */
    private void setupDatePickers() {
        LocalDate today = LocalDate.now();

        // Configure check-in date picker
        if (checkInDatePicker != null) {
            checkInDatePicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    // Disable past dates and dates too far in future
                    setDisable(empty || date.isBefore(today) || date.isAfter(today.plusDays(MAX_ADVANCE_BOOKING_DAYS)));

                    // Highlight weekends
                    if (!isEmpty() && !isDisabled() && date.getDayOfWeek().getValue() >= 5) {
                        setStyle("-fx-background-color: #fff3e0;");
                    }
                }
            });
            checkInDatePicker.setValue(today);

            checkInDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                onCheckInDateChanged(newVal);
            });
        }

        // Configure check-out date picker
        if (checkOutDatePicker != null) {
            updateCheckOutDatePicker();
            checkOutDatePicker.setValue(today.plusDays(1));

            checkOutDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                onCheckOutDateChanged(newVal);
            });
        }
    }

    /**
     * Update check-out date picker based on check-in selection.
     */
    private void updateCheckOutDatePicker() {
        checkOutDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate checkIn = checkInDatePicker.getValue();
                LocalDate minCheckOut = (checkIn != null) ? checkIn.plusDays(1) : LocalDate.now().plusDays(1);
                LocalDate maxCheckOut = (checkIn != null) ? checkIn.plusDays(MAX_ADVANCE_BOOKING_DAYS + 30) : LocalDate.now().plusDays(MAX_ADVANCE_BOOKING_DAYS + 30);

                setDisable(empty || date.isBefore(minCheckOut) || date.isAfter(maxCheckOut));

                // Highlight weekends
                if (!isEmpty() && !isDisabled() && date.getDayOfWeek().getValue() >= 5) {
                    setStyle("-fx-background-color: #fff3e0;");
                }
            }
        });
    }

    /**
     * Handle check-in date changes.
     */
    private void onCheckInDateChanged(LocalDate newCheckIn) {
        if (newCheckIn == null) return;

        clearErrors();

        // Update check-out minimum date
        if (checkOutDatePicker != null) {
            LocalDate minCheckOut = newCheckIn.plusDays(MIN_NIGHTS);

            // If current check-out is before new minimum, update it
            if (checkOutDatePicker.getValue() != null &&
                    checkOutDatePicker.getValue().isBefore(minCheckOut)) {
                checkOutDatePicker.setValue(minCheckOut);
            }

            updateCheckOutDatePicker();
        }

        updateNightsDisplay();
    }

    /**
     * Handle check-out date changes.
     */
    private void onCheckOutDateChanged(LocalDate newCheckOut) {
        clearErrors();
        updateNightsDisplay();
    }

    /**
     * Load any existing values from the booking session.
     */
    private void loadExistingValues() {
        if (bookingSession.getCheckInDate() != null && checkInDatePicker != null) {
            checkInDatePicker.setValue(bookingSession.getCheckInDate());
        }
        if (bookingSession.getCheckOutDate() != null && checkOutDatePicker != null) {
            checkOutDatePicker.setValue(bookingSession.getCheckOutDate());
        }
    }

    /**
     * Update the nights display label.
     */
    private void updateNightsDisplay() {
        if (nightsLabel == null) return;

        LocalDate checkIn = getCheckInDate();
        LocalDate checkOut = getCheckOutDate();

        if (checkIn != null && checkOut != null && checkOut.isAfter(checkIn)) {
            long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
            nightsLabel.setText(nights + (nights == 1 ? " night" : " nights"));
        } else {
            nightsLabel.setText("-- nights");
        }
    }

    /**
     * Validate the selected dates.
     */
    private boolean validateDates() {
        LocalDate checkIn = getCheckInDate();
        LocalDate checkOut = getCheckOutDate();
        LocalDate today = LocalDate.now();

        clearErrors();

        // Validate check-in date
        if (checkIn == null) {
            showError("Please select a check-in date.", checkInErrorLabel);
            return false;
        }

        if (checkIn.isBefore(today)) {
            showError("Check-in date cannot be in the past.", checkInErrorLabel);
            return false;
        }

        // Validate check-out date
        if (checkOut == null) {
            showError("Please select a check-out date.", checkOutErrorLabel);
            return false;
        }

        // Validate date range
        if (!checkOut.isAfter(checkIn)) {
            showError("Check-out date must be after check-in date.", checkOutErrorLabel);
            return false;
        }

        // Validate minimum stay
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights < MIN_NIGHTS) {
            showError("Minimum stay is " + MIN_NIGHTS + " night(s).", errorLabel);
            return false;
        }

        return true;
    }

    /**
     * Get the selected check-in date.
     */
    private LocalDate getCheckInDate() {
        return checkInDatePicker != null ? checkInDatePicker.getValue() : null;
    }

    /**
     * Get the selected check-out date.
     */
    private LocalDate getCheckOutDate() {
        return checkOutDatePicker != null ? checkOutDatePicker.getValue() : null;
    }

    /**
     * Clear all error messages.
     */
    private void clearErrors() {
        if (errorLabel != null) errorLabel.setVisible(false);
        if (checkInErrorLabel != null) checkInErrorLabel.setVisible(false);
        if (checkOutErrorLabel != null) checkOutErrorLabel.setVisible(false);
    }

    /**
     * Display an error message.
     */
    private void showError(String message, Label targetLabel) {
        if (targetLabel != null) {
            targetLabel.setText(message);
            targetLabel.setVisible(true);
        } else if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        } else {
            // Fallback to alert
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText("Invalid Date Selection");
            alert.setContentText(message);
            alert.showAndWait();
        }
    }

    /**
     * Check if the selected dates include weekends (for pricing info).
     */
    public boolean includesWeekend() {
        LocalDate checkIn = getCheckInDate();
        LocalDate checkOut = getCheckOutDate();

        if (checkIn == null || checkOut == null) return false;

        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            int dayOfWeek = current.getDayOfWeek().getValue();
            if (dayOfWeek >= 5) { // Friday, Saturday, Sunday
                return true;
            }
            current = current.plusDays(1);
        }
        return false;
    }

    /**
     * Handle the "Back" button click.
     */
    @FXML
    public void handleBack(ActionEvent event) {
        LOGGER.info("User clicked Back - returning to guest count screen");
        navigationService.goToGuestCount();
    }

    /**
     * Handle the "Check Availability" / "Next" button click.
     */
    @FXML
    public void handleCheckAvailability(ActionEvent event) {
        LOGGER.info("User clicked Check Availability");

        if (!validateDates()) {
            LOGGER.warning("Date validation failed");
            return;
        }

        LocalDate checkIn = getCheckInDate();
        LocalDate checkOut = getCheckOutDate();
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

        // Store values in session
        bookingSession.setCheckInDate(checkIn);
        bookingSession.setCheckOutDate(checkOut);

        LOGGER.info(String.format("Dates saved: Check-in %s, Check-out %s (%d nights)",
                checkIn.format(DATE_FORMATTER),
                checkOut.format(DATE_FORMATTER),
                nights));

        if (includesWeekend()) {
            LOGGER.info("Selected dates include weekend - weekend rates may apply");
        }

        // Navigate to Step 3: Room Selection
        navigationService.goToRoomSelection();
    }

    /**
     * Handle the "Rules and Regulations" button click.
     */
    @FXML
    public void handleRulesAndRegulations(ActionEvent event) {
        LOGGER.info("User clicked Rules and Regulations");
        navigationService.showRulesAndRegulations();
    }

    // Setters for dependency injection
    public void setNavigationService(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    public void setBookingSession(BookingSession bookingSession) {
        this.bookingSession = bookingSession;
    }

    public void setRoomService(RoomService roomService) {
        this.roomService = roomService;
    }
}