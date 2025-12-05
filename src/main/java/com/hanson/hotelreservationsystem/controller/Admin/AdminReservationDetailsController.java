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
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for Admin Reservation Details.
 * * UPDATED:
 * - Syncs with ReservationAddOn Entity (uses getUnitPrice, getTotalPrice).
 * - Displays Services/Add-ons correctly.
 * - Calculates Final Billing Summary (Subtotal, Tax, Total, Paid, Balance).
 */
public class AdminReservationDetailsController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(AdminReservationDetailsController.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.13"); // 13% Tax

    // ==================== UI Controls ====================
    // Header
    @FXML private Label statusLabel;

    // Guest Info
    @FXML private Label guestNameLabel;
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;

    // Reservation Info
    @FXML private Label confirmationLabel;
    @FXML private Label checkInLabel;
    @FXML private Label checkOutLabel;
    @FXML private Label roomsLabel;

    // Room Table
    @FXML private TableView<ReservationRoom> roomsTable;
    @FXML private TableColumn<ReservationRoom, String> roomTypeColumn;
    @FXML private TableColumn<ReservationRoom, String> roomNumberColumn;
    @FXML private TableColumn<ReservationRoom, String> rateColumn;
    @FXML private TableColumn<ReservationRoom, String> nightsColumn;
    @FXML private TableColumn<ReservationRoom, String> subtotalColumn;

    // Services (Add-ons) Table
    @FXML private TableView<ReservationAddOn> servicesTable;
    @FXML private TableColumn<ReservationAddOn, String> serviceNameColumn;
    @FXML private TableColumn<ReservationAddOn, String> serviceDateColumn;
    @FXML private TableColumn<ReservationAddOn, String> serviceCostColumn;
    @FXML private TableColumn<ReservationAddOn, String> serviceQtyColumn;
    @FXML private TableColumn<ReservationAddOn, String> serviceTotalColumn;

    // Financial Summary
    @FXML private Label subtotalLabel;
    @FXML private Label taxLabel;
    @FXML private Label discountLabel;
    @FXML private Label totalLabel;
    @FXML private Label paidLabel;
    @FXML private Label balanceLabel;

    // ==================== Data & Services ====================
    private Reservation currentReservation;
    private final ObservableList<ReservationRoom> roomsList = FXCollections.observableArrayList();
    private final ObservableList<ReservationAddOn> servicesList = FXCollections.observableArrayList();

    private final NavigationService navigationService;
    private final ReservationService reservationService;
    private final AdminSession adminSession;
    private final ActivityLogger activityLogger;

    // Default Constructor (Required by FXML Loader)
    public AdminReservationDetailsController() {
        this.navigationService = NavigationService.getInstance();
        this.reservationService = ReservationService.getInstance();
        this.adminSession = AdminSession.getInstance();
        this.activityLogger = ActivityLogger.getInstance();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!adminSession.isLoggedIn()) {
            Platform.runLater(navigationService::goToAdminLogin);
            return;
        }

        currentReservation = adminSession.getCurrentReservation();
        if (currentReservation == null) {
            LOGGER.warning("No reservation found in session. Redirecting.");
            Platform.runLater(this::handleBack);
            return;
        }

        setupRoomTable();
        setupServiceTable();
        loadReservationData();
    }

    // ==================== Setup Methods ====================

    private void setupRoomTable() {
        // Room Type
        roomTypeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getRoom().getRoomType().getDisplayName()));

        // Room Number
        roomNumberColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getRoom().getRoomNumber()));

        // Rate per Night
        rateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatCurrency(cellData.getValue().getRoomPrice()) + "/night"));

        // Nights Count
        nightsColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(currentReservation.getNumberOfNights())));

        // Room Subtotal
        subtotalColumn.setCellValueFactory(cellData -> {
            BigDecimal price = cellData.getValue().getRoomPrice();
            BigDecimal subtotal = price.multiply(new BigDecimal(currentReservation.getNumberOfNights()));
            return new SimpleStringProperty(formatCurrency(subtotal));
        });

        roomsTable.setItems(roomsList);
    }

    private void setupServiceTable() {
        // 1. Service Name (From Enum)
        serviceNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getAddOnType().getDisplayName()));

        // 2. Date Added (From Entity field)
        serviceDateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDateAdded() != null) {
                return new SimpleStringProperty(cellData.getValue().getDateAdded().format(DATE_FORMAT));
            }
            return new SimpleStringProperty("-");
        });

        // 3. Unit Price (Locked price from DB)
        serviceCostColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatCurrency(cellData.getValue().getUnitPrice())));

        // 4. Quantity
        serviceQtyColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getQuantity())));

        // 5. Total Price (Stored total from DB)
        serviceTotalColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatCurrency(cellData.getValue().getTotalPrice())));

        servicesTable.setItems(servicesList);
    }

    private void loadReservationData() {
        updateBasicInfo();

        // Load Rooms
        roomsList.clear();
        if (currentReservation.getReservationRooms() != null) {
            roomsList.addAll(currentReservation.getReservationRooms());
        }

        // Load Services
        servicesList.clear();
        // NOTE: Ensure your Reservation entity has a getAddOns() method
        if (currentReservation.getAddOns() != null) {
            servicesList.addAll(currentReservation.getAddOns());
        }

        calculateFinancials();
    }

    private void updateBasicInfo() {
        // Status with Styling
        statusLabel.setText(currentReservation.getStatus().getDisplayName());
        statusLabel.getStyleClass().removeAll("status-pending", "status-confirmed", "status-checked-in", "status-checked-out", "status-cancelled");
        String statusClass = "status-" + currentReservation.getStatus().name().toLowerCase().replace("_", "-");
        statusLabel.getStyleClass().add(statusClass);

        // Guest Info
        Guest guest = currentReservation.getGuest();
        guestNameLabel.setText(guest != null ? guest.getFullName() : "N/A");
        emailLabel.setText(guest != null ? guest.getEmail() : "N/A");
        phoneLabel.setText(guest != null ? guest.getPhone() : "N/A");

        // Dates
        confirmationLabel.setText(currentReservation.getConfirmationNumber());
        checkInLabel.setText(currentReservation.getCheckInDate().format(DATE_FORMAT));
        checkOutLabel.setText(currentReservation.getCheckOutDate().format(DATE_FORMAT));

        // Room Count Label
        String roomCount = (currentReservation.getReservationRooms() != null)
                ? String.valueOf(currentReservation.getReservationRooms().size())
                : "0";
        roomsLabel.setText(roomCount + " Room(s)");
    }

    // ==================== Financial Logic ====================

    private void calculateFinancials() {
        BigDecimal subtotal = BigDecimal.ZERO;

        // 1. Add Room Costs
        for (ReservationRoom rr : roomsList) {
            BigDecimal roomTotal = rr.getRoomPrice().multiply(new BigDecimal(currentReservation.getNumberOfNights()));
            subtotal = subtotal.add(roomTotal);
        }

        // 2. Add Service Costs (Using stored TotalPrice from Entity)
        for (ReservationAddOn service : servicesList) {
            if (service.getTotalPrice() != null) {
                subtotal = subtotal.add(service.getTotalPrice());
            }
        }

        // 3. Calculate Tax
        BigDecimal tax = subtotal.multiply(TAX_RATE);

        // 4. Apply Discount
        BigDecimal discount = currentReservation.getDiscountAmount();
        if (discount == null) discount = BigDecimal.ZERO;

        // 5. Calculate Grand Total
        BigDecimal total = subtotal.add(tax).subtract(discount);

        // 6. Calculate Paid Amount
        BigDecimal paid = BigDecimal.ZERO;
        if (currentReservation.getPayments() != null) {
            for (Payment p : currentReservation.getPayments()) {
                paid = paid.add(p.getAmount());
            }
        }

        // 7. Calculate Balance Due
        BigDecimal balance = total.subtract(paid);

        // Update UI Labels
        subtotalLabel.setText(formatCurrency(subtotal));
        taxLabel.setText(formatCurrency(tax));
        discountLabel.setText("-" + formatCurrency(discount));
        totalLabel.setText(formatCurrency(total));
        paidLabel.setText(formatCurrency(paid));
        balanceLabel.setText(formatCurrency(balance));
    }

    // ==================== Action Handlers ====================

    @FXML
    public void handleEdit(ActionEvent event) {
        adminSession.setCurrentReservation(currentReservation);
        navigationService.goToAdminReservationForm();
    }

    @FXML
    public void handlePayment(ActionEvent event) {
        adminSession.setCurrentReservation(currentReservation);
        navigationService.goToAdminPayment();
    }

    @FXML
    public void handleCheckout(ActionEvent event) {
        if (currentReservation.getStatus() != ReservationStatus.CHECKED_IN) {
            showAlert(Alert.AlertType.WARNING, "Invalid Status",
                    "Only checked-in guests can be checked out.");
            return;
        }

        // Re-calculate balance for validation
        // (Using logic mirroring calculateFinancials or calling model method)
        BigDecimal balance = currentReservation.getOutstandingBalance();

        // Allow a small threshold for floating point errors if necessary, or strict ZERO
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            showAlert(Alert.AlertType.ERROR, "Checkout Blocked",
                    "Balance Due: " + formatCurrency(balance) + "\nGuest must settle the bill before checkout.");
            return;
        }

        adminSession.setCurrentReservation(currentReservation);
        navigationService.goToAdminCheckout();
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        if (currentReservation.getStatus() == ReservationStatus.CHECKED_OUT
                || currentReservation.getStatus() == ReservationStatus.CANCELLED) {
            showAlert(Alert.AlertType.WARNING, "Action Not Allowed", "Cannot cancel this reservation.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to cancel this reservation?", ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    // CORRECT FIX: Use the service method.
                    // This handles room release, point refunds, AND saving.
                    reservationService.cancelReservation(currentReservation, "Cancelled by Admin");

                    // Log the activity
                    activityLogger.logActivity(adminSession.getActorName(), "CANCEL", "RESERVATION",
                            currentReservation.getConfirmationNumber(), "Reservation cancelled by admin");

                    loadReservationData(); // Refresh UI to show 'Cancelled'
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Reservation cancelled.");
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error cancelling reservation", e);
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to cancel reservation: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    public void handleBack(ActionEvent event) {
        handleBack(); // Overload
    }

    // Internal helper for back navigation
    private void handleBack() {
        adminSession.clearCurrentReservation();
        navigationService.goToAdminDashboard();
    }

    // ==================== Helpers ====================

    private String formatCurrency(BigDecimal amount) {
        return String.format("$%.2f", amount != null ? amount : BigDecimal.ZERO);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}