package com.hanson.hotelreservationsystem.session;

import com.hanson.hotelreservationsystem.model.enums.RoomType;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Singleton class that holds the current booking session state across all kiosk screens.
 * This allows data to be shared between different steps of the booking process.
 *
 * Pattern: Singleton
 */
public class BookingSession {

    // ==================== Singleton Instance ====================
    private static BookingSession instance;

    // ==================== Guest Count Properties ====================
    private final IntegerProperty adultCount = new SimpleIntegerProperty(1);
    private final IntegerProperty childCount = new SimpleIntegerProperty(0);

    // ==================== Date Properties ====================
    private final ObjectProperty<LocalDate> checkInDate = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> checkOutDate = new SimpleObjectProperty<>();

    // ==================== Room Selection ====================
    private final ObservableList<RoomSelection> selectedRooms = FXCollections.observableArrayList();

    // ==================== Guest Details ====================
    private final StringProperty firstName = new SimpleStringProperty();
    private final StringProperty lastName = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty phone = new SimpleStringProperty();
    private final StringProperty country = new SimpleStringProperty();
    private final StringProperty idNumber = new SimpleStringProperty();
    private final StringProperty address = new SimpleStringProperty();
    private final StringProperty city = new SimpleStringProperty();
    private final StringProperty state = new SimpleStringProperty();
    private final StringProperty postalCode = new SimpleStringProperty();
    private final StringProperty specialRequests = new SimpleStringProperty();

    // ==================== Loyalty Program ====================
    private final BooleanProperty isLoyaltyMember = new SimpleBooleanProperty(false);
    private final StringProperty loyaltyNumber = new SimpleStringProperty();
    private final IntegerProperty loyaltyPoints = new SimpleIntegerProperty(0);
    private final IntegerProperty loyaltyPointsUsed = new SimpleIntegerProperty(0);
    private final BooleanProperty wantsToJoinLoyalty = new SimpleBooleanProperty(false);

    // ==================== Add-on Services ====================
    private final BooleanProperty wifiSelected = new SimpleBooleanProperty(false);
    private final BooleanProperty breakfastSelected = new SimpleBooleanProperty(false);
    private final BooleanProperty parkingSelected = new SimpleBooleanProperty(false);
    private final BooleanProperty spaSelected = new SimpleBooleanProperty(false);

