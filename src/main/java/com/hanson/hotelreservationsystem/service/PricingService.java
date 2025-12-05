package com.hanson.hotelreservationsystem.service;

import com.hanson.hotelreservationsystem.model.Reservation;
import com.hanson.hotelreservationsystem.model.enums.RoomType;
import com.hanson.hotelreservationsystem.config.PricingConfig;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Service for calculating room prices with dynamic pricing support.
 *
 * Responsibilities:
 * - Calculate room prices based on room type and dates
 * - Apply dynamic pricing (weekday/weekend/seasonal multipliers)
 * - Calculate stay totals with tax
 * - Support different billing strategies
 * - Provide pricing breakdowns for invoices
 *
 * Patterns Used:
 * - Singleton: Single instance accessed via getInstance()
 * - Strategy: Supports different billing calculation strategies
 *
 * Business Rules:
 * - Weekend pricing (Fri-Sun): 20% higher than weekday
 * - Seasonal pricing: Configurable multiplier during peak periods
 * - Tax rate: 13% HST (configurable)
 * - Prices rounded to 2 decimal places
 */
public class PricingService {

    private static final Logger LOGGER = Logger.getLogger(PricingService.class.getName());

    // Singleton instance
    private static PricingService instance;

    // Configuration
    private PricingConfig pricingConfig;

    // Default tax rate (13% HST)
    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("0.13");

    // Billing strategy (for Strategy pattern)
    private BillingStrategy billingStrategy;

    /**
     * Private constructor for Singleton pattern.
     */
    private PricingService() {
        this.pricingConfig = new PricingConfig();
        this.billingStrategy = new StandardBillingStrategy();
    }

    /**
     * Constructor with dependency injection.
     */
    public PricingService(PricingConfig pricingConfig) {
        this.pricingConfig = pricingConfig != null ? pricingConfig : new PricingConfig();
        this.billingStrategy = new StandardBillingStrategy();
    }

    /**
     * Get the singleton instance.
     */
    public static synchronized PricingService getInstance() {
        if (instance == null) {
            instance = new PricingService();
        }
        return instance;
    }

    // ==================== Room Price Methods ====================

    /**
     * Get the base price for a room type.
     *
     * @param roomType The room type
     * @return Base price per night
     */
    public BigDecimal getBasePrice(RoomType roomType) {
        return BigDecimal.valueOf(roomType.getBasePrice());
    }

