package com.hanson.hotelreservationsystem.controller.Kiosk;

import com.hanson.hotelreservationsystem.service.NavigationService;
import com.hanson.hotelreservationsystem.service.PrintService;
import com.hanson.hotelreservationsystem.session.BookingSession;
import com.hanson.hotelreservationsystem.session.BookingSession.RoomSelection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Kiosk Confirmation Screen.
 *
 * Responsibilities:
 * - Display booking confirmation details
 * - Show confirmation number prominently
 * - Provide print receipt option
 * - Guide guest to next steps
 * - Reset session for next guest
 *
 * This is the final screen in the booking flow.
 * Payment will be handled at the front desk.
 */
public class KioskConfirmationController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(KioskConfirmationController.class.getName());

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    // Confirmation display
    @FXML private Label confirmationNumberLabel1;

    // Summary labels
    @FXML private Label checkInSummary1;
    @FXML private Label checkOutSummary1;
    @FXML private Label nightsSummary1;
    @FXML private Label guestsSummary1;
    @FXML private Label roomsSummary1;
    @FXML private Label totalSummary1;

    // Services
    private NavigationService navigationService;
    private BookingSession bookingSession;
    private PrintService printService;

    /**
     * Default constructor for FXML loader.
     */
    public KioskConfirmationController() {
        this.navigationService = NavigationService.getInstance();
        this.bookingSession = BookingSession.getInstance();
    }

    /**
     * Constructor with dependency injection.
     */
    public KioskConfirmationController(NavigationService navigationService,
                                       BookingSession bookingSession,
                                       PrintService printService) {
        this.navigationService = navigationService;
        this.bookingSession = bookingSession;
        this.printService = printService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("Initializing Kiosk Confirmation Screen");

        populateConfirmationDetails();
        populateSummary();

        LOGGER.info("Booking confirmed - Confirmation #: " + bookingSession.getConfirmationNumber());
    }

    /**
     * Populate the confirmation number display.
     */
    private void populateConfirmationDetails() {
        String confirmationNumber = bookingSession.getConfirmationNumber();

        if (confirmationNumberLabel1 != null) {
            confirmationNumberLabel1.setText(confirmationNumber != null ? confirmationNumber : "HRS-PENDING");
        }
    }

    /**
     * Populate the reservation summary.
     */
    private void populateSummary() {
        // Check-in date
        if (checkInSummary1 != null && bookingSession.getCheckInDate() != null) {
            checkInSummary1.setText(bookingSession.getCheckInDate().format(DATE_FORMATTER));
        }

        // Check-out date
        if (checkOutSummary1 != null && bookingSession.getCheckOutDate() != null) {
            checkOutSummary1.setText(bookingSession.getCheckOutDate().format(DATE_FORMATTER));
        }

        // Number of nights
        if (nightsSummary1 != null) {
            long nights = bookingSession.getNights();
            nightsSummary1.setText(nights + (nights == 1 ? " Night" : " Nights"));
        }

        // Guests
        if (guestsSummary1 != null) {
            int adults = bookingSession.getAdultCount();
            int children = bookingSession.getChildCount();
            StringBuilder guestText = new StringBuilder();
            guestText.append(adults).append(adults == 1 ? " Adult" : " Adults");
            if (children > 0) {
                guestText.append(", ").append(children).append(children == 1 ? " Child" : " Children");
            }
            guestsSummary1.setText(guestText.toString());
        }

        // Rooms
        if (roomsSummary1 != null) {
            StringBuilder roomsText = new StringBuilder();
            for (RoomSelection selection : bookingSession.getSelectedRooms()) {
                if (selection.getQuantity() > 0) {
                    if (roomsText.length() > 0) roomsText.append(", ");
                    roomsText.append(selection.getQuantity())
                            .append(" ")
                            .append(selection.getRoomType().getDisplayName());
                }
            }
            roomsSummary1.setText(roomsText.toString());
        }

        // Total
        if (totalSummary1 != null) {
            totalSummary1.setText(String.format("$%.2f", bookingSession.getTotalAmount()));
        }
    }

    /**
     * Handle the "Print Receipt" button click.
     */
    @FXML
    public void handlePrintReceipt(ActionEvent event) {
        LOGGER.info("User requested print receipt");

        try {
            printReceipt();

            showAlert(Alert.AlertType.INFORMATION, "Printing",
                    "Your confirmation receipt is being printed.\n" +
                            "Please collect it from the printer near the kiosk.");

            LOGGER.info("Receipt printed for confirmation #: " + bookingSession.getConfirmationNumber());

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to print receipt", e);

            showAlert(Alert.AlertType.WARNING, "Print Failed",
                    "We were unable to print your receipt. " +
                            "Please contact the front desk for a printed copy.");
        }
    }

    /**
     * Print the confirmation receipt.
     */
    private void printReceipt() {
        // In production, this would call printService.printConfirmation(...)
        if (printService != null) {
            // printService.printBookingConfirmation(bookingSession);
        }

        // Simulate printing delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        LOGGER.info("Receipt printed");
    }

    /**
     * Handle the "Done" button click.
     * Resets the session and returns to the welcome screen.
     */
    @FXML
    public void handleDone(ActionEvent event) {
        LOGGER.info("User clicked Done - completing booking flow");

        // Log completion for activity tracking
        LOGGER.info(String.format(
                "Booking completed - Confirmation: %s, Guest: %s, Total: $%.2f",
                bookingSession.getConfirmationNumber(),
                bookingSession.getFullName(),
                bookingSession.getTotalAmount()
        ));

        // Ask if they want to leave feedback
        Alert feedbackAlert = new Alert(Alert.AlertType.CONFIRMATION);
        feedbackAlert.setTitle("Feedback");
        feedbackAlert.setHeaderText("Would you like to leave feedback about our service?");
        feedbackAlert.setContentText("Your feedback helps us improve our guest experience.");

        feedbackAlert.showAndWait().ifPresent(response -> {
            // Reset session for next guest
            bookingSession.reset();

            if (response == javafx.scene.control.ButtonType.OK) {
                LOGGER.info("Guest opted to leave feedback");
                navigationService.goToFeedback();
            } else {
                LOGGER.info("Guest declined feedback - returning to welcome");
                navigationService.goToWelcome();
            }
        });
    }

    /**
     * Show an alert dialog.
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

    public void setPrintService(PrintService printService) {
        this.printService = printService;
    }
}