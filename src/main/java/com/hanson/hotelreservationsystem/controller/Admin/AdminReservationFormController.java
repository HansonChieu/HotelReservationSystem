package com.hanson.hotelreservationsystem.controller.Admin;

import com.hanson.hotelreservationsystem.model.*;
import com.hanson.hotelreservationsystem.model.enums.AddOnType;
import com.hanson.hotelreservationsystem.model.enums.ReservationStatus;
import com.hanson.hotelreservationsystem.model.enums.Role;
import com.hanson.hotelreservationsystem.model.enums.RoomType;
import com.hanson.hotelreservationsystem.service.*;
import com.hanson.hotelreservationsystem.session.AdminSession;
import com.hanson.hotelreservationsystem.util.ActivityLogger;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Admin Reservation Form Screen.
 *
 * Responsibilities:
 * - Create new reservations (via phone booking)
 * - Edit existing reservations
 * - Search for existing guests
 * - Validate occupancy rules and room availability
 * - Calculate dynamic pricing with add-ons
 * - Support group bookings with multiple rooms
 *
 * Business Rules Enforced:
 * - Single room: max 2 people
 * - Double room: max 4 people
 * - Deluxe/Penthouse: max 2 people
 * - Check-in date must be today or future
 * - Check-out must be after check-in
 * - At least one room must be selected
 * - At least one adult required
 *
 * Patterns Used:
 * - MVC: Controller mediates between FXML view and service layer
 * - Dependency Injection: Services injected via setters or constructor
 */
