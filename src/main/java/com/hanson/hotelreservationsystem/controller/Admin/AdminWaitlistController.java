package com.hanson.hotelreservationsystem.controller.Admin;

import com.hanson.hotelreservationsystem.model.*;
import com.hanson.hotelreservationsystem.model.enums.*;
import com.hanson.hotelreservationsystem.events.RoomAvailabilityEvent;
import com.hanson.hotelreservationsystem.events.RoomAvailabilityObserver;
import com.hanson.hotelreservationsystem.repository.WaitlistRepository;
import com.hanson.hotelreservationsystem.repository.GuestRepository;
import com.hanson.hotelreservationsystem.service.NavigationService;
import com.hanson.hotelreservationsystem.service.RoomService;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class AdminWaitlistController implements Initializable, RoomAvailabilityObserver {

    private static final Logger LOGGER = Logger.getLogger(AdminWaitlistController.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @FXML private ComboBox<String> roomTypeFilter;
    @FXML private DatePicker dateFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private CheckBox autoNotifyCheck;

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

    private ObservableList<WaitlistEntry> allEntries = FXCollections.observableArrayList();
    private FilteredList<WaitlistEntry> filteredEntries;

    // Dependencies
    private NavigationService navigationService = NavigationService.getInstance();
    private AdminSession adminSession = AdminSession.getInstance();
    private ActivityLogger activityLogger = ActivityLogger.getInstance();
    private WaitlistRepository waitlistRepository = WaitlistRepository.getInstance();
    private GuestRepository guestRepository = GuestRepository.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!adminSession.isLoggedIn()) {
            Platform.runLater(() -> navigationService.goToAdminLogin());
            return;
        }

        // Register observer
        RoomService.getInstance().addObserver(this);

        setupFilters();
        setupTableColumns();
        setupActionsColumn();
        loadWaitlistData(); // Now loads from DB
    }
    @FXML
    public void handleAddToWaitlist(ActionEvent event) {
        Dialog<WaitlistFormData> dialog = new Dialog<>();
        dialog.setTitle("Add to Waitlist");
        dialog.setHeaderText("Enter Guest Details and Preferences");

        // Set the button types
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Create labels and fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        nameField.setPromptText("Full Name");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone Number");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        ComboBox<RoomType> roomTypeCombo = new ComboBox<>();
        roomTypeCombo.getItems().setAll(RoomType.values());
        roomTypeCombo.getSelectionModel().selectFirst();

        DatePicker checkInDate = new DatePicker(LocalDate.now());
        DatePicker checkOutDate = new DatePicker(LocalDate.now().plusDays(1));

        Spinner<Integer> guestCount = new Spinner<>(1, 10, 1);

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Phone:"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Room Type:"), 0, 3);
        grid.add(roomTypeCombo, 1, 3);
        grid.add(new Label("Check-In:"), 0, 4);
        grid.add(checkInDate, 1, 4);
        grid.add(new Label("Check-Out:"), 0, 5);
        grid.add(checkOutDate, 1, 5);
        grid.add(new Label("Guests:"), 0, 6);
        grid.add(guestCount, 1, 6);

        dialog.getDialogPane().setContent(grid);

        // Convert result to a temporary DTO (Data Transfer Object)
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return new WaitlistFormData(
                        nameField.getText(),
                        phoneField.getText(),
                        emailField.getText(),
                        roomTypeCombo.getValue(),
                        checkInDate.getValue(),
                        checkOutDate.getValue(),
                        guestCount.getValue()
                );
            }
            return null;
        });

        Optional<WaitlistFormData> result = dialog.showAndWait();

        result.ifPresent(data -> {
            try {
                // --- DATABASE OPERATIONS START HERE ---

                // 1. Create and Save Guest FIRST
                // This ensures the Guest is attached to the Persistence Context
                Guest guest = new Guest();
                String[] names = data.name.split(" ");
                guest.setFirstName(names.length > 0 ? names[0] : data.name);
                guest.setLastName(names.length > 1 ? names[1] : "Guest");
                guest.setEmail(data.email);
                guest.setPhone(data.phone);

                // The repository calls em.persist(). Since WaitlistRepository will start
                // a transaction shortly, this guest will be flushed with it.
                Guest savedGuest = guestRepository.save(guest);

                // 2. Create WaitlistEntry with the PERSISTED Guest reference
                WaitlistEntry entry = new WaitlistEntry(
                        savedGuest,
                        data.roomType,
                        data.checkIn,
                        data.checkOut,
                        data.guests
                );
                entry.setAddedBy(adminSession.getActorName());

                // 3. Save WaitlistEntry (This commits the transaction for BOTH entities)
                waitlistRepository.save(entry);

                // --- DATABASE OPERATIONS END ---

                loadWaitlistData(); // Refresh table

                activityLogger.logActivity(adminSession.getActorName(), "WAITLIST_ADD",
                        "WAITLIST", "N/A", "Added " + savedGuest.getFullName() + " to waitlist");

            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add to waitlist: " + e.getMessage());
            }
        });
    }

    // Inner class to hold form data temporarily
    private static class WaitlistFormData {
        String name, phone, email;
        RoomType roomType;
        LocalDate checkIn, checkOut;
        int guests;

        public WaitlistFormData(String name, String phone, String email, RoomType roomType, LocalDate checkIn, LocalDate checkOut, int guests) {
            this.name = name; this.phone = phone; this.email = email;
            this.roomType = roomType; this.checkIn = checkIn; this.checkOut = checkOut;
            this.guests = guests;
        }
    }

    // --- 2. LOAD REAL DATA ---
    private void loadWaitlistData() {
        allEntries.clear();
        allEntries.addAll(waitlistRepository.findAll());

        filteredEntries = new FilteredList<>(allEntries, p -> true);
        waitlistTable.setItems(filteredEntries);
        waitlistTable.refresh();
    }

    // --- 3. CONVERT TO RESERVATION ---
    private void handleConvertEntry(WaitlistEntry entry) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Convert to Reservation");
        confirm.setHeaderText("Convert this waitlist entry to a booking?");
        confirm.setContentText("Guest: " + entry.getGuest().getFullName());

        if (confirm.showAndWait().get() == ButtonType.OK) {
            // 1. Set the guest in session so the form picks it up
            adminSession.setCurrentGuest(entry.getGuest());

            // 2. Update Waitlist Status
            entry.setStatus("CONVERTED");
            entry.setConvertedAt(LocalDateTime.now());
            waitlistRepository.save(entry);

            loadWaitlistData();

            // 3. Navigate to Reservation Form
            navigationService.goToAdminReservationForm();
        }
    }

    private void handleNotifyEntry(WaitlistEntry entry) {
        entry.markNotified();
        waitlistRepository.save(entry);
        loadWaitlistData();

        showAlert(Alert.AlertType.INFORMATION, "Guest Notified",
                "Notification sent to " + entry.getContactEmail());
    }

    private void handleRemoveEntry(WaitlistEntry entry) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Entry");
        confirm.setHeaderText("Remove " + entry.getGuest().getFullName() + " from waitlist?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            waitlistRepository.delete(entry);
            loadWaitlistData();
            activityLogger.logActivity(adminSession.getActorName(), "WAITLIST_REMOVE",
                    "WAITLIST", String.valueOf(entry.getId()), "Removed waitlist entry");
        }
    }

    @FXML public void handleFilterChange(ActionEvent event) {
        if (filteredEntries == null) return;
        filteredEntries.setPredicate(entry -> {
            // Room Type Filter
            if (roomTypeFilter.getValue() != null && !"All Types".equals(roomTypeFilter.getValue())) {
                if (!entry.getDesiredRoomType().getDisplayName().equals(roomTypeFilter.getValue())) return false;
            }
            // Status Filter
            if (statusFilter.getValue() != null && !"All Statuses".equals(statusFilter.getValue())) {
                if (!entry.getStatus().equals(statusFilter.getValue())) return false;
            }
            // Date Filter
            if (dateFilter.getValue() != null) {
                if (!entry.getDesiredCheckIn().equals(dateFilter.getValue())) return false;
            }
            return true;
        });
    }

    @Override
    public void onAvailabilityChange(RoomAvailabilityEvent event) {
        // When a room opens up, check if any PENDING waitlist entries match
        Platform.runLater(() -> {
            boolean matchFound = false;
            for (WaitlistEntry entry : allEntries) {
                if ("WAITING".equals(entry.getStatus()) &&
                        entry.getDesiredRoomType() == event.getRoomType()) {

                    if (autoNotifyCheck.isSelected()) {
                        handleNotifyEntry(entry);
                    }
                    matchFound = true;
                }
            }
            if (matchFound) {
                loadWaitlistData(); // Refresh UI to show updated statuses
            }
        });
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
                    "All Statuses", "WAITING", "NOTIFIED", "CONVERTED", "EXPIRED"
            ));
            statusFilter.getSelectionModel().selectFirst();
        }
    }

    private void setupTableColumns() {
        // Same as your code, but ensure CellValueFactories use the real getters
        idColumn.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        guestColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGuest().getFullName()));
        phoneColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGuest().getPhone()));
        roomTypeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDesiredRoomType().toString()));
        checkInColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDesiredCheckIn().toString()));
        checkOutColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDesiredCheckOut().toString()));
        statusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        // Keep your existing styling logic in setCellFactory
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button notifyBtn = new Button("Notify");
            private final Button convertBtn = new Button("Convert");
            private final Button removeBtn = new Button("Remove");

            {
                notifyBtn.getStyleClass().add("small-button");
                convertBtn.getStyleClass().add("small-button");
                removeBtn.getStyleClass().add("small-button");

                notifyBtn.setOnAction(e -> handleNotifyEntry(getTableView().getItems().get(getIndex())));
                convertBtn.setOnAction(e -> handleConvertEntry(getTableView().getItems().get(getIndex())));
                removeBtn.setOnAction(e -> handleRemoveEntry(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    javafx.scene.layout.HBox pane = new javafx.scene.layout.HBox(5);
                    WaitlistEntry entry = getTableView().getItems().get(getIndex());
                    if ("WAITING".equals(entry.getStatus())) {
                        pane.getChildren().addAll(notifyBtn, convertBtn, removeBtn);
                    } else {
                        pane.getChildren().addAll(convertBtn, removeBtn);
                    }
                    setGraphic(pane);
                }
            }
        });
    }

    @FXML
    private void handleBack(){
        LOGGER.info("Navigating back to dashboard");
        navigationService.goToAdminDashboard();
    }

    @Override
    public String getObserverId() { return "AdminWaitlistController"; }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}