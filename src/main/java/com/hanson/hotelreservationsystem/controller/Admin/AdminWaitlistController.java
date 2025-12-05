package com.hanson.hotelreservationsystem.controller.Admin;

import com.hanson.hotelreservationsystem.model.enums.*;
import com.hanson.hotelreservationsystem.events.RoomAvailabilityEvent;
import com.hanson.hotelreservationsystem.events.RoomAvailabilityObserver;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

/**
 * Controller for the Admin Waitlist Management Screen.
 *
 * CORRECTED VERSION - Aligned with adminWaitlist.fxml
 *
 * Implements RoomAvailabilityObserver (Observer pattern) to receive notifications
 * when rooms become available.
 *
 * Responsibilities:
 * - Display waitlist entries in a searchable table
 * - Filter by room type, date, and status
 * - Process waitlist when rooms become available
 * - Notify guests of availability
 */
public class AdminWaitlistController implements Initializable, RoomAvailabilityObserver {

    private static final Logger LOGGER = Logger.getLogger(AdminWaitlistController.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    // ==================== Filters ====================
    @FXML private ComboBox<String> roomTypeFilter;
    @FXML private DatePicker dateFilter;
    @FXML private ComboBox<String> statusFilter;

    // ==================== Waitlist Table ====================
    @FXML private TableView<WaitlistEntry> waitlistTable;
    @FXML private TableColumn<WaitlistEntry, String> idColumn;
    @FXML private TableColumn<WaitlistEntry, String> guestColumn;
    @FXML private TableColumn<WaitlistEntry, String> phoneColumn;
    @FXML private TableColumn<WaitlistEntry, String> roomTypeColumn;
    @FXML private TableColumn<WaitlistEntry, String> checkInColumn;
    @FXML private TableColumn<WaitlistEntry, String> checkOutColumn;
    @FXML private TableColumn<WaitlistEntry, String> priorityColumn;
    @FXML private TableColumn<WaitlistEntry, String> statusColumn;
    @FXML private TableColumn<WaitlistEntry, String> addedColumn;
    @FXML private TableColumn<WaitlistEntry, String> actionsColumn;

    // ==================== Auto-Notify ====================
    @FXML private CheckBox autoNotifyCheck;

    // ==================== Data ====================
    private ObservableList<WaitlistEntry> allEntries = FXCollections.observableArrayList();
    private FilteredList<WaitlistEntry> filteredEntries;

    // ==================== Services ====================
    private NavigationService navigationService;
    private AdminSession adminSession;
    private ActivityLogger activityLogger;

    public AdminWaitlistController() {
        this.navigationService = NavigationService.getInstance();
        this.adminSession = AdminSession.getInstance();
        this.activityLogger = ActivityLogger.getInstance();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("Initializing Admin Waitlist Screen");

        if (!adminSession.isLoggedIn()) {
            Platform.runLater(() -> navigationService.goToAdminLogin());
            return;
        }

        setupFilters();
        setupTableColumns();
        setupActionsColumn();
        loadWaitlistData();
    }

    private void setupFilters() {
        // Room type filter
        if (roomTypeFilter != null) {
            ObservableList<String> types = FXCollections.observableArrayList("All Types");
            for (RoomType type : RoomType.values()) {
                types.add(type.getDisplayName());
            }
            roomTypeFilter.setItems(types);
            roomTypeFilter.getSelectionModel().selectFirst();
        }

        // Status filter
        if (statusFilter != null) {
            statusFilter.setItems(FXCollections.observableArrayList(
                    "All Statuses", "PENDING", "NOTIFIED", "CONVERTED", "EXPIRED"
            ));
            statusFilter.getSelectionModel().selectFirst();
        }

        // Date filter
        if (dateFilter != null) {
            dateFilter.setValue(null);
        }
    }

    private void setupTableColumns() {
        if (waitlistTable == null) return;

        // ID column
        if (idColumn != null) {
            idColumn.setCellValueFactory(c ->
                    new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        }

        // Guest name column
        if (guestColumn != null) {
            guestColumn.setCellValueFactory(c ->
                    new SimpleStringProperty(c.getValue().getGuestName()));
        }

        // Phone column
        if (phoneColumn != null) {
            phoneColumn.setCellValueFactory(c ->
                    new SimpleStringProperty(c.getValue().getPhone()));
        }

        // Room type column
        if (roomTypeColumn != null) {
            roomTypeColumn.setCellValueFactory(c -> {
                RoomType type = c.getValue().getRequestedRoomType();
                return new SimpleStringProperty(type != null ? type.getDisplayName() : "N/A");
            });
        }

        // Check-in column
        if (checkInColumn != null) {
            checkInColumn.setCellValueFactory(c -> {
                LocalDate date = c.getValue().getRequestedCheckIn();
                return new SimpleStringProperty(date != null ? date.format(DATE_FORMAT) : "N/A");
            });
        }

        // Check-out column
        if (checkOutColumn != null) {
            checkOutColumn.setCellValueFactory(c -> {
                LocalDate date = c.getValue().getRequestedCheckOut();
                return new SimpleStringProperty(date != null ? date.format(DATE_FORMAT) : "N/A");
            });
        }

        // Priority column
        if (priorityColumn != null) {
            priorityColumn.setCellValueFactory(c ->
                    new SimpleStringProperty(String.valueOf(c.getValue().getPriority())));
        }

        // Status column with styling
        if (statusColumn != null) {
            statusColumn.setCellValueFactory(c ->
                    new SimpleStringProperty(c.getValue().getStatus()));
            statusColumn.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    if (empty || status == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(status);
                        setStyle(switch (status) {
                            case "PENDING" -> "-fx-text-fill: #17a2b8;";
                            case "NOTIFIED" -> "-fx-text-fill: #28a745;";
                            case "CONVERTED" -> "-fx-text-fill: #6c757d;";
                            case "EXPIRED" -> "-fx-text-fill: #dc3545;";
                            default -> "";
                        });
                    }
                }
            });
        }

