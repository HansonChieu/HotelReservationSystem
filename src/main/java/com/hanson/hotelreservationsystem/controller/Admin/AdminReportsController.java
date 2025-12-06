// ============================================================================
// AdminReportsController.java - UPDATED FOR EXPORT REQUIREMENTS
// ============================================================================
package com.hanson.hotelreservationsystem.controller.Admin;

import com.hanson.hotelreservationsystem.config.JPAUtil;
import com.hanson.hotelreservationsystem.model.ActivityLog;
import com.hanson.hotelreservationsystem.model.Feedback;
import com.hanson.hotelreservationsystem.model.Reservation;
import com.hanson.hotelreservationsystem.repository.ReservationRepository;
import com.hanson.hotelreservationsystem.repository.RoomRepository;
import com.hanson.hotelreservationsystem.service.FeedbackService;
import com.hanson.hotelreservationsystem.service.NavigationService;
import com.hanson.hotelreservationsystem.model.enums.ReservationStatus;
import com.hanson.hotelreservationsystem.model.enums.RoomType;

import java.io.File;
import java.io.FileWriter;
import javafx.stage.FileChooser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminReportsController {

    // ... [Previous FXML fields remain unchanged] ...

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
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final FeedbackService feedbackService;

    public AdminReportsController() {
        this.navigationService = NavigationService.getInstance();
        this.reservationRepository = ReservationRepository.getInstance();
        this.roomRepository = RoomRepository.getInstance();
        this.feedbackService = FeedbackService.getInstance();
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
        activityDateFilter.setValue(LocalDate.now());

        // Load initial data for the default tab
        handleGenerateRevenue();
    }

    private void setupComboBoxes() {
        // Revenue period options
        revenuePeriodCombo.getItems().addAll("Daily", "Monthly", "Custom");
        revenuePeriodCombo.setValue("Monthly");

        // Occupancy period options
        occupancyPeriodCombo.getItems().addAll("Daily", "Custom");
        occupancyPeriodCombo.setValue("Daily");

        // Room type options
        roomTypeCombo.getItems().add("All Room Types");
        for(RoomType type : RoomType.values()){
            roomTypeCombo.getItems().add(type.toString());
        }
        roomTypeCombo.setValue("All Room Types");

        // Activity type options
        activityTypeFilter.getItems().addAll("All Actions", "LOGIN", "CREATE_RESERVATION", "CHECKOUT", "PROCESS_PAYMENT", "CANCEL_RESERVATION");
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

        // Show selected content pane and refresh data
        if (newTab == revenueTab) {
            revenueContent.setVisible(true);
            handleGenerateRevenue();
        } else if (newTab == occupancyTab) {
            occupancyContent.setVisible(true);
            handleGenerateOccupancy();
        } else if (newTab == activityTab) {
            activityContent.setVisible(true);
            handleSearchActivity();
        } else if (newTab == feedbackSummaryTab) {
            feedbackSummaryContent.setVisible(true);
            handleGenerateFeedback();
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
        String periodMode = revenuePeriodCombo.getValue();

        List<Reservation> reservations = reservationRepository.findByDateRange(start, end);

        Map<String, RevenueRow> bucketMap = new HashMap<>();
        DateTimeFormatter dailyFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter monthlyFmt = DateTimeFormatter.ofPattern("yyyy-MM");

        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            if(res.getStatus() == ReservationStatus.CANCELLED) continue;

            String key;
            if ("Daily".equals(periodMode)) {
                key = res.getCheckInDate().format(dailyFmt);
            } else if ("Monthly".equals(periodMode)) {
                key = res.getCheckInDate().format(monthlyFmt);
            } else {
                key = "Total Period";
            }

            RevenueRow row = bucketMap.getOrDefault(key, new RevenueRow(key, 0, "$0.00", "$0.00", "$0.00", "$0.00"));

            BigDecimal rowSub = new BigDecimal(row.subtotal.replace("$", "").replace(",", ""));
            BigDecimal rowTax = new BigDecimal(row.tax.replace("$", "").replace(",", ""));
            BigDecimal rowDisc = new BigDecimal(row.discount.replace("$", "").replace(",", ""));
            BigDecimal rowTot = new BigDecimal(row.total.replace("$", "").replace(",", ""));

            row.count++;
            row.subtotal = String.format("$%.2f", rowSub.add(res.getSubtotal()));
            row.tax = String.format("$%.2f", rowTax.add(res.getTaxAmount()));
            row.discount = String.format("$%.2f", rowDisc.add(res.getDiscountAmount().add(res.getLoyaltyDiscount())));
            row.total = String.format("$%.2f", rowTot.add(res.getTotalAmount()));

            bucketMap.put(key, row);
            grandTotal = grandTotal.add(res.getTotalAmount());
        }

        List<RevenueRow> rows = new ArrayList<>(bucketMap.values());
        rows.sort((r1, r2) -> r1.period.compareTo(r2.period));
        revenueData.addAll(rows);

        totalRevenueLabel.setText(String.format("$%.2f", grandTotal));
        totalReservationsLabel.setText(String.valueOf(reservations.size()));

        if(!reservations.isEmpty()){
            avgRevenueLabel.setText(String.format("$%.2f", grandTotal.divide(BigDecimal.valueOf(reservations.size()), 2, RoundingMode.HALF_UP)));
        } else {
            avgRevenueLabel.setText("$0.00");
        }
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
        occupancyData.clear();
        LocalDate start = occupancyStartDate.getValue();
        LocalDate end = occupancyEndDate.getValue();
        String selectedRoomType = roomTypeCombo.getValue();

        long totalRooms;
        if (selectedRoomType == null || "All Room Types".equals(selectedRoomType)) {
            totalRooms = roomRepository.count();
        } else {
            totalRooms = roomRepository.countByRoomType(RoomType.valueOf(selectedRoomType));
        }

        if (totalRooms == 0) return;

        List<Reservation> activeReservations = reservationRepository.findByCheckInDateBetween(start.minusDays(30), end);

        long totalOccupiedCount = 0;
        long maxOccupied = 0;
        long totalDays = ChronoUnit.DAYS.between(start, end) + 1;

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            final LocalDate currentDate = date;

            long occupiedCount = activeReservations.stream()
                    .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                    .filter(r -> !r.getCheckInDate().isAfter(currentDate) && r.getCheckOutDate().isAfter(currentDate))
                    .count();

            double percentage = (double) occupiedCount / totalRooms * 100.0;

            occupancyData.add(new OccupancyRow(
                    date.toString(),
                    (int)(totalRooms - occupiedCount),
                    (int)occupiedCount,
                    String.format("%.1f%%", percentage),
                    "N/A"
            ));

            totalOccupiedCount += occupiedCount;
            if(occupiedCount > maxOccupied) maxOccupied = occupiedCount;
        }

        double avgOcc = (double) totalOccupiedCount / (totalRooms * totalDays) * 100.0;
        avgOccupancyLabel.setText(String.format("%.1f%%", avgOcc));
        peakOccupancyLabel.setText(String.format("%d Rooms", maxOccupied));
        totalRoomNightsLabel.setText(String.valueOf(totalOccupiedCount));
    }

    // ========================================================================
    // ACTIVITY LOG HANDLERS
    // ========================================================================

    @FXML
    private void handleSearchActivity() {
        activityData.clear();

        String searchTerm = activitySearchField.getText().trim().toLowerCase();
        String typeFilter = activityTypeFilter.getValue();
        LocalDate dateFilter = activityDateFilter.getValue();

        EntityManager em = JPAUtil.createEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT a FROM ActivityLog a WHERE 1=1");

            if (dateFilter != null) {
                jpql.append(" AND a.timestamp BETWEEN :startOfDay AND :endOfDay");
            }
            if (typeFilter != null && !"All Actions".equals(typeFilter)) {
                jpql.append(" AND a.action = :action");
            }
            if (!searchTerm.isEmpty()) {
                jpql.append(" AND (LOWER(a.actor) LIKE :search OR LOWER(a.message) LIKE :search)");
            }
            jpql.append(" ORDER BY a.timestamp DESC");

            TypedQuery<ActivityLog> query = em.createQuery(jpql.toString(), ActivityLog.class);

            if (dateFilter != null) {
                query.setParameter("startOfDay", dateFilter.atStartOfDay());
                query.setParameter("endOfDay", dateFilter.atTime(23, 59, 59));
            }
            if (typeFilter != null && !"All Actions".equals(typeFilter)) {
                query.setParameter("action", typeFilter);
            }
            if (!searchTerm.isEmpty()) {
                query.setParameter("search", "%" + searchTerm + "%");
            }

            List<ActivityLog> logs = query.getResultList();

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (ActivityLog log : logs) {
                activityData.add(new ActivityRow(
                        log.getTimestamp().format(dtf),
                        log.getActor(),
                        log.getAction(),
                        log.getEntityType(),
                        log.getEntityId() != null ? log.getEntityId().toString() : "-",
                        log.getMessage()
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    // ========================================================================
    // FEEDBACK SUMMARY HANDLERS
    // ========================================================================

    @FXML
    private void handleGenerateFeedback() {
        feedbackData.clear();
        LocalDate start = feedbackStartDate.getValue();
        LocalDate end = feedbackEndDate.getValue();
        String ratingFilter = feedbackRatingFilter.getValue();

        List<Feedback> allFeedback = feedbackService.findAll();

        List<Feedback> filtered = allFeedback.stream()
                .filter(f -> {
                    if (f.getSubmittedAt() == null) return false;
                    LocalDate subDate = f.getSubmittedAt().toLocalDate();
                    return !subDate.isBefore(start) && !subDate.isAfter(end);
                })
                .filter(f -> {
                    if (ratingFilter == null || "All Ratings".equals(ratingFilter)) return true;
                    int rating = Integer.parseInt(ratingFilter.split(" ")[0]);
                    return f.getRating() == rating;
                })
                .collect(Collectors.toList());

        int total = filtered.size();
        double avg = filtered.stream().mapToInt(Feedback::getRating).average().orElse(0.0);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (Feedback f : filtered) {
            String sentiment = f.getSentimentTags().isEmpty() ? "N/A" :
                    f.getSentimentTags().stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(", "));

            feedbackData.add(new FeedbackRow(
                    f.getSubmittedAt().format(dtf),
                    f.getGuest() != null ? f.getGuest().getFullName() : "Anonymous",
                    f.getRating(),
                    sentiment,
                    f.getComments()
            ));
        }

        feedbackTotalLabel.setText(String.valueOf(total));
        feedbackAvgLabel.setText(String.format("%.1f", avg));
        feedbackTagsLabel.setText(total > 0 ? "Data Loaded" : "No Data");
    }

    // ========================================================================
    // EXPORT HANDLER - MODIFIED FOR CSV AND TXT
    // ========================================================================

    @FXML
    private void handleExport() {
        // 1. Setup File Chooser with CSV and TXT filters
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report");

        String defaultName = "Report";
        if(revenueContent.isVisible()) defaultName = "RevenueReport";
        else if(occupancyContent.isVisible()) defaultName = "OccupancyReport";
        else if(activityContent.isVisible()) defaultName = "ActivityLog";
        else if(feedbackSummaryContent.isVisible()) defaultName = "FeedbackReport";

        fileChooser.setInitialFileName(defaultName);
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"),
                new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt")
        );

        // 2. Get File
        File file = fileChooser.showSaveDialog(reportTabs.getScene().getWindow());

        if (file != null) {
            // Determine delimiter based on file extension
            String delimiter = file.getName().toLowerCase().endsWith(".txt") ? "\t" : ",";

            try (FileWriter writer = new FileWriter(file)) {
                if (revenueContent.isVisible()) {
                    exportRevenue(writer, delimiter);
                } else if (occupancyContent.isVisible()) {
                    exportOccupancy(writer, delimiter);
                } else if (activityContent.isVisible()) {
                    exportActivity(writer, delimiter);
                } else {
                    exportFeedback(writer, delimiter);
                }

                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Export Successful!");
                alert.showAndWait();

            } catch (IOException e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Export Failed: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void exportRevenue(FileWriter writer, String d) throws IOException {
        writer.write("Period" + d + "Reservations" + d + "Subtotal" + d + "Tax" + d + "Total\n");
        for (RevenueRow row : revenueData) {
            writer.write(String.format("%s%s%d%s%s%s%s%s%s\n",
                    row.period, d, row.count, d,
                    row.subtotal.replace("$","").replace(",",""), d,
                    row.tax.replace("$","").replace(",",""), d,
                    row.total.replace("$","").replace(",","")));
        }
    }

    private void exportOccupancy(FileWriter writer, String d) throws IOException {
        writer.write("Date" + d + "Available" + d + "Occupied" + d + "Percentage\n");
        for (OccupancyRow row : occupancyData) {
            writer.write(String.format("%s%s%d%s%d%s%s\n",
                    row.date, d, row.available, d, row.occupied, d, row.percentage));
        }
    }

    private void exportActivity(FileWriter writer, String d) throws IOException {
        // [UPDATED] Headers now include Entity ID
        writer.write("Timestamp" + d + "Actor" + d + "Action" + d + "Entity" + d + "Entity ID" + d + "Message\n");

        for (ActivityRow row : activityData) {
            // Sanitize content to prevent breaking the delimiter
            String safeMessage = row.message.replace(d, " ").replace("\n", " ");

            writer.write(String.format("%s%s%s%s%s%s%s%s%s%s%s\n",
                    row.timestamp, d,
                    row.actor, d,
                    row.action, d,
                    row.entity, d,
                    row.id, d, // [UPDATED] Added Entity Identifier
                    safeMessage));
        }
    }

    private void exportFeedback(FileWriter writer, String d) throws IOException {
        writer.write("Date" + d + "Guest" + d + "Rating" + d + "Sentiment" + d + "Comment\n");
        for (FeedbackRow row : feedbackData) {
            String safeComment = row.comment.replace(d, " ").replace("\n", " ");
            writer.write(String.format("%s%s%s%s%d%s%s%s%s\n",
                    row.date, d, row.guest, d, row.rating, d, row.sentiment, d, safeComment));
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