package com.hanson.hotelreservationsystem.controller.Kiosk;

import com.hanson.hotelreservationsystem.service.NavigationService;
import com.hanson.hotelreservationsystem.service.PricingService;
import com.hanson.hotelreservationsystem.session.BookingSession;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Controller for the Kiosk Add-On Services Screen (Step 5 of 5).
 *
 * Implements the Decorator pattern for add-on services pricing.
 *
 * Responsibilities:
 * - Display available add-on services with prices
 * - Allow selection/deselection of services
 * - Calculate and display total add-on cost
 * - Show price impact of each selection
 *
 * Add-On Services:
 * - Premium Wi-Fi: $15/night
 * - Breakfast Buffet: $25/night
 * - Valet Parking: $20/night
 * - Spa Access: $50/reservation (one-time)
 */
public class KioskAddOnServicesController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(KioskAddOnServicesController.class.getName());

    // Default add-on prices (would come from PricingService in production)
    private static final BigDecimal WIFI_PRICE_PER_NIGHT = new BigDecimal("15.00");
    private static final BigDecimal BREAKFAST_PRICE_PER_NIGHT = new BigDecimal("25.00");
    private static final BigDecimal PARKING_PRICE_PER_NIGHT = new BigDecimal("20.00");
    private static final BigDecimal SPA_PRICE_PER_RESERVATION = new BigDecimal("50.00");

    // Checkboxes for add-on selection
    @FXML
    private CheckBox wifiCheckBox;

    @FXML
    private CheckBox breakfastCheckBox;

    @FXML
    private CheckBox parkingCheckBox;

    @FXML
    private CheckBox spaCheckBox;

    // Price labels
    @FXML
    private Label wifiPriceLabel;

    @FXML
    private Label breakfastPriceLabel;

    @FXML
    private Label parkingPriceLabel;

    @FXML
    private Label spaPriceLabel;

    // Total display
    @FXML
    private Label addOnsTotal;

    @FXML
    private Label priceImpactLabel;

    // Navigation buttons
    @FXML
    private Button backButton;

    @FXML
    private Button nextButton;

    // Total add-ons property for binding
    private final ObjectProperty<BigDecimal> totalAddOns = new SimpleObjectProperty<>(BigDecimal.ZERO);

    // Services
    private NavigationService navigationService;
    private BookingSession bookingSession;
    private PricingService pricingService;

    // Current prices (may be modified by PricingService)
    private BigDecimal wifiPrice = WIFI_PRICE_PER_NIGHT;
    private BigDecimal breakfastPrice = BREAKFAST_PRICE_PER_NIGHT;
    private BigDecimal parkingPrice = PARKING_PRICE_PER_NIGHT;
    private BigDecimal spaPrice = SPA_PRICE_PER_RESERVATION;

    /**
     * Default constructor for FXML loader.
     */
    public KioskAddOnServicesController() {
        this.navigationService = NavigationService.getInstance();
        this.bookingSession = BookingSession.getInstance();
    }

    /**
     * Constructor with dependency injection.
     */
    public KioskAddOnServicesController(NavigationService navigationService,
                                        BookingSession bookingSession,
                                        PricingService pricingService) {
        this.navigationService = navigationService;
        this.bookingSession = bookingSession;
        this.pricingService = pricingService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("Initializing Kiosk Add-On Services Screen (Step 5)");

        loadPrices();
        setupCheckboxListeners();
        setupPriceLabels();
        loadExistingSelections();
        updateTotalDisplay();
    }

    /**
     * Load prices from PricingService (may include dynamic pricing).
     */
    private void loadPrices() {
        if (pricingService != null) {
            // In production, get prices from service
            // wifiPrice = pricingService.getAddOnPrice(AddOnType.WIFI);
            // breakfastPrice = pricingService.getAddOnPrice(AddOnType.BREAKFAST);
            // etc.
        }

        LOGGER.info(String.format("Add-on prices: WiFi=$%.2f, Breakfast=$%.2f, Parking=$%.2f, Spa=$%.2f",
                wifiPrice, breakfastPrice, parkingPrice, spaPrice));
    }

    /**
     * Setup checkbox listeners for price updates.
     */
    private void setupCheckboxListeners() {
        if (wifiCheckBox != null) {
            wifiCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                onSelectionChanged("Wi-Fi", newVal);
            });
        }

        if (breakfastCheckBox != null) {
            breakfastCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                onSelectionChanged("Breakfast", newVal);
            });
        }

        if (parkingCheckBox != null) {
            parkingCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                onSelectionChanged("Parking", newVal);
            });
        }

        if (spaCheckBox != null) {
            spaCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                onSelectionChanged("Spa", newVal);
            });
        }
    }

    /**
     * Handle selection changes.
     */
    private void onSelectionChanged(String serviceName, boolean selected) {
        LOGGER.fine(serviceName + " " + (selected ? "selected" : "deselected"));
        updateTotalDisplay();
    }

    /**
     * Setup price labels with current pricing.
     */
    private void setupPriceLabels() {
        long nights = bookingSession.getNights();

        if (wifiPriceLabel != null) {
            wifiPriceLabel.setText(String.format("$%.2f / night", wifiPrice));
        }

        if (breakfastPriceLabel != null) {
            breakfastPriceLabel.setText(String.format("$%.2f / night", breakfastPrice));
        }

        if (parkingPriceLabel != null) {
            parkingPriceLabel.setText(String.format("$%.2f / night", parkingPrice));
        }

        if (spaPriceLabel != null) {
            spaPriceLabel.setText(String.format("$%.2f / reservation", spaPrice));
        }
    }

    /**
     * Load any existing selections from the booking session.
     */
    private void loadExistingSelections() {
        if (wifiCheckBox != null) {
            wifiCheckBox.setSelected(bookingSession.isWifiSelected());
        }
        if (breakfastCheckBox != null) {
            breakfastCheckBox.setSelected(bookingSession.isBreakfastSelected());
        }
        if (parkingCheckBox != null) {
            parkingCheckBox.setSelected(bookingSession.isParkingSelected());
        }
        if (spaCheckBox != null) {
            spaCheckBox.setSelected(bookingSession.isSpaSelected());
        }
    }

    /**
     * Calculate the total add-ons cost.
     */
    private BigDecimal calculateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        long nights = bookingSession.getNights();

        if (isWifiSelected()) {
            total = total.add(wifiPrice.multiply(BigDecimal.valueOf(nights)));
        }

        if (isBreakfastSelected()) {
            total = total.add(breakfastPrice.multiply(BigDecimal.valueOf(nights)));
        }

        if (isParkingSelected()) {
            total = total.add(parkingPrice.multiply(BigDecimal.valueOf(nights)));
        }

        if (isSpaSelected()) {
            total = total.add(spaPrice); // One-time charge
        }

        return total;
    }

    /**
     * Update the total display label.
     */
    private void updateTotalDisplay() {
        BigDecimal total = calculateTotal();
        totalAddOns.set(total);

        if (addOnsTotal != null) {
            addOnsTotal.setText(String.format("$%.2f", total));
        }

        // Update price impact label
        if (priceImpactLabel != null) {
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                priceImpactLabel.setText(generatePriceBreakdown());
            } else {
                priceImpactLabel.setText("Select services to see price impact");
            }
        }
    }

    /**
     * Generate a price breakdown string.
     */
    private String generatePriceBreakdown() {
        StringBuilder breakdown = new StringBuilder();
        long nights = bookingSession.getNights();

        if (isWifiSelected()) {
            BigDecimal wifiTotal = wifiPrice.multiply(BigDecimal.valueOf(nights));
            breakdown.append(String.format("Wi-Fi: $%.2f × %d nights = $%.2f", wifiPrice, nights, wifiTotal));
        }

        if (isBreakfastSelected()) {
            if (breakdown.length() > 0) breakdown.append(" | ");
            BigDecimal breakfastTotal = breakfastPrice.multiply(BigDecimal.valueOf(nights));
            breakdown.append(String.format("Breakfast: $%.2f × %d nights = $%.2f", breakfastPrice, nights, breakfastTotal));
        }

        if (isParkingSelected()) {
            if (breakdown.length() > 0) breakdown.append(" | ");
            BigDecimal parkingTotal = parkingPrice.multiply(BigDecimal.valueOf(nights));
            breakdown.append(String.format("Parking: $%.2f × %d nights = $%.2f", parkingPrice, nights, parkingTotal));
        }

        if (isSpaSelected()) {
            if (breakdown.length() > 0) breakdown.append(" | ");
            breakdown.append(String.format("Spa: $%.2f (one-time)", spaPrice));
        }

        return breakdown.toString();
    }

    // ==================== Selection Getters ====================

    private boolean isWifiSelected() {
        return wifiCheckBox != null && wifiCheckBox.isSelected();
    }

    private boolean isBreakfastSelected() {
        return breakfastCheckBox != null && breakfastCheckBox.isSelected();
    }

    private boolean isParkingSelected() {
        return parkingCheckBox != null && parkingCheckBox.isSelected();
    }

    private boolean isSpaSelected() {
        return spaCheckBox != null && spaCheckBox.isSelected();
    }

    /**
     * Save add-on selections to the booking session.
     */
    private void saveSelections() {
        bookingSession.setWifiSelected(isWifiSelected());
        bookingSession.setBreakfastSelected(isBreakfastSelected());
        bookingSession.setParkingSelected(isParkingSelected());
        bookingSession.setSpaSelected(isSpaSelected());

        // Calculate and store add-ons subtotal
        BigDecimal total = calculateTotal();
        bookingSession.setAddOnsSubtotal(total);

        // Calculate tax and grand total
        BigDecimal roomSubtotal = bookingSession.getRoomSubtotal();
        BigDecimal subtotal = roomSubtotal.add(total);

        // Tax calculation (13% as per typical hotel tax)
        BigDecimal taxRate = new BigDecimal("0.13");
        BigDecimal tax = subtotal.multiply(taxRate);
        bookingSession.setTaxAmount(tax);

        // Calculate grand total
        BigDecimal grandTotal = subtotal.add(tax);

        // Apply loyalty discount if applicable
        if (bookingSession.isLoyaltyMember()) {
            // Example: 5% loyalty discount
            BigDecimal loyaltyDiscount = grandTotal.multiply(new BigDecimal("0.05"));
            bookingSession.setLoyaltyDiscount(loyaltyDiscount);
            grandTotal = grandTotal.subtract(loyaltyDiscount);
        }

        bookingSession.setTotalAmount(grandTotal);

        LOGGER.info(String.format("Add-ons saved. Total: $%.2f, Tax: $%.2f, Grand Total: $%.2f",
                total, tax, grandTotal));
    }

    // ==================== Navigation Handlers ====================

    /**
     * Handle the "Back" button click.
     */
    @FXML
    public void handleBack(ActionEvent event) {
        LOGGER.info("User clicked Back - returning to guest details");
        saveSelections(); // Save partial progress
        navigationService.goToGuestDetails();
    }

    /**
     * Handle the "Next" button click.
     */
    @FXML
    public void handleNext(ActionEvent event) {
        LOGGER.info("User clicked Next - proceeding to booking summary");

        saveSelections();

        // Navigate to Booking Summary
        navigationService.goToBookingSummary();
    }

    /**
     * Handle the "Rules and Regulations" button click.
     */
    @FXML
    public void handleRulesAndRegulations(ActionEvent event) {
        LOGGER.info("User clicked Rules and Regulations");
        navigationService.showRulesAndRegulations();
    }

    // ==================== Property Getters ====================

    public ObjectProperty<BigDecimal> totalAddOnsProperty() {
        return totalAddOns;
    }

    public BigDecimal getTotalAddOns() {
        return totalAddOns.get();
    }

    // Setters for dependency injection
    public void setNavigationService(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    public void setBookingSession(BookingSession bookingSession) {
        this.bookingSession = bookingSession;
    }

    public void setPricingService(PricingService pricingService) {
        this.pricingService = pricingService;
    }
}