// ============================================================================
// AdminReportsController.java - COMPLETE REWRITE
// ============================================================================
// The controller needs to be rewritten to match the FXML's TabPane design.
// The FXML has 4 tabs: Revenue, Occupancy, Activity Logs, Feedback Summary
// Each tab has its own filters, summary cards, and table.
// NOTE: No charts (per requirements - CSV/PDF/TXT export only)
// ============================================================================

package com.hanson.hotelreservationsystem.controller.Admin;

import com.hanson.hotelreservationsystem.model.Reservation;
import com.hanson.hotelreservationsystem.repository.ReservationRepository;
import com.hanson.hotelreservationsystem.service.NavigationService;
import java.io.File;
import java.io.FileWriter;
import javafx.stage.FileChooser;
import com.hanson.hotelreservationsystem.service.PricingService;
import com.hanson.hotelreservationsystem.service.ReservationService;
import com.hanson.hotelreservationsystem.service.RoomService;
import com.hanson.hotelreservationsystem.session.AdminSession;
import com.hanson.hotelreservationsystem.util.ActivityLogger;
import jakarta.persistence.TypedQuery;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class AdminReportsController {

    // ========================================================================
    // TAB PANE AND CONTENT PANES
    // ========================================================================

    @FXML private TabPane reportTabs;
    @FXML private Tab revenueTab;
    @FXML private Tab occupancyTab;
    @FXML private Tab activityTab;
    @FXML private Tab feedbackSummaryTab;

    @FXML private StackPane reportContentPane;
    @FXML private VBox revenueContent;
    @FXML private VBox occupancyContent;
    @FXML private VBox activityContent;
    @FXML private VBox feedbackSummaryContent;

    // ========================================================================
    // REVENUE TAB FIELDS
    // ========================================================================
    @FXML private ComboBox<String> revenuePeriodCombo;
    @FXML private DatePicker revenueStartDate;
    @FXML private DatePicker revenueEndDate;

    // Revenue summary cards
    @FXML private Label totalRevenueLabel;
    @FXML private Label totalReservationsLabel;
    @FXML private Label avgRevenueLabel;

    // Revenue table
    @FXML private TableView<RevenueRow> revenueTable;
    @FXML private TableColumn<RevenueRow, String> revPeriodColumn;
    @FXML private TableColumn<RevenueRow, Integer> revCountColumn;
    @FXML private TableColumn<RevenueRow, String> revSubtotalColumn;
    @FXML private TableColumn<RevenueRow, String> revTaxColumn;
    @FXML private TableColumn<RevenueRow, String> revDiscountColumn;
    @FXML private TableColumn<RevenueRow, String> revTotalColumn;

    // ========================================================================
    // OCCUPANCY TAB FIELDS
    // ========================================================================

    @FXML private ComboBox<String> occupancyPeriodCombo;
    @FXML private DatePicker occupancyStartDate;
    @FXML private DatePicker occupancyEndDate;
    @FXML private ComboBox<String> roomTypeCombo;

    // Occupancy summary cards
    @FXML private Label avgOccupancyLabel;
    @FXML private Label peakOccupancyLabel;
    @FXML private Label totalRoomNightsLabel;

    // Occupancy table
    @FXML private TableView<OccupancyRow> occupancyTable;
    @FXML private TableColumn<OccupancyRow, String> occDateColumn;
    @FXML private TableColumn<OccupancyRow, Integer> occAvailableColumn;
    @FXML private TableColumn<OccupancyRow, Integer> occOccupiedColumn;
    @FXML private TableColumn<OccupancyRow, String> occPercentageColumn;
    @FXML private TableColumn<OccupancyRow, String> occRevenueColumn;

    // ========================================================================
    // ACTIVITY LOGS TAB FIELDS
    // ========================================================================

    @FXML private TextField activitySearchField;
    @FXML private ComboBox<String> activityTypeFilter;
    @FXML private DatePicker activityDateFilter;

    // Activity table
    @FXML private TableView<ActivityRow> activityTable;
    @FXML private TableColumn<ActivityRow, String> actTimestampColumn;
    @FXML private TableColumn<ActivityRow, String> actActorColumn;
    @FXML private TableColumn<ActivityRow, String> actActionColumn;
    @FXML private TableColumn<ActivityRow, String> actEntityColumn;
    @FXML private TableColumn<ActivityRow, String> actIdColumn;
    @FXML private TableColumn<ActivityRow, String> actMessageColumn;

    // ========================================================================
    // FEEDBACK SUMMARY TAB FIELDS
    // ========================================================================

    @FXML private DatePicker feedbackStartDate;
    @FXML private DatePicker feedbackEndDate;
    @FXML private ComboBox<String> feedbackRatingFilter;

    // Feedback summary labels
    @FXML private Label feedbackTotalLabel;
    @FXML private Label feedbackAvgLabel;
    @FXML private Label feedbackTagsLabel;

    // Feedback table
    @FXML private TableView<FeedbackRow> feedbackSummaryTable;
    @FXML private TableColumn<FeedbackRow, String> fbDateColumn;
    @FXML private TableColumn<FeedbackRow, String> fbGuestColumn;
    @FXML private TableColumn<FeedbackRow, Integer> fbRatingColumn;
    @FXML private TableColumn<FeedbackRow, String> fbSentimentColumn;
    @FXML private TableColumn<FeedbackRow, String> fbCommentColumn;

    // ========================================================================
    // DATA COLLECTIONS
    // ========================================================================

    private ObservableList<RevenueRow> revenueData = FXCollections.observableArrayList();
    private ObservableList<OccupancyRow> occupancyData = FXCollections.observableArrayList();
    private ObservableList<ActivityRow> activityData = FXCollections.observableArrayList();
    private ObservableList<FeedbackRow> feedbackData = FXCollections.observableArrayList();

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    private final NavigationService navigationService;

    public AdminReportsController() {
        this.navigationService = NavigationService.getInstance();
    }

    @FXML
    public void initialize() {
        // Setup tab change listener
        reportTabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            handleTabChange(newTab);
        });

        // Initialize combo boxes
        setupComboBoxes();

        // Setup table columns
        setupRevenueTable();
        setupOccupancyTable();
        setupActivityTable();
        setupFeedbackTable();

        // Set default dates
        revenueStartDate.setValue(LocalDate.now().minusMonths(1));
        revenueEndDate.setValue(LocalDate.now());
        occupancyStartDate.setValue(LocalDate.now().minusMonths(1));
        occupancyEndDate.setValue(LocalDate.now());
        feedbackStartDate.setValue(LocalDate.now().minusMonths(1));
        feedbackEndDate.setValue(LocalDate.now());

        // Load initial data
        handleGenerateRevenue();
    }

    private void setupComboBoxes() {
        // Revenue period options
        revenuePeriodCombo.getItems().addAll("Daily", "Weekly", "Monthly", "Yearly", "Custom");
        revenuePeriodCombo.setValue("Monthly");

        // Occupancy period options
        occupancyPeriodCombo.getItems().addAll("Daily", "Weekly", "Monthly", "Custom");
        occupancyPeriodCombo.setValue("Daily");

        // Room type options
        roomTypeCombo.getItems().addAll("All Room Types", "Single", "Double", "Deluxe", "Penthouse");
        roomTypeCombo.setValue("All Room Types");

        // Activity type options
        activityTypeFilter.getItems().addAll("All Actions", "LOGIN", "LOGOUT", "CREATE", "UPDATE", "DELETE", "PAYMENT", "CHECKOUT");
        activityTypeFilter.setValue("All Actions");

        // Feedback rating options
        feedbackRatingFilter.getItems().addAll("All Ratings", "5 Stars", "4 Stars", "3 Stars", "2 Stars", "1 Star");
        feedbackRatingFilter.setValue("All Ratings");
    }

    private void setupRevenueTable() {
        revPeriodColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().period));
        revCountColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().count));
        revSubtotalColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().subtotal));
        revTaxColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().tax));
        revDiscountColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().discount));
        revTotalColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().total));
        revenueTable.setItems(revenueData);
    }

    private void setupOccupancyTable() {
        occDateColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().date));
        occAvailableColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().available));
        occOccupiedColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().occupied));
        occPercentageColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().percentage));
        occRevenueColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().revenue));
        occupancyTable.setItems(occupancyData);
    }

    private void setupActivityTable() {
        actTimestampColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().timestamp));
        actActorColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().actor));
        actActionColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().action));
        actEntityColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().entity));
        actIdColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().id));
        actMessageColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().message));
        activityTable.setItems(activityData);
    }

    private void setupFeedbackTable() {
        fbDateColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().date));
        fbGuestColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().guest));
        fbRatingColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().rating));
        fbSentimentColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().sentiment));
        fbCommentColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().comment));
        feedbackSummaryTable.setItems(feedbackData);
    }

    // ========================================================================
    // TAB CHANGE HANDLER
    // ========================================================================

    private void handleTabChange(Tab newTab) {
        // Hide all content panes
        revenueContent.setVisible(false);
        occupancyContent.setVisible(false);
        activityContent.setVisible(false);
        feedbackSummaryContent.setVisible(false);

        // Show selected content pane
        if (newTab == revenueTab) {
            revenueContent.setVisible(true);
        } else if (newTab == occupancyTab) {
            occupancyContent.setVisible(true);
        } else if (newTab == activityTab) {
            activityContent.setVisible(true);
        } else if (newTab == feedbackSummaryTab) {
            feedbackSummaryContent.setVisible(true);
        }
    }

    // ========================================================================
    // REVENUE HANDLERS
    // ========================================================================

    @FXML
    private void handleRevenuePeriodChange() {
        String period = revenuePeriodCombo.getValue();
        boolean isCustom = "Custom".equals(period);
        revenueStartDate.setDisable(!isCustom);
        revenueEndDate.setDisable(!isCustom);
    }

    @FXML
    private void handleGenerateRevenue() {
        revenueData.clear();
        LocalDate start = revenueStartDate.getValue();
        LocalDate end = revenueEndDate.getValue();

        // 1. Fetch real data
        List<Reservation> reservations = ReservationRepository.getInstance().findByDateRange(start, end);

        // 2. Aggregate Data
        BigDecimal totalRev = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            totalRev = totalRev.add(res.getTotalAmount());
            totalTax = totalTax.add(res.getTaxAmount());
        }

        // 3. Add to Table
        // You can group by month if you want, here is a simple total row
        revenueData.add(new RevenueRow(
                "Total Period",
                reservations.size(),
                String.format("$%.2f", totalRev.subtract(totalTax)), // Subtotal
                String.format("$%.2f", totalTax),
                "$0.00",
                String.format("$%.2f", totalRev)
        ));

        // Update Cards
        totalRevenueLabel.setText(String.format("$%.2f", totalRev));
        totalReservationsLabel.setText(String.valueOf(reservations.size()));
    }

    // ========================================================================
    // OCCUPANCY HANDLERS
    // ========================================================================

    @FXML
    private void handleOccupancyPeriodChange() {
        String period = occupancyPeriodCombo.getValue();
        boolean isCustom = "Custom".equals(period);
        occupancyStartDate.setDisable(!isCustom);
        occupancyEndDate.setDisable(!isCustom);
    }

    @FXML
    private void handleGenerateOccupancy() {
        // Clear existing data
        occupancyData.clear();

        // TODO: Load actual data from service
        // For now, add sample data
        occupancyData.add(new OccupancyRow("2025-11-15", 40, 32, "80%", "$3,200.00"));
        occupancyData.add(new OccupancyRow("2025-11-16", 40, 35, "87.5%", "$3,500.00"));
        occupancyData.add(new OccupancyRow("2025-11-17", 40, 38, "95%", "$3,800.00"));

        // Update summary cards
        avgOccupancyLabel.setText("87.5%");
        peakOccupancyLabel.setText("95%");
        totalRoomNightsLabel.setText("105");
    }

    // ========================================================================
    // ACTIVITY LOG HANDLERS
    // ========================================================================

    @FXML
    private void handleSearchActivity() {
        // Clear existing data
        activityData.clear();

        String searchTerm = activitySearchField.getText().trim().toLowerCase();
        String typeFilter = activityTypeFilter.getValue();
        LocalDate dateFilter = activityDateFilter.getValue();

        // TODO: Load actual data from ActivityLogger service
        // For now, add sample data
        activityData.add(new ActivityRow("2025-11-18 14:32:15", "admin", "LOGIN", "User", "1", "Admin logged in"));
        activityData.add(new ActivityRow("2025-11-18 14:35:20", "admin", "CREATE", "Reservation", "1001", "Created reservation for John Doe"));
        activityData.add(new ActivityRow("2025-11-18 15:10:45", "admin", "PAYMENT", "Payment", "TXN-001", "Processed payment of $450.00"));
    }

    // ========================================================================
    // FEEDBACK SUMMARY HANDLERS
    // ========================================================================

    @FXML
    private void handleGenerateFeedback() {
        // Clear existing data
        feedbackData.clear();

        // TODO: Load actual data from FeedbackService
        // For now, add sample data
        feedbackData.add(new FeedbackRow("2025-11-15", "John Doe", 5, "POSITIVE", "Excellent stay! Very clean rooms."));
        feedbackData.add(new FeedbackRow("2025-11-16", "Jane Smith", 4, "POSITIVE", "Good service, will come again."));
        feedbackData.add(new FeedbackRow("2025-11-17", "Bob Wilson", 3, "NEUTRAL", "Average experience overall."));

        // Update summary labels
        feedbackTotalLabel.setText("3");
        feedbackAvgLabel.setText("4.0");
        feedbackTagsLabel.setText("Clean, Service, Value");
    }

    // ========================================================================
    // EXPORT HANDLER
    // ========================================================================

    @FXML
    private void handleExport() {
        // 1. Setup File Chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report");
        fileChooser.setInitialFileName("RevenueReport.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        // 2. Get File
        File file = fileChooser.showSaveDialog(reportTabs.getScene().getWindow());

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                // 3. Write Header
                writer.write("Period,Reservations,Subtotal,Tax,Total\n");

                // 4. Write Data rows
                for (RevenueRow row : revenueData) {
                    writer.write(String.format("%s,%d,%s,%s,%s\n",
                            row.period,
                            row.count,
                            row.subtotal.replace("$","").replace(",",""), // Clean currency formatting
                            row.tax.replace("$","").replace(",",""),
                            row.total.replace("$","").replace(",","")
                    ));
                }

                // 5. Show Success
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Export Successful!");
                alert.showAndWait();

            } catch (IOException e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Export Failed: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }


    @FXML
    private void handleBack() {
        navigationService.goToAdminDashboard();
    }

    // ========================================================================
    // INNER CLASSES FOR TABLE ROWS
    // ========================================================================

    public static class RevenueRow {
        public String period;
        public int count;
        public String subtotal;
        public String tax;
        public String discount;
        public String total;

        public RevenueRow(String period, int count, String subtotal, String tax, String discount, String total) {
            this.period = period;
            this.count = count;
            this.subtotal = subtotal;
            this.tax = tax;
            this.discount = discount;
            this.total = total;
        }
    }

    public static class OccupancyRow {
        public String date;
        public int available;
        public int occupied;
        public String percentage;
        public String revenue;

        public OccupancyRow(String date, int available, int occupied, String percentage, String revenue) {
            this.date = date;
            this.available = available;
            this.occupied = occupied;
            this.percentage = percentage;
            this.revenue = revenue;
        }
    }

    public static class ActivityRow {
        public String timestamp;
        public String actor;
        public String action;
        public String entity;
        public String id;
        public String message;

        public ActivityRow(String timestamp, String actor, String action, String entity, String id, String message) {
            this.timestamp = timestamp;
            this.actor = actor;
            this.action = action;
            this.entity = entity;
            this.id = id;
            this.message = message;
        }
    }

    public static class FeedbackRow {
        public String date;
        public String guest;
        public int rating;
        public String sentiment;
        public String comment;

        public FeedbackRow(String date, String guest, int rating, String sentiment, String comment) {
            this.date = date;
            this.guest = guest;
            this.rating = rating;
            this.sentiment = sentiment;
            this.comment = comment;
        }
    }
}