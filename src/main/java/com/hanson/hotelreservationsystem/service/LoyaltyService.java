package com.hanson.hotelreservationsystem.service;

import com.hanson.hotelreservationsystem.model.Guest;
import com.hanson.hotelreservationsystem.model.LoyaltyAccount;
import com.hanson.hotelreservationsystem.model.LoyaltyTransaction;
import com.hanson.hotelreservationsystem.model.Reservation;
import com.hanson.hotelreservationsystem.model.enums.LoyaltyTransactionType;
import com.hanson.hotelreservationsystem.repository.GuestRepository;
import com.hanson.hotelreservationsystem.repository.LoyaltyAccountRepository;
import com.hanson.hotelreservationsystem.repository.LoyaltyTransactionRepository;
import com.hanson.hotelreservationsystem.config.LoyaltyConfig;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Service for managing the hotel loyalty program.
 *
 * Responsibilities:
 * - Enroll guests in the loyalty program and issue loyalty numbers
 * - Calculate and award points based on payment amounts
 * - Redeem points for discounts with configurable caps
 * - Maintain accurate point balances with audit trails
 * - Provide loyalty dashboard data (balances, earning history, redemption activity)
 *
 * Business Rules:
 * - Points are earned per paid amount using a configurable rate (default: 1 point per $1)
 * - Points redemption is capped per reservation (configurable maximum)
 * - Points redemption value is configurable (default: 1 point = $0.01 discount)
 * - All transactions are logged for audit purposes
 *
 * Pattern: Singleton (accessed via getInstance())
 */
public class LoyaltyService {

    private static final Logger LOGGER = Logger.getLogger(LoyaltyService.class.getName());

    // Singleton instance
    private static LoyaltyService instance;

    // Dependencies (injected via constructor or setters)
    private LoyaltyAccountRepository loyaltyAccountRepository;
    private LoyaltyTransactionRepository transactionRepository;

    // Configuration
    private final LoyaltyConfig config;

    // Default configuration values
    private static final BigDecimal DEFAULT_EARNING_RATE = BigDecimal.ONE; // 1 point per $1
    private static final BigDecimal DEFAULT_REDEMPTION_VALUE = new BigDecimal("0.01"); // 1 point = $0.01
    private static final int DEFAULT_MAX_REDEMPTION_POINTS = 10000; // Max points per reservation
    private static final int DEFAULT_WELCOME_BONUS = 100; // Bonus points on enrollment

    /**
     * Private constructor for Singleton pattern.
     */
    private LoyaltyService() {
        this.config = new LoyaltyConfig();
        initializeDefaultConfig();

        // FIX: Auto-wire the singleton repository
        this.loyaltyAccountRepository = LoyaltyAccountRepository.getInstance();

        // Note: LoyaltyTransactionRepository is not a singleton in your code,
        // so we need to grab the EntityManager from the AccountRepo to create it
        if (this.loyaltyAccountRepository.getEntityManager() != null) {
            this.transactionRepository = new LoyaltyTransactionRepository(
                    this.loyaltyAccountRepository.getEntityManager()
            );
        }
    }

    /**
     * Constructor with dependency injection.
     */
    public LoyaltyService(LoyaltyAccountRepository loyaltyAccountRepository,
                          LoyaltyTransactionRepository transactionRepository,
                          LoyaltyConfig config) {
        this.loyaltyAccountRepository = loyaltyAccountRepository;
        this.transactionRepository = transactionRepository;
        this.config = config != null ? config : new LoyaltyConfig();
        initializeDefaultConfig();
    }

    /**
     * Get the singleton instance.
     */
    public static synchronized LoyaltyService getInstance() {
        if (instance == null) {
            instance = new LoyaltyService();
        }
        return instance;
    }

    /**
     * Initialize default configuration values.
     */
    private void initializeDefaultConfig() {
        if (config.getEarningRate() == null) {
            config.setEarningRate(DEFAULT_EARNING_RATE);
        }
        if (config.getRedemptionValue() == null) {
            config.setRedemptionValue(DEFAULT_REDEMPTION_VALUE);
        }
        if (config.getMaxRedemptionPoints() == 0) {
            config.setMaxRedemptionPoints(DEFAULT_MAX_REDEMPTION_POINTS);
        }
        if (config.getWelcomeBonus() == 0) {
            config.setWelcomeBonus(DEFAULT_WELCOME_BONUS);
        }
    }