    /**
     * Get the price for a room type on a specific date.
     * Applies dynamic pricing multipliers.
     *
     * @param roomType The room type
     * @param date The date
     * @return Price for that night including dynamic pricing
     */
    public BigDecimal getRoomPrice(RoomType roomType, LocalDate date) {
        BigDecimal basePrice = getBasePrice(roomType);
        BigDecimal multiplier = calculatePriceMultiplier(date);

        return basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate the price multiplier for a specific date.
     *
     * @param date The date
     * @return Combined multiplier for the date
     */
    public BigDecimal calculatePriceMultiplier(LocalDate date) {
        BigDecimal multiplier = BigDecimal.ONE;

        // Apply weekday/weekend multiplier
        if (isWeekend(date)) {
            multiplier = multiplier.multiply(pricingConfig.getWeekendMultiplier());
        } else {
            multiplier = multiplier.multiply(pricingConfig.getWeekdayMultiplier());
        }

        // Apply seasonal multiplier if applicable
        if (isSeasonalPeriod(date)) {
            multiplier = multiplier.multiply(getSeasonalMultiplier(date));
        }

        return multiplier;
    }

    /**
     * Check if a date is a weekend (Friday, Saturday, or Sunday).
     *
     * @param date The date to check
     * @return true if the date is a weekend
     */
    public boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    /**
     * Check if a date falls within a seasonal pricing period.
     *
     * @param date The date to check
     * @return true if the date is in a seasonal period
     */
    public boolean isSeasonalPeriod(LocalDate date) {
        if (pricingConfig == null || pricingConfig.getSeasonalPeriods() == null) {
            return false;
        }

        for (PricingConfig.SeasonalPeriod period : pricingConfig.getSeasonalPeriods()) {
            if (period.contains(date)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get the seasonal multiplier for a specific date.
     *
     * @param date The date
     * @return Seasonal multiplier (or 1.0 if not in a seasonal period)
     */
    public BigDecimal getSeasonalMultiplier(LocalDate date) {
        if (pricingConfig == null || pricingConfig.getSeasonalPeriods() == null) {
            return BigDecimal.ONE;
        }

        for (PricingConfig.SeasonalPeriod period : pricingConfig.getSeasonalPeriods()) {
            if (period.contains(date)) {
                return period.getMultiplier();
            }
        }

        return BigDecimal.ONE;
    }

    // ==================== Stay Calculation Methods ====================

    /**
     * Calculate the total price for a room stay.
     *
     * @param roomType The room type
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     * @return Total price for the stay (before tax)
     */
    public BigDecimal calculateStayPrice(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            total = total.add(getRoomPrice(roomType, current));
            current = current.plusDays(1);
        }

        return total;
    }

    /**
     * Calculate the number of nights for a stay.
     *
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     * @return Number of nights
     */
    public long calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /**
     * Get a detailed price breakdown for a stay.
     *
     * @param roomType The room type
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     * @return PriceBreakdown with detailed pricing information
     */
    public PriceBreakdown getStayPriceBreakdown(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        PriceBreakdown breakdown = new PriceBreakdown();
        breakdown.setRoomType(roomType);
        breakdown.setCheckIn(checkIn);
        breakdown.setCheckOut(checkOut);

        long nights = calculateNights(checkIn, checkOut);
        breakdown.setNights(nights);

        // Calculate night-by-night prices
        BigDecimal subtotal = BigDecimal.ZERO;
        int weekendNights = 0;
        int weekdayNights = 0;

        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            BigDecimal nightPrice = getRoomPrice(roomType, current);
            subtotal = subtotal.add(nightPrice);

            if (isWeekend(current)) {
                weekendNights++;
            } else {
                weekdayNights++;
            }

            current = current.plusDays(1);
        }

        breakdown.setSubtotal(subtotal);
        breakdown.setWeekendNights(weekendNights);
        breakdown.setWeekdayNights(weekdayNights);

        // Calculate tax
        BigDecimal taxRate = getTaxRate();
        BigDecimal taxAmount = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        breakdown.setTaxRate(taxRate);
        breakdown.setTaxAmount(taxAmount);

        // Calculate total
        breakdown.setTotal(subtotal.add(taxAmount));

        return breakdown;
    }

    // ==================== Multi-Room Calculation Methods ====================

    /**
     * Calculate total price for multiple rooms.
     *
     * @param roomSelections Map of room type to quantity
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     * @return Total price for all rooms (before tax)
     */
    public BigDecimal calculateMultiRoomPrice(Map<RoomType, Integer> roomSelections,
                                              LocalDate checkIn, LocalDate checkOut) {
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<RoomType, Integer> entry : roomSelections.entrySet()) {
            RoomType roomType = entry.getKey();
            int quantity = entry.getValue();

            BigDecimal roomTotal = calculateStayPrice(roomType, checkIn, checkOut);
            total = total.add(roomTotal.multiply(BigDecimal.valueOf(quantity)));
        }

        return total;
    }

    /**
     * Calculate total price per night for multiple rooms.
     *
     * @param roomSelections Map of room type to quantity
     * @param date The date to calculate for
     * @return Total price per night for all rooms
     */
    public BigDecimal calculatePerNightPrice(Map<RoomType, Integer> roomSelections, LocalDate date) {
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<RoomType, Integer> entry : roomSelections.entrySet()) {
            RoomType roomType = entry.getKey();
            int quantity = entry.getValue();

            BigDecimal roomPrice = getRoomPrice(roomType, date);
            total = total.add(roomPrice.multiply(BigDecimal.valueOf(quantity)));
        }

        return total;
    }

    /**
     * Get average nightly rate for a room type over a date range.
     *
     * @param roomType The room type
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     * @return Average nightly rate
     */
    public BigDecimal getAverageNightlyRate(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal total = calculateStayPrice(roomType, checkIn, checkOut);
        long nights = calculateNights(checkIn, checkOut);

        if (nights == 0) {
            return getBasePrice(roomType);
        }

        return total.divide(BigDecimal.valueOf(nights), 2, RoundingMode.HALF_UP);
    }

