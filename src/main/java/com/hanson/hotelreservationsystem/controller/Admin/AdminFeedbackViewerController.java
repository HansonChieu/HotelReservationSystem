package com.hanson.hotelreservationsystem.controller.Admin;

import javafx.scene.input.KeyEvent;
import com.hanson.hotelreservationsystem.model.*;
import com.hanson.hotelreservationsystem.model.enums.*;
import com.hanson.hotelreservationsystem.service.*;
import com.hanson.hotelreservationsystem.session.AdminSession;
import com.hanson.hotelreservationsystem.util.ActivityLogger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Controller for the Admin Feedback Viewer Screen.
 *
 * Responsibilities:
 * - Display all guest feedback in a sortable table
 * - Filter by sentiment (Positive/Neutral/Negative)
 * - Filter by rating and date range
 * - View detailed feedback comments
 * - Link feedback to reservations
 * - Export feedback reports
 *
 * Sentiment Analysis:
 * - 4-5 stars = POSITIVE
 * - 3 stars = NEUTRAL
 * - 1-2 stars = NEGATIVE
 */
public class AdminFeedbackViewerController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(AdminFeedbackViewerController.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    // ==================== Statistics Cards ====================
    @FXML private Label totalFeedbackLabel;
    @FXML private Label averageRatingLabel;
    @FXML private Label positiveFeedbackLabel;
    @FXML private Label negativeFeedbackLabel;

    // ==================== Filter Section ====================
    @FXML private ComboBox<String> sentimentFilter;
    @FXML private ComboBox<String> ratingFilter;
    @FXML private DatePicker fromDateFilter;
    @FXML private DatePicker toDateFilter;
    @FXML private TextField searchField;
    @FXML private Button clearFiltersButton;
    @FXML private Button refreshButton;

    // ==================== Feedback Table ====================
    @FXML private TableView<Feedback> feedbackTable;
    @FXML private TableColumn<Feedback, String> dateColumn;
    @FXML private TableColumn<Feedback, String> guestColumn;
    @FXML private TableColumn<Feedback, String> reservationColumn;
    @FXML private TableColumn<Feedback, String> ratingColumn;
    @FXML private TableColumn<Feedback, String> sentimentColumn;
    @FXML private TableColumn<Feedback, String> commentPreviewColumn;

    // ==================== Feedback Details Panel ====================
    @FXML private Label detailGuestLabel;
    @FXML private Label detailReservationLabel;
    @FXML private Label detailDateLabel;
    @FXML private Label detailRatingLabel;
    @FXML private Label detailSentimentLabel;
    @FXML private TextArea detailCommentArea;

    // ==================== Action Buttons ====================
    @FXML private Button viewReservationButton;
    @FXML private Button exportButton;

    // ==================== Data ====================
    private ObservableList<Feedback> allFeedback = FXCollections.observableArrayList();
    private FilteredList<Feedback> filteredFeedback;
    private Feedback selectedFeedback;

    // ==================== Services ====================
    private NavigationService navigationService;
    private FeedbackService feedbackService;
    private AdminSession adminSession;
    private ActivityLogger activityLogger;

    public AdminFeedbackViewerController() {
        this.navigationService = NavigationService.getInstance();
        this.adminSession = AdminSession.getInstance();
        this.activityLogger = ActivityLogger.getInstance();
    }

    public AdminFeedbackViewerController(NavigationService navigationService,
                                         FeedbackService feedbackService,
                                         AdminSession adminSession,
                                         ActivityLogger activityLogger) {
        this.navigationService = navigationService;
        this.feedbackService = feedbackService;
        this.adminSession = adminSession;
        this.activityLogger = activityLogger;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("Initializing Admin Feedback Viewer");

        if (!adminSession.isLoggedIn()) {
            Platform.runLater(() -> navigationService.goToAdminLogin());
            return;
        }

        setupFilters();
        setupTable();
        setupTableSelectionListener();
        loadFeedbackData();
        updateStatistics();
        clearDetails();

        logActivity("VIEW_FEEDBACK", "FEEDBACK", "N/A", "Admin accessed feedback viewer");
    }

    // ==================== Helper Method for Sentiment Tags ====================

    /**
     * Gets the primary sentiment tag from a Feedback's Set of sentiment tags.
     * Prioritizes POSITIVE > NEGATIVE > NEUTRAL if multiple tags exist.
     *
     * @param feedback The feedback to get the sentiment from
     * @return The primary SentimentTag, or null if none exist
     */
    private SentimentTag getPrimarySentimentTag(Feedback feedback) {
        if (feedback == null) {
            return null;
        }
        Set<SentimentTag> tags = feedback.getSentimentTags();
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        // Return first tag found, prioritizing POSITIVE > NEGATIVE > NEUTRAL
        if (tags.contains(SentimentTag.POSITIVE)) return SentimentTag.POSITIVE;
        if (tags.contains(SentimentTag.NEGATIVE)) return SentimentTag.NEGATIVE;
        if (tags.contains(SentimentTag.NEUTRAL)) return SentimentTag.NEUTRAL;
        return tags.iterator().next();
    }

    private void setupFilters() {
        // Sentiment filter
        if (sentimentFilter != null) {
            sentimentFilter.setItems(FXCollections.observableArrayList(
                    "All Sentiments", "Positive", "Neutral", "Negative"
            ));
            sentimentFilter.getSelectionModel().selectFirst();
            sentimentFilter.setOnAction(e -> applyFilters());
        }

        // Rating filter
        if (ratingFilter != null) {
            ratingFilter.setItems(FXCollections.observableArrayList(
                    "All Ratings", "5 Stars", "4 Stars", "3 Stars", "2 Stars", "1 Star"
            ));
            ratingFilter.getSelectionModel().selectFirst();
            ratingFilter.setOnAction(e -> applyFilters());
        }

        // Date filters
        if (fromDateFilter != null) {
            fromDateFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
        if (toDateFilter != null) {
            toDateFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }

        // Search field
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
    }

    private void setupTable() {
        if (feedbackTable == null) return;

        if (dateColumn != null) {
            dateColumn.setCellValueFactory(cell -> {
                LocalDateTime date = cell.getValue().getSubmittedAt(); // FIXED: was getCreatedAt()
                return new SimpleStringProperty(date != null ? date.format(DATETIME_FORMAT) : "N/A");
            });
        }

        if (guestColumn != null) {
            guestColumn.setCellValueFactory(cell -> {
                Guest guest = cell.getValue().getGuest();
                return new SimpleStringProperty(guest != null ? guest.getFullName() : "Anonymous");
            });
        }

        if (reservationColumn != null) {
            reservationColumn.setCellValueFactory(cell -> {
                Reservation res = cell.getValue().getReservation();
                return new SimpleStringProperty(res != null ? res.getConfirmationNumber() : "-");
            });
        }

        if (ratingColumn != null) {
            ratingColumn.setCellValueFactory(cell ->
                    new SimpleStringProperty(getStarDisplay(cell.getValue().getRating())));

            ratingColumn.setCellFactory(column -> new TableCell<Feedback, String>() {
                @Override
                protected void updateItem(String stars, boolean empty) {
                    super.updateItem(stars, empty);
                    if (empty || stars == null) {
                        setText(null);
                    } else {
                        setText(stars);
                        setStyle("-fx-text-fill: #FFD700;"); // Gold color for stars
                    }
                }
            });
        }

        if (sentimentColumn != null) {
            sentimentColumn.setCellValueFactory(cell -> {
                SentimentTag tag = getPrimarySentimentTag(cell.getValue()); // FIXED: was getSentimentTags()
                return new SimpleStringProperty(tag != null ? tag.getDisplayName() : "Unknown");
            });

            sentimentColumn.setCellFactory(column -> new TableCell<Feedback, String>() {
                @Override
                protected void updateItem(String sentiment, boolean empty) {
                    super.updateItem(sentiment, empty);
                    if (empty || sentiment == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(sentiment);
                        switch (sentiment) {
                            case "Positive" -> setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                            case "Negative" -> setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                            default -> setStyle("-fx-text-fill: #6c757d;");
                        }
                    }
                }
            });
        }

        if (commentPreviewColumn != null) {
            commentPreviewColumn.setCellValueFactory(cell -> {
                String comment = cell.getValue().getComments(); // FIXED: was getComment()
                if (comment == null || comment.isEmpty()) return new SimpleStringProperty("-");
                return new SimpleStringProperty(comment.length() > 50
                        ? comment.substring(0, 50) + "..." : comment);
            });
        }

        feedbackTable.setItems(allFeedback);
    }

    private String getStarDisplay(int rating) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            stars.append(i < rating ? "★" : "☆");
        }
        return stars.toString();
    }

    private void setupTableSelectionListener() {
        if (feedbackTable != null) {
            feedbackTable.getSelectionModel().selectedItemProperty().addListener(
                    (obs, oldSel, newSel) -> {
                        selectedFeedback = newSel;
                        updateDetails(newSel);
                        updateActionButtons(newSel);
                    });
        }
    }

    private void updateDetails(Feedback feedback) {
        if (feedback == null) {
            clearDetails();
            return;
        }

        Guest guest = feedback.getGuest();
        Reservation res = feedback.getReservation();

        setLabelText(detailGuestLabel, guest != null ? guest.getFullName() : "Anonymous");
        setLabelText(detailReservationLabel, res != null ? res.getConfirmationNumber() : "-");
        setLabelText(detailDateLabel, feedback.getSubmittedAt() != null  // FIXED: was getCreatedAt()
                ? feedback.getSubmittedAt().format(DATETIME_FORMAT) : "N/A");
        setLabelText(detailRatingLabel, getStarDisplay(feedback.getRating()) +
                " (" + feedback.getRating() + "/5)");

        SentimentTag primaryTag = getPrimarySentimentTag(feedback); // FIXED: was getSentimentTag()
        setLabelText(detailSentimentLabel, primaryTag != null
                ? primaryTag.getDisplayName() : "Unknown");

        if (detailCommentArea != null) {
            detailCommentArea.setText(feedback.getComments() != null  // FIXED: was getComment()
                    ? feedback.getComments() : "(No comment provided)");
        }
    }

    private void clearDetails() {
        setLabelText(detailGuestLabel, "Select feedback to view details");
        setLabelText(detailReservationLabel, "-");
        setLabelText(detailDateLabel, "-");
        setLabelText(detailRatingLabel, "-");
        setLabelText(detailSentimentLabel, "-");
        if (detailCommentArea != null) detailCommentArea.clear();
    }

    private void updateActionButtons(Feedback selected) {
        boolean hasSelection = selected != null;
        boolean hasReservation = hasSelection && selected.getReservation() != null;

        if (viewReservationButton != null) {
            viewReservationButton.setDisable(!hasReservation);
        }
    }

    private void loadFeedbackData() {
        allFeedback.clear();
        // In production: allFeedback.addAll(feedbackService.findAll());

        filteredFeedback = new FilteredList<>(allFeedback, p -> true);
        feedbackTable.setItems(filteredFeedback);

        LOGGER.info("Loaded " + allFeedback.size() + " feedback entries");
    }

    private void applyFilters() {
        if (filteredFeedback == null) return;

        filteredFeedback.setPredicate(feedback -> {
            // Sentiment filter
            if (sentimentFilter != null && sentimentFilter.getValue() != null &&
                    !"All Sentiments".equals(sentimentFilter.getValue())) {
                SentimentTag tag = getPrimarySentimentTag(feedback); // FIXED: was getSentimentTag()
                if (tag == null || !tag.getDisplayName().equals(sentimentFilter.getValue())) {
                    return false;
                }
            }

            // Rating filter
            if (ratingFilter != null && ratingFilter.getValue() != null &&
                    !"All Ratings".equals(ratingFilter.getValue())) {
                int filterRating = Integer.parseInt(ratingFilter.getValue().split(" ")[0]);
                if (feedback.getRating() != filterRating) return false;
            }

            // Date range filter
            if (fromDateFilter != null && fromDateFilter.getValue() != null) {
                if (feedback.getSubmittedAt() != null &&  // FIXED: was getCreatedAt()
                        feedback.getSubmittedAt().toLocalDate().isBefore(fromDateFilter.getValue())) {
                    return false;
                }
            }

            if (toDateFilter != null && toDateFilter.getValue() != null) {
                if (feedback.getSubmittedAt() != null &&  // FIXED: was getCreatedAt()
                        feedback.getSubmittedAt().toLocalDate().isAfter(toDateFilter.getValue())) {
                    return false;
                }
            }

            // Search filter
            String search = searchField != null ? searchField.getText().toLowerCase() : "";
            if (!search.isEmpty()) {
                Guest guest = feedback.getGuest();
                String comment = feedback.getComments(); // FIXED: was getComment()
                boolean matchesSearch =
                        (guest != null && guest.getFullName().toLowerCase().contains(search)) ||
                                (comment != null && comment.toLowerCase().contains(search));
                if (!matchesSearch) return false;
            }

            return true;
        });

        updateStatistics();
    }

    private void updateStatistics() {
        int total = allFeedback.size();
        double avgRating = allFeedback.stream()
                .mapToInt(Feedback::getRating)
                .average()
                .orElse(0.0);
        long positive = allFeedback.stream()
                .filter(f -> getPrimarySentimentTag(f) == SentimentTag.POSITIVE) // FIXED: was getSentimentTag()
                .count();
        long negative = allFeedback.stream()
                .filter(f -> getPrimarySentimentTag(f) == SentimentTag.NEGATIVE) // FIXED: was getSentimentTag()
                .count();

        setLabelText(totalFeedbackLabel, String.valueOf(total));
        setLabelText(averageRatingLabel, String.format("%.1f/5", avgRating));
        setLabelText(positiveFeedbackLabel, String.valueOf(positive));
        setLabelText(negativeFeedbackLabel, String.valueOf(negative));
    }

    // ==================== Event Handlers ====================

    @FXML
    public void handleFilterChange(ActionEvent event) {
        applyFilters();
    }

    @FXML
    public void handleSearch(KeyEvent event) {
        applyFilters();
    }

    @FXML
    public void handleClearFilters(ActionEvent event) {
        if (sentimentFilter != null) sentimentFilter.getSelectionModel().selectFirst();
        if (ratingFilter != null) ratingFilter.getSelectionModel().selectFirst();
        if (fromDateFilter != null) fromDateFilter.setValue(null);
        if (toDateFilter != null) toDateFilter.setValue(null);
        if (searchField != null) searchField.clear();
        applyFilters();
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        loadFeedbackData();
        updateStatistics();
    }

    @FXML
    public void handleViewReservation(ActionEvent event) {
        if (selectedFeedback == null || selectedFeedback.getReservation() == null) return;

        adminSession.setCurrentReservation(selectedFeedback.getReservation());
        navigationService.navigateTo("/com/hanson/hotelreservationsystem/admin/adminReservationDetails.fxml");
    }

    @FXML
    public void handleExport(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Export",
                "Feedback report would be exported to CSV/Excel here.");
        logActivity("EXPORT_FEEDBACK", "FEEDBACK", "N/A", "Exported feedback report");
    }

    @FXML
    public void handleBack(ActionEvent event) {
        navigationService.navigateTo("/com/hanson/hotelreservationsystem/admin/adminDashboard.fxml");
    }

    private void setLabelText(Label label, String text) {
        if (label != null) label.setText(text != null ? text : "N/A");
    }

    private void logActivity(String action, String entityType, String entityId, String message) {
        if (activityLogger != null) {
            activityLogger.logActivity(adminSession.getActorName(), action, entityType, entityId, message);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Setters for DI
    public void setNavigationService(NavigationService navigationService) { this.navigationService = navigationService; }
    public void setFeedbackService(FeedbackService feedbackService) { this.feedbackService = feedbackService; }
    public void setAdminSession(AdminSession adminSession) { this.adminSession = adminSession; }
    public void setActivityLogger(ActivityLogger activityLogger) { this.activityLogger = activityLogger; }
}