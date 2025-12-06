package com.hanson.hotelreservationsystem.controller;

import com.hanson.hotelreservationsystem.service.NavigationService;
import com.hanson.hotelreservationsystem.service.FeedbackService;
import com.hanson.hotelreservationsystem.session.BookingSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Guest Feedback Screen.
 *
 * Responsibilities:
 * - Collect star rating (1-5)
 * - Collect optional comments
 * - Validate feedback before submission
 * - Submit feedback to the system
 * - Link feedback to reservation if available
 *
 * Business Rules:
 * - Rating is required (1-5 stars)
 * - Comments are optional but encouraged
 * - Maximum comment length: 500 characters
 * - Feedback stored linked to reservation and guest
 */
public class GuestFeedbackController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(GuestFeedbackController.class.getName());

    // Constants
    private static final int MAX_COMMENT_LENGTH = 500;
    private static final String[] RATING_DESCRIPTIONS = {
            "Select a rating",
            "Poor - Did not meet expectations",
            "Fair - Below average experience",
            "Good - Met expectations",
            "Very Good - Above average experience",
            "Excellent - Exceeded all expectations!"
    };

    // Star rating buttons
    @FXML private HBox starsBox;
    @FXML private Button star1;
    @FXML private Button star2;
    @FXML private Button star3;
    @FXML private Button star4;
    @FXML private Button star5;

    // Rating display
    @FXML private Label ratingDescriptionLabel;

    // Comment section
    @FXML private TextArea commentArea;
    @FXML private Label charCountLabel;

    // Submit button
    @FXML private Button submitButton;

    // Current rating state
    private int currentRating = 0;
    private Button[] starButtons;

    // Services
    private NavigationService navigationService;
    private BookingSession bookingSession;
    private FeedbackService feedbackService;

    /**
     * Default constructor for FXML loader.
     */
    public GuestFeedbackController() {
        this.navigationService = NavigationService.getInstance();
        this.bookingSession = BookingSession.getInstance();
        this.feedbackService = FeedbackService.getInstance();
    }

    /**
     * Constructor with dependency injection.
     */
    public GuestFeedbackController(NavigationService navigationService,
                                   BookingSession bookingSession,
                                   FeedbackService feedbackService) {
        this.navigationService = navigationService;
        this.bookingSession = bookingSession;
        this.feedbackService = feedbackService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("Initializing Guest Feedback Screen");

        setupStarButtons();
        setupCommentArea();
        updateSubmitButtonState();
    }

    /**
     * Setup star rating buttons.
     */
    private void setupStarButtons() {
        starButtons = new Button[] { star1, star2, star3, star4, star5 };

        // Initially show empty stars
        updateStarDisplay(0);

        // Add hover effects
        for (int i = 0; i < starButtons.length; i++) {
            if (starButtons[i] != null) {
                final int rating = i + 1;

                // Hover effect - preview the rating
                starButtons[i].setOnMouseEntered(e -> updateStarDisplay(rating));
                starButtons[i].setOnMouseExited(e -> updateStarDisplay(currentRating));
            }
        }
    }

    /**
     * Update the visual display of stars.
     */
    private void updateStarDisplay(int highlightedRating) {
        for (int i = 0; i < starButtons.length; i++) {
            if (starButtons[i] != null) {
                if (i < highlightedRating) {
                    // Filled star
                    starButtons[i].setText("★");
                    starButtons[i].setStyle("-fx-font-size: 36px; -fx-text-fill: #FFD700; -fx-background-color: transparent; -fx-cursor: hand;");
                } else {
                    // Empty star
                    starButtons[i].setText("☆");
                    starButtons[i].setStyle("-fx-font-size: 36px; -fx-text-fill: #CCCCCC; -fx-background-color: transparent; -fx-cursor: hand;");
                }
            }
        }

        // Update description
        if (ratingDescriptionLabel != null) {
            int descIndex = Math.min(Math.max(highlightedRating, 0), RATING_DESCRIPTIONS.length - 1);
            ratingDescriptionLabel.setText(RATING_DESCRIPTIONS[descIndex]);
        }
    }

    /**
     * Setup comment text area with character limit.
     */
    private void setupCommentArea() {
        if (commentArea != null) {
            // Add listener for character count
            commentArea.textProperty().addListener((obs, oldText, newText) -> {
                updateCharacterCount(newText);

                // Enforce max length
                if (newText != null && newText.length() > MAX_COMMENT_LENGTH) {
                    commentArea.setText(newText.substring(0, MAX_COMMENT_LENGTH));
                }
            });

            // Set initial prompt
            commentArea.setPromptText("Share your experience with us (optional)...");
        }

        updateCharacterCount("");
    }

    /**
     * Update the character count display.
     */
    private void updateCharacterCount(String text) {
        if (charCountLabel != null) {
            int count = text != null ? text.length() : 0;
            charCountLabel.setText(count + " / " + MAX_COMMENT_LENGTH + " characters");

            // Change color when approaching limit
            if (count > MAX_COMMENT_LENGTH * 0.9) {
                charCountLabel.setStyle("-fx-text-fill: #ff6b6b;");
            } else if (count > MAX_COMMENT_LENGTH * 0.75) {
                charCountLabel.setStyle("-fx-text-fill: #f0ad4e;");
            } else {
                charCountLabel.setStyle("-fx-text-fill: #666666;");
            }
        }
    }

    /**
     * Update the submit button state based on form validity.
     */
    private void updateSubmitButtonState() {
        if (submitButton != null) {
            submitButton.setDisable(currentRating == 0);
        }
    }

    // ==================== Star Click Handlers ====================

    @FXML
    public void handleStar1(ActionEvent event) {
        setRating(1);
    }

    @FXML
    public void handleStar2(ActionEvent event) {
        setRating(2);
    }

    @FXML
    public void handleStar3(ActionEvent event) {
        setRating(3);
    }

    @FXML
    public void handleStar4(ActionEvent event) {
        setRating(4);
    }

    @FXML
    public void handleStar5(ActionEvent event) {
        setRating(5);
    }

    /**
     * Set the current rating.
     */
    private void setRating(int rating) {
        currentRating = rating;
        updateStarDisplay(rating);
        updateSubmitButtonState();

        LOGGER.fine("Rating selected: " + rating + " stars");
    }

    // ==================== Action Handlers ====================

    /**
     * Handle the "Skip" button click.
     */
    @FXML
    public void handleSkip(ActionEvent event) {
        LOGGER.info("User skipped feedback");

        // Return to welcome screen
        navigationService.goToWelcome();
    }

    /**
     * Handle the "Submit Feedback" button click.
     */
    @FXML
    public void handleSubmit(ActionEvent event) {
        LOGGER.info("User submitting feedback");

        if (!validateFeedback()) {
            return;
        }

        // Disable button to prevent double submission
        if (submitButton != null) {
            submitButton.setDisable(true);
            submitButton.setText("Submitting...");
        }

        try {
            submitFeedback();

            // Show success message
            showSuccessAlert();

            // Return to welcome screen
            navigationService.goToWelcome();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to submit feedback", e);

            // Re-enable button
            if (submitButton != null) {
                submitButton.setDisable(false);
                submitButton.setText("✓ Submit Feedback");
            }

            // Show error
            showErrorAlert();
        }
    }

    /**
     * Validate the feedback before submission.
     */
    private boolean validateFeedback() {
        if (currentRating == 0) {
            showAlert(Alert.AlertType.WARNING, "Rating Required",
                    "Please select a star rating before submitting your feedback.");
            return false;
        }

        // Comment validation (optional but check length)
        String comment = getComment();
        if (comment.length() > MAX_COMMENT_LENGTH) {
            showAlert(Alert.AlertType.WARNING, "Comment Too Long",
                    "Your comment exceeds the maximum length of " + MAX_COMMENT_LENGTH + " characters.");
            return false;
        }

        return true;
    }

    /**
     * Get the comment text.
     */
    private String getComment() {
        return commentArea != null && commentArea.getText() != null
                ? commentArea.getText().trim()
                : "";
    }

    /**
     * Submit the feedback to the system.
     */
    private void submitFeedback() {
        String comment = getComment();

        // Retrieve data from the singleton session
        Long reservationId = bookingSession.getReservationId();
        String guestEmail = bookingSession.getEmail();

        if (reservationId == null) {
            LOGGER.warning("No active reservation ID in session. Cannot submit feedback.");
            return;
        }

        if (feedbackService != null) {
            feedbackService.submitFeedback(
                    reservationId,
                    guestEmail,
                    currentRating,
                    comment
            );
        }

        LOGGER.info("Feedback submitted successfully.");
    }

    /**
     * Determine sentiment tag based on rating and comment.
     * In production, this might use NLP analysis.
     */
    private String determineSentimentTag(int rating, String comment) {
        if (rating <= 2) {
            return "NEGATIVE";
        } else if (rating == 3) {
            return "NEUTRAL";
        } else {
            return "POSITIVE";
        }
    }

    /**
     * Show success alert after submission.
     */
    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thank You!");
        alert.setHeaderText("Feedback Submitted Successfully");
        alert.setContentText(
                "Thank you for taking the time to share your feedback!\n\n" +
                        "Your input helps us improve our services and provide " +
                        "a better experience for all our guests.\n\n" +
                        "We hope to see you again soon at ARC Hotel!"
        );
        alert.showAndWait();
    }

    /**
     * Show error alert if submission fails.
     */
    private void showErrorAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Submission Error");
        alert.setHeaderText("Unable to Submit Feedback");
        alert.setContentText(
                "We apologize, but we were unable to submit your feedback at this time.\n\n" +
                        "Please try again or contact the front desk to share your experience."
        );
        alert.showAndWait();
    }

    /**
     * Show a generic alert.
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ==================== Getters for Testing ====================

    public int getCurrentRating() {
        return currentRating;
    }

    // Setters for dependency injection
    public void setNavigationService(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    public void setBookingSession(BookingSession bookingSession) {
        this.bookingSession = bookingSession;
    }

    public void setFeedbackService(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }
}