package com.hanson.hotelreservationsystem.controller.Kiosk;

import com.hanson.hotelreservationsystem.service.NavigationService;
import com.hanson.hotelreservationsystem.session.BookingSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Controller for the Kiosk Welcome Screen.
 * This is the entry point for guests using the self-service kiosk.
 *
 * Responsibilities:
 * - Display welcome message and instructional content
 * - Initiate new booking flow
 * - Provide access to rules/regulations and feedback
 * - Navigate to admin login
 */
public class KioskWelcomeController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(KioskWelcomeController.class.getName());

    @FXML
    private Button startBookingButton;

    @FXML
    private Button feedbackButton;

    // Services (injected via setter or constructor in full DI setup)
    private NavigationService navigationService;
    private BookingSession bookingSession;

    /**
     * Default constructor for FXML loader.
     * Services are obtained from singletons for simplicity.
     * In a full DI setup, these would be injected.
     */
    public KioskWelcomeController() {
        this.navigationService = NavigationService.getInstance();
        this.bookingSession = BookingSession.getInstance();
    }

    /**
     * Constructor with dependency injection.
     */
    public KioskWelcomeController(NavigationService navigationService, BookingSession bookingSession) {
        this.navigationService = navigationService;
        this.bookingSession = bookingSession;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("Initializing Kiosk Welcome Screen");

        // Reset the booking session for a fresh start
        bookingSession.reset();

        // Configure button styling if needed
        if (startBookingButton != null) {
            startBookingButton.setDefaultButton(true);
        }
    }

    /**
     * Handle the "Start Booking" button click.
     * Navigates to the guest count screen (Step 1).
     */
    @FXML
    public void handleStartBooking(ActionEvent event) {
        LOGGER.info("User clicked Start Booking - beginning new reservation flow");

        // Reset session to ensure clean state
        bookingSession.reset();

        // Navigate to first step: Guest Count
        navigationService.goToGuestCount();
    }

    /**
     * Handle the "Leave Feedback" button click.
     * Navigates to the guest feedback screen.
     */
    @FXML
    public void handleFeedback(ActionEvent event) {
        LOGGER.info("User clicked Leave Feedback");
        navigationService.goToFeedback();
    }

    /**
     * Handle the "Admin Login" button click.
     * Navigates to the admin login screen.
     */
    @FXML
    public void handleAdminLogin(ActionEvent event) {
        LOGGER.info("User clicked Admin Login");
        navigationService.goToAdminLogin();
    }

    /**
     * Handle the "Rules and Regulations" button click.
     * Shows the rules and regulations dialog.
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