    // ==================== Member Lookup Methods ====================

    /**
     * Find a loyalty account by email address.
     *
     * @param email The email address to search for
     * @return Optional containing the LoyaltyAccount if found
     */
    public Optional<LoyaltyAccount> findAccountByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }

        LOGGER.info("Searching for loyalty account by email: " + maskEmail(email));

        if (loyaltyAccountRepository != null) {
            return loyaltyAccountRepository.findByGuestEmail(email.trim().toLowerCase());
        }
        return Optional.empty();
    }

    /**
     * Find a loyalty account by phone number.
     *
     * @param phone The phone number to search for
     * @return Optional containing the LoyaltyAccount if found
     */
    public Optional<LoyaltyAccount> findAccountByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return Optional.empty();
        }

        String normalizedPhone = normalizePhoneNumber(phone);
        LOGGER.info("Searching for loyalty account by phone: " + maskPhone(normalizedPhone));

        if (loyaltyAccountRepository != null) {
            return loyaltyAccountRepository.findByGuestPhone(normalizedPhone);
        }
        return Optional.empty();
    }

    /**
     * Find a loyalty account by email or phone.
     *
     * @param email The email address to search for
     * @param phone The phone number to search for
     * @return Optional containing the LoyaltyAccount if found
     */
    public Optional<LoyaltyAccount> findAccountByEmailOrPhone(String email, String phone) {
        // Try email first
        Optional<LoyaltyAccount> account = findAccountByEmail(email);
        if (account.isPresent()) {
            return account;
        }

        // Try phone
        return findAccountByPhone(phone);
    }

    /**
     * Find a loyalty account by loyalty number.
     *
     * @param loyaltyNumber The loyalty number to search for
     * @return Optional containing the LoyaltyAccount if found
     */
    public Optional<LoyaltyAccount> findAccountByLoyaltyNumber(String loyaltyNumber) {
        if (loyaltyNumber == null || loyaltyNumber.trim().isEmpty()) {
            return Optional.empty();
        }

        LOGGER.info("Searching for loyalty account by number: " + loyaltyNumber);

        if (loyaltyAccountRepository != null) {
            return loyaltyAccountRepository.findByLoyaltyNumber(loyaltyNumber.trim().toUpperCase());
        }
        return Optional.empty();
    }

    /**
     * Check if a guest has a loyalty account.
     *
     * @param guest The guest to check
     * @return true if the guest has a loyalty account
     */
    public boolean hasAccount(Guest guest) {
        if (guest == null) {
            return false;
        }
        return findAccountByEmailOrPhone(guest.getEmail(), guest.getPhone()).isPresent();
    }

    // ==================== Enrollment Methods ====================

    /**
     * Enroll a guest in the loyalty program.
     *
     * @param guest The guest to enroll
     * @return The newly created LoyaltyAccount
     * @throws IllegalArgumentException if the guest is already enrolled
     * @throws IllegalArgumentException if required guest information is missing
     */
    public LoyaltyAccount enrollGuest(Guest guest) {
        validateGuestForEnrollment(guest);

        if (loyaltyAccountRepository == null) {
            throw new IllegalStateException("LoyaltyAccountRepository is not initialized!");
        }

        if (hasAccount(guest)) {
            throw new IllegalArgumentException("Guest is already enrolled in the loyalty program");
        }

        // 1. Create Account
        LoyaltyAccount account = new LoyaltyAccount(guest);
        account.setPointsBalance(0);
        account.setLifetimePoints(0);
        account.setTier("BRONZE");
        account.setActive(true);

        guest.setLoyaltyMember(true);
        guest.setLoyaltyAccount(account);

        // 2. Initial Save (To generate ID)
        account = loyaltyAccountRepository.save(account);

        GuestRepository.getInstance().save(guest);

        LOGGER.info("Enrolled new loyalty account: " + account.getLoyaltyNumber() + " for " + guest.getFullName());

        // 3. Handle Welcome Bonus
        if (config.getWelcomeBonus() > 0) {
            // Update balance in object
            account.earnPoints(config.getWelcomeBonus(), 1.0);

            // Create Transaction Record
            LoyaltyTransaction transaction = new LoyaltyTransaction();
            transaction.setLoyaltyAccount(account); // Link to Parent
            transaction.setTransactionType(LoyaltyTransactionType.BONUS);
            transaction.setPoints(config.getWelcomeBonus());
            transaction.setDescription("Welcome bonus for joining ARC Rewards");
            transaction.setBalanceAfter(account.getPointsBalance());

            // Link Parent to Child
            account.addTransaction(transaction);

        }

        // 4. Final Save (Updates balance and saves transaction if Cascading is on)
        if (loyaltyAccountRepository != null) {
            account = loyaltyAccountRepository.save(account);
        }

        return account;
    }

    /**
     * Validate guest information for enrollment.
     */
    private void validateGuestForEnrollment(Guest guest) {
        if (guest == null) {
            throw new IllegalArgumentException("Guest cannot be null");
        }
        if (guest.getEmail() == null || guest.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required for loyalty enrollment");
        }
        if (guest.getFirstName() == null || guest.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required for loyalty enrollment");
        }
        if (guest.getLastName() == null || guest.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required for loyalty enrollment");
        }
    }

    // ==================== Points Earning Methods ====================

    /**
     * Calculate points to earn for a payment amount.
     *
     * @param paymentAmount The payment amount
     * @return The number of points to earn
     */
    public int calculatePointsToEarn(BigDecimal paymentAmount) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        BigDecimal points = paymentAmount.multiply(config.getEarningRate());
        return points.setScale(0, RoundingMode.FLOOR).intValue();
    }

    /**
     * Award points to a loyalty account for a payment.
     * Uses the LoyaltyAccount's built-in earnPoints method which handles tier bonuses.
     *
     * @param account The loyalty account
     * @param paymentAmount The payment amount
     * @param reservation The associated reservation (optional)
     * @return The number of points awarded
     */
    public int awardPointsForPayment(LoyaltyAccount account, BigDecimal paymentAmount, Reservation reservation) {
        if (account == null || !account.isActive()) {
            LOGGER.warning("Cannot award points: account is null or inactive");
            return 0;
        }

        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        // Use LoyaltyAccount's earnPoints method which handles tier bonuses
        double earningRate = config.getEarningRate().doubleValue() * account.getTierBonusMultiplier();
        int pointsAwarded = account.earnPoints(paymentAmount.doubleValue(), earningRate);

        // Create transaction record
        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.setLoyaltyAccount(account);
        transaction.setTransactionType(LoyaltyTransactionType.EARN);
        transaction.setPoints(pointsAwarded);
        transaction.setDescription(String.format("Points earned for payment of $%.2f", paymentAmount));
        transaction.setReservationId(reservation != null ? reservation.getId() : null);
        transaction.setBalanceAfter(account.getPointsBalance());
        account.addTransaction(transaction);

        // Save account
        if (loyaltyAccountRepository != null) {
            loyaltyAccountRepository.save(account);
        }

        LOGGER.info(String.format("Awarded %d points to account %s. New balance: %d",
                pointsAwarded, account.getLoyaltyNumber(), account.getPointsBalance()));

        return pointsAwarded;
    }

    /**
     * Award bonus points to a loyalty account (for promotions, adjustments, etc.).
     *
     * @param account The loyalty account
     * @param points The number of points to award
     * @param description Description of the transaction
     * @param reservation The associated reservation (optional)
     */
    public void awardBonusPoints(LoyaltyAccount account, int points, String description, Reservation reservation) {
        if (account == null || points <= 0) {
            return;
        }

        // Use earnPoints with 1:1 rate for bonus
        account.earnPoints(points, 1.0);

        // Create transaction record
        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.setLoyaltyAccount(account);
        transaction.setTransactionType(LoyaltyTransactionType.BONUS);
        transaction.setPoints(points);
        transaction.setDescription(description);
        transaction.setReservationId(reservation != null ? reservation.getId() : null);
        transaction.setBalanceAfter(account.getPointsBalance());
        account.addTransaction(transaction);

        // Save account
        if (loyaltyAccountRepository != null) {
            loyaltyAccountRepository.save(account);
        }

        LOGGER.info(String.format("Awarded %d bonus points to account %s. New balance: %d",
                points, account.getLoyaltyNumber(), account.getPointsBalance()));
    }

    // ==================== Points Redemption Methods ====================

    /**
     * Calculate the discount value for a given number of points.
     *
     * @param points The number of points to redeem
     * @return The discount amount in dollars
     */
    public BigDecimal calculateRedemptionValue(int points) {
        if (points <= 0) {
            return BigDecimal.ZERO;
        }
        return config.getRedemptionValue().multiply(new BigDecimal(points))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate the maximum points that can be redeemed for a reservation.
     *
     * @param account The loyalty account
     * @param reservationTotal The total reservation amount
     * @return The maximum points that can be redeemed
     */
    public int calculateMaxRedeemablePoints(LoyaltyAccount account, BigDecimal reservationTotal) {
        if (account == null || !account.isActive()) {
            return 0;
        }

        // Limit by account's balance
        int maxByBalance = account.getPointsBalance();

        // Limit by configuration cap
        int maxByCap = config.getMaxRedemptionPoints();

        // Limit by reservation total (cannot redeem more than the reservation value)
        // Using the account's conversion rate
        double conversionRate = 100.0; // Default: 100 points = $1
        int maxByReservation = (int) (reservationTotal.doubleValue() * conversionRate);

        // Return the minimum of all limits
        return Math.min(Math.min(maxByBalance, maxByCap), maxByReservation);
    }

    /**
     * Redeem points for a discount.
     * Uses the LoyaltyAccount's built-in redeemPoints method.
     *
     * @param account The loyalty account
     * @param pointsToRedeem The number of points to redeem
     * @param reservation The associated reservation
     * @return The discount amount
     * @throws IllegalArgumentException if redemption is invalid
     */
    public BigDecimal redeemPoints(LoyaltyAccount account, int pointsToRedeem, Reservation reservation) {
        validateRedemption(account, pointsToRedeem, reservation);

        // Use the account's redeemPoints method which enforces caps
        int actualRedeemed = account.redeemPoints(pointsToRedeem, config.getMaxRedemptionPoints());

        // Calculate discount using the account's method
        double conversionRate = 100.0; // Default: 100 points = $1
        BigDecimal discountAmount = BigDecimal.valueOf(
                        account.calculateDiscountFromPoints(actualRedeemed, conversionRate))
                .setScale(2, RoundingMode.HALF_UP);

        // Create transaction record
        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.setLoyaltyAccount(account);
        transaction.setTransactionType(LoyaltyTransactionType.REDEEM);
        transaction.setPoints(-actualRedeemed);
        transaction.setDescription(String.format("Redeemed %d points for $%.2f discount",
                actualRedeemed, discountAmount));
        transaction.setReservationId(reservation != null ? reservation.getId() : null);
        transaction.setBalanceAfter(account.getPointsBalance());
        account.addTransaction(transaction);

        // Save account
        if (loyaltyAccountRepository != null) {
            loyaltyAccountRepository.save(account);
        }

        LOGGER.info(String.format("Redeemed %d points for account %s. Discount: $%.2f. New balance: %d",
                actualRedeemed, account.getLoyaltyNumber(), discountAmount, account.getPointsBalance()));

        return discountAmount;
    }

    /**
     * Validate a points redemption request.
     */
    private void validateRedemption(LoyaltyAccount account, int pointsToRedeem, Reservation reservation) {
        if (account == null) {
            throw new IllegalArgumentException("Loyalty account cannot be null");
        }
        if (!account.isActive()) {
            throw new IllegalArgumentException("Loyalty account is not active");
        }
        if (pointsToRedeem <= 0) {
            throw new IllegalArgumentException("Points to redeem must be positive");
        }
        if (!account.hasEnoughPoints(pointsToRedeem)) {
            throw new IllegalArgumentException("Insufficient points balance. Available: " +
                    account.getPointsBalance() + ", Requested: " + pointsToRedeem);
        }
        if (pointsToRedeem > config.getMaxRedemptionPoints()) {
            throw new IllegalArgumentException("Exceeds maximum redemption limit of " +
                    config.getMaxRedemptionPoints() + " points per reservation");
        }
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation is required for points redemption");
        }
    }

    // ==================== Dashboard Methods ====================

    /**
     * Get the transaction history for a loyalty account.
     *
     * @param account The loyalty account
     * @return List of loyalty transactions
     */
    public List<LoyaltyTransaction> getTransactionHistory(LoyaltyAccount account) {
        if (account == null) {
            return List.of();
        }
        // Use the account's built-in transaction list (already loaded via relationship)
        return account.getTransactions();
    }

    /**
     * Get earning history for a loyalty account.
     *
     * @param account The loyalty account
     * @return List of earning transactions
     */
    public List<LoyaltyTransaction> getEarningHistory(LoyaltyAccount account) {
        if (account == null) {
            return List.of();
        }
        return account.getTransactions().stream()
                .filter(t -> t.getTransactionType() == LoyaltyTransactionType.EARN ||
                        t.getTransactionType() == LoyaltyTransactionType.BONUS)
                .toList();
    }

    /**
     * Get redemption history for a loyalty account.
     *
     * @param account The loyalty account
     * @return List of redemption transactions
     */
    public List<LoyaltyTransaction> getRedemptionHistory(LoyaltyAccount account) {
        if (account == null) {
            return List.of();
        }
        return account.getTransactions().stream()
                .filter(t -> t.getTransactionType() == LoyaltyTransactionType.REDEEM)
                .toList();
    }

    /**
     * Get all active loyalty accounts.
     *
     * @return List of active loyalty accounts
     */
    public List<LoyaltyAccount> getAllActiveAccounts() {
        if (loyaltyAccountRepository == null) {
            return List.of();
        }
        return loyaltyAccountRepository.findByActiveTrue();
    }

    /**
     * Get loyalty program statistics.
     *
     * @return LoyaltyStats object with program statistics
     */
    public LoyaltyStats getLoyaltyStats() {
        LoyaltyStats stats = new LoyaltyStats();

        if (loyaltyAccountRepository != null) {
            stats.setTotalMembers(loyaltyAccountRepository.countByActiveTrue());
            stats.setTotalPointsIssued(loyaltyAccountRepository.sumLifetimePoints());
            stats.setTotalPointsOutstanding(loyaltyAccountRepository.sumPointsBalance());
        }

        return stats;
    }

    // ==================== Utility Methods ====================
    /**
     * Generate a unique loyalty number.
     * Format: LOY + Current Timestamp (digits)
     */
    private String generateLoyaltyNumber() {
        // Generates a string like "LOY1701234567"
        // Uses system time to ensure relative uniqueness for this project scope
        long number = System.currentTimeMillis();
        return "LOY" + number;
    }

    /**
     * Normalize phone number for consistent storage and lookup.
     */
    private String normalizePhoneNumber(String phone) {
        if (phone == null) {
            return null;
        }
        // Remove all non-digit characters
        return phone.replaceAll("[^0-9]", "");
    }

    /**
     * Mask email for logging (privacy protection).
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) {
            return "***" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    /**
     * Mask phone for logging (privacy protection).
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }
        return "***" + phone.substring(phone.length() - 4);
    }

    // ==================== Setters for Dependency Injection ====================

    public void setLoyaltyAccountRepository(LoyaltyAccountRepository loyaltyAccountRepository) {
        this.loyaltyAccountRepository = loyaltyAccountRepository;
    }

    public void setTransactionRepository(LoyaltyTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // ==================== Getters for Configuration ====================

    public LoyaltyConfig getConfig() {
        return config;
    }

    public BigDecimal getEarningRate() {
        return config.getEarningRate();
    }

    public BigDecimal getRedemptionValue() {
        return config.getRedemptionValue();
    }

    public int getMaxRedemptionPoints() {
        return config.getMaxRedemptionPoints();
    }

    // ==================== Inner Classes ====================

    /**
     * Statistics for the loyalty program dashboard.
     */
    public static class LoyaltyStats {
        private long totalMembers;
        private long totalPointsIssued;
        private long totalPointsOutstanding;

        public long getTotalMembers() {
            return totalMembers;
        }

        public void setTotalMembers(long totalMembers) {
            this.totalMembers = totalMembers;
        }

        public long getTotalPointsIssued() {
            return totalPointsIssued;
        }

        public void setTotalPointsIssued(long totalPointsIssued) {
            this.totalPointsIssued = totalPointsIssued;
        }

        public long getTotalPointsRedeemed() {
            // Calculate redeemed as: lifetime - current balance
            return totalPointsIssued - totalPointsOutstanding;
        }

        public long getTotalPointsOutstanding() {
            return totalPointsOutstanding;
        }

        public void setTotalPointsOutstanding(long totalPointsOutstanding) {
            this.totalPointsOutstanding = totalPointsOutstanding;
        }

        public BigDecimal getTotalPointsValue() {
            // 100 points = $1 by default
            return BigDecimal.valueOf(totalPointsOutstanding / 100.0)
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }
}