    // ==================== Pricing (Calculated) ====================
    private final ObjectProperty<BigDecimal> roomSubtotal = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> addOnsSubtotal = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> taxAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> loyaltyDiscount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);

    // ==================== Confirmation ====================
    private final StringProperty confirmationNumber = new SimpleStringProperty();
    private final ObjectProperty<Long> reservationId = new SimpleObjectProperty<>();

    // ==================== Constructor (Private for Singleton) ====================

    private BookingSession() {
        // Private constructor - use getInstance()
    }

    // ==================== Singleton Access ====================

    /**
     * Get the singleton instance of BookingSession.
     * Thread-safe implementation.
     *
     * @return The singleton BookingSession instance
     */
    public static synchronized BookingSession getInstance() {
        if (instance == null) {
            instance = new BookingSession();
        }
        return instance;
    }

    // ==================== Session Management ====================

    /**
     * Reset the session for a new booking.
     * Should be called when starting a fresh booking or after confirmation.
     */
    public void reset() {
        // Guest count
        adultCount.set(1);
        childCount.set(0);

        // Dates
        checkInDate.set(null);
        checkOutDate.set(null);

        // Room selection
        selectedRooms.clear();

        // Guest details
        firstName.set(null);
        lastName.set(null);
        email.set(null);
        phone.set(null);
        country.set(null);
        idNumber.set(null);
        address.set(null);
        city.set(null);
        state.set(null);
        postalCode.set(null);
        specialRequests.set(null);

        // Loyalty program
        isLoyaltyMember.set(false);
        loyaltyNumber.set(null);
        loyaltyPoints.set(0);
        loyaltyPointsUsed.set(0);
        wantsToJoinLoyalty.set(false);

        // Add-on services
        wifiSelected.set(false);
        breakfastSelected.set(false);
        parkingSelected.set(false);
        spaSelected.set(false);

        // Pricing
        roomSubtotal.set(BigDecimal.ZERO);
        addOnsSubtotal.set(BigDecimal.ZERO);
        taxAmount.set(BigDecimal.ZERO);
        loyaltyDiscount.set(BigDecimal.ZERO);
        totalAmount.set(BigDecimal.ZERO);

        // Confirmation
        confirmationNumber.set(null);
        reservationId.set(null);
    }

    // ==================== Convenience Methods ====================

    /**
     * Calculate the number of nights between check-in and check-out.
     *
     * @return Number of nights, or 0 if dates are not set
     */
    public long getNights() {
        if (checkInDate.get() != null && checkOutDate.get() != null) {
            return ChronoUnit.DAYS.between(checkInDate.get(), checkOutDate.get());
        }
        return 0;
    }

    /**
     * Get total guest count (adults + children).
     *
     * @return Total number of guests
     */
    public int getTotalGuestCount() {
        return adultCount.get() + childCount.get();
    }

    /**
     * Get the full name of the guest.
     *
     * @return Full name (first + last)
     */
    public String getFullName() {
        String first = firstName.get() != null ? firstName.get() : "";
        String last = lastName.get() != null ? lastName.get() : "";
        return (first + " " + last).trim();
    }

    // ==================== Guest Count Accessors ====================

    public IntegerProperty adultCountProperty() {
        return adultCount;
    }

    public int getAdultCount() {
        return adultCount.get();
    }

    public void setAdultCount(int value) {
        adultCount.set(value);
    }

    public IntegerProperty childCountProperty() {
        return childCount;
    }

    public int getChildCount() {
        return childCount.get();
    }

    public void setChildCount(int value) {
        childCount.set(value);
    }

    // ==================== Date Accessors ====================

    public ObjectProperty<LocalDate> checkInDateProperty() {
        return checkInDate;
    }

    public LocalDate getCheckInDate() {
        return checkInDate.get();
    }

    public void setCheckInDate(LocalDate date) {
        checkInDate.set(date);
    }

    public ObjectProperty<LocalDate> checkOutDateProperty() {
        return checkOutDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate.get();
    }

    public void setCheckOutDate(LocalDate date) {
        checkOutDate.set(date);
    }

    // ==================== Room Selection Accessors ====================

    public ObservableList<RoomSelection> getSelectedRooms() {
        return selectedRooms;
    }

    // ==================== Guest Details Accessors ====================

    public StringProperty firstNameProperty() {
        return firstName;
    }

    public String getFirstName() {
        return firstName.get();
    }

    public void setFirstName(String value) {
        firstName.set(value);
    }

    public StringProperty lastNameProperty() {
        return lastName;
    }

    public String getLastName() {
        return lastName.get();
    }

    public void setLastName(String value) {
        lastName.set(value);
    }

    public StringProperty emailProperty() {
        return email;
    }

    public String getEmail() {
        return email.get();
    }

    public void setEmail(String value) {
        email.set(value);
    }

    public StringProperty phoneProperty() {
        return phone;
    }

    public String getPhone() {
        return phone.get();
    }

    public void setPhone(String value) {
        phone.set(value);
    }

    public StringProperty countryProperty() {
        return country;
    }

    public String getCountry() {
        return country.get();
    }

    public void setCountry(String value) {
        country.set(value);
    }

    public StringProperty idNumberProperty() {
        return idNumber;
    }

    public String getIdNumber() {
        return idNumber.get();
    }

    public void setIdNumber(String value) {
        idNumber.set(value);
    }

    public StringProperty addressProperty() {
        return address;
    }

    public String getAddress() {
        return address.get();
    }

    public void setAddress(String value) {
        address.set(value);
    }

    public StringProperty cityProperty() {
        return city;
    }

    public String getCity() {
        return city.get();
    }

    public void setCity(String value) {
        city.set(value);
    }

    public StringProperty stateProperty() {
        return state;
    }

    public String getState() {
        return state.get();
    }

    public void setState(String value) {
        state.set(value);
    }

    public StringProperty postalCodeProperty() {
        return postalCode;
    }

    public String getPostalCode() {
        return postalCode.get();
    }

    public void setPostalCode(String value) {
        postalCode.set(value);
    }

    public StringProperty specialRequestsProperty() {
        return specialRequests;
    }

    public String getSpecialRequests() {
        return specialRequests.get();
    }

    public void setSpecialRequests(String value) {
        specialRequests.set(value);
    }

    // ==================== Loyalty Program Accessors ====================

    public BooleanProperty isLoyaltyMemberProperty() {
        return isLoyaltyMember;
    }

    public boolean isLoyaltyMember() {
        return isLoyaltyMember.get();
    }

    public void setLoyaltyMember(boolean value) {
        isLoyaltyMember.set(value);
    }

    public StringProperty loyaltyNumberProperty() {
        return loyaltyNumber;
    }

    public String getLoyaltyNumber() {
        return loyaltyNumber.get();
    }

    public void setLoyaltyNumber(String value) {
        loyaltyNumber.set(value);
    }

    public IntegerProperty loyaltyPointsProperty() {
        return loyaltyPoints;
    }

    public int getLoyaltyPoints() {
        return loyaltyPoints.get();
    }

    public void setLoyaltyPoints(int value) {
        loyaltyPoints.set(value);
    }

    public IntegerProperty loyaltyPointsUsedProperty() {
        return loyaltyPointsUsed;
    }

    public int getLoyaltyPointsUsed() {
        return loyaltyPointsUsed.get();
    }

    public void setLoyaltyPointsUsed(int value) {
        loyaltyPointsUsed.set(value);
    }

    public BooleanProperty wantsToJoinLoyaltyProperty() {
        return wantsToJoinLoyalty;
    }

    public boolean getWantsToJoinLoyalty() {
        return wantsToJoinLoyalty.get();
    }

    public void setWantsToJoinLoyalty(boolean value) {
        wantsToJoinLoyalty.set(value);
    }

    // ==================== Add-on Services Accessors ====================

    public BooleanProperty wifiSelectedProperty() {
        return wifiSelected;
    }

    public boolean isWifiSelected() {
        return wifiSelected.get();
    }

    public void setWifiSelected(boolean value) {
        wifiSelected.set(value);
    }

    public BooleanProperty breakfastSelectedProperty() {
        return breakfastSelected;
    }

    public boolean isBreakfastSelected() {
        return breakfastSelected.get();
    }

    public void setBreakfastSelected(boolean value) {
        breakfastSelected.set(value);
    }

    public BooleanProperty parkingSelectedProperty() {
        return parkingSelected;
    }

    public boolean isParkingSelected() {
        return parkingSelected.get();
    }

    public void setParkingSelected(boolean value) {
        parkingSelected.set(value);
    }

    public BooleanProperty spaSelectedProperty() {
        return spaSelected;
    }

    public boolean isSpaSelected() {
        return spaSelected.get();
    }

    public void setSpaSelected(boolean value) {
        spaSelected.set(value);
    }

    // ==================== Pricing Accessors ====================

    public ObjectProperty<BigDecimal> roomSubtotalProperty() {
        return roomSubtotal;
    }

    public BigDecimal getRoomSubtotal() {
        return roomSubtotal.get();
    }

    public void setRoomSubtotal(BigDecimal value) {
        roomSubtotal.set(value);
    }

    public ObjectProperty<BigDecimal> addOnsSubtotalProperty() {
        return addOnsSubtotal;
    }

    public BigDecimal getAddOnsSubtotal() {
        return addOnsSubtotal.get();
    }

    public void setAddOnsSubtotal(BigDecimal value) {
        addOnsSubtotal.set(value);
    }

    public ObjectProperty<BigDecimal> taxAmountProperty() {
        return taxAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount.get();
    }

    public void setTaxAmount(BigDecimal value) {
        taxAmount.set(value);
    }

    public ObjectProperty<BigDecimal> loyaltyDiscountProperty() {
        return loyaltyDiscount;
    }

    public BigDecimal getLoyaltyDiscount() {
        return loyaltyDiscount.get();
    }

    public void setLoyaltyDiscount(BigDecimal value) {
        loyaltyDiscount.set(value);
    }

    public ObjectProperty<BigDecimal> totalAmountProperty() {
        return totalAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount.get();
    }

    public void setTotalAmount(BigDecimal value) {
        totalAmount.set(value);
    }

    // ==================== Confirmation Accessors ====================

    public StringProperty confirmationNumberProperty() {
        return confirmationNumber;
    }

    public String getConfirmationNumber() {
        return confirmationNumber.get();
    }

    public void setConfirmationNumber(String value) {
        confirmationNumber.set(value);
    }

    public ObjectProperty<Long> reservationIdProperty() {
        return reservationId;
    }

    public Long getReservationId() {
        return reservationId.get();
    }

    public void setReservationId(Long value) {
        reservationId.set(value);
    }

    // ==================== Inner Classes ====================

    /**
     * Inner class to represent a room selection with quantity and pricing.
     */
    public static class RoomSelection {

        private final ObjectProperty<RoomType> roomType = new SimpleObjectProperty<>();
        private final IntegerProperty quantity = new SimpleIntegerProperty(0);
        private final ObjectProperty<BigDecimal> pricePerNight = new SimpleObjectProperty<>();
        private final IntegerProperty availableCount = new SimpleIntegerProperty();

        // ==================== Constructors ====================

        /**
         * Create a room selection with double price.
         *
         * @param roomType       The type of room
         * @param quantity       Number of rooms selected
         * @param pricePerNight  Price per night (as double)
         * @param availableCount Number of rooms available
         */
        public RoomSelection(RoomType roomType, int quantity, double pricePerNight, int availableCount) {
            this.roomType.set(roomType);
            this.quantity.set(quantity);
            this.pricePerNight.set(BigDecimal.valueOf(pricePerNight));
            this.availableCount.set(availableCount);
        }

        /**
         * Create a room selection with BigDecimal price.
         *
         * @param roomType       The type of room
         * @param quantity       Number of rooms selected
         * @param pricePerNight  Price per night (as BigDecimal)
         * @param availableCount Number of rooms available
         */
        public RoomSelection(RoomType roomType, int quantity, BigDecimal pricePerNight, int availableCount) {
            this.roomType.set(roomType);
            this.quantity.set(quantity);
            this.pricePerNight.set(pricePerNight);
            this.availableCount.set(availableCount);
        }

        // ==================== Room Type Accessors ====================

        public ObjectProperty<RoomType> roomTypeProperty() {
            return roomType;
        }

        public RoomType getRoomType() {
            return roomType.get();
        }

        public void setRoomType(RoomType value) {
            roomType.set(value);
        }

        // ==================== Quantity Accessors ====================

        public IntegerProperty quantityProperty() {
            return quantity;
        }

        public int getQuantity() {
            return quantity.get();
        }

        public void setQuantity(int value) {
            quantity.set(value);
        }

        // ==================== Price Per Night Accessors ====================

        public ObjectProperty<BigDecimal> pricePerNightProperty() {
            return pricePerNight;
        }

        public BigDecimal getPricePerNight() {
            return pricePerNight.get();
        }

        public void setPricePerNight(BigDecimal value) {
            pricePerNight.set(value);
        }

        // ==================== Available Count Accessors ====================

        public IntegerProperty availableCountProperty() {
            return availableCount;
        }

        public int getAvailableCount() {
            return availableCount.get();
        }

        public void setAvailableCount(int value) {
            availableCount.set(value);
        }

        // ==================== Utility Methods ====================

        /**
         * Calculate total price for this room selection.
         *
         * @param nights Number of nights
         * @return Total price (quantity × pricePerNight × nights)
         */
        public BigDecimal calculateTotal(long nights) {
            if (pricePerNight.get() == null || nights <= 0) {
                return BigDecimal.ZERO;
            }
            return pricePerNight.get()
                    .multiply(BigDecimal.valueOf(quantity.get()))
                    .multiply(BigDecimal.valueOf(nights));
        }

        @Override
        public String toString() {
            return String.format("%s x%d @ $%s/night",
                    roomType.get() != null ? roomType.get().getDisplayName() : "Unknown",
                    quantity.get(),
                    pricePerNight.get() != null ? pricePerNight.get().toString() : "0");
        }
    }
}