        // Added date column
        if (addedColumn != null) {
            addedColumn.setCellValueFactory(c -> {
                LocalDateTime date = c.getValue().getAddedDate();
                return new SimpleStringProperty(date != null ? date.format(DATE_FORMAT) : "N/A");
            });
        }

        // Setup filtered list
        filteredEntries = new FilteredList<>(allEntries, p -> true);
        waitlistTable.setItems(filteredEntries);
    }

    private void setupActionsColumn() {
        if (actionsColumn == null) return;

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button notifyBtn = new Button("Notify");
            private final Button convertBtn = new Button("Convert");
            private final Button removeBtn = new Button("Remove");

            {
                notifyBtn.getStyleClass().add("small-button");
                convertBtn.getStyleClass().add("small-button");
                removeBtn.getStyleClass().addAll("small-button", "cancel-button");

                notifyBtn.setOnAction(e -> {
                    WaitlistEntry entry = getTableView().getItems().get(getIndex());
                    handleNotifyEntry(entry);
                });

                convertBtn.setOnAction(e -> {
                    WaitlistEntry entry = getTableView().getItems().get(getIndex());
                    handleConvertEntry(entry);
                });

                removeBtn.setOnAction(e -> {
                    WaitlistEntry entry = getTableView().getItems().get(getIndex());
                    handleRemoveEntry(entry);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    WaitlistEntry entry = getTableView().getItems().get(getIndex());
                    javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(5);

                    if ("PENDING".equals(entry.getStatus())) {
                        box.getChildren().addAll(notifyBtn, convertBtn, removeBtn);
                    } else if ("NOTIFIED".equals(entry.getStatus())) {
                        box.getChildren().addAll(convertBtn, removeBtn);
                    } else {
                        box.getChildren().add(removeBtn);
                    }

                    setGraphic(box);
                }
            }
        });
    }

    private void loadWaitlistData() {
        // Sample data for demonstration
        allEntries.clear();

        WaitlistEntry entry1 = new WaitlistEntry();
        entry1.setId(1L);
        entry1.setGuestName("John Smith");
        entry1.setEmail("john@example.com");
        entry1.setPhone("555-0101");
        entry1.setRequestedRoomType(RoomType.DELUXE);
        entry1.setRequestedCheckIn(LocalDate.now().plusDays(7));
        entry1.setRequestedCheckOut(LocalDate.now().plusDays(10));
        entry1.setPriority(1);
        entry1.setStatus("PENDING");
        entry1.setAddedDate(LocalDateTime.now().minusDays(2));

        WaitlistEntry entry2 = new WaitlistEntry();
        entry2.setId(2L);
        entry2.setGuestName("Jane Doe");
        entry2.setEmail("jane@example.com");
        entry2.setPhone("555-0102");
        entry2.setRequestedRoomType(RoomType.PENTHOUSE);
        entry2.setRequestedCheckIn(LocalDate.now().plusDays(14));
        entry2.setRequestedCheckOut(LocalDate.now().plusDays(17));
        entry2.setPriority(2);
        entry2.setStatus("NOTIFIED");
        entry2.setAddedDate(LocalDateTime.now().minusDays(5));

        allEntries.addAll(entry1, entry2);
    }

    private void applyFilters() {
        if (filteredEntries == null) return;

        filteredEntries.setPredicate(entry -> {
            // Room type filter
            if (roomTypeFilter != null && roomTypeFilter.getValue() != null
                    && !"All Types".equals(roomTypeFilter.getValue())) {
                if (entry.getRequestedRoomType() == null ||
                        !entry.getRequestedRoomType().getDisplayName().equals(roomTypeFilter.getValue())) {
                    return false;
                }
            }

            // Status filter
            if (statusFilter != null && statusFilter.getValue() != null
                    && !"All Statuses".equals(statusFilter.getValue())) {
                if (!statusFilter.getValue().equals(entry.getStatus())) {
                    return false;
                }
            }

            // Date filter
            if (dateFilter != null && dateFilter.getValue() != null) {
                LocalDate filterDate = dateFilter.getValue();
                if (entry.getRequestedCheckIn() == null ||
                        !entry.getRequestedCheckIn().equals(filterDate)) {
                    return false;
                }
            }

            return true;
        });
    }

    // ==================== Observer Pattern ====================


    @Override
    public void onAvailabilityChange(RoomAvailabilityEvent event) {
        // Called when a room becomes available
        Platform.runLater(() -> {
            if (autoNotifyCheck != null && autoNotifyCheck.isSelected()) {
                // Find matching waitlist entries and notify
                for (WaitlistEntry entry : allEntries) {
                    if ("PENDING".equals(entry.getStatus()) &&
                            entry.getRequestedRoomType() == event.getRoomType()) {
                        handleNotifyEntry(entry);
                        break; // Notify first matching entry
                    }
                }
            }
            waitlistTable.refresh();
        });
    }

    @Override
    public String getObserverId() {
        return "AdminWaitlistController";
    }

    // ==================== FXML Event Handlers ====================

    @FXML
    public void handleAddToWaitlist(ActionEvent event) {
        // Open a dialog to add new waitlist entry
        // For now, show a placeholder alert
        showAlert(Alert.AlertType.INFORMATION, "Add to Waitlist",
                "This would open a form to add a new waitlist entry.");
    }

    @FXML
    public void handleFilterChange(ActionEvent event) {
        applyFilters();
    }

    // ==================== Action Handlers ====================

    private void handleNotifyEntry(WaitlistEntry entry) {
        entry.setStatus("NOTIFIED");
        entry.setNotifiedDate(LocalDateTime.now());
        waitlistTable.refresh();

        logActivity("WAITLIST_NOTIFY", "WAITLIST", entry.getEmail(),
                "Notified: " + entry.getGuestName());
        showAlert(Alert.AlertType.INFORMATION, "Notified",
                "Guest " + entry.getGuestName() + " has been notified of availability.");
    }

    private void handleConvertEntry(WaitlistEntry entry) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Convert to Reservation");
        confirm.setHeaderText("Create reservation from waitlist entry?");
        confirm.setContentText("Guest: " + entry.getGuestName());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            entry.setStatus("CONVERTED");
            waitlistTable.refresh();

            logActivity("WAITLIST_CONVERT", "WAITLIST", entry.getEmail(),
                    "Converted: " + entry.getGuestName());

            // Navigate to reservation form with pre-filled data
            // In real implementation, pass entry data to form
            navigationService.navigateTo("/com/hanson/hotelreservationsystem/admin/adminReservationForm.fxml");
        }
    }

    private void handleRemoveEntry(WaitlistEntry entry) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Entry");
        confirm.setHeaderText("Remove " + entry.getGuestName() + " from waitlist?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            allEntries.remove(entry);
            logActivity("WAITLIST_REMOVE", "WAITLIST", entry.getEmail(),
                    "Removed: " + entry.getGuestName());
        }
    }

    // ==================== Helper Methods ====================

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void logActivity(String action, String entityType, String entityId, String message) {
        if (activityLogger != null) {
            activityLogger.logActivity(adminSession.getActorName(), action, entityType, entityId, message);
        }
    }

    // ==================== Waitlist Entry Inner Class ====================

    public static class WaitlistEntry {
        private Long id;
        private String guestName;
        private String email;
        private String phone;
        private RoomType requestedRoomType;
        private LocalDate requestedCheckIn;
        private LocalDate requestedCheckOut;
        private int guestCount;
        private int priority;
        private String notes;
        private String status;
        private LocalDateTime addedDate;
        private LocalDateTime notifiedDate;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getGuestName() { return guestName; }
        public void setGuestName(String guestName) { this.guestName = guestName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public RoomType getRequestedRoomType() { return requestedRoomType; }
        public void setRequestedRoomType(RoomType requestedRoomType) { this.requestedRoomType = requestedRoomType; }
        public LocalDate getRequestedCheckIn() { return requestedCheckIn; }
        public void setRequestedCheckIn(LocalDate requestedCheckIn) { this.requestedCheckIn = requestedCheckIn; }
        public LocalDate getRequestedCheckOut() { return requestedCheckOut; }
        public void setRequestedCheckOut(LocalDate requestedCheckOut) { this.requestedCheckOut = requestedCheckOut; }
        public int getGuestCount() { return guestCount; }
        public void setGuestCount(int guestCount) { this.guestCount = guestCount; }
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getAddedDate() { return addedDate; }
        public void setAddedDate(LocalDateTime addedDate) { this.addedDate = addedDate; }
        public LocalDateTime getNotifiedDate() { return notifiedDate; }
        public void setNotifiedDate(LocalDateTime notifiedDate) { this.notifiedDate = notifiedDate; }
    }

    // ==================== Setters for DI ====================

    public void setNavigationService(NavigationService navigationService) {
        this.navigationService = navigationService;
    }
    public void setAdminSession(AdminSession adminSession) {
        this.adminSession = adminSession;
    }
    public void setActivityLogger(ActivityLogger activityLogger) {
        this.activityLogger = activityLogger;
    }
}