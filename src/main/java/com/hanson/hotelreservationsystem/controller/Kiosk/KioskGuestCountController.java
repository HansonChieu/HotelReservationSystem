package com.hanson.hotelreservationsystem.controller.Kiosk;

import com.hanson.hotelreservationsystem.model.enums.RoomType;
import com.hanson.hotelreservationsystem.service.NavigationService;
import com.hanson.hotelreservationsystem.session.BookingSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Controller for the Kiosk Guest Count Screen (Step 1 of 5).
 *
 * Responsibilities:
 * - Collect number of adults and children
 * - Validate occupancy against room type limits
 * - Provide helpful information about room capacities
 * - Navigate to date selection on successful validation
 *
 * Business Rules:
 * - Single/Deluxe/Penthouse rooms: max 2 guests
 * - Double rooms: max 4 guests
 * - At least 1 adult required
 */
public class KioskGuestCountController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(KioskGuestCountController.class.getName());

    // Occupancy constants
    private static final int MIN_ADULTS = 1;
    private static final int MAX_ADULTS = 20; // Reasonable max for group bookings
    private static final int MIN_CHILDREN = 0;
    private static final int MAX_CHILDREN = 20;

    @FXML
    private Spinner<Integer> adultsSpinner;

    @FXML
    private Spinner<Integer> childrenSpinner;

    @FXML
    private Button backButton;

    @FXML
    private Button nextButton;

    @FXML
    private Label errorLabel;

    // Services
    private NavigationService navigationService;
    private BookingSession bookingSession;

    /**
     * Default constructor for FXML loader.
     */
    public KioskGuestCountController() {
        this.navigationService = NavigationService.getInstance();
        this.bookingSession = BookingSession.getInstance();
    }

    /**
     * Constructor with dependency injection.
     */
    public KioskGuestCountController(NavigationService navigationService, BookingSession bookingSession) {
        this.navigationService = navigationService;
        this.bookingSession = bookingSession;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("Initializing Kiosk Guest Count Screen (Step 1)");

        setupSpinners();
        loadExistingValues();
    }

    /**
     * Configure the spinner controls with appropriate value factories and listeners.
     */
    private void setupSpinners() {
        // Adults spinner: 1 to MAX_ADULTS, default 1
        if (adultsSpinner != null) {
            SpinnerValueFactory<Integer> adultFactory =
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(MIN_ADULTS, MAX_ADULTS, MIN_ADULTS);
            adultsSpinner.setValueFactory(adultFactory);
            adultsSpinner.setEditable(true);

            // Add listener for validation feedback
            adultsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
                validateGuestCount();
            });
        }

        // Children spinner: 0 to MAX_CHILDREN, default 0
        if (childrenSpinner != null) {
            SpinnerValueFactory<Integer> childFactory =
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(MIN_CHILDREN, MAX_CHILDREN, MIN_CHILDREN);
            childrenSpinner.setValueFactory(childFactory);
            childrenSpinner.setEditable(true);

            // Add listener for validation feedback
            childrenSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
                validateGuestCount();
            });
        }
    }

    /**
     * Load any existing values from the booking session (for back navigation).
     */
    private void loadExistingValues() {
        if (bookingSession.getAdultCount() > 0 && adultsSpinner != null) {
            adultsSpinner.getValueFactory().setValue(bookingSession.getAdultCount());
        }
        if (bookingSession.getChildCount() >= 0 && childrenSpinner != null) {
            childrenSpinner.getValueFactory().setValue(bookingSession.getChildCount());
        }
    }

    /**
     * Validate the current guest count and provide feedback.
     */
    private boolean validateGuestCount() {
        int adults = getAdultCount();
        int children = getChildCount();
        int total = adults + children;

        // Clear any previous error
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }

        // Validate minimum adults
        if (adults < MIN_ADULTS) {
            showError("At least " + MIN_ADULTS + " adult is required.");
            return false;
        }

        // Validate total guests (basic sanity check)
        if (total > MAX_ADULTS + MAX_CHILDREN) {
            showError("Please contact the front desk for groups larger than " + (MAX_ADULTS + MAX_CHILDREN) + " guests.");
            return false;
        }

        return true;
    }

    /**
     * Get the current adult count from the spinner.
     */
    private int getAdultCount() {
        if (adultsSpinner != null && adultsSpinner.getValue() != null) {
            return adultsSpinner.getValue();
        }
        return MIN_ADULTS;
    }

    /**
     * Get the current children count from the spinner.
     */
    private int getChildCount() {
        if (childrenSpinner != null && childrenSpinner.getValue() != null) {
            return childrenSpinner.getValue();
        }
        return MIN_CHILDREN;
    }

    /**
     * Display an error message to the user.
     */
    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        } else {
            // Fallback to alert if no error label is defined
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText("Invalid Guest Count");
            alert.setContentText(message);
            alert.showAndWait();
        }
    }

    /**
     * Check if a single-person booking is being made (allowed per requirements).
     */
    private boolean isSinglePersonBooking() {
        return getAdultCount() == 1 && getChildCount() == 0;
    }

    /**
     * Get a suggestion for room types based on guest count.
     */
    public String getRoomSuggestion() {
        int total = getAdultCount() + getChildCount();

        if (total <= 2) {
            return "Recommended: Single Room, Deluxe Room, or Penthouse Room";
        } else if (total <= 4) {
            return "Recommended: 1 Double Room or 2 Single Rooms";
        } else {
            int doubleRoomsNeeded = (int) Math.ceil(total / 4.0);
            return "Recommended: " + doubleRoomsNeeded + " Double Room(s) for your group";
        }
    }

    /**
     * Handle the "Back" button click.
     * Returns to the welcome screen.
     */
    @FXML
    public void handleBack(ActionEvent event) {
        LOGGER.info("User clicked Back - returning to welcome screen");
        navigationService.goToWelcome();
    }

    /**
     * Handle the "Next" button click.
     * Validates input and navigates to date selection.
     */
    @FXML
    public void handleNext(ActionEvent event) {
        LOGGER.info("User clicked Next - validating guest count");

        if (!validateGuestCount()) {
            LOGGER.warning("Guest count validation failed");
            return;
        }

        // Store values in session
        int adults = getAdultCount();
        int children = getChildCount();

        bookingSession.setAdultCount(adults);
        bookingSession.setChildCount(children);

        LOGGER.info(String.format("Guest count saved: %d adults, %d children", adults, children));

        // Log suggestion for reference
        LOGGER.info("Room suggestion: " + getRoomSuggestion());

        // Navigate to Step 2: Date Selection
        navigationService.goToDateSelection();
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
}