package com.hanson.hotelreservationsystem.controller.Kiosk;

import com.hanson.hotelreservationsystem.model.Guest;
import com.hanson.hotelreservationsystem.model.Reservation;
import com.hanson.hotelreservationsystem.model.enums.ReservationStatus;
import com.hanson.hotelreservationsystem.repository.GuestRepository;
import com.hanson.hotelreservationsystem.repository.ReservationRepository;
import com.hanson.hotelreservationsystem.service.NavigationService;
import com.hanson.hotelreservationsystem.session.BookingSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;

import java.net.URL;
import java.util.List;
import java.util.Optional;
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
        if (bookingSession != null) {
            bookingSession.reset();
        }

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
     * Now requires verification of email/phone and check-out status.
     */
    @FXML
    public void handleFeedback(ActionEvent event) {
        LOGGER.info("User clicked Leave Feedback");

        // 1. Prompt for Email or Phone
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Feedback Verification");
        dialog.setHeaderText("Find Your Booking");
        dialog.setContentText("Please enter your Email or Phone Number:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String input = result.get().trim();
            if (input.isEmpty()) return;

            // 2. Lookup Guest
            GuestRepository guestRepo = GuestRepository.getInstance();
            // findByEmailOrPhone looks up by email first, then phone
            Optional<Guest> guestOpt = guestRepo.findByEmailOrPhone(input, input);

            if (guestOpt.isPresent()) {
                Guest guest = guestOpt.get();

                // 3. Find Eligible Reservation (Checked Out)
                // We use the Repository directly here as a simple lookup
                ReservationRepository resRepo = ReservationRepository.getInstance();
                List<Reservation> reservations = resRepo.findByGuestId(guest.getId());

                // Find the most recent checked-out reservation
                // List is ordered by date DESC, so findFirst() gives the latest
                Optional<Reservation> validRes = reservations.stream()
                        .filter(r -> r.getStatus() == ReservationStatus.CHECKED_OUT)
                        .findFirst();

                if (validRes.isPresent()) {
                    Reservation res = validRes.get();

                    // Optional: Check if feedback was already left?
                    // if (res.getFeedback() != null) { ... show message "Feedback already submitted" ... }

                    // 4. Setup Session for the Feedback Controller
                    bookingSession.reset();
                    bookingSession.setReservationId(res.getId());
                    bookingSession.setEmail(guest.getEmail());
                    bookingSession.setFirstName(guest.getFirstName());
                    bookingSession.setLastName(guest.getLastName());

                    LOGGER.info("Eligible reservation found for feedback: " + res.getConfirmationNumber());
                    navigationService.goToFeedback();
                } else {
                    showAlert(Alert.AlertType.WARNING, "No Eligible Booking",
                            "We could not find a completed stay (Checked Out) for this guest.\n" +
                                    "Feedback can only be provided after checking out.");
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Guest Not Found",
                        "We could not find a guest with that email or phone number.");
            }
        }
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

    /**
     * Helper to show alerts.
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Setters for dependency injection
    public void setNavigationService(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    public void setBookingSession(BookingSession bookingSession) {
        this.bookingSession = bookingSession;
    }
}