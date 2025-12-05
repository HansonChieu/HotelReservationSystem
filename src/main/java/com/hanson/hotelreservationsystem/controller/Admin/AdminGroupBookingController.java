package com.hanson.hotelreservationsystem.controller.Admin;

import com.hanson.hotelreservationsystem.model.*;
import com.hanson.hotelreservationsystem.model.enums.*;
import com.hanson.hotelreservationsystem.service.*;
import com.hanson.hotelreservationsystem.session.AdminSession;
import com.hanson.hotelreservationsystem.util.ActivityLogger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Admin Group Booking Screen.
 *
 * Handles large group reservations with multiple rooms, special group discounts,
 * and bulk guest management.
 *
 * Responsibilities:
 * - Create group reservations with multiple rooms
 * - Apply group discounts (based on number of rooms)
 * - Manage individual guest details for each room
 * - Calculate group pricing with bulk discounts
 * - Support corporate/event bookings
 *
 * Group Discount Tiers:
 * - 5-9 rooms: 5% discount
 * - 10-19 rooms: 10% discount
 * - 20+ rooms: 15% discount
 */
public class AdminGroupBookingController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(AdminGroupBookingController.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.13");

    // Group discount tiers
    private static final int TIER_1_MIN_ROOMS = 5;
    private static final int TIER_2_MIN_ROOMS = 10;
    private static final int TIER_3_MIN_ROOMS = 20;
    private static final BigDecimal TIER_1_DISCOUNT = new BigDecimal("0.05"); // 5%
    private static final BigDecimal TIER_2_DISCOUNT = new BigDecimal("0.10"); // 10%
    private static final BigDecimal TIER_3_DISCOUNT = new BigDecimal("0.15"); // 15%

    // ==================== Group Info ====================
    @FXML private TextField groupNameField;
    @FXML private TextField organizerNameField;
    @FXML private TextField organizerEmailField;
    @FXML private TextField organizerPhoneField;
    @FXML private ComboBox<String> bookingTypeCombo;
    @FXML private TextArea groupNotesArea;

    // ==================== Stay Details ====================
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private Label nightsLabel;

    // ==================== Room Selection ====================
    @FXML private Spinner<Integer> singleRoomSpinner;
    @FXML private Spinner<Integer> doubleRoomSpinner;
    @FXML private Spinner<Integer> deluxeRoomSpinner;
    @FXML private Spinner<Integer> penthouseSpinner;
    @FXML private Label singleAvailableLabel;
    @FXML private Label doubleAvailableLabel;
    @FXML private Label deluxeAvailableLabel;
    @FXML private Label penthouseAvailableLabel;
    @FXML private Label totalRoomsLabel;
    @FXML private Label totalGuestsLabel;

    // ==================== Guest List ====================
    @FXML private TableView<GuestEntry> guestTable;
    @FXML private TableColumn<GuestEntry, String> guestNameColumn;
    @FXML private TableColumn<GuestEntry, String> guestEmailColumn;
    @FXML private TableColumn<GuestEntry, String> guestPhoneColumn;
    @FXML private TableColumn<GuestEntry, String> roomAssignmentColumn;
    @FXML private Button addGuestButton;
    @FXML private Button removeGuestButton;
    @FXML private Button importGuestsButton;

    // ==================== Pricing Summary ====================
    @FXML private Label roomSubtotalLabel;
    @FXML private Label groupDiscountLabel;
    @FXML private Label groupDiscountAmountLabel;
    @FXML private Spinner<Integer> additionalDiscountSpinner;
    @FXML private Label additionalDiscountAmountLabel;
    @FXML private Label taxLabel;
    @FXML private Label totalLabel;
    @FXML private Label depositRequiredLabel;

    // ==================== Actions ====================
    @FXML private Button createBookingButton;
    @FXML private Button saveQuoteButton;
    @FXML private Button cancelButton;
    @FXML private Label errorLabel;

    // ==================== Data ====================
    private ObservableList<GuestEntry> guestList = FXCollections.observableArrayList();
    private Map<RoomType, Integer> roomAvailability = new HashMap<>();

    // Pricing
    private BigDecimal roomSubtotal = BigDecimal.ZERO;
    private BigDecimal groupDiscountAmount = BigDecimal.ZERO;
    private BigDecimal additionalDiscountAmount = BigDecimal.ZERO;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // ==================== Services ====================
    private NavigationService navigationService;
    private ReservationService reservationService;
    private RoomService roomService;
    private AdminSession adminSession;
    private ActivityLogger activityLogger;

    public AdminGroupBookingController() {
        this.navigationService = NavigationService.getInstance();
        this.reservationService = ReservationService.getInstance();
        this.roomService = RoomService.getInstance();
        this.adminSession = AdminSession.getInstance();
        this.activityLogger = ActivityLogger.getInstance();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("Initializing Admin Group Booking Screen");

        if (!adminSession.isLoggedIn()) {
            Platform.runLater(() -> navigationService.goToAdminLogin());
            return;
        }

        setupBookingTypeCombo();
        setupDatePickers();
        setupRoomSpinners();
        setupDiscountSpinner();
        setupGuestTable();
        loadRoomAvailability();
        updatePricingSummary();
    }

    private void setupBookingTypeCombo() {
        if (bookingTypeCombo != null) {
            bookingTypeCombo.setItems(FXCollections.observableArrayList(
                    "Corporate Event",
                    "Wedding Party",
                    "Conference",
                    "Tour Group",
                    "Sports Team",
                    "Family Reunion",
                    "Other"
            ));
            bookingTypeCombo.getSelectionModel().selectFirst();
        }
    }

    private void setupDatePickers() {
        LocalDate today = LocalDate.now();
        if (checkInPicker != null) {
            checkInPicker.setValue(today);
            checkInPicker.valueProperty().addListener((obs, o, n) -> {
                if (n != null && checkOutPicker.getValue() != null && !checkOutPicker.getValue().isAfter(n)) {
                    checkOutPicker.setValue(n.plusDays(1));
                }
                updateNightsDisplay();
                loadRoomAvailability();
                updatePricingSummary();
            });
        }
        if (checkOutPicker != null) {
            checkOutPicker.setValue(today.plusDays(1));
            checkOutPicker.valueProperty().addListener((obs, o, n) -> {
                updateNightsDisplay();
                loadRoomAvailability();
                updatePricingSummary();
            });
        }
    }

    private void updateNightsDisplay() {
        long nights = calculateNights();
        if (nightsLabel != null) nightsLabel.setText(nights + " night(s)");
    }

    private long calculateNights() {
        LocalDate checkIn = checkInPicker != null ? checkInPicker.getValue() : null;
        LocalDate checkOut = checkOutPicker != null ? checkOutPicker.getValue() : null;
        if (checkIn != null && checkOut != null && checkOut.isAfter(checkIn)) {
            return ChronoUnit.DAYS.between(checkIn, checkOut);
        }
        return 0;
    }

    private void setupRoomSpinners() {
        setupRoomSpinner(singleRoomSpinner, RoomType.SINGLE);
        setupRoomSpinner(doubleRoomSpinner, RoomType.DOUBLE);
        setupRoomSpinner(deluxeRoomSpinner, RoomType.DELUXE);
        setupRoomSpinner(penthouseSpinner, RoomType.PENTHOUSE);
    }

    private void setupRoomSpinner(Spinner<Integer> spinner, RoomType roomType) {
        if (spinner == null) return;
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 50, 0));
        spinner.valueProperty().addListener((obs, o, n) -> {
            updateRoomTotals();
            updatePricingSummary();
        });
    }

    private void setupDiscountSpinner() {
        int maxDiscount = adminSession.getCurrentAdmin() != null
                ? adminSession.getCurrentAdmin().getRole().getMaxDiscountPercentage() : 0;

        if (additionalDiscountSpinner != null) {
            additionalDiscountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, maxDiscount, 0));
            additionalDiscountSpinner.valueProperty().addListener((obs, o, n) -> updatePricingSummary());
        }
    }

    private void setupGuestTable() {
        if (guestTable == null) return;

        if (guestNameColumn != null)
            guestNameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        if (guestEmailColumn != null)
            guestEmailColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        if (guestPhoneColumn != null)
            guestPhoneColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPhone()));
        if (roomAssignmentColumn != null)
            roomAssignmentColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRoomAssignment()));

        guestTable.setItems(guestList);
    }

    private void loadRoomAvailability() {
        roomAvailability.put(RoomType.SINGLE, 20);
        roomAvailability.put(RoomType.DOUBLE, 15);
        roomAvailability.put(RoomType.DELUXE, 10);
        roomAvailability.put(RoomType.PENTHOUSE, 3);

        updateAvailabilityLabels();
    }

    private void updateAvailabilityLabels() {
        if (singleAvailableLabel != null) singleAvailableLabel.setText(roomAvailability.getOrDefault(RoomType.SINGLE, 0) + " available");
        if (doubleAvailableLabel != null) doubleAvailableLabel.setText(roomAvailability.getOrDefault(RoomType.DOUBLE, 0) + " available");
        if (deluxeAvailableLabel != null) deluxeAvailableLabel.setText(roomAvailability.getOrDefault(RoomType.DELUXE, 0) + " available");
        if (penthouseAvailableLabel != null) penthouseAvailableLabel.setText(roomAvailability.getOrDefault(RoomType.PENTHOUSE, 0) + " available");
    }

    private void updateRoomTotals() {
        int totalRooms = getTotalRoomCount();
        int totalGuests = calculateTotalCapacity();

        if (totalRoomsLabel != null) totalRoomsLabel.setText(totalRooms + " room(s)");
        if (totalGuestsLabel != null) totalGuestsLabel.setText("Capacity: " + totalGuests + " guest(s)");
    }

    private int getTotalRoomCount() {
        return getSpinnerValue(singleRoomSpinner) + getSpinnerValue(doubleRoomSpinner)
                + getSpinnerValue(deluxeRoomSpinner) + getSpinnerValue(penthouseSpinner);
    }

    private int calculateTotalCapacity() {
        return getSpinnerValue(singleRoomSpinner) * RoomType.SINGLE.getMaxOccupancy()
                + getSpinnerValue(doubleRoomSpinner) * RoomType.DOUBLE.getMaxOccupancy()
                + getSpinnerValue(deluxeRoomSpinner) * RoomType.DELUXE.getMaxOccupancy()
                + getSpinnerValue(penthouseSpinner) * RoomType.PENTHOUSE.getMaxOccupancy();
    }

    private int getSpinnerValue(Spinner<Integer> spinner) {
        return spinner != null && spinner.getValue() != null ? spinner.getValue() : 0;
    }

    private void updatePricingSummary() {
        long nights = calculateNights();
        if (nights <= 0) {
            clearPricing();
            return;
        }

        // Room subtotal
        roomSubtotal = BigDecimal.ZERO;
        roomSubtotal = roomSubtotal.add(calculateRoomCost(singleRoomSpinner, RoomType.SINGLE, nights));
        roomSubtotal = roomSubtotal.add(calculateRoomCost(doubleRoomSpinner, RoomType.DOUBLE, nights));
        roomSubtotal = roomSubtotal.add(calculateRoomCost(deluxeRoomSpinner, RoomType.DELUXE, nights));
        roomSubtotal = roomSubtotal.add(calculateRoomCost(penthouseSpinner, RoomType.PENTHOUSE, nights));

        // Group discount
        int totalRooms = getTotalRoomCount();
        BigDecimal groupDiscountRate = getGroupDiscountRate(totalRooms);
        groupDiscountAmount = roomSubtotal.multiply(groupDiscountRate).setScale(2, RoundingMode.HALF_UP);

        // Additional discount
        int additionalPct = additionalDiscountSpinner != null ? additionalDiscountSpinner.getValue() : 0;
        BigDecimal afterGroupDiscount = roomSubtotal.subtract(groupDiscountAmount);
        additionalDiscountAmount = afterGroupDiscount.multiply(BigDecimal.valueOf(additionalPct))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Tax and total
        BigDecimal afterAllDiscounts = afterGroupDiscount.subtract(additionalDiscountAmount);
        taxAmount = afterAllDiscounts.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        totalAmount = afterAllDiscounts.add(taxAmount);

        // Update labels
        updatePricingLabels(groupDiscountRate);
    }

    private BigDecimal calculateRoomCost(Spinner<Integer> spinner, RoomType type, long nights) {
        int count = getSpinnerValue(spinner);
        if (count <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(type.getBasePrice()).multiply(BigDecimal.valueOf(count)).multiply(BigDecimal.valueOf(nights));
    }

    private BigDecimal getGroupDiscountRate(int totalRooms) {
        if (totalRooms >= TIER_3_MIN_ROOMS) return TIER_3_DISCOUNT;
        if (totalRooms >= TIER_2_MIN_ROOMS) return TIER_2_DISCOUNT;
        if (totalRooms >= TIER_1_MIN_ROOMS) return TIER_1_DISCOUNT;
        return BigDecimal.ZERO;
    }

    private void updatePricingLabels(BigDecimal groupDiscountRate) {
        if (roomSubtotalLabel != null) roomSubtotalLabel.setText(formatCurrency(roomSubtotal));

        int discountPct = groupDiscountRate.multiply(BigDecimal.valueOf(100)).intValue();
        if (groupDiscountLabel != null) groupDiscountLabel.setText("Group Discount (" + discountPct + "%):");
        if (groupDiscountAmountLabel != null) groupDiscountAmountLabel.setText("-" + formatCurrency(groupDiscountAmount));
        if (additionalDiscountAmountLabel != null) additionalDiscountAmountLabel.setText("-" + formatCurrency(additionalDiscountAmount));
        if (taxLabel != null) taxLabel.setText(formatCurrency(taxAmount));
        if (totalLabel != null) totalLabel.setText(formatCurrency(totalAmount));

        // Deposit (20% for groups)
        BigDecimal deposit = totalAmount.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
        if (depositRequiredLabel != null) depositRequiredLabel.setText("Deposit (20%): " + formatCurrency(deposit));
    }

    private void clearPricing() {
        if (roomSubtotalLabel != null) roomSubtotalLabel.setText("$0.00");
        if (groupDiscountAmountLabel != null) groupDiscountAmountLabel.setText("-$0.00");
        if (additionalDiscountAmountLabel != null) additionalDiscountAmountLabel.setText("-$0.00");
        if (taxLabel != null) taxLabel.setText("$0.00");
        if (totalLabel != null) totalLabel.setText("$0.00");
        if (depositRequiredLabel != null) depositRequiredLabel.setText("Deposit: $0.00");
    }

    // ==================== Event Handlers ====================

    @FXML
    public void handleAddGuest(ActionEvent event) {
        Dialog<GuestEntry> dialog = createGuestDialog(null);
        Optional<GuestEntry> result = dialog.showAndWait();
        result.ifPresent(guest -> guestList.add(guest));
    }

    @FXML
    public void handleRemoveGuest(ActionEvent event) {
        GuestEntry selected = guestTable.getSelectionModel().getSelectedItem();
        if (selected != null) guestList.remove(selected);
    }

    @FXML
    public void handleImportGuests(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Import", "CSV import feature - upload guest list from file.");
    }

    private Dialog<GuestEntry> createGuestDialog(GuestEntry existing) {
        Dialog<GuestEntry> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Guest" : "Edit Guest");

        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        TextField emailField = new TextField(existing != null ? existing.getEmail() : "");
        TextField phoneField = new TextField(existing != null ? existing.getPhone() : "");
        TextField roomField = new TextField(existing != null ? existing.getRoomAssignment() : "");

        nameField.setPromptText("Full Name");
        emailField.setPromptText("Email");
        phoneField.setPromptText("Phone");
        roomField.setPromptText("Room Assignment (optional)");

        dialog.getDialogPane().setContent(new javafx.scene.layout.VBox(10, nameField, emailField, phoneField, roomField));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return new GuestEntry(nameField.getText(), emailField.getText(), phoneField.getText(), roomField.getText());
            }
            return null;
        });

        return dialog;
    }

    @FXML
    public void handleCreateBooking(ActionEvent event) {
        if (!validateForm()) return;

        try {
            String groupName = groupNameField != null ? groupNameField.getText().trim() : "Group Booking";
            int totalRooms = getTotalRoomCount();

            logActivity("CREATE_GROUP_BOOKING", "GROUP_RESERVATION", groupName,
                    "Created group booking: " + totalRooms + " rooms, " + formatCurrency(totalAmount));

            showAlert(Alert.AlertType.INFORMATION, "Group Booking Created",
                    "Group: " + groupName + "\n" +
                            "Rooms: " + totalRooms + "\n" +
                            "Total: " + formatCurrency(totalAmount) + "\n" +
                            "Deposit Required: " + formatCurrency(totalAmount.multiply(new BigDecimal("0.20"))));

            navigationService.navigateTo("/com/hanson/hotelreservationsystem/admin/adminDashboard.fxml");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create group booking", e);
            showError("Failed to create booking: " + e.getMessage());
        }
    }

    @FXML
    public void handleSaveQuote(ActionEvent event) {
        String groupName = groupNameField != null ? groupNameField.getText().trim() : "Group Quote";
        logActivity("SAVE_QUOTE", "QUOTE", groupName, "Saved quote: " + formatCurrency(totalAmount));
        showAlert(Alert.AlertType.INFORMATION, "Quote Saved", "Quote saved for " + groupName);
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        navigationService.navigateTo("/com/hanson/hotelreservationsystem/admin/adminDashboard.fxml");
    }

    private boolean validateForm() {
        clearError();

        if (groupNameField == null || groupNameField.getText().trim().isEmpty()) {
            showError("Group name is required.");
            return false;
        }
        if (organizerEmailField == null || organizerEmailField.getText().trim().isEmpty()) {
            showError("Organizer email is required.");
            return false;
        }
        if (getTotalRoomCount() == 0) {
            showError("Please select at least one room.");
            return false;
        }
        return true;
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("$%,.2f", amount != null ? amount : BigDecimal.ZERO);
    }

    private void showError(String message) {
        if (errorLabel != null) { errorLabel.setText(message); errorLabel.setVisible(true); }
    }

    private void clearError() {
        if (errorLabel != null) errorLabel.setVisible(false);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void logActivity(String action, String entityType, String entityId, String message) {
        if (activityLogger != null) activityLogger.logActivity(adminSession.getActorName(), action, entityType, entityId, message);
    }

    // ==================== Guest Entry Class ====================

    public static class GuestEntry {
        private String name, email, phone, roomAssignment;

        public GuestEntry(String name, String email, String phone, String roomAssignment) {
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.roomAssignment = roomAssignment;
        }

        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getRoomAssignment() { return roomAssignment; }
    }

    // Setters
    public void setNavigationService(NavigationService nav) { this.navigationService = nav; }
    public void setReservationService(ReservationService svc) { this.reservationService = svc; }
    public void setRoomService(RoomService svc) { this.roomService = svc; }
    public void setAdminSession(AdminSession session) { this.adminSession = session; }
    public void setActivityLogger(ActivityLogger logger) { this.activityLogger = logger; }
}