    // ==================== Tax Methods ====================

    /**
     * Get the current tax rate.
     *
     * @return Tax rate as a decimal (e.g., 0.13 for 13%)
     */
    public BigDecimal getTaxRate() {
        if (pricingConfig != null && pricingConfig.getTaxRate() != null) {
            return pricingConfig.getTaxRate();
        }
        return DEFAULT_TAX_RATE;
    }

    /**
     * Calculate tax for a given amount.
     *
     * @param amount The amount to calculate tax on
     * @return Tax amount
     */
    public BigDecimal calculateTax(BigDecimal amount) {
        return amount.multiply(getTaxRate()).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate total with tax.
     *
     * @param subtotal The subtotal before tax
     * @return Total including tax
     */
    public BigDecimal calculateTotalWithTax(BigDecimal subtotal) {
        BigDecimal tax = calculateTax(subtotal);
        return subtotal.add(tax);
    }

    // ==================== Discount Methods ====================

    /**
     * Apply a percentage discount to an amount.
     *
     * @param amount The original amount
     * @param discountPercent The discount percentage (e.g., 15 for 15%)
     * @return Discounted amount
     */
    public BigDecimal applyDiscount(BigDecimal amount, BigDecimal discountPercent) {
        if (discountPercent == null || discountPercent.compareTo(BigDecimal.ZERO) <= 0) {
            return amount;
        }

        BigDecimal discountFactor = BigDecimal.ONE.subtract(
                discountPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));

        return amount.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate the discount amount for a given percentage.
     *
     * @param amount The original amount
     * @param discountPercent The discount percentage
     * @return The discount amount
     */
    public BigDecimal calculateDiscountAmount(BigDecimal amount, BigDecimal discountPercent) {
        if (discountPercent == null || discountPercent.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return amount.multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Apply a loyalty points discount.
     *
     * @param amount The original amount
     * @param pointsToRedeem Number of points to redeem
     * @param pointsConversionRate Points per dollar (e.g., 100 points = $1)
     * @return Discounted amount
     */
    public BigDecimal applyLoyaltyDiscount(BigDecimal amount, int pointsToRedeem, double pointsConversionRate) {
        if (pointsToRedeem <= 0 || pointsConversionRate <= 0) {
            return amount;
        }

        BigDecimal discount = BigDecimal.valueOf(pointsToRedeem / pointsConversionRate)
                .setScale(2, RoundingMode.HALF_UP);

        // Don't allow discount to exceed the amount
        if (discount.compareTo(amount) > 0) {
            discount = amount;
        }

        return amount.subtract(discount);
    }

    // ==================== Strategy Pattern Methods ====================

    /**
     * Set the billing strategy.
     *
     * @param strategy The billing strategy to use
     */
    public void setBillingStrategy(BillingStrategy strategy) {
        this.billingStrategy = strategy;
    }

    /**
     * Calculate total using the current billing strategy.
     *
     * @param reservation The reservation to calculate
     * @return Calculated total
     */
    public BigDecimal calculateWithStrategy(Reservation reservation) {
        if (billingStrategy == null) {
            billingStrategy = new StandardBillingStrategy();
        }
        return billingStrategy.calculate(reservation, this);
    }

    // ==================== Invoice Generation ====================

    /**
     * Generate an invoice summary for a reservation.
     *
     * @param reservation The reservation
     * @return InvoiceSummary with all pricing details
     */
    public InvoiceSummary generateInvoiceSummary(Reservation reservation) {
        InvoiceSummary invoice = new InvoiceSummary();

        invoice.setConfirmationNumber(reservation.getConfirmationNumber());
        invoice.setCheckIn(reservation.getCheckInDate());
        invoice.setCheckOut(reservation.getCheckOutDate());
        invoice.setNights(reservation.getNumberOfNights());

        invoice.setRoomSubtotal(reservation.getSubtotal());
        invoice.setAddOnsTotal(reservation.getAddOnsTotal());
        invoice.setDiscountAmount(reservation.getDiscountAmount());
        invoice.setDiscountPercentage(reservation.getDiscountPercentage());
        invoice.setLoyaltyDiscount(reservation.getLoyaltyDiscount());
        invoice.setTaxAmount(reservation.getTaxAmount());
        invoice.setTotalAmount(reservation.getTotalAmount());
        invoice.setAmountPaid(reservation.getAmountPaid());
        invoice.setBalanceDue(reservation.getOutstandingBalance());

        return invoice;
    }

    // ==================== Configuration Methods ====================

    /**
     * Set the pricing configuration.
     */
    public void setPricingConfig(PricingConfig pricingConfig) {
        this.pricingConfig = pricingConfig;
    }

    /**
     * Get the pricing configuration.
     */
    public PricingConfig getPricingConfig() {
        return pricingConfig;
    }

    // ==================== Inner Classes ====================

    /**
     * Detailed price breakdown for a stay.
     */
    public static class PriceBreakdown {
        private RoomType roomType;
        private LocalDate checkIn;
        private LocalDate checkOut;
        private long nights;
        private int weekdayNights;
        private int weekendNights;
        private BigDecimal subtotal;
        private BigDecimal taxRate;
        private BigDecimal taxAmount;
        private BigDecimal total;

        // Getters and setters
        public RoomType getRoomType() { return roomType; }
        public void setRoomType(RoomType roomType) { this.roomType = roomType; }

        public LocalDate getCheckIn() { return checkIn; }
        public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }

        public LocalDate getCheckOut() { return checkOut; }
        public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }

        public long getNights() { return nights; }
        public void setNights(long nights) { this.nights = nights; }

        public int getWeekdayNights() { return weekdayNights; }
        public void setWeekdayNights(int weekdayNights) { this.weekdayNights = weekdayNights; }

        public int getWeekendNights() { return weekendNights; }
        public void setWeekendNights(int weekendNights) { this.weekendNights = weekendNights; }

        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

        public BigDecimal getTaxRate() { return taxRate; }
        public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }

        public BigDecimal getTaxAmount() { return taxAmount; }
        public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

        public BigDecimal getTotal() { return total; }
        public void setTotal(BigDecimal total) { this.total = total; }

        public BigDecimal getAverageNightlyRate() {
            if (nights == 0) return BigDecimal.ZERO;
            return subtotal.divide(BigDecimal.valueOf(nights), 2, RoundingMode.HALF_UP);
        }

        @Override
        public String toString() {
            return String.format("PriceBreakdown{%s, %d nights, subtotal=$%.2f, tax=$%.2f, total=$%.2f}",
                    roomType, nights, subtotal, taxAmount, total);
        }
    }

