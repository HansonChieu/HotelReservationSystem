package com.hanson.hotelreservationsystem.controller.Kiosk;

import com.hanson.hotelreservationsystem.model.enums.RoomType;
import com.hanson.hotelreservationsystem.service.NavigationService;
import com.hanson.hotelreservationsystem.service.RoomService;
import com.hanson.hotelreservationsystem.service.PricingService;
import com.hanson.hotelreservationsystem.session.BookingSession;
import com.hanson.hotelreservationsystem.session.BookingSession.RoomSelection;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Controller for the Kiosk Room Selection Screen (Step 3 of 5).
 *
 * Responsibilities:
 * - Display available room types with prices and availability
 * - Allow selection of room quantities
 * - Validate occupancy against guest count
 * - Calculate running total
 * - Provide room suggestions based on group size
 *
 * Business Rules:
 * - Single room: max 2 guests
 * - Double room: max 4 guests
 * - Deluxe/Penthouse: max 2 guests
 * - Total room capacity must accommodate all guests
 */
public class KioskRoomSelectionController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(KioskRoomSelectionController.class.getName());

    @FXML
    private Spinner<Integer> singleRoomSpinner;

    @FXML
    private Spinner<Integer> doubleRoomSpinner;

    @FXML
    private Spinner<Integer> deluxeRoomSpinner;

    @FXML
    private Spinner<Integer> penthouseRoomSpinner;

    @FXML
    private Label singlePriceLabel;

    @FXML
    private Label doublePriceLabel;

    @FXML
    private Label deluxePriceLabel;

    @FXML
    private Label penthousePriceLabel;

    @FXML
    private Label singleAvailableLabel;

    @FXML
    private Label doubleAvailableLabel;

    @FXML
    private Label deluxeAvailableLabel;

    @FXML
    private Label penthouseAvailableLabel;

    @FXML
    private Label totalCapacityLabel;

    @FXML
    private Label totalPriceLabel;

    @FXML
    private Label errorLabel;

    @FXML
    private Label suggestionLabel;

    @FXML
    private Button backButton;

    @FXML
    private Button continueButton;

    @FXML
    private VBox roomSuggestionBox;

    // Room availability (would come from RoomService in production)
    private Map<RoomType, Integer> roomAvailability = new HashMap<>();

    // Room selection counts
    private final IntegerProperty singleCount = new SimpleIntegerProperty(0);
    private final IntegerProperty doubleCount = new SimpleIntegerProperty(0);
    private final IntegerProperty deluxeCount = new SimpleIntegerProperty(0);
    private final IntegerProperty penthouseCount = new SimpleIntegerProperty(0);

    // Services
    private NavigationService navigationService;
    private BookingSession bookingSession;
    private RoomService roomService;
    private PricingService pricingService;

    /**
     * Default constructor for FXML loader.
     */
    public KioskRoomSelectionController() {
        this.navigationService = NavigationService.getInstance();
        this.bookingSession = BookingSession.getInstance();
    }

    /**
     * Constructor with dependency injection.
     */
    public KioskRoomSelectionController(NavigationService navigationService,
                                        BookingSession bookingSession,
                                        RoomService roomService,
                                        PricingService pricingService) {
        this.navigationService = navigationService;
        this.bookingSession = bookingSession;
        this.roomService = roomService;
        this.pricingService = pricingService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("Initializing Kiosk Room Selection Screen (Step 3)");

        loadRoomAvailability();
        setupSpinners();
        setupPriceLabels();
        setupBindings();
        loadExistingSelections();
        showRoomSuggestion();
    }

    /**
     * Load room availability for the selected dates.
     */
    private void loadRoomAvailability() {
        // In production, this would call roomService.getAvailability(checkIn, checkOut)
        // For now, use default values
        roomAvailability.put(RoomType.SINGLE, 4);
        roomAvailability.put(RoomType.DOUBLE, 3);
        roomAvailability.put(RoomType.DELUXE, 3);
        roomAvailability.put(RoomType.PENTHOUSE, 1);

        // If room service is available, use it
        if (roomService != null && bookingSession.getCheckInDate() != null) {
            // roomAvailability = roomService.getAvailability(
            //     bookingSession.getCheckInDate(),
            //     bookingSession.getCheckOutDate()
            // );
        }

        LOGGER.info("Room availability loaded: " + roomAvailability);
    }

    /**
     * Setup spinners with value factories based on availability.
     */
    private void setupSpinners() {
        setupSpinner(singleRoomSpinner, RoomType.SINGLE, singleCount, singleAvailableLabel);
        setupSpinner(doubleRoomSpinner, RoomType.DOUBLE, doubleCount, doubleAvailableLabel);
        setupSpinner(deluxeRoomSpinner, RoomType.DELUXE, deluxeCount, deluxeAvailableLabel);
        setupSpinner(penthouseRoomSpinner, RoomType.PENTHOUSE, penthouseCount, penthouseAvailableLabel);
    }

    /**
     * Setup a single spinner with its value factory and listeners.
     */
    private void setupSpinner(Spinner<Integer> spinner, RoomType roomType,
                              IntegerProperty countProperty, Label availableLabel) {
        if (spinner == null) return;

        int available = roomAvailability.getOrDefault(roomType, 0);

        SpinnerValueFactory<Integer> factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, available, 0);
        spinner.setValueFactory(factory);
        spinner.setEditable(false);

        // Bind count property to spinner value
        spinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            countProperty.set(newVal != null ? newVal : 0);
            updateTotals();
            validateOccupancy();
        });

        // Update availability label
        if (availableLabel != null) {
            availableLabel.setText("Available Rooms: " + available);
        }
    }

    /**
     * Setup price labels with current pricing.
     */
    private void setupPriceLabels() {
        // Get prices (would use PricingService in production with date-based pricing)
        updatePriceLabel(singlePriceLabel, RoomType.SINGLE);
        updatePriceLabel(doublePriceLabel, RoomType.DOUBLE);
        updatePriceLabel(deluxePriceLabel, RoomType.DELUXE);
        updatePriceLabel(penthousePriceLabel, RoomType.PENTHOUSE);
    }

    private void updatePriceLabel(Label label, RoomType roomType) {
        if (label == null) return;

        double price = getRoomPrice(roomType);
        // Format as currency with 2 decimal places
        label.setText(String.format("$%.2f", price));
    }

    /**
     * Get the current price for a room type.
     */
    private double getRoomPrice(RoomType roomType) {
        // In production, use pricingService with date-based pricing
        if (pricingService != null && bookingSession.getCheckInDate() != null) {
            // return pricingService.getRoomPrice(roomType, bookingSession.getCheckInDate());
        }
        // FIX: Removed (long) cast to prevent losing cents
        return roomType.getBasePrice();
    }

    /**
     * Setup bindings for the continue button.
     */
    private void setupBindings() {
        if (continueButton != null) {
            // Disable continue button if no rooms selected or occupancy not met
            BooleanBinding hasSelection = Bindings.createBooleanBinding(
                    () -> getTotalRoomCount() > 0 && isOccupancyMet(),
                    singleCount, doubleCount, deluxeCount, penthouseCount
            );
            continueButton.disableProperty().bind(hasSelection.not());
        }
    }

    /**
     * Load any existing room selections from the session.
     */
    private void loadExistingSelections() {
        for (RoomSelection selection : bookingSession.getSelectedRooms()) {
            switch (selection.getRoomType()) {
                case SINGLE:
                    if (singleRoomSpinner != null) singleRoomSpinner.getValueFactory().setValue(selection.getQuantity());
                    break;
                case DOUBLE:
                    if (doubleRoomSpinner != null) doubleRoomSpinner.getValueFactory().setValue(selection.getQuantity());
                    break;
                case DELUXE:
                    if (deluxeRoomSpinner != null) deluxeRoomSpinner.getValueFactory().setValue(selection.getQuantity());
                    break;
                case PENTHOUSE:
                    if (penthouseRoomSpinner != null) penthouseRoomSpinner.getValueFactory().setValue(selection.getQuantity());
                    break;
            }
        }
    }

    /**
     * Show room suggestion based on guest count.
     */
    private void showRoomSuggestion() {
        if (suggestionLabel == null) return;

        int totalGuests = bookingSession.getTotalGuestCount();
        String suggestion = generateRoomSuggestion(totalGuests);
        suggestionLabel.setText(suggestion);
    }

    /**
     * Generate a room suggestion based on guest count.
     */
    private String generateRoomSuggestion(int totalGuests) {
        if (totalGuests <= 2) {
            return "Suggestion: 1 Single, Deluxe, or Penthouse room";
        } else if (totalGuests <= 4) {
            return "Suggestion: 1 Double room or 2 Single rooms";
        } else {
            int doubleRooms = (int) Math.ceil(totalGuests / 4.0);
            return "Suggestion: " + doubleRooms + " Double room(s)";
        }
    }

    /**
     * Update total capacity and price labels.
     */
    private void updateTotals() {
        int totalCapacity = calculateTotalCapacity();
        BigDecimal totalPrice = calculateTotalPrice();

        if (totalCapacityLabel != null) {
            totalCapacityLabel.setText("Total Capacity: " + totalCapacity + " guests");
        }

        if (totalPriceLabel != null) {
            long nights = bookingSession.getNights();
            BigDecimal perNightTotal = totalPrice;
            BigDecimal grandTotal = totalPrice.multiply(BigDecimal.valueOf(nights));
            totalPriceLabel.setText(String.format("$%.2f/night × %d nights = $%.2f",
                    perNightTotal, nights, grandTotal));
        }
    }

    /**
     * Calculate total room capacity based on current selection.
     */
    private int calculateTotalCapacity() {
        return (singleCount.get() * RoomType.SINGLE.getMaxOccupancy()) +
                (doubleCount.get() * RoomType.DOUBLE.getMaxOccupancy()) +
                (deluxeCount.get() * RoomType.DELUXE.getMaxOccupancy()) +
                (penthouseCount.get() * RoomType.PENTHOUSE.getMaxOccupancy());
    }

    /**
     * Calculate total price per night based on current selection.
     */
    private BigDecimal calculateTotalPrice() {
        BigDecimal total = BigDecimal.ZERO;

        // Helper to calculate subtotal for a specific room type
        // Formula: (Price * Count)
        total = total.add(BigDecimal.valueOf(getRoomPrice(RoomType.SINGLE))
                .multiply(BigDecimal.valueOf(singleCount.get())));

        total = total.add(BigDecimal.valueOf(getRoomPrice(RoomType.DOUBLE))
                .multiply(BigDecimal.valueOf(doubleCount.get())));

        total = total.add(BigDecimal.valueOf(getRoomPrice(RoomType.DELUXE))
                .multiply(BigDecimal.valueOf(deluxeCount.get())));

        total = total.add(BigDecimal.valueOf(getRoomPrice(RoomType.PENTHOUSE))
                .multiply(BigDecimal.valueOf(penthouseCount.get())));

        return total;
    }

    /**
     * Get total number of rooms selected.
     */
    private int getTotalRoomCount() {
        return singleCount.get() + doubleCount.get() + deluxeCount.get() + penthouseCount.get();
    }

    /**
     * Check if selected rooms can accommodate all guests.
     */
    private boolean isOccupancyMet() {
        int totalGuests = bookingSession.getTotalGuestCount();
        int totalCapacity = calculateTotalCapacity();
        return totalCapacity >= totalGuests;
    }

    /**
     * Validate that selected rooms can accommodate all guests.
     */
    private boolean validateOccupancy() {
        int totalGuests = bookingSession.getTotalGuestCount();
        int totalCapacity = calculateTotalCapacity();

        clearError();

        if (getTotalRoomCount() == 0) {
            showError("Please select at least one room.");
            return false;
        }

        if (totalCapacity < totalGuests) {
            showError(String.format(
                    "Selected rooms can only accommodate %d guests. You need rooms for %d guests.",
                    totalCapacity, totalGuests));
            return false;
        }

        return true;
    }

    /**
     * Clear error message.
     */
    private void clearError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
    }

    /**
     * Show error message.
     */
    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        }
    }

    /**
     * Save room selections to the booking session.
     */
    private void saveSelections() {
        bookingSession.getSelectedRooms().clear();

        if (singleCount.get() > 0) {
            bookingSession.getSelectedRooms().add(new RoomSelection(
                    RoomType.SINGLE, singleCount.get(),
                    getRoomPrice(RoomType.SINGLE),
                    roomAvailability.getOrDefault(RoomType.SINGLE, 0)
            ));
        }

        if (doubleCount.get() > 0) {
            bookingSession.getSelectedRooms().add(new RoomSelection(
                    RoomType.DOUBLE, doubleCount.get(),
                    getRoomPrice(RoomType.DOUBLE),
                    roomAvailability.getOrDefault(RoomType.DOUBLE, 0)
            ));
        }

        if (deluxeCount.get() > 0) {
            bookingSession.getSelectedRooms().add(new RoomSelection(
                    RoomType.DELUXE, deluxeCount.get(),
                    getRoomPrice(RoomType.DELUXE),
                    roomAvailability.getOrDefault(RoomType.DELUXE, 0)
            ));
        }

        if (penthouseCount.get() > 0) {
            bookingSession.getSelectedRooms().add(new RoomSelection(
                    RoomType.PENTHOUSE, penthouseCount.get(),
                    getRoomPrice(RoomType.PENTHOUSE),
                    roomAvailability.getOrDefault(RoomType.PENTHOUSE, 0)
            ));
        }

        // Calculate and store room subtotal
        BigDecimal perNightTotal = calculateTotalPrice();
        BigDecimal totalForStay = perNightTotal.multiply(BigDecimal.valueOf(bookingSession.getNights()));
        bookingSession.setRoomSubtotal(totalForStay);

        LOGGER.info("Room selections saved: " + bookingSession.getSelectedRooms().size() + " room type(s)");
    }

    /**
     * Handle the "Back" button click.
     */
    @FXML
    public void handleBack(ActionEvent event) {
        LOGGER.info("User clicked Back - returning to date selection");
        navigationService.goToDateSelection();
    }

    /**
     * Handle the "Continue to Guest Details" button click.
     */
    @FXML
    public void handleContinue(ActionEvent event) {
        LOGGER.info("User clicked Continue to Guest Details");

        if (!validateOccupancy()) {
            LOGGER.warning("Occupancy validation failed");
            return;
        }

        saveSelections();

        // Navigate to Step 4: Guest Details
        navigationService.goToGuestDetails();
    }

    /**
     * Handle the "Rules and Regulations" button click.
     */
    @FXML
    public void handleRulesAndRegulations(ActionEvent event) {
        LOGGER.info("User clicked Rules and Regulations");
        navigationService.showRulesAndRegulations();
    }

    // Setters for dependency injection
    public void setNavigationService(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    public void setBookingSession(BookingSession bookingSession) {
        this.bookingSession = bookingSession;
    }

    public void setRoomService(RoomService roomService) {
        this.roomService = roomService;
    }

    public void setPricingService(PricingService pricingService) {
        this.pricingService = pricingService;
    }
}