public class AdminReservationFormController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(AdminReservationFormController.class.getName());

    // Tax rate (13% as per assignment)
    private static final BigDecimal TAX_RATE = new BigDecimal("0.13");

    // ==================== FXML Fields ====================

    // --- Form Header ---
    @FXML private Label formTitle;
    @FXML private Label reservationIdLabel;

    // --- Guest Information ---
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField idNumberField;
    @FXML private TextField countryField;
    @FXML private TextField stateField;
    @FXML private TextField postalCodeField;

    // --- Guest Information Error Labels ---
    @FXML private Label firstNameError;
    @FXML private Label lastNameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;

    // --- Guest Search ---
    @FXML private Label existingGuestLabel;

    // --- Reservation Details ---
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private Label checkInError;
    @FXML private Label checkOutError;
    @FXML private Label nightsLabel;
    @FXML private Spinner<Integer> adultsSpinner;
    @FXML private Spinner<Integer> childrenSpinner;
    @FXML private Label totalGuestsLabel;

    // --- Room Selection ---
    @FXML private Spinner<Integer> singleRoomSpinner;
    @FXML private Spinner<Integer> doubleRoomSpinner;
    @FXML private Spinner<Integer> deluxeRoomSpinner;
    @FXML private Spinner<Integer> penthouseSpinner;
    @FXML private VBox selectedRoomsBox;
    @FXML private Label availabilityLabel;
    @FXML private Label noRoomsLabel;

    // --- Add-On Services ---
    @FXML private CheckBox wifiCheckbox;
    @FXML private CheckBox breakfastCheckbox;
    @FXML private CheckBox parkingCheckbox;
    @FXML private CheckBox spaCheckbox;

    // --- Special Requests ---
    @FXML private TextArea specialRequestsArea;

    // --- Discount ---
    @FXML private Label maxDiscountLabel;
    @FXML private Label discountAmountLabel;
    @FXML private Spinner<Integer> discountSpinner;
    private int maxDiscountAllowed = 0;

    // --- Pricing Summary ---
    @FXML private Label roomChargesLabel;
    @FXML private Label servicesChargesLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label taxLabel;
    @FXML private Label totalLabel;

    // --- Action Buttons ---
    @FXML private Button submitButton;

    // ==================== Services ====================

    private NavigationService navigationService;
    private ReservationService reservationService;
    private RoomService roomService;
    private PricingService pricingService;
    private AdminSession adminSession;
    private ActivityLogger activityLogger;

    // ==================== State ====================

    private Reservation currentReservation;
    private Guest currentGuest;
    private boolean isEditMode = false;

    // Room pricing (base prices per night)
    private static final BigDecimal SINGLE_ROOM_PRICE = new BigDecimal("100.00");
    private static final BigDecimal DOUBLE_ROOM_PRICE = new BigDecimal("150.00");
    private static final BigDecimal DELUXE_ROOM_PRICE = new BigDecimal("250.00");
    private static final BigDecimal PENTHOUSE_PRICE = new BigDecimal("500.00");

    // Add-on pricing
    private static final BigDecimal WIFI_PRICE_PER_NIGHT = new BigDecimal("15.00");
    private static final BigDecimal BREAKFAST_PRICE_PER_PERSON_NIGHT = new BigDecimal("25.00");
    private static final BigDecimal PARKING_PRICE_PER_NIGHT = new BigDecimal("20.00");
    private static final BigDecimal SPA_PRICE_PER_PERSON = new BigDecimal("75.00");

    // Validation patterns
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    private static final String PHONE_PATTERN = "^[\\d\\s()+-]{7,20}$";
    private static final String NAME_PATTERN = "^[A-Za-z\\s'-]{1,50}$";


    /**
     * Default constructor for FXML loader.
     */
    public AdminReservationFormController() {
        this.navigationService = NavigationService.getInstance();
        this.reservationService = ReservationService.getInstance();
        this.roomService = RoomService.getInstance();
        this.pricingService = PricingService.getInstance();
        this.adminSession = AdminSession.getInstance();
        this.activityLogger = ActivityLogger.getInstance();
    }

    /**
     * Constructor with dependency injection for testing.
     */
    public AdminReservationFormController(NavigationService navigationService,
                                          ReservationService reservationService,
                                          RoomService roomService,
                                          PricingService pricingService,
                                          AdminSession adminSession,
                                          ActivityLogger activityLogger) {
        this.navigationService = navigationService;
        this.reservationService = reservationService;
        this.roomService = roomService;
        this.pricingService = pricingService;
        this.adminSession = adminSession;
        this.activityLogger = activityLogger;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("Initializing Admin Reservation Form");
        Role role = AdminSession.getInstance().getCurrentAdmin().getRole();

        // Verify admin session
        if (!adminSession.isLoggedIn()) {
            LOGGER.warning("No admin session - redirecting to login");
            Platform.runLater(() -> navigationService.goToAdminLogin());
            return;
        }

        setupSpinners();
        setupDatePickers();
        setupListeners();
        hideAllErrors();

        this.maxDiscountAllowed = role.getMaxDiscountPercentage();
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, maxDiscountAllowed, 0);
        discountSpinner.setValueFactory(valueFactory);

        // Check if editing existing reservation
        currentReservation = adminSession.getCurrentReservation();
        if (currentReservation != null) {
            isEditMode = true;
            populateFormForEdit();
        } else {
            isEditMode = false;
            formTitle.setText("New Reservation");
            submitButton.setText("Create Reservation");
        }

        // Initial pricing calculation
        calculatePricing();
        setupDiscountControls();
    }

    /**
     * Setup spinner value factories and constraints.
     */
    private void setupSpinners() {
        // Guest count spinners
        adultsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1));
        childrenSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 20, 0));

        // Room count spinners
        singleRoomSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, 0));
        doubleRoomSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, 0));
        deluxeRoomSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, 0));
        penthouseSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5, 0));

        // Make spinners editable but validate input
        setupSpinnerEditor(adultsSpinner);
        setupSpinnerEditor(childrenSpinner);
        setupSpinnerEditor(singleRoomSpinner);
        setupSpinnerEditor(doubleRoomSpinner);
        setupSpinnerEditor(deluxeRoomSpinner);
        setupSpinnerEditor(penthouseSpinner);
    }

    /**
     * Setup spinner editor to handle manual input.
     */
    private void setupSpinnerEditor(Spinner<Integer> spinner) {
        spinner.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                spinner.getEditor().setText(oldVal);
            }
        });

        spinner.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                try {
                    Integer value = Integer.parseInt(spinner.getEditor().getText());
                    spinner.getValueFactory().setValue(value);
                } catch (NumberFormatException e) {
                    spinner.getEditor().setText(String.valueOf(spinner.getValue()));
                }
            }
        });
    }

    /**
     * Automatically configures the discount limit based on the logged-in user's role.
     */
    private void setupDiscountControls() {
        // 1. Get current user from Session
        Admin currentUser = adminSession.getCurrentAdmin();
        if (currentUser == null) return; // Safety check

        // 2. Get Role and Limit from your Enum
        Role role = currentUser.getRole();
        int maxAllowed = role.getMaxDiscountPercentage(); // Returns 15 or 30 based on your Enum

        // 3. Configure Spinner Limits
        // The Spinner will physically prevent entering a number higher than 'maxAllowed'
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, maxAllowed, 0);

        discountSpinner.setValueFactory(valueFactory);

        // 4. Update UI Label to inform user
        // e.g., "Administrator Limit: 15%"
        maxDiscountLabel.setText(String.format("%s Limit: %d%%", role.getDisplayName(), maxAllowed));

        // 5. Add listener to recalculate totals immediately when spinner changes
        discountSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            calculatePricing();
        });
    }

    /**
     * Setup date pickers with constraints.
     */
    private void setupDatePickers() {
        // Disable past dates for check-in
        checkInPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        // Check-out date cell factory will be updated when check-in changes
        updateCheckOutDatePicker();
    }

    /**
     * Update check-out date picker to disable dates before check-in.
     */
    private void updateCheckOutDatePicker() {
        checkOutPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate checkIn = checkInPicker.getValue();
                if (checkIn != null) {
                    setDisable(empty || date.isBefore(checkIn.plusDays(1)));
                } else {
                    setDisable(empty || date.isBefore(LocalDate.now().plusDays(1)));
                }
            }
        });
    }

    /**
     * Setup change listeners for dynamic updates.
     */
    private void setupListeners() {
        // Guest count listeners
        adultsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateTotalGuests();
            validateOccupancySilent();
            calculatePricing();
        });

        childrenSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateTotalGuests();
            validateOccupancySilent();
            calculatePricing();
        });

        // Room spinner listeners
        singleRoomSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateRoomSelection();
            validateOccupancySilent();
            calculatePricing();
        });

        doubleRoomSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateRoomSelection();
            validateOccupancySilent();
            calculatePricing();
        });

        deluxeRoomSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateRoomSelection();
            validateOccupancySilent();
            calculatePricing();
        });

        penthouseSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateRoomSelection();
            validateOccupancySilent();
            calculatePricing();
        });
    }

    /**
     * Hide all error labels.
     */
    private void hideAllErrors() {
        if (firstNameError != null) firstNameError.setVisible(false);
        if (lastNameError != null) lastNameError.setVisible(false);
        if (emailError != null) emailError.setVisible(false);
        if (phoneError != null) phoneError.setVisible(false);
        if (checkInError != null) checkInError.setVisible(false);
        if (checkOutError != null) checkOutError.setVisible(false);
        if (noRoomsLabel != null) noRoomsLabel.setVisible(false);
        if (existingGuestLabel != null) existingGuestLabel.setText("");
        if (availabilityLabel != null) availabilityLabel.setText("");
    }

    /**
     * Populate form fields for editing an existing reservation.
     */
    private void populateFormForEdit() {
        if (currentReservation == null) return;

        formTitle.setText("Edit Reservation");
        reservationIdLabel.setText("ID: " + currentReservation.getConfirmationNumber());
        submitButton.setText("Update Reservation");

        // Populate guest information
        Guest guest = currentReservation.getGuest();
        if (guest != null) {
            currentGuest = guest;
            firstNameField.setText(guest.getFirstName());
            lastNameField.setText(guest.getLastName());
            emailField.setText(guest.getEmail());
            phoneField.setText(guest.getPhone());
            if (idNumberField != null && guest.getIdNumber() != null) {
                idNumberField.setText(guest.getIdNumber());
            }
            if (countryField != null && guest.getCountry() != null) {
                countryField.setText(guest.getCountry());
            }
            if (stateField != null && guest.getStateProvince() != null) {
                stateField.setText(guest.getStateProvince());
            }
            if (postalCodeField != null && guest.getPostalCode() != null) {
                postalCodeField.setText(guest.getPostalCode());
            }
        }

        // Populate dates
        checkInPicker.setValue(currentReservation.getCheckInDate());
        checkOutPicker.setValue(currentReservation.getCheckOutDate());

        // Populate guest counts
        adultsSpinner.getValueFactory().setValue(currentReservation.getNumAdults());
        childrenSpinner.getValueFactory().setValue(currentReservation.getNumChildren());

        // Populate room selections
        populateRoomSelections();

        // Populate add-ons
        populateAddOns();

        // Populate special requests
        if (specialRequestsArea != null && currentReservation.getSpecialRequests() != null) {
            specialRequestsArea.setText(currentReservation.getSpecialRequests());
        }

        // Update displays
        updateNightsLabel();
        updateTotalGuests();
        calculatePricing();
    }

    /**
     * Populate room spinners from existing reservation.
     */
    private void populateRoomSelections() {
        if (currentReservation.getReservationRooms() == null) return;

        int singleCount = 0, doubleCount = 0, deluxeCount = 0, penthouseCount = 0;

        for (ReservationRoom rr : currentReservation.getReservationRooms()) {
            if (rr.getRoom() != null) {
                switch (rr.getRoom().getRoomType()) {
                    case SINGLE -> singleCount++;
                    case DOUBLE -> doubleCount++;
                    case DELUXE -> deluxeCount++;
                    case PENTHOUSE -> penthouseCount++;
                }
            }
        }

        singleRoomSpinner.getValueFactory().setValue(singleCount);
        doubleRoomSpinner.getValueFactory().setValue(doubleCount);
        deluxeRoomSpinner.getValueFactory().setValue(deluxeCount);
        penthouseSpinner.getValueFactory().setValue(penthouseCount);
    }

    /**
     * Populate add-on checkboxes from existing reservation.
     */
    private void populateAddOns() {
        if (currentReservation.getAddOns() == null) return;

        for (ReservationAddOn addOn : currentReservation.getAddOns()) {
            if (addOn.getAddOnType() != null) {
                switch (addOn.getAddOnType()) {
                    case WIFI -> wifiCheckbox.setSelected(true);
                    case BREAKFAST -> breakfastCheckbox.setSelected(true);
                    case PARKING -> parkingCheckbox.setSelected(true);
                    case SPA -> spaCheckbox.setSelected(true);
                }
            }
        }
    }

    // ==================== Event Handlers ====================

    /**
     * Handle back button click.
     */
    @FXML
    private void handleBack() {
        LOGGER.info("Navigating back to dashboard");
        adminSession.clearCurrentReservation();
        navigationService.goToAdminDashboard();
    }

    /**
     * Handle search guest button click.
     * Uses ReservationService.findByGuestEmail() to search for existing guests.
     */
    @FXML
    private void handleSearchGuest() {
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        if (email.isEmpty() && phone.isEmpty()) {
            existingGuestLabel.setText("Enter email or phone to search.");
            existingGuestLabel.setStyle("-fx-text-fill: #856404;");
            return;
        }

        try {
            List<Reservation> reservations = null;

            // Search by email using ReservationService.findByGuestEmail()
            if (reservationService != null && !email.isEmpty()) {
                reservations = reservationService.findByGuestEmail(email);
            }

            if (reservations != null && !reservations.isEmpty()) {
                // Get guest from first reservation found
                currentGuest = reservations.get(0).getGuest();
                populateGuestFields(currentGuest);
                existingGuestLabel.setText("✓ Guest found: " + currentGuest.getFullName());
                existingGuestLabel.setStyle("-fx-text-fill: #28a745;");

                logActivity("SEARCH_GUEST", "GUEST", String.valueOf(currentGuest.getId()),
                        "Found existing guest: " + currentGuest.getFullName());
            } else {
                currentGuest = null;
                existingGuestLabel.setText("No existing guest found. New guest will be created.");
                existingGuestLabel.setStyle("-fx-text-fill: #17a2b8;");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error searching for guest", e);
            existingGuestLabel.setText("Error searching: " + e.getMessage());
            existingGuestLabel.setStyle("-fx-text-fill: #dc3545;");
        }
    }

    /**
     * Populate guest fields from found guest.
     */
    private void populateGuestFields(Guest guest) {
        firstNameField.setText(guest.getFirstName());
        lastNameField.setText(guest.getLastName());
        emailField.setText(guest.getEmail());
        phoneField.setText(guest.getPhone());
        if (idNumberField != null && guest.getIdNumber() != null) {
            idNumberField.setText(guest.getIdNumber());
        }
        if (countryField != null && guest.getCountry() != null) {
            countryField.setText(guest.getCountry());
        }
        if (stateField != null && guest.getStateProvince() != null) {
            stateField.setText(guest.getStateProvince());
        }
        if (postalCodeField != null && guest.getPostalCode() != null) {
            postalCodeField.setText(guest.getPostalCode());
        }
    }

    /**
     * Handle check-in date change.
     */
    @FXML
    private void handleCheckInChange() {
        updateCheckOutDatePicker();
        updateNightsLabel();
        calculatePricing();
        clearAvailabilityStatus();
    }

    /**
     * Handle check-out date change.
     */
    @FXML
    private void handleCheckOutChange() {
        updateNightsLabel();
        calculatePricing();
        clearAvailabilityStatus();
    }

    /**
     * Handle check availability button click.
     * Uses RoomService.getAvailableRoomsByType() to check room availability.
     */
    @FXML
    private void handleCheckAvailability() {
        // Validate dates first
        if (checkInPicker.getValue() == null) {
            availabilityLabel.setText("Please select a check-in date.");
            availabilityLabel.setStyle("-fx-text-fill: #dc3545;");
            return;
        }
        if (checkOutPicker.getValue() == null) {
            availabilityLabel.setText("Please select a check-out date.");
            availabilityLabel.setStyle("-fx-text-fill: #dc3545;");
            return;
        }

        // Get requested room counts
        int singleCount = singleRoomSpinner.getValue();
        int doubleCount = doubleRoomSpinner.getValue();
        int deluxeCount = deluxeRoomSpinner.getValue();
        int penthouseCount = penthouseSpinner.getValue();

        if (singleCount + doubleCount + deluxeCount + penthouseCount == 0) {
            availabilityLabel.setText("Please select at least one room.");
            availabilityLabel.setStyle("-fx-text-fill: #dc3545;");
            return;
        }

        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();

        // Check availability using RoomService.getAvailableRoomsByType()
        boolean allAvailable = true;
        StringBuilder message = new StringBuilder();

        if (roomService != null) {
            // Check single rooms
            if (singleCount > 0) {
                List<Room> available = roomService.getAvailableRoomsByType(RoomType.SINGLE, checkIn, checkOut);
                int availableCount = (available != null) ? available.size() : 0;
                if (availableCount < singleCount) {
                    allAvailable = false;
                    message.append(String.format("Single: %d available (requested %d). ", availableCount, singleCount));
                }
            }

            // Check double rooms
            if (doubleCount > 0) {
                List<Room> available = roomService.getAvailableRoomsByType(RoomType.DOUBLE, checkIn, checkOut);
                int availableCount = (available != null) ? available.size() : 0;
                if (availableCount < doubleCount) {
                    allAvailable = false;
                    message.append(String.format("Double: %d available (requested %d). ", availableCount, doubleCount));
                }
            }

            // Check deluxe rooms
            if (deluxeCount > 0) {
                List<Room> available = roomService.getAvailableRoomsByType(RoomType.DELUXE, checkIn, checkOut);
                int availableCount = (available != null) ? available.size() : 0;
                if (availableCount < deluxeCount) {
                    allAvailable = false;
                    message.append(String.format("Deluxe: %d available (requested %d). ", availableCount, deluxeCount));
                }
            }

            // Check penthouse rooms
            if (penthouseCount > 0) {
                List<Room> available = roomService.getAvailableRoomsByType(RoomType.PENTHOUSE, checkIn, checkOut);
                int availableCount = (available != null) ? available.size() : 0;
                if (availableCount < penthouseCount) {
                    allAvailable = false;
                    message.append(String.format("Penthouse: %d available (requested %d). ", availableCount, penthouseCount));
                }
            }
        }

        if (allAvailable) {
            availabilityLabel.setText("✓ All selected rooms are available!");
            availabilityLabel.setStyle("-fx-text-fill: #28a745;");
        } else {
            availabilityLabel.setText("⚠ " + message.toString().trim());
            availabilityLabel.setStyle("-fx-text-fill: #dc3545;");
        }
    }

    /**
     * Handle add-on service checkbox change.
     */
    @FXML
    private void handleServiceChange() {
        calculatePricing();
    }

    /**
     * Handle cancel button click.
     */
    @FXML
    private void handleCancel() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Cancel");
        confirm.setHeaderText("Cancel Reservation Form?");
        confirm.setContentText("Any unsaved changes will be lost.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            adminSession.clearCurrentReservation();
            navigationService.goToAdminDashboard();
        }
    }

    /**
     * Handle save draft button click.
     * Uses ReservationService.updateReservation() to save the draft.
     */
    @FXML
    private void handleSaveDraft() {
        // Validate minimum required fields
        if (firstNameField.getText().trim().isEmpty() || lastNameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Incomplete",
                    "Please enter at least the guest's first and last name to save a draft.");
            return;
        }

        try {
            Reservation draft = buildReservation();
            draft.setStatus(ReservationStatus.PENDING);

            // Use ReservationService.updateReservation() to save
            if (reservationService != null) {
                draft = reservationService.updateReservation(draft);
            }

            showAlert(Alert.AlertType.INFORMATION, "Draft Saved",
                    "Reservation draft saved with ID: " + draft.getConfirmationNumber());

            logActivity("SAVE_DRAFT", "RESERVATION", draft.getConfirmationNumber(),
                    "Saved reservation as draft");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save draft", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save draft: " + e.getMessage());
        }
    }

    /**
     * Handle submit button click.
     * Uses ReservationService.updateReservation() to create/update the reservation.
     */
    @FXML
    private void handleSubmit() {
        if (!validateForm()) {
            showAlert(Alert.AlertType.WARNING, "Validation Failed",
                    "Please correct the errors before submitting.");
            return;
        }

        // Validate occupancy rules
        if (!validateOccupancy()) {
            return;
        }
        if (currentGuest == null) {
            String email = emailField.getText().trim();
            List<Reservation> existing = reservationService.findByGuestEmail(email);
            if (!existing.isEmpty()) {
                // Found a guest with this email!
                currentGuest = existing.get(0).getGuest();
                LOGGER.info("Auto-linked existing guest found by email: " + email);
            }
        }

        try {
            Reservation reservation = buildReservation();
            reservation.setStatus(ReservationStatus.CONFIRMED);

            // Use ReservationService.updateReservation() to save
            if (reservationService != null) {
                reservation = reservationService.updateReservation(reservation);
            }

            String action = isEditMode ? "updated" : "created";
            showAlert(Alert.AlertType.INFORMATION, "Success",
                    String.format("Reservation %s successfully!\nConfirmation #: %s",
                            action, reservation.getConfirmationNumber()));

            logActivity(isEditMode ? "UPDATE_RESERVATION" : "CREATE_RESERVATION",
                    "RESERVATION", reservation.getConfirmationNumber(),
                    "Reservation " + action + " by admin");

            adminSession.setCurrentReservation(reservation);;
            navigationService.goToAdminReservationDetails();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to " + (isEditMode ? "update" : "create") + " reservation", e);
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to " + (isEditMode ? "update" : "create") + " reservation: " + e.getMessage());
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Update nights label based on selected dates.
     */
    private void updateNightsLabel() {
        if (checkInPicker.getValue() != null && checkOutPicker.getValue() != null) {
            long nights = ChronoUnit.DAYS.between(checkInPicker.getValue(), checkOutPicker.getValue());
            if (nights > 0) {
                nightsLabel.setText(nights + " night" + (nights > 1 ? "s" : ""));
            } else {
                nightsLabel.setText("Invalid dates");
            }
        } else {
            nightsLabel.setText("0 nights");
        }
    }

    /**
     * Update total guests label.
     */
    private void updateTotalGuests() {
        int total = adultsSpinner.getValue() + childrenSpinner.getValue();
        totalGuestsLabel.setText(total + " Guest" + (total != 1 ? "s" : ""));
    }

    /**
     * Update room selection display.
     */
    private void updateRoomSelection() {
        int totalRooms = singleRoomSpinner.getValue() +
                doubleRoomSpinner.getValue() +
                deluxeRoomSpinner.getValue() +
                penthouseSpinner.getValue();

        if (totalRooms == 0) {
            noRoomsLabel.setText("No rooms selected");
            noRoomsLabel.setVisible(true);
        } else {
            noRoomsLabel.setVisible(false);
        }
    }

    /**
     * Clear availability status label.
     */
    private void clearAvailabilityStatus() {
        if (availabilityLabel != null) {
            availabilityLabel.setText("");
        }
    }

    /**
     * Calculate and display pricing.
     */
    private void calculatePricing() {
        int nights = getNights();
        if (nights <= 0) {
            resetPricingLabels();
            return;
        }

        // --- 1. Calculate Room Charges ---
        BigDecimal roomCharges = BigDecimal.ZERO;

        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();

        // Use PricingService for dynamic pricing if available
        if (pricingService != null && checkIn != null && checkOut != null) {
            roomCharges = roomCharges.add(
                    pricingService.getAverageNightlyRate(RoomType.SINGLE, checkIn, checkOut)
                            .multiply(BigDecimal.valueOf(singleRoomSpinner.getValue()))
                            .multiply(BigDecimal.valueOf(nights)));
            roomCharges = roomCharges.add(
                    pricingService.getAverageNightlyRate(RoomType.DOUBLE, checkIn, checkOut)
                            .multiply(BigDecimal.valueOf(doubleRoomSpinner.getValue()))
                            .multiply(BigDecimal.valueOf(nights)));
            roomCharges = roomCharges.add(
                    pricingService.getAverageNightlyRate(RoomType.DELUXE, checkIn, checkOut)
                            .multiply(BigDecimal.valueOf(deluxeRoomSpinner.getValue()))
                            .multiply(BigDecimal.valueOf(nights)));
            roomCharges = roomCharges.add(
                    pricingService.getAverageNightlyRate(RoomType.PENTHOUSE, checkIn, checkOut)
                            .multiply(BigDecimal.valueOf(penthouseSpinner.getValue()))
                            .multiply(BigDecimal.valueOf(nights)));
        } else {
            // Fallback to base prices
            roomCharges = roomCharges.add(SINGLE_ROOM_PRICE
                    .multiply(BigDecimal.valueOf(singleRoomSpinner.getValue()))
                    .multiply(BigDecimal.valueOf(nights)));
            roomCharges = roomCharges.add(DOUBLE_ROOM_PRICE
                    .multiply(BigDecimal.valueOf(doubleRoomSpinner.getValue()))
                    .multiply(BigDecimal.valueOf(nights)));
            roomCharges = roomCharges.add(DELUXE_ROOM_PRICE
                    .multiply(BigDecimal.valueOf(deluxeRoomSpinner.getValue()))
                    .multiply(BigDecimal.valueOf(nights)));
            roomCharges = roomCharges.add(PENTHOUSE_PRICE
                    .multiply(BigDecimal.valueOf(penthouseSpinner.getValue()))
                    .multiply(BigDecimal.valueOf(nights)));
        }

        // --- 2. Calculate Service Charges ---
        BigDecimal serviceCharges = BigDecimal.ZERO;
        int totalGuests = adultsSpinner.getValue() + childrenSpinner.getValue();

        if (wifiCheckbox.isSelected()) {
            serviceCharges = serviceCharges.add(WIFI_PRICE_PER_NIGHT.multiply(BigDecimal.valueOf(nights)));
        }
        if (breakfastCheckbox.isSelected()) {
            serviceCharges = serviceCharges.add(BREAKFAST_PRICE_PER_PERSON_NIGHT
                    .multiply(BigDecimal.valueOf(totalGuests))
                    .multiply(BigDecimal.valueOf(nights)));
        }
        if (parkingCheckbox.isSelected()) {
            serviceCharges = serviceCharges.add(PARKING_PRICE_PER_NIGHT.multiply(BigDecimal.valueOf(nights)));
        }
        if (spaCheckbox.isSelected()) {
            serviceCharges = serviceCharges.add(SPA_PRICE_PER_PERSON.multiply(BigDecimal.valueOf(totalGuests)));
        }

        // --- 3. Calculate Subtotal ---
        BigDecimal subtotal = roomCharges.add(serviceCharges);

        // --- 4. NEW: Calculate Discount ---
        int discountPercent = 0;
        if (discountSpinner != null && discountSpinner.getValue() != null) {
            discountPercent = discountSpinner.getValue();
        }

        // Double-check the limit (defensive coding)
        if (discountPercent > maxDiscountAllowed) {
            discountPercent = maxDiscountAllowed;
        }

        BigDecimal discountAmount = subtotal.multiply(new BigDecimal(discountPercent))
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);

        // --- 5. Calculate Taxable Amount (Subtotal - Discount) ---
        BigDecimal taxableAmount = subtotal.subtract(discountAmount);

        // Prevent negative totals
        if (taxableAmount.compareTo(BigDecimal.ZERO) < 0) {
            taxableAmount = BigDecimal.ZERO;
        }

        // --- 6. Calculate Tax & Final Total ---
        BigDecimal tax = taxableAmount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = taxableAmount.add(tax);

        // --- 7. Update UI Labels ---
        roomChargesLabel.setText(String.format("$%.2f", roomCharges));
        servicesChargesLabel.setText(String.format("$%.2f", serviceCharges));
        subtotalLabel.setText(String.format("$%.2f", subtotal));

        // Update Discount Label
        if (discountAmountLabel != null) {
            discountAmountLabel.setText(String.format("-$%.2f", discountAmount));
        }

        taxLabel.setText(String.format("$%.2f", tax));
        totalLabel.setText(String.format("$%.2f", total));
    }

    /**
     * Reset pricing labels to zero.
     */
    private void resetPricingLabels() {
        roomChargesLabel.setText("$0.00");
        servicesChargesLabel.setText("$0.00");
        subtotalLabel.setText("$0.00");
        taxLabel.setText("$0.00");
        totalLabel.setText("$0.00");
    }

    /**
     * Get number of nights from selected dates.
     */
    private int getNights() {
        if (checkInPicker.getValue() != null && checkOutPicker.getValue() != null) {
            long nights = ChronoUnit.DAYS.between(checkInPicker.getValue(), checkOutPicker.getValue());
            return nights > 0 ? (int) nights : 0;
        }
        return 0;
    }

    /**
     * Validate occupancy silently (no alert, for real-time feedback).
     */
    private void validateOccupancySilent() {
        int totalGuests = adultsSpinner.getValue() + childrenSpinner.getValue();
        int totalCapacity = calculateTotalCapacity();

        if (totalCapacity > 0 && totalCapacity < totalGuests) {
            noRoomsLabel.setText("⚠ Need more rooms for " + totalGuests + " guests");
            noRoomsLabel.setVisible(true);
        }
    }

    /**
     * Calculate total room capacity using RoomService.getMaxOccupancy().
     */
    private int calculateTotalCapacity() {
        int totalCapacity = 0;

        if (roomService != null) {
            totalCapacity += singleRoomSpinner.getValue() * roomService.getMaxOccupancy(RoomType.SINGLE);
            totalCapacity += doubleRoomSpinner.getValue() * roomService.getMaxOccupancy(RoomType.DOUBLE);
            totalCapacity += deluxeRoomSpinner.getValue() * roomService.getMaxOccupancy(RoomType.DELUXE);
            totalCapacity += penthouseSpinner.getValue() * roomService.getMaxOccupancy(RoomType.PENTHOUSE);
        } else {
            // Fallback to hardcoded values per assignment
            totalCapacity += singleRoomSpinner.getValue() * 2;
            totalCapacity += doubleRoomSpinner.getValue() * 4;
            totalCapacity += deluxeRoomSpinner.getValue() * 2;
            totalCapacity += penthouseSpinner.getValue() * 2;
        }

        return totalCapacity;
    }

    /**
     * Validate occupancy rules per assignment requirements (with alert).
     *
     * Business Rules:
     * - Single room: max 2 people
     * - Double room: max 4 people
     * - Deluxe room: max 2 people
     * - Penthouse: max 2 people
     */
    private boolean validateOccupancy() {
        int totalGuests = adultsSpinner.getValue() + childrenSpinner.getValue();
        int totalCapacity = calculateTotalCapacity();

        if (totalCapacity < totalGuests) {
            String message = String.format(
                    "Selected rooms can accommodate %d guests, but %d selected.\n\n" +
                            "Occupancy limits:\n" +
                            "• Single Room: 2 people\n" +
                            "• Double Room: 4 people\n" +
                            "• Deluxe Room: 2 people\n" +
                            "• Penthouse: 2 people",
                    totalCapacity, totalGuests);

            showAlert(Alert.AlertType.WARNING, "Occupancy Exceeded", message);
            return false;
        }

        return true;
    }

    /**
     * Validate all form fields using regex patterns.
     */
    private boolean validateForm() {
        boolean valid = true;
        hideAllErrors();

        // Validate first name
        String firstName = firstNameField.getText().trim();
        if (firstName.isEmpty()) {
            showFieldError(firstNameError, "First name is required");
            valid = false;
        } else if (!firstName.matches(NAME_PATTERN)) {
            showFieldError(firstNameError, "Invalid first name");
            valid = false;
        }

        // Validate last name
        String lastName = lastNameField.getText().trim();
        if (lastName.isEmpty()) {
            showFieldError(lastNameError, "Last name is required");
            valid = false;
        } else if (!lastName.matches(NAME_PATTERN)) {
            showFieldError(lastNameError, "Invalid last name");
            valid = false;
        }

        // Validate email
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showFieldError(emailError, "Email is required");
            valid = false;
        } else if (!email.matches(EMAIL_PATTERN)) {
            showFieldError(emailError, "Invalid email format");
            valid = false;
        }

        // Validate phone
        String phone = phoneField.getText().trim();
        if (phone.isEmpty()) {
            showFieldError(phoneError, "Phone is required");
            valid = false;
        } else if (!phone.matches(PHONE_PATTERN)) {
            showFieldError(phoneError, "Invalid phone format");
            valid = false;
        }

        // Validate check-in date
        if (checkInPicker.getValue() == null) {
            showFieldError(checkInError, "Check-in date is required");
            valid = false;
        } else if (checkInPicker.getValue().isBefore(LocalDate.now())) {
            showFieldError(checkInError, "Check-in cannot be in the past");
            valid = false;
        }

        // Validate check-out date
        if (checkOutPicker.getValue() == null) {
            showFieldError(checkOutError, "Check-out date is required");
            valid = false;
        } else if (checkInPicker.getValue() != null &&
                !checkOutPicker.getValue().isAfter(checkInPicker.getValue())) {
            showFieldError(checkOutError, "Check-out must be after check-in");
            valid = false;
        }

        // Validate room selection
        int totalRooms = singleRoomSpinner.getValue() +
                doubleRoomSpinner.getValue() +
                deluxeRoomSpinner.getValue() +
                penthouseSpinner.getValue();

        if (totalRooms == 0) {
            noRoomsLabel.setText("Please select at least one room");
            noRoomsLabel.setVisible(true);
            valid = false;
        }

        return valid;
    }

    /**
     * Show field error.
     */
    private void showFieldError(Label errorLabel, String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        }
    }
    /**
     * Add rooms of a specific type to the reservation.
     * Uses RoomService to get available rooms and creates ReservationRoom objects.
     */
    private void addRoomsToReservation(Reservation reservation, RoomType roomType, int count,
                                       LocalDate checkIn, LocalDate checkOut) {
        if (count <= 0) {
            LOGGER.fine("Skipping room type " + roomType + " - count is 0");
            return;
        }

        if (roomService == null) {
            LOGGER.warning("RoomService is null - cannot add rooms");
            return;
        }

        // Get available rooms from RoomService
        List<Room> availableRooms = roomService.getAvailableRoomsByType(roomType, checkIn, checkOut);

        LOGGER.info("Adding " + count + " " + roomType + " room(s). Available: " +
                (availableRooms != null ? availableRooms.size() : 0));

        if (availableRooms == null || availableRooms.isEmpty()) {
            LOGGER.warning("No available " + roomType + " rooms found!");
            return;
        }

        if (availableRooms.size() < count) {
            LOGGER.warning("Only " + availableRooms.size() + " " + roomType +
                    " rooms available, but " + count + " requested");
        }

        // Calculate guests per room (distribute evenly)
        int totalGuests = adultsSpinner.getValue() + childrenSpinner.getValue();
        int totalRooms = singleRoomSpinner.getValue() + doubleRoomSpinner.getValue() +
                deluxeRoomSpinner.getValue() + penthouseSpinner.getValue();
        int guestsPerRoom = totalRooms > 0 ? Math.max(1, totalGuests / totalRooms) : 1;

        BigDecimal roomPrice;
        if (pricingService != null) {
            roomPrice = pricingService.getAverageNightlyRate(roomType, checkIn, checkOut);
        } else {
            // Fallback to base price (same as form fallback)
            roomPrice = BigDecimal.valueOf(roomType.getBasePrice());
        }

        // Add rooms to reservation
        for (int i = 0; i < count && i < availableRooms.size(); i++) {
            Room room = availableRooms.get(i);

            ReservationRoom reservationRoom = new ReservationRoom();
            reservationRoom.setRoom(room);
            reservationRoom.setRoomPrice(roomPrice);
            reservationRoom.setNumGuests(Math.min(guestsPerRoom, roomType.getMaxOccupancy())); // CRITICAL: Set numGuests!

            reservation.addRoom(reservationRoom);

            LOGGER.info("  Added room " + room.getRoomNumber() + " (" + roomType + ")");
        }

        LOGGER.info("Reservation now has " + reservation.getReservationRooms().size() + " total rooms");
    }

    /**
     * Build a Reservation object from form data.
     */
    private Reservation buildReservation() {
        // Create or update guest
        Guest guest = currentGuest != null ? currentGuest : new Guest();
        guest.setFirstName(firstNameField.getText().trim());
        guest.setLastName(lastNameField.getText().trim());
        guest.setEmail(emailField.getText().trim());
        guest.setPhone(phoneField.getText().trim());
        if (idNumberField != null) guest.setIdNumber(idNumberField.getText().trim());
        if (countryField != null) guest.setCountry(countryField.getText().trim());
        if (stateField != null) guest.setStateProvince(stateField.getText().trim());
        if (postalCodeField != null) guest.setPostalCode(postalCodeField.getText().trim());

        // Create or update reservation
        Reservation reservation = currentReservation != null ? currentReservation : new Reservation();
        reservation.setGuest(guest);
        reservation.setCheckInDate(checkInPicker.getValue());
        reservation.setCheckOutDate(checkOutPicker.getValue());
        reservation.setNumAdults(adultsSpinner.getValue());
        reservation.setNumChildren(childrenSpinner.getValue());
        reservation.setBookedViaKiosk(false);

        // Clear existing rooms (for edit mode)
        if (reservation.getReservationRooms() == null) {
            reservation.setReservationRooms(new ArrayList<>());
        } else {
            reservation.getReservationRooms().clear();
        }

        // Add rooms from spinners
        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();

        addRoomsToReservation(reservation, RoomType.SINGLE, singleRoomSpinner.getValue(), checkIn, checkOut);
        addRoomsToReservation(reservation, RoomType.DOUBLE, doubleRoomSpinner.getValue(), checkIn, checkOut);
        addRoomsToReservation(reservation, RoomType.DELUXE, deluxeRoomSpinner.getValue(), checkIn, checkOut);
        addRoomsToReservation(reservation, RoomType.PENTHOUSE, penthouseSpinner.getValue(), checkIn, checkOut);

        if (specialRequestsArea != null) {
            reservation.setSpecialRequests(specialRequestsArea.getText());
        }

        // Clear existing add-ons
        if (reservation.getAddOns() != null) {
            reservation.getAddOns().clear();
        }

        int nights = getNights();
        int totalGuests = adultsSpinner.getValue() + childrenSpinner.getValue();

        if (wifiCheckbox.isSelected()) {
            addAddOnToReservation(reservation, AddOnType.WIFI, nights);
        }
        if (breakfastCheckbox.isSelected()) {
            addAddOnToReservation(reservation, AddOnType.BREAKFAST, nights * totalGuests);
        }
        if (parkingCheckbox.isSelected()) {
            addAddOnToReservation(reservation, AddOnType.PARKING, nights);
        }
        if (spaCheckbox.isSelected()) {
            addAddOnToReservation(reservation, AddOnType.SPA, totalGuests);
        }

        if (discountSpinner != null && discountSpinner.getValue() != null) {
            BigDecimal discount = BigDecimal.valueOf(discountSpinner.getValue());
            reservation.setDiscountPercentage(discount); // Assuming this field exists in Reservation

            // You should also record WHO applied it
            if (adminSession.getCurrentAdmin() != null) {
                reservation.setDiscountAppliedBy(adminSession.getCurrentAdmin().getUsername());
            }
        }


        // Calculate totals
        reservation.calculateTotal();

        return reservation;
    }

    /**
     * Add an add-on to the reservation.
     */
    private void addAddOnToReservation(Reservation reservation, AddOnType addOnType, int quantity) {
        ReservationAddOn addOn = new ReservationAddOn();
        addOn.setAddOnType(addOnType);
        addOn.setUnitPrice(addOnType.getBasePrice());
        addOn.setQuantity(quantity);
        addOn.setTotalPrice(addOnType.getBasePrice().multiply(BigDecimal.valueOf(quantity)));
        addOn.setDateAdded(LocalDate.now());
        reservation.addAddOn(addOn);
    }

    /**
     * Log admin activity.
     */
    private void logActivity(String action, String entityType, String entityId, String message) {
        if (activityLogger != null && adminSession != null) {
            activityLogger.logActivity(
                    adminSession.getActorName(),
                    action,
                    entityType,
                    entityId,
                    message
            );
        }
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

    // ==================== Dependency Injection Setters ====================

    public void setNavigationService(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    public void setReservationService(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void setRoomService(RoomService roomService) {
        this.roomService = roomService;
    }

    public void setPricingService(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    public void setAdminSession(AdminSession adminSession) {
        this.adminSession = adminSession;
    }

    public void setActivityLogger(ActivityLogger activityLogger) {
        this.activityLogger = activityLogger;
    }
}