    /**
     * Invoice summary for a reservation.
     */
    public static class InvoiceSummary {
        private String confirmationNumber;
        private LocalDate checkIn;
        private LocalDate checkOut;
        private long nights;
        private BigDecimal roomSubtotal;
        private BigDecimal addOnsTotal;
        private BigDecimal discountAmount;
        private BigDecimal discountPercentage;
        private BigDecimal loyaltyDiscount;
        private BigDecimal taxAmount;
        private BigDecimal totalAmount;
        private BigDecimal amountPaid;
        private BigDecimal balanceDue;

        // Getters and setters
        public String getConfirmationNumber() { return confirmationNumber; }
        public void setConfirmationNumber(String confirmationNumber) { this.confirmationNumber = confirmationNumber; }

        public LocalDate getCheckIn() { return checkIn; }
        public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }

        public LocalDate getCheckOut() { return checkOut; }
        public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }

        public long getNights() { return nights; }
        public void setNights(long nights) { this.nights = nights; }

        public BigDecimal getRoomSubtotal() { return roomSubtotal; }
        public void setRoomSubtotal(BigDecimal roomSubtotal) { this.roomSubtotal = roomSubtotal; }

        public BigDecimal getAddOnsTotal() { return addOnsTotal; }
        public void setAddOnsTotal(BigDecimal addOnsTotal) { this.addOnsTotal = addOnsTotal; }

        public BigDecimal getDiscountAmount() { return discountAmount; }
        public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

        public BigDecimal getDiscountPercentage() { return discountPercentage; }
        public void setDiscountPercentage(BigDecimal discountPercentage) { this.discountPercentage = discountPercentage; }

        public BigDecimal getLoyaltyDiscount() { return loyaltyDiscount; }
        public void setLoyaltyDiscount(BigDecimal loyaltyDiscount) { this.loyaltyDiscount = loyaltyDiscount; }

        public BigDecimal getTaxAmount() { return taxAmount; }
        public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

        public BigDecimal getAmountPaid() { return amountPaid; }
        public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

        public BigDecimal getBalanceDue() { return balanceDue; }
        public void setBalanceDue(BigDecimal balanceDue) { this.balanceDue = balanceDue; }

        public boolean isFullyPaid() {
            return balanceDue.compareTo(BigDecimal.ZERO) <= 0;
        }

        public BigDecimal getTotalDiscounts() {
            BigDecimal total = BigDecimal.ZERO;
            if (discountAmount != null) total = total.add(discountAmount);
            if (loyaltyDiscount != null) total = total.add(loyaltyDiscount);
            return total;
        }
    }

    // ==================== Strategy Pattern Interfaces ====================

    /**
     * Interface for billing calculation strategies.
     */
    public interface BillingStrategy {
        BigDecimal calculate(Reservation reservation, PricingService pricingService);
    }

    /**
     * Standard billing strategy - calculates based on room prices and add-ons.
     */
    public static class StandardBillingStrategy implements BillingStrategy {
        @Override
        public BigDecimal calculate(Reservation reservation, PricingService pricingService) {
            // Use the reservation's built-in calculation
            reservation.calculateTotal();
            return reservation.getTotalAmount();
        }
    }

    /**
     * Discounted billing strategy - applies a percentage discount.
     */
    public static class DiscountBillingStrategy implements BillingStrategy {
        private final BigDecimal discountPercent;

        public DiscountBillingStrategy(BigDecimal discountPercent) {
            this.discountPercent = discountPercent;
        }

        @Override
        public BigDecimal calculate(Reservation reservation, PricingService pricingService) {
            reservation.setDiscountPercentage(discountPercent);
            reservation.calculateTotal();
            return reservation.getTotalAmount();
        }
    }

    /**
     * Loyalty billing strategy - applies loyalty points discount.
     */
    public static class LoyaltyBillingStrategy implements BillingStrategy {
        private final int pointsToRedeem;
        private final double conversionRate;

        public LoyaltyBillingStrategy(int pointsToRedeem, double conversionRate) {
            this.pointsToRedeem = pointsToRedeem;
            this.conversionRate = conversionRate;
        }

        @Override
        public BigDecimal calculate(Reservation reservation, PricingService pricingService) {
            BigDecimal loyaltyDiscount = BigDecimal.valueOf(pointsToRedeem / conversionRate)
                    .setScale(2, RoundingMode.HALF_UP);

            reservation.setLoyaltyDiscount(loyaltyDiscount);
            reservation.setLoyaltyPointsUsed(pointsToRedeem);
            reservation.calculateTotal();

            return reservation.getTotalAmount();
        }
    }

    /**
     * Combined billing strategy - applies both percentage discount and loyalty discount.
     */
    public static class CombinedBillingStrategy implements BillingStrategy {
        private final BigDecimal discountPercent;
        private final int pointsToRedeem;
        private final double conversionRate;

        public CombinedBillingStrategy(BigDecimal discountPercent, int pointsToRedeem, double conversionRate) {
            this.discountPercent = discountPercent;
            this.pointsToRedeem = pointsToRedeem;
            this.conversionRate = conversionRate;
        }

        @Override
        public BigDecimal calculate(Reservation reservation, PricingService pricingService) {
            // Apply percentage discount
            reservation.setDiscountPercentage(discountPercent);

            // Apply loyalty discount
            BigDecimal loyaltyDiscount = BigDecimal.valueOf(pointsToRedeem / conversionRate)
                    .setScale(2, RoundingMode.HALF_UP);
            reservation.setLoyaltyDiscount(loyaltyDiscount);
            reservation.setLoyaltyPointsUsed(pointsToRedeem);

            reservation.calculateTotal();
            return reservation.getTotalAmount();
        }
    }
}