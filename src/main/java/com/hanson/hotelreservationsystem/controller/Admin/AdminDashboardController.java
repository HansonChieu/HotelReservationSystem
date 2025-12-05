package com.hanson.hotelreservationsystem.controller.Admin;

import com.hanson.hotelreservationsystem.model.Reservation;
import com.hanson.hotelreservationsystem.model.enums.ReservationStatus;
import com.hanson.hotelreservationsystem.model.enums.RoomType;
import com.hanson.hotelreservationsystem.service.NavigationService;
import com.hanson.hotelreservationsystem.service.ReservationService;
import com.hanson.hotelreservationsystem.session.AdminSession;
import com.hanson.hotelreservationsystem.util.ActivityLogger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Admin Dashboard Screen.
 *
 * Responsibilities:
 * - Display reservations in a searchable, sortable TableView
 * - Provide search and filter functionality
 * - Navigate to reservation details, payment, checkout screens
 * - Auto-refresh data when returning to this screen
 */
public class AdminDashboardController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(AdminDashboardController.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a");
    private static final int DEFAULT_PAGE_SIZE = 20;

    // ==================== FXML Components ====================

    // CRITICAL: Linked to the root element in adminDashboard.fxml to detect scene changes
    @FXML private BorderPane rootPane;

    // Tab navigation buttons
    @FXML private Button reservationsTab;
    @FXML private Button waitlistTab;
    @FXML private Button loyaltyTab;
    @FXML private Button feedbackTab;
    @FXML private Button reportsTab;

    // Search and filter section
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> roomTypeFilter;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Button searchButton;
    @FXML private Button clearFiltersButton;
    @FXML private Button refreshButton;

    // Reservations TableView
    @FXML private TableView<Reservation> reservationsTable;
    @FXML private TableColumn<Reservation, String> confirmationColumn;
    @FXML private TableColumn<Reservation, String> guestNameColumn;
    @FXML private TableColumn<Reservation, String> emailColumn;
    @FXML private TableColumn<Reservation, String> phoneColumn;
    @FXML private TableColumn<Reservation, String> checkInColumn;
    @FXML private TableColumn<Reservation, String> checkOutColumn;
    @FXML private TableColumn<Reservation, String> roomsColumn;
    @FXML private TableColumn<Reservation, String> statusColumn;
    @FXML private TableColumn<Reservation, String> totalColumn;
    @FXML private TableColumn<Reservation, String> balanceColumn;

    // Pagination controls
    @FXML private TextField currentPageField;
    @FXML private Label totalPagesLabel;
    @FXML private ComboBox<Integer> itemsPerPageCombo;
    @FXML private Label totalRecordsLabel;

    // Quick action buttons
    @FXML private Button newReservationButton;
    @FXML private Button viewDetailsButton;
    @FXML private Button processPaymentButton;
    @FXML private Button checkOutButton;
    @FXML private Button cancelButton;

    // Status bar
    @FXML private Label adminNameLabel;
    @FXML private Label roleLabel;
    @FXML private Label currentDateTimeLabel;
    @FXML private Label statusLabel;
    @FXML private Label roomAvailabilityLabel;
    @FXML private Button logoutButton;

    // ==================== Data ====================

    private ObservableList<Reservation> allReservations = FXCollections.observableArrayList();
    private FilteredList<Reservation> filteredReservations;
    private SortedList<Reservation> sortedReservations;

    // Pagination state
    private int currentPage = 1;
    private int totalPages = 1;
    private int itemsPerPage = DEFAULT_PAGE_SIZE;

    // ==================== Services ====================

    private NavigationService navigationService;
    private ReservationService reservationService;
    private AdminSession adminSession;
    private ActivityLogger activityLogger;

    /**
     * Default constructor for FXML loader.
     */
    public AdminDashboardController() {
        this.navigationService = NavigationService.getInstance();
        this.reservationService = ReservationService.getInstance();
        this.adminSession = AdminSession.getInstance();
        this.activityLogger = ActivityLogger.getInstance();
    }

    /**
     * Constructor with dependency injection.
     */
    public AdminDashboardController(NavigationService navigationService,
                                    ReservationService reservationService,
                                    AdminSession adminSession,
                                    ActivityLogger activityLogger) {
        this.navigationService = navigationService;
        this.reservationService = reservationService;
        this.adminSession = adminSession;
        this.activityLogger = activityLogger;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("Initializing Admin Dashboard");

        // Verify admin is logged in
        if (!adminSession.isLoggedIn()) {
            LOGGER.warning("No admin session - redirecting to login");
            Platform.runLater(() -> navigationService.goToAdminLogin());
            return;
        }

        setupStatusBar();
        setupFilters();
        setupTableColumns();
        setupTableSelectionListener();
        setupQuickActionButtons();
        setupPagination();

        // Initial data load
        loadReservations();
        updateDateTime();

        // Log activity
        logActivity("DASHBOARD_ACCESS", "SYSTEM", "N/A", "Admin accessed dashboard");

        // ==================== AUTO-REFRESH LISTENER ====================
        // This listener detects when the Dashboard View is placed onto the Screen.
        // It triggers a reload so data is fresh when returning from "Details" or "Payment".
        if (rootPane != null) {
            rootPane.sceneProperty().addListener((observable, oldScene, newScene) -> {
                if (newScene != null) {
                    LOGGER.info("Dashboard view active - auto-refreshing data...");
                    loadReservations();
                    updateRoomAvailability();
                    updateDateTime();
                }
            });
        } else {
            LOGGER.warning("rootPane is null. Ensure fx:id='rootPane' is added to the BorderPane in adminDashboard.fxml");
        }
    }

    /**
     * Setup the status bar with admin info.
     */
    private void setupStatusBar() {
        if (adminSession.getCurrentAdmin() != null) {
            if (adminNameLabel != null) {
                adminNameLabel.setText("Admin: " + adminSession.getCurrentAdmin().getFullName());
            }
            if (roleLabel != null) {
                roleLabel.setText("(" + adminSession.getCurrentAdmin().getRole().getDisplayName() + ")");
            }
        }
        if (statusLabel != null) {
            statusLabel.setText("Ready");
        }
        updateRoomAvailability();
    }

    /**
     * Update current date/time display.
     */
    private void updateDateTime() {
        if (currentDateTimeLabel != null) {
            currentDateTimeLabel.setText(LocalDateTime.now().format(DATETIME_FORMAT));
        }
    }

    /**
     * Update room availability display.
     */
    private void updateRoomAvailability() {
        if (roomAvailabilityLabel != null) {
            // In a real app, call RoomService to get these counts
            int total = 40;
            int occupied = 0;
            // Simple calculation based on loaded reservations that are CHECKED_IN
            if (allReservations != null) {
                occupied = (int) allReservations.stream()
                        .filter(r -> r.getStatus() == ReservationStatus.CHECKED_IN)
                        .count();
            }
            int available = total - occupied;
            roomAvailabilityLabel.setText(String.format("Available Rooms: %d | Occupied: %d | Total: %d",
                    available, occupied, total));
        }
    }

    /**
     * Setup filter controls.
     */
    private void setupFilters() {
        // Status filter
        if (statusFilter != null) {
            ObservableList<String> statusOptions = FXCollections.observableArrayList();
            statusOptions.add("All Statuses");
            for (ReservationStatus status : ReservationStatus.values()) {
                statusOptions.add(status.getDisplayName());
            }
            statusFilter.setItems(statusOptions);
            statusFilter.getSelectionModel().selectFirst();
        }

        // Room type filter
        if (roomTypeFilter != null) {
            ObservableList<String> roomTypeOptions = FXCollections.observableArrayList();
            roomTypeOptions.add("All Room Types");
            for (RoomType type : RoomType.values()) {
                roomTypeOptions.add(type.getDisplayName());
            }
            roomTypeFilter.setItems(roomTypeOptions);
            roomTypeFilter.getSelectionModel().selectFirst();
        }
    }

    /**
     * Setup pagination controls.
     */
    private void setupPagination() {
        if (itemsPerPageCombo != null) {
            itemsPerPageCombo.setItems(FXCollections.observableArrayList(10, 20, 50, 100));
            itemsPerPageCombo.setValue(DEFAULT_PAGE_SIZE);
        }
        if (currentPageField != null) {
            currentPageField.setText("1");
        }
        if (totalPagesLabel != null) {
            totalPagesLabel.setText("1");
        }
    }

    /**
     * Setup table columns with cell value factories.
     */
    private void setupTableColumns() {
        if (reservationsTable == null) return;

        // Confirmation number
        if (confirmationColumn != null) {
            confirmationColumn.setCellValueFactory(new PropertyValueFactory<>("confirmationNumber"));
        }

        // Guest name
        if (guestNameColumn != null) {
            guestNameColumn.setCellValueFactory(cellData -> {
                Reservation res = cellData.getValue();
                String name = res.getGuest() != null ? res.getGuest().getFullName() : "N/A";
                return new SimpleStringProperty(name);
            });
        }

        // Email
        if (emailColumn != null) {
            emailColumn.setCellValueFactory(cellData -> {
                Reservation res = cellData.getValue();
                String email = res.getGuest() != null ? res.getGuest().getEmail() : "N/A";
                return new SimpleStringProperty(email);
            });
        }

        // Phone
        if (phoneColumn != null) {
            phoneColumn.setCellValueFactory(cellData -> {
                Reservation res = cellData.getValue();
                String phone = res.getGuest() != null ? res.getGuest().getPhone() : "N/A";
                return new SimpleStringProperty(phone);
            });
        }

        // Check-in date
        if (checkInColumn != null) {
            checkInColumn.setCellValueFactory(cellData -> {
                LocalDate date = cellData.getValue().getCheckInDate();
                return new SimpleStringProperty(date != null ? date.format(DATE_FORMAT) : "N/A");
            });
        }

        // Check-out date
        if (checkOutColumn != null) {
            checkOutColumn.setCellValueFactory(cellData -> {
                LocalDate date = cellData.getValue().getCheckOutDate();
                return new SimpleStringProperty(date != null ? date.format(DATE_FORMAT) : "N/A");
            });
        }

        // Rooms (count)
        if (roomsColumn != null) {
            roomsColumn.setCellValueFactory(cellData -> {
                Reservation res = cellData.getValue();
                int roomCount = res.getReservationRooms() != null ? res.getReservationRooms().size() : 0;
                return new SimpleStringProperty(roomCount + " room(s)");
            });
        }

        // Status with styling
        if (statusColumn != null) {
            statusColumn.setCellValueFactory(cellData -> {
                ReservationStatus status = cellData.getValue().getStatus();
                return new SimpleStringProperty(status != null ? status.getDisplayName() : "Unknown");
            });

            statusColumn.setCellFactory(column -> new TableCell<Reservation, String>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    if (empty || status == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(status);
                        switch (status) {
                            case "Confirmed" -> setStyle("-fx-text-fill: #28a745;"); // Green
                            case "Checked In" -> setStyle("-fx-text-fill: #007bff;"); // Blue
                            case "Checked Out" -> setStyle("-fx-text-fill: #6c757d;"); // Gray
                            case "Cancelled" -> setStyle("-fx-text-fill: #dc3545;"); // Red
                            case "No Show" -> setStyle("-fx-text-fill: #ffc107;"); // Yellow
                            default -> setStyle("-fx-text-fill: #17a2b8;"); // Teal for Pending
                        }
                    }
                }
            });
        }

        // Total amount
        if (totalColumn != null) {
            totalColumn.setCellValueFactory(cellData -> {
                BigDecimal total = cellData.getValue().getTotalAmount();
                return new SimpleStringProperty(String.format("$%.2f", total != null ? total : BigDecimal.ZERO));
            });
        }

        // Outstanding balance
        if (balanceColumn != null) {
            balanceColumn.setCellValueFactory(cellData -> {
                BigDecimal balance = cellData.getValue().getOutstandingBalance();
                return new SimpleStringProperty(String.format("$%.2f", balance != null ? balance : BigDecimal.ZERO));
            });

            balanceColumn.setCellFactory(column -> new TableCell<Reservation, String>() {
                @Override
                protected void updateItem(String balance, boolean empty) {
                    super.updateItem(balance, empty);
                    if (empty || balance == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(balance);
                        if (!balance.equals("$0.00")) {
                            setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;"); // Red/Bold
                        } else {
                            setStyle("-fx-text-fill: #28a745;"); // Green
                        }
                    }
                }
            });
        }

        // Enable row double-click to view details
        reservationsTable.setRowFactory(tv -> {
            TableRow<Reservation> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleViewDetails(null);
                }
            });
            return row;
        });
    }

    /**
     * Setup table selection listener to enable/disable action buttons.
     */
    private void setupTableSelectionListener() {
        if (reservationsTable == null) return;

        reservationsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> updateActionButtons(newSelection));
    }

    /**
     * Update action button states based on selected reservation.
     */
    private void updateActionButtons(Reservation selected) {
        boolean hasSelection = selected != null;

        if (viewDetailsButton != null) {
            viewDetailsButton.setDisable(!hasSelection);
        }

        if (processPaymentButton != null) {
            // Enable only for reservations with outstanding balance
            boolean canPay = hasSelection && selected.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0;
            processPaymentButton.setDisable(!canPay);
        }

        if (checkOutButton != null) {
            // Enable only for checked-in reservations that are fully paid
            boolean canCheckOut = hasSelection &&
                    selected.getStatus() == ReservationStatus.CHECKED_IN &&
                    selected.isFullyPaid();
            checkOutButton.setDisable(!canCheckOut);
        }

        if (cancelButton != null) {
            // Enable only for pending or confirmed reservations
            boolean canCancel = hasSelection &&
                    (selected.getStatus() == ReservationStatus.PENDING ||
                            selected.getStatus() == ReservationStatus.CONFIRMED);
            cancelButton.setDisable(!canCancel);
        }
    }

    /**
     * Setup quick action buttons.
     */
    private void setupQuickActionButtons() {
        updateActionButtons(null);
    }

    /**
     * Load reservations from the service.
     */
    private void loadReservations() {
        LOGGER.info("Loading reservations");

        // Clear existing data
        allReservations.clear();

        // Load from service (fetches fresh data from DB)
        if (reservationService != null) {
            try {
                List<Reservation> reservations = reservationService.findAll();
                if (reservations != null) {
                    allReservations.addAll(reservations);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load reservations from service", e);
            }
        }

        // Setup filtered and sorted lists
        filteredReservations = new FilteredList<>(allReservations, p -> true);
        sortedReservations = new SortedList<>(filteredReservations);
        sortedReservations.comparatorProperty().bind(reservationsTable.comparatorProperty());

        reservationsTable.setItems(sortedReservations);

        // Update record count and pagination
        updateRecordCount();
        updatePagination();
        applyFilters(); // Re-apply filters if user had typed something

        if (statusLabel != null) {
            statusLabel.setText("Loaded " + allReservations.size() + " reservations");
        }

        LOGGER.info("Loaded " + allReservations.size() + " reservations");
    }

    /**
     * Apply all current filters to the reservation list.
     */
    private void applyFilters() {
        if (filteredReservations == null) return;

        filteredReservations.setPredicate(createFilterPredicate());
        updateRecordCount();
        updatePagination();

        adminSession.updateActivity();
    }

    /**
     * Create a combined predicate for all active filters.
     */
    private Predicate<Reservation> createFilterPredicate() {
        return reservation -> {
            // Search text filter
            String searchText = searchField != null ? searchField.getText() : "";
            if (!searchText.isEmpty()) {
                String lowerSearch = searchText.toLowerCase();
                boolean matchesConfirmation = reservation.getConfirmationNumber() != null &&
                        reservation.getConfirmationNumber().toLowerCase().contains(lowerSearch);
                boolean matchesName = reservation.getGuest() != null &&
                        reservation.getGuest().getFullName().toLowerCase().contains(lowerSearch);
                boolean matchesEmail = reservation.getGuest() != null &&
                        reservation.getGuest().getEmail() != null &&
                        reservation.getGuest().getEmail().toLowerCase().contains(lowerSearch);
                boolean matchesPhone = reservation.getGuest() != null &&
                        reservation.getGuest().getPhone() != null &&
                        reservation.getGuest().getPhone().contains(searchText);

                if (!matchesConfirmation && !matchesName && !matchesEmail && !matchesPhone) {
                    return false;
                }
            }

            // Status filter
            if (statusFilter != null && statusFilter.getValue() != null &&
                    !statusFilter.getValue().equals("All Statuses")) {
                String selectedStatus = statusFilter.getValue();
                if (reservation.getStatus() == null ||
                        !reservation.getStatus().getDisplayName().equals(selectedStatus)) {
                    return false;
                }
            }

            // Room type filter
            if (roomTypeFilter != null && roomTypeFilter.getValue() != null &&
                    !roomTypeFilter.getValue().equals("All Room Types")) {
                String selectedRoomType = roomTypeFilter.getValue();
                if (reservation.getReservationRooms() == null ||
                        reservation.getReservationRooms().stream()
                                .noneMatch(rr -> rr.getRoom() != null &&
                                        rr.getRoom().getRoomType().getDisplayName().equals(selectedRoomType))) {
                    return false;
                }
            }

            // Date range filter
            LocalDate fromDate = fromDatePicker != null ? fromDatePicker.getValue() : null;
            LocalDate toDate = toDatePicker != null ? toDatePicker.getValue() : null;

            if (fromDate != null && reservation.getCheckInDate() != null) {
                if (reservation.getCheckInDate().isBefore(fromDate)) {
                    return false;
                }
            }

            if (toDate != null && reservation.getCheckInDate() != null) {
                if (reservation.getCheckInDate().isAfter(toDate)) {
                    return false;
                }
            }

            return true;
        };
    }

    private void updateRecordCount() {
        if (totalRecordsLabel != null && filteredReservations != null) {
            int filtered = filteredReservations.size();
            totalRecordsLabel.setText(String.format("(%d found)", filtered));
        }
    }

    private void updatePagination() {
        if (filteredReservations == null) return;

        int totalRecords = filteredReservations.size();
        totalPages = Math.max(1, (int) Math.ceil((double) totalRecords / itemsPerPage));

        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        if (currentPageField != null) currentPageField.setText(String.valueOf(currentPage));
        if (totalPagesLabel != null) totalPagesLabel.setText(String.valueOf(totalPages));
    }

    private Reservation getSelectedReservation() {
        if (reservationsTable != null) {
            return reservationsTable.getSelectionModel().getSelectedItem();
        }
        return null;
    }

    private void logActivity(String action, String entityType, String entityId, String message) {
        if (activityLogger != null) {
            activityLogger.logActivity(adminSession.getActorName(), action, entityType, entityId, message);
        }
    }

    // ==================== Event Handlers ====================

    @FXML
    public void handleSearch(ActionEvent event) {
        applyFilters();
        logActivity("SEARCH", "RESERVATION", "N/A", "Search performed: " + (searchField != null ? searchField.getText() : ""));
    }

    @FXML
    public void handleFilterChange(ActionEvent event) {
        applyFilters();
        LOGGER.fine("Filters updated");
    }

    @FXML
    public void handleClearFilters(ActionEvent event) {
        if (searchField != null) searchField.clear();
        if (statusFilter != null) statusFilter.getSelectionModel().selectFirst();
        if (roomTypeFilter != null) roomTypeFilter.getSelectionModel().selectFirst();
        if (fromDatePicker != null) fromDatePicker.setValue(null);
        if (toDatePicker != null) toDatePicker.setValue(null);

        applyFilters();
        LOGGER.info("Filters cleared");
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        loadReservations();
        updateDateTime();
        LOGGER.info("Data refreshed");
    }

    @FXML
    public void handleExport(ActionEvent event) {
        if (filteredReservations == null || filteredReservations.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Data", "No reservations to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Reservations");
        fileChooser.setInitialFileName("reservations_" + LocalDate.now() + ".csv");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        Stage stage = (Stage) reservationsTable.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("Confirmation #,Guest Name,Email,Phone,Check-In,Check-Out,Rooms,Status,Total,Balance");
                for (Reservation res : filteredReservations) {
                    writer.printf("%s,%s,%s,%s,%s,%s,%d,%s,%.2f,%.2f%n",
                            res.getConfirmationNumber() != null ? res.getConfirmationNumber() : "",
                            res.getGuest() != null ? res.getGuest().getFullName() : "",
                            res.getGuest() != null && res.getGuest().getEmail() != null ? res.getGuest().getEmail() : "",
                            res.getGuest() != null && res.getGuest().getPhone() != null ? res.getGuest().getPhone() : "",
                            res.getCheckInDate() != null ? res.getCheckInDate().format(DATE_FORMAT) : "",
                            res.getCheckOutDate() != null ? res.getCheckOutDate().format(DATE_FORMAT) : "",
                            res.getReservationRooms() != null ? res.getReservationRooms().size() : 0,
                            res.getStatus() != null ? res.getStatus().getDisplayName() : "",
                            res.getTotalAmount() != null ? res.getTotalAmount() : BigDecimal.ZERO,
                            res.getOutstandingBalance() != null ? res.getOutstandingBalance() : BigDecimal.ZERO
                    );
                }
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", "Reservations exported to: " + file.getName());
                logActivity("EXPORT", "RESERVATION", "N/A", "Exported " + filteredReservations.size() + " reservations to CSV");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to export reservations", e);
                showAlert(Alert.AlertType.ERROR, "Export Failed", "Failed to export: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleReservationsTab(ActionEvent event) {
        loadReservations();
        LOGGER.info("Reservations tab selected");
    }

    @FXML
    public void handleNewReservation(ActionEvent event) {
        LOGGER.info("Opening new reservation form");
        adminSession.clearCurrentReservation();
        navigationService.goToAdminReservationForm();
        logActivity("CREATE_RESERVATION_START", "RESERVATION", "N/A", "Started new reservation");
    }

    @FXML
    public void handleViewDetails(ActionEvent event) {
        Reservation selected = getSelectedReservation();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a reservation to view.");
            return;
        }
        LOGGER.info("Viewing reservation details: " + selected.getConfirmationNumber());
        adminSession.setCurrentReservation(selected);
        navigateToScreen("/com/hanson/hotelreservationsystem/admin/adminReservationDetails.fxml");
        logActivity("VIEW_RESERVATION", "RESERVATION", selected.getConfirmationNumber(), "Viewed reservation details");
    }

    @FXML
    public void handleProcessPayment(ActionEvent event) {
        Reservation selected = getSelectedReservation();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a reservation.");
            return;
        }
        if (selected.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            showAlert(Alert.AlertType.INFORMATION, "Fully Paid", "This reservation has no outstanding balance.");
            return;
        }
        LOGGER.info("Opening payment screen for: " + selected.getConfirmationNumber());
        adminSession.setCurrentReservation(selected);
        navigateToScreen("/com/hanson/hotelreservationsystem/admin/adminPayment.fxml");
        logActivity("PAYMENT_START", "RESERVATION", selected.getConfirmationNumber(), "Started payment processing");
    }

    @FXML
    public void handleCheckOut(ActionEvent event) {
        Reservation selected = getSelectedReservation();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a reservation.");
            return;
        }
        if (selected.getStatus() != ReservationStatus.CHECKED_IN) {
            showAlert(Alert.AlertType.WARNING, "Invalid Status", "Only checked-in reservations can be checked out.");
            return;
        }
        if (!selected.isFullyPaid()) {
            showAlert(Alert.AlertType.WARNING, "Outstanding Balance", "Please process payment before checkout. Outstanding: $" + String.format("%.2f", selected.getOutstandingBalance()));
            return;
        }
        LOGGER.info("Opening checkout screen for: " + selected.getConfirmationNumber());
        adminSession.setCurrentReservation(selected);
        navigateToScreen("/com/hanson/hotelreservationsystem/admin/adminCheckout.fxml");
        logActivity("CHECKOUT_START", "RESERVATION", selected.getConfirmationNumber(), "Started checkout process");
    }

    @FXML
    public void handleCancelReservation(ActionEvent event) {
        Reservation selected = getSelectedReservation();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a reservation.");
            return;
        }
        if (selected.getStatus() != ReservationStatus.PENDING && selected.getStatus() != ReservationStatus.CONFIRMED) {
            showAlert(Alert.AlertType.WARNING, "Cannot Cancel", "Only pending or confirmed reservations can be cancelled.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Cancellation");
        confirmAlert.setHeaderText("Cancel Reservation " + selected.getConfirmationNumber() + "?");
        confirmAlert.setContentText("This action cannot be undone. Any payments will need to be refunded manually.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            TextInputDialog reasonDialog = new TextInputDialog();
            reasonDialog.setTitle("Cancellation Reason");
            reasonDialog.setHeaderText("Provide a reason for cancelling:");
            reasonDialog.setContentText("Reason:");

            Optional<String> reasonResult = reasonDialog.showAndWait();

            if (!reasonResult.isPresent() || reasonResult.get().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "No Reason", "Cancellation reason is required.");
                return;
            }

            String reasonText = reasonResult.get().trim();
            try {
                if (reservationService != null) {
                    reservationService.cancelReservation(selected, reasonText);
                }
                showAlert(Alert.AlertType.INFORMATION, "Cancelled", "Reservation " + selected.getConfirmationNumber() + " has been cancelled.");
                loadReservations();
                logActivity("CANCEL_RESERVATION", "RESERVATION", selected.getConfirmationNumber(), "Reservation cancelled: " + reasonText);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to cancel reservation", e);
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to cancel reservation: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleCheckIn(ActionEvent event) {
        Reservation selected = getSelectedReservation();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a reservation.");
            return;
        }
        if (selected.getStatus() != ReservationStatus.CONFIRMED && selected.getStatus() != ReservationStatus.PENDING) {
            showAlert(Alert.AlertType.WARNING, "Invalid Status", "Only confirmed or pending reservations can be checked in.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Check-In");
        confirmAlert.setHeaderText("Check in " + selected.getGuest().getFullName() + "?");
        confirmAlert.setContentText("Reservation: " + selected.getConfirmationNumber());

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (reservationService != null) {
                    reservationService.checkIn(selected);
                } else {
                    selected.checkIn();
                }
                showAlert(Alert.AlertType.INFORMATION, "Checked In", "Guest " + selected.getGuest().getFullName() + " has been checked in.");
                loadReservations();
                logActivity("CHECK_IN", "RESERVATION", selected.getConfirmationNumber(), "Guest checked in: " + selected.getGuest().getFullName());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to check in", e);
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to check in: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        LOGGER.info("Admin logging out");
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Logout");
        confirmAlert.setHeaderText("Are you sure you want to logout?");
        confirmAlert.setContentText("Any unsaved changes will be lost.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            logActivity("LOGOUT", "ADMIN", adminSession.getCurrentAdmin().getUsername(), "Admin logged out");
            adminSession.endSession();
            navigationService.goToWelcome();
        }
    }

    @FXML
    public void handleSettings(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Settings", "Settings functionality will be available in a future update.");
    }

    @FXML
    public void handleUserManagement(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "User Management", "User management functionality will be available in a future update.");
    }

    @FXML
    public void handleOpenWaitlist(ActionEvent event) {
        navigateToScreen("/com/hanson/hotelreservationsystem/admin/adminWaitlist.fxml");
    }

    @FXML
    public void handleOpenLoyalty(ActionEvent event) {
        navigateToScreen("/com/hanson/hotelreservationsystem/admin/adminLoyaltyDashboard.fxml");
    }

    @FXML
    public void handleOpenFeedback(ActionEvent event) {
        navigateToScreen("/com/hanson/hotelreservationsystem/admin/adminFeedbackViewer.fxml");
    }

    @FXML
    public void handleOpenReports(ActionEvent event) {
        navigateToScreen("/com/hanson/hotelreservationsystem/admin/adminReports.fxml");
    }

    // ==================== Pagination Handlers ====================

    @FXML
    public void handleFirstPage(ActionEvent event) {
        currentPage = 1;
        updatePagination();
    }

    @FXML
    public void handlePreviousPage(ActionEvent event) {
        if (currentPage > 1) {
            currentPage--;
            updatePagination();
        }
    }

    @FXML
    public void handleNextPage(ActionEvent event) {
        if (currentPage < totalPages) {
            currentPage++;
            updatePagination();
        }
    }

    @FXML
    public void handleLastPage(ActionEvent event) {
        currentPage = totalPages;
        updatePagination();
    }

    @FXML
    public void handleItemsPerPageChange(ActionEvent event) {
        if (itemsPerPageCombo != null && itemsPerPageCombo.getValue() != null) {
            itemsPerPage = itemsPerPageCombo.getValue();
            currentPage = 1;
            updatePagination();
        }
    }

    // ==================== Helper Methods ====================

    private void navigateToScreen(String fxmlPath) {
        try {
            navigationService.navigateTo(fxmlPath);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Navigation failed: " + fxmlPath, e);
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to open screen: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ==================== Setters ====================

    public void setNavigationService(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    public void setReservationService(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void setAdminSession(AdminSession adminSession) {
        this.adminSession = adminSession;
    }

    public void setActivityLogger(ActivityLogger activityLogger) {
        this.activityLogger = activityLogger;
    }
}