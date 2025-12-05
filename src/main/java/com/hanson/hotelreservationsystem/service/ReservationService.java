package com.hanson.hotelreservationsystem.service;

import com.hanson.hotelreservationsystem.config.JPAUtil;
import com.hanson.hotelreservationsystem.model.*;
import com.hanson.hotelreservationsystem.model.enums.AddOnType;
import com.hanson.hotelreservationsystem.model.enums.PaymentStatus;
import com.hanson.hotelreservationsystem.model.enums.ReservationStatus;
import com.hanson.hotelreservationsystem.model.enums.RoomStatus;
import com.hanson.hotelreservationsystem.model.enums.RoomType;
import com.hanson.hotelreservationsystem.repository.ReservationRepository;
import com.hanson.hotelreservationsystem.repository.RoomRepository;
import com.hanson.hotelreservationsystem.repository.GuestRepository;
import com.hanson.hotelreservationsystem.session.BookingSession;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service for managing hotel reservations.
 *
 * Responsibilities:
 * - Create reservations from kiosk or admin bookings
 * - Manage reservation lifecycle (confirm, check-in, check-out, cancel)
 * - Process payments and update balances
 * - Award loyalty points upon payment
 * - Validate business rules before operations
 * - Provide search and retrieval methods
 * - Generate reservation reports
 *
 * Patterns Used:
 * - Singleton: Single instance accessed via getInstance()
 * - Observer: Notifies when reservation status changes (via RoomService)
 *
 * Business Rules:
 * - Reservations require at least one room
 * - Check-in requires confirmed or pending status
 * - Check-out requires full payment
 * - Cancellation not allowed after check-in
 * - Loyalty points awarded after successful payment
 */
public class ReservationService {

    private static final Logger LOGGER = Logger.getLogger(ReservationService.class.getName());

    // Singleton instance
    private static ReservationService instance;

    // Dependencies
    private ReservationRepository reservationRepository;
    private RoomRepository roomRepository;
    private GuestRepository guestRepository;
    private LoyaltyService loyaltyService;
    private ValidationService validationService;
    private PricingService pricingService;
    private RoomService roomService;

    // Tax rate (default 13%)
    private static final BigDecimal TAX_RATE = new BigDecimal("0.13");

    /**
     * Private constructor for Singleton pattern.
     */
    private ReservationService() {
        this.validationService = ValidationService.getInstance();
        this.loyaltyService = LoyaltyService.getInstance();
        this.pricingService = PricingService.getInstance();
        this.roomService = RoomService.getInstance();

    }

    /**
     * Constructor with dependency injection.
     */
    public ReservationService(ReservationRepository reservationRepository,
                              RoomRepository roomRepository,
                              GuestRepository guestRepository,
                              LoyaltyService loyaltyService,
                              ValidationService validationService,
                              PricingService pricingService,
                              RoomService roomService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.guestRepository = guestRepository;
        this.loyaltyService = loyaltyService;
        this.validationService = validationService;
        this.pricingService = pricingService;
        this.roomService = roomService;
    }

    /**
     * Get the singleton instance.
     */
    public static synchronized ReservationService getInstance() {
        if (instance == null) {
            instance = new ReservationService();
        }
        return instance;
    }

    // ==================== Reservation Creation ====================

    /**
     * Create a reservation from a BookingSession (kiosk flow).
     *
     * @param session The booking session containing all reservation data
     * @return The confirmation number of the created reservation
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException if rooms are no longer available
     */
    public String createReservation(BookingSession session) {
        LOGGER.info("Creating reservation from booking session");

        // 1. Create a FRESH, TEMPORARY EntityManager for this specific booking
        // This ensures we don't interfere with the Admin Dashboard's open connection
        EntityManager em = JPAUtil.createEntityManager();

        // 2. Create LOCAL repositories.
        // CRITICAL FIX: We do NOT assign these to 'this.reservationRepository'.
        // This keeps the global service safe for the Admin Dashboard.
        ReservationRepository localResRepo = new ReservationRepository(em);
        GuestRepository localGuestRepo = new GuestRepository(em);
        RoomRepository localRoomRepo = new RoomRepository(em);

        try {
            em.getTransaction().begin();

            // --- LOGIC START ---

            // 1. Validate (Same as before)
            validateBookingSession(session);

            // 2. Handle Guest (Re-implemented locally to use localGuestRepo)
            Guest guest = null;
            if (session.getEmail() != null) {
                Optional<Guest> existing = localGuestRepo.findByEmail(session.getEmail());
                if (existing.isPresent()) guest = existing.get();
            }

            if (guest == null) {
                guest = new Guest();
                guest.setFirstName(session.getFirstName());
                guest.setLastName(session.getLastName());
                guest.setEmail(session.getEmail());
                guest.setPhone(session.getPhone());
                guest.setCountry(session.getCountry());
                guest.setAddress(session.getAddress());
                guest.setCity(session.getCity());
                guest.setStateProvince(session.getState());
                guest.setPostalCode(session.getPostalCode());
                guest = localGuestRepo.save(guest); // Save using LOCAL repo
            }

            // 3. Create Reservation Object
            Reservation reservation = new Reservation(
                    guest,
                    session.getCheckInDate(),
                    session.getCheckOutDate(),
                    session.getAdultCount()
            );
            reservation.setNumChildren(session.getChildCount());
            reservation.setBookedViaKiosk(true);
            reservation.setSpecialRequests(session.getSpecialRequests());

            // Save initially to get an ID
            reservation = localResRepo.save(reservation);

            // 4. Assign Rooms (Re-implemented locally to use localRoomRepo)
            for (BookingSession.RoomSelection selection : session.getSelectedRooms()) {
                if (selection.getQuantity() > 0) {
                    // Find strictly available rooms using the local repo
                    List<Room> availableRooms = localRoomRepo.findStrictlyAvailableRooms(
                            selection.getRoomType(),
                            session.getCheckInDate(),
                            session.getCheckOutDate()
                    );

                    if (availableRooms.size() < selection.getQuantity()) {
                        throw new IllegalStateException("Not enough " + selection.getRoomType() + " rooms available.");
                    }

                    // Assign the rooms
                    for (int i = 0; i < selection.getQuantity(); i++) {
                        Room room = availableRooms.get(i);

                        ReservationRoom resRoom = new ReservationRoom();
                        resRoom.setRoom(room);
                        resRoom.setRoomPrice(selection.getPricePerNight()); // Use price from session
                        resRoom.setNumGuests(1); // Default, can be refined
                        resRoom.setReservation(reservation); // Link to reservation

                        reservation.addRoom(resRoom);

                        // Mark room reserved in this transaction
                        room.setStatus(RoomStatus.RESERVED);
                        localRoomRepo.save(room);
                    }
                }
            }

            // 5. Add Services (Standard logic)
            addServicesToReservation(reservation, session);

            // 6. Finalize & Save
            reservation.calculateTotal();
            reservation.setStatus(ReservationStatus.CONFIRMED);

            localResRepo.save(reservation); // Final save

            // --- COMMIT THE TRANSACTION ---
            em.getTransaction().commit();
            LOGGER.info("Reservation created successfully: " + reservation.getConfirmationNumber());

            return reservation.getConfirmationNumber();

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            LOGGER.log(Level.SEVERE, "Failed to create reservation", e);
            throw e; // Re-throw to alert the UI
        } finally {
            // Close only this temporary connection.
            // The Admin Dashboard's connection remains open and healthy.
            em.close();
        }
    }
    /**
     * Create a reservation from admin input.
     *
     * @param guest The guest
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     * @param numAdults Number of adults
     * @param numChildren Number of children
     * @param roomSelections Map of room type to quantity
     * @return The created reservation
     */
    public Reservation createReservation(Guest guest, LocalDate checkIn, LocalDate checkOut,
                                         int numAdults, int numChildren,
                                         Map<RoomType, Integer> roomSelections) {
        LOGGER.info("Creating reservation from admin input");

        // Validate inputs
        if (guest == null) {
            throw new IllegalArgumentException("Guest is required");
        }

        ValidationService.ValidationResult dateValidation =
                validationService.validateDateRange(checkIn, checkOut);
        if (!dateValidation.isValid()) {
            throw new IllegalArgumentException(dateValidation.getFirstErrorMessage());
        }

        // Create reservation
        Reservation reservation = new Reservation(guest, checkIn, checkOut, numAdults);
        reservation.setNumChildren(numChildren);
        reservation.setBookedViaKiosk(false);
        reservation.setStatus(ReservationStatus.PENDING);

        // Assign rooms
        assignRoomsToReservation(reservation, roomSelections, checkIn, checkOut);

        // Calculate totals
        reservation.calculateTotal();

        // Save reservation
        if (reservationRepository != null) {
            reservation = reservationRepository.save(reservation);
        }

        LOGGER.info("Reservation created: " + reservation.getConfirmationNumber());

        return reservation;
    }

    /**
     * Validate booking session data.
     */
    private void validateBookingSession(BookingSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Booking session cannot be null");
        }

        // Validate dates
        ValidationService.ValidationResult dateResult =
                validationService.validateDateRange(session.getCheckInDate(), session.getCheckOutDate());
        if (!dateResult.isValid()) {
            throw new IllegalArgumentException("Invalid dates: " + dateResult);
        }

        // Validate guest count
        if (session.getAdultCount() < 1) {
            throw new IllegalArgumentException("At least one adult is required");
        }

        // Validate room selections
        if (session.getSelectedRooms() == null || session.getSelectedRooms().isEmpty()) {
            throw new IllegalArgumentException("At least one room must be selected");
        }

        // Validate occupancy
        int totalCapacity = session.getSelectedRooms().stream()
                .mapToInt(r -> r.getRoomType().getMaxOccupancy() * r.getQuantity())
                .sum();
        int totalGuests = session.getTotalGuestCount();

        if (totalCapacity < totalGuests) {
            throw new IllegalArgumentException(
                    String.format("Selected rooms can only accommodate %d guests, but %d required",
                            totalCapacity, totalGuests));
        }
    }

    /**
     * Find existing guest or create new one.
     */
    private Guest findOrCreateGuest(BookingSession session) {
        Guest guest = null;

        // Try to find existing guest by email
        if (guestRepository != null && session.getEmail() != null) {
            Optional<Guest> existingGuest = guestRepository.findByEmail(session.getEmail());
            if (existingGuest.isPresent()) {
                guest = existingGuest.get();
                LOGGER.info("Found existing guest: " + guest.getFullName());
            }
        }

        // Create new guest if not found
        if (guest == null) {
            guest = new Guest();
            guest.setFirstName(session.getFirstName());
            guest.setLastName(session.getLastName());
            guest.setEmail(session.getEmail());
            guest.setPhone(session.getPhone());
            guest.setCountry(session.getCountry());
            guest.setAddress(session.getAddress());
            guest.setCity(session.getCity());
            guest.setStateProvince(session.getState());
            guest.setPostalCode(session.getPostalCode());

            if (guestRepository != null) {
                guest = guestRepository.save(guest);
            }

            LOGGER.info("Created new guest: " + guest.getFullName());
        }

        return guest;
    }

    /**
     * Assign rooms to reservation from booking session.
     */
    private void assignRoomsToReservation(Reservation reservation, BookingSession session) {
        for (BookingSession.RoomSelection selection : session.getSelectedRooms()) {
            if (selection.getQuantity() > 0) {
                assignRoomsOfType(reservation, selection.getRoomType(),
                        selection.getQuantity(), selection.getPricePerNight(),
                        session.getCheckInDate(), session.getCheckOutDate());
            }
        }
    }

    /**
     * Assign rooms to reservation from room type map.
     */
    private void assignRoomsToReservation(Reservation reservation,
                                          Map<RoomType, Integer> roomSelections,
                                          LocalDate checkIn, LocalDate checkOut) {
        for (Map.Entry<RoomType, Integer> entry : roomSelections.entrySet()) {
            RoomType roomType = entry.getKey();
            int quantity = entry.getValue();

            if (quantity > 0) {
                BigDecimal pricePerNight = pricingService.getAverageNightlyRate(roomType, checkIn, checkOut);
                assignRoomsOfType(reservation, roomType, quantity, pricePerNight, checkIn, checkOut);
            }
        }
    }

    /**
     * Assign specific number of rooms of a type to reservation.
     */
    private void assignRoomsOfType(Reservation reservation, RoomType roomType,
                                   int quantity, BigDecimal pricePerNight,
                                   LocalDate checkIn, LocalDate checkOut) {
        // Get available rooms of this type
        List<Room> availableRooms = roomService.getAvailableRoomsByType(roomType, checkIn, checkOut);

        if (availableRooms.size() < quantity) {
            throw new IllegalStateException(
                    String.format("Only %d %s rooms available, but %d requested",
                            availableRooms.size(), roomType.getDisplayName(), quantity));
        }

        // Calculate guests per room (distribute total guests across all rooms)
        int totalGuests = reservation.getTotalGuests();
        int totalRooms = reservation.getReservationRooms().size() + quantity;
        int guestsPerRoom = Math.max(1, totalGuests / totalRooms);

        // Assign rooms
        for (int i = 0; i < quantity; i++) {
            Room room = availableRooms.get(i);

            ReservationRoom reservationRoom = new ReservationRoom();
            reservationRoom.setRoom(room);
            reservationRoom.setRoomPrice(pricePerNight);
            reservationRoom.setNumGuests(guestsPerRoom);  // Add this line

            reservation.addRoom(reservationRoom);

            // Mark room as reserved
            room.setStatus(RoomStatus.RESERVED);
            if (roomRepository != null) {
                roomRepository.save(room);
            }
        }

        LOGGER.info(String.format("Assigned %d %s room(s) to reservation",
                quantity, roomType.getDisplayName()));
    }

    /**
     * Add add-on services to reservation from booking session.
     */
    private void addServicesToReservation(Reservation reservation, BookingSession session) {
        long nights = session.getNights();

        if (session.isWifiSelected()) {
            addAddOn(reservation, AddOnType.WIFI, (int) nights);
        }
        if (session.isBreakfastSelected()) {
            addAddOn(reservation, AddOnType.BREAKFAST, (int) nights);
        }
        if (session.isParkingSelected()) {
            addAddOn(reservation, AddOnType.PARKING, (int) nights);
        }
        if (session.isSpaSelected()) {
            addAddOn(reservation, AddOnType.SPA, 1); // One-time
        }
    }

    /**
     * Add a single add-on service to reservation using AddOnType.
     */
    private void addAddOn(Reservation reservation, AddOnType addOnType, int quantity) {
        ReservationAddOn addOn = new ReservationAddOn();
        addOn.setAddOnType(addOnType);
        addOn.setUnitPrice(addOnType.getBasePrice());
        addOn.setQuantity(quantity);
        addOn.setTotalPrice(addOnType.getBasePrice().multiply(BigDecimal.valueOf(quantity)));

        reservation.addAddOn(addOn);

        LOGGER.info(String.format("Added add-on service: %s x%d", addOnType.getDisplayName(), quantity));
    }

    // ==================== Reservation Retrieval ====================

    /**
     * Find a reservation by ID.
     */
    public Optional<Reservation> findById(Long id) {
        if (reservationRepository == null) {
            return Optional.empty();
        }
        return reservationRepository.findById(id);
    }

    /**
     * Find a reservation by confirmation number.
     */
    public Optional<Reservation> findByConfirmationNumber(String confirmationNumber) {
        if (reservationRepository == null || confirmationNumber == null) {
            return Optional.empty();
        }
        return reservationRepository.findByConfirmationNumber(confirmationNumber.toUpperCase());
    }

    /**
     * Find all reservations.
     *
     * @return List of all reservations
     */
    public List<Reservation> findAll() {
        if (reservationRepository == null) {
            return Collections.emptyList();
        }
        return reservationRepository.findAll();
    }

    /**
     * Find all reservations for a guest.
     */
    public List<Reservation> findByGuest(Guest guest) {
        if (reservationRepository == null || guest == null) {
            return Collections.emptyList();
        }
        return reservationRepository.findByGuestId(guest.getId());
    }

    /**
     * Find all reservations for a guest by email.
     */
    public List<Reservation> findByGuestEmail(String email) {
        if (reservationRepository == null || email == null) {
            return Collections.emptyList();
        }
        return reservationRepository.findByGuestEmail(email);
    }

    /**
     * Find reservations by status.
     */
    public List<Reservation> findByStatus(ReservationStatus status) {
        if (reservationRepository == null) {
            return Collections.emptyList();
        }
        return reservationRepository.findByStatus(status);
    }

    /**
     * Find today's check-ins.
     */
    public List<Reservation> getTodaysCheckIns() {
        if (reservationRepository == null) {
            return Collections.emptyList();
        }
        return reservationRepository.findByCheckInDate(LocalDate.now());
    }

    /**
     * Find today's check-outs.
     */
    public List<Reservation> getTodaysCheckOuts() {
        if (reservationRepository == null) {
            return Collections.emptyList();
        }
        return reservationRepository.findByCheckOutDate(LocalDate.now());
    }

    /**
     * Find upcoming reservations.
     */
    public List<Reservation> getUpcomingReservations() {
        if (reservationRepository == null) {
            return Collections.emptyList();
        }
        return reservationRepository.findUpcomingReservations();
    }

    /**
     * Find checked-in reservations with outstanding balance.
     */
    public List<Reservation> getCheckedInWithBalance() {
        if (reservationRepository == null) {
            return Collections.emptyList();
        }
        return reservationRepository.findCheckedInWithBalance();
    }

    /**
     * Search reservations by guest name or confirmation number.
     */
    public List<Reservation> search(String searchTerm) {
        if (reservationRepository == null || searchTerm == null || searchTerm.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<Reservation> results = new ArrayList<>();

        // Search by confirmation number
        results.addAll(reservationRepository.searchByConfirmationNumber(searchTerm));

        // Search by guest name
        results.addAll(reservationRepository.searchByGuestName(searchTerm));

        // Remove duplicates and return
        return results.stream().distinct().collect(Collectors.toList());
    }

    // ==================== Reservation Status Management ====================

    /**
     * Confirm a pending reservation.
     *
     * @param reservation The reservation to confirm
     * @return The confirmed reservation
     */
    public Reservation confirmReservation(Reservation reservation) {
        validateStatusTransition(reservation, ReservationStatus.CONFIRMED);

        reservation.confirm();

        if (reservationRepository != null) {
            reservation = reservationRepository.save(reservation);
        }

        LOGGER.info("Reservation confirmed: " + reservation.getConfirmationNumber());

        return reservation;
    }

    /**
     * Save or update an existing reservation entity.
     *
     * This method is intended for use by administrative controllers that
     * construct the full Reservation object (including rooms and add-ons)
     * before persistence.
     *
     * @param reservation The reservation object to save
     * @return The persisted reservation
     */
    public Reservation updateReservation(Reservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation cannot be null.");
        }

        // Ensure totals are accurate before saving
        reservation.calculateTotal();

        // Handle creation vs. update based on existence of ID/ConfirmationNumber
        if (reservation.getId() == null && reservation.getConfirmationNumber() == null) {
            if (reservation.getReservationRooms() == null || reservation.getReservationRooms().isEmpty()) {
                throw new IllegalArgumentException("Reservation must contain booked rooms.");
            }
        }

        // Perform persistence
        if (reservationRepository != null) {
            reservation = reservationRepository.save(reservation);
        }

        LOGGER.info("Reservation updated/saved: " + reservation.getConfirmationNumber());

        return reservation;
    }

    /**
     * Check in a guest.
     *
     * @param reservation The reservation to check in
     * @return The updated reservation
     */
    public Reservation checkIn(Reservation reservation) {
        validateStatusTransition(reservation, ReservationStatus.CHECKED_IN);

        // Verify check-in date
        LocalDate today = LocalDate.now();
        if (reservation.getCheckInDate().isAfter(today)) {
            throw new IllegalStateException("Cannot check in before the reservation date");
        }

        reservation.checkIn();

        // Update room statuses
        for (ReservationRoom rr : reservation.getReservationRooms()) {
            Room room = rr.getRoom();
            room.setStatus(RoomStatus.OCCUPIED);
            if (roomRepository != null) {
                roomRepository.save(room);
            }
        }

        if (reservationRepository != null) {
            reservation = reservationRepository.save(reservation);
        }

        LOGGER.info("Guest checked in: " + reservation.getConfirmationNumber());

        return reservation;
    }

    /**
     * Check out a guest.
     *
     * @param reservation The reservation to check out
     * @return The updated reservation
     * @throws IllegalStateException if there is an outstanding balance
     */
    public Reservation checkOut(Reservation reservation) {
        // Validate payment status
        ValidationService.ValidationResult balanceCheck =
                validationService.validateCheckoutBalance(reservation.getOutstandingBalance());
        if (!balanceCheck.isValid()) {
            throw new IllegalStateException(balanceCheck.getFirstErrorMessage());
        }

        validateStatusTransition(reservation, ReservationStatus.CHECKED_OUT);

        reservation.checkOut();

        // Update room statuses and notify observers
        for (ReservationRoom rr : reservation.getReservationRooms()) {
            Room room = rr.getRoom();
            room.setStatus(RoomStatus.CLEANING);
            if (roomRepository != null) {
                roomRepository.save(room);
            }

            // Notify observers that room will be available after cleaning
            roomService.markRoomAvailable(room, LocalDate.now().plusDays(1));
        }

        if (reservationRepository != null) {
            reservation = reservationRepository.save(reservation);
        }

        LOGGER.info("Guest checked out: " + reservation.getConfirmationNumber());

        return reservation;
    }

    /**
     * Cancel a reservation.
     *
     * @param reservation The reservation to cancel
     * @param reason Cancellation reason
     * @return The cancelled reservation
     */
    public Reservation cancelReservation(Reservation reservation, String reason) {
        validateStatusTransition(reservation, ReservationStatus.CANCELLED);

        reservation.cancel();

        // Release rooms
        for (ReservationRoom rr : reservation.getReservationRooms()) {
            Room room = rr.getRoom();
            room.setStatus(RoomStatus.AVAILABLE);
            if (roomRepository != null) {
                roomRepository.save(room);
            }

            // Notify observers
            roomService.markRoomAvailable(room, reservation.getCheckInDate());
        }

        // Refund loyalty points if used
        if (reservation.getLoyaltyPointsUsed() > 0) {
            refundLoyaltyPoints(reservation);
        }

        if (reservationRepository != null) {
            reservation = reservationRepository.save(reservation);
        }

        LOGGER.info("Reservation cancelled: " + reservation.getConfirmationNumber() +
                " Reason: " + reason);

        return reservation;
    }

    /**
     * Validate status transition is allowed.
     */
    private void validateStatusTransition(Reservation reservation, ReservationStatus newStatus) {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation cannot be null");
        }

        ReservationStatus currentStatus = reservation.getStatus();

        boolean valid = switch (newStatus) {
            case CONFIRMED -> currentStatus == ReservationStatus.PENDING;
            case CHECKED_IN -> currentStatus == ReservationStatus.PENDING ||
                    currentStatus == ReservationStatus.CONFIRMED;
            case CHECKED_OUT -> currentStatus == ReservationStatus.CHECKED_IN;
            case CANCELLED -> currentStatus != ReservationStatus.CHECKED_IN &&
                    currentStatus != ReservationStatus.CHECKED_OUT &&
                    currentStatus != ReservationStatus.CANCELLED;
            default -> false;
        };

        if (!valid) {
            throw new IllegalStateException(
                    String.format("Cannot transition from %s to %s", currentStatus, newStatus));
        }
    }

    // ==================== Payment Processing ====================

    /**
     * Process a payment for a reservation.
     *
     * @param reservation The reservation
     * @param amount Payment amount
     * @param paymentMethod Payment method (e.g., "CREDIT_CARD", "DEBIT", "CASH")
     * @return The created payment
     */
    public Payment processPayment(Reservation reservation, BigDecimal amount, String paymentMethod) {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation cannot be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        // Validate payment doesn't exceed balance
        BigDecimal outstandingBalance = reservation.getOutstandingBalance();
        if (amount.compareTo(outstandingBalance) > 0) {
            LOGGER.warning("Payment amount exceeds balance. Adjusting to: " + outstandingBalance);
            amount = outstandingBalance;
        }

        // Create payment record
        Payment payment = new Payment();
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setStatus(PaymentStatus.COMPLETED);

        reservation.addPayment(payment);

        // Award loyalty points
        awardLoyaltyPointsForPayment(reservation, amount);

        if (reservationRepository != null) {
            reservation = reservationRepository.save(reservation);
        }

        LOGGER.info(String.format("Payment processed: $%.2f for reservation %s",
                amount, reservation.getConfirmationNumber()));

        return payment;
    }

    /**
     * Award loyalty points for a payment.
     */
    private void awardLoyaltyPointsForPayment(Reservation reservation, BigDecimal paymentAmount) {
        if (loyaltyService == null) return;

        Guest guest = reservation.getGuest();
        if (guest == null) return;

        // Find loyalty account
        Optional<LoyaltyAccount> accountOpt =
                loyaltyService.findAccountByEmailOrPhone(guest.getEmail(), guest.getPhone());

        if (accountOpt.isPresent()) {
            LoyaltyAccount account = accountOpt.get();
            int pointsAwarded = loyaltyService.awardPointsForPayment(account, paymentAmount, reservation);
            LOGGER.info(String.format("Awarded %d loyalty points for payment", pointsAwarded));
        }
    }

    /**
     * Refund loyalty points when reservation is cancelled.
     */
    private void refundLoyaltyPoints(Reservation reservation) {
        if (loyaltyService == null) return;

        Guest guest = reservation.getGuest();
        if (guest == null) return;

        Optional<LoyaltyAccount> accountOpt =
                loyaltyService.findAccountByEmailOrPhone(guest.getEmail(), guest.getPhone());

        if (accountOpt.isPresent()) {
            LoyaltyAccount account = accountOpt.get();
            loyaltyService.awardBonusPoints(account,
                    reservation.getLoyaltyPointsUsed(),
                    "Points refunded for cancelled reservation " + reservation.getConfirmationNumber(),
                    reservation);

            LOGGER.info(String.format("Refunded %d loyalty points for cancelled reservation",
                    reservation.getLoyaltyPointsUsed()));
        }
    }

    // ==================== Discount Application ====================

    /**
     * Apply a percentage discount to a reservation.
     *
     * @param reservation The reservation
     * @param discountPercent Discount percentage (e.g., 15 for 15%)
     * @param appliedBy Username of admin applying discount
     * @return The updated reservation
     */
    public Reservation applyDiscount(Reservation reservation, BigDecimal discountPercent, String appliedBy) {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation cannot be null");
        }

        // Validate discount using ValidationService
        ValidationService.ValidationResult result =
                validationService.validateDiscount(discountPercent, null); // Role checked separately

        if (!result.isValid()) {
            throw new IllegalArgumentException(result.getFirstErrorMessage());
        }

        reservation.setDiscountPercentage(discountPercent);
        reservation.setDiscountAppliedBy(appliedBy);
        reservation.calculateTotal();

        if (reservationRepository != null) {
            reservation = reservationRepository.save(reservation);
        }

        LOGGER.info(String.format("Applied %.2f%% discount to reservation %s by %s",
                discountPercent, reservation.getConfirmationNumber(), appliedBy));

        return reservation;
    }

    /**
     * Apply loyalty points discount to a reservation.
     *
     * @param reservation The reservation
     * @param pointsToRedeem Number of points to redeem
     * @return The updated reservation
     */
    public Reservation applyLoyaltyDiscount(Reservation reservation, int pointsToRedeem) {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation cannot be null");
        }

        Guest guest = reservation.getGuest();
        if (guest == null) {
            throw new IllegalArgumentException("Reservation must have a guest");
        }

        if (loyaltyService == null) {
            throw new IllegalStateException("Loyalty service not available");
        }

        // Find loyalty account
        Optional<LoyaltyAccount> accountOpt =
                loyaltyService.findAccountByEmailOrPhone(guest.getEmail(), guest.getPhone());

        if (accountOpt.isEmpty()) {
            throw new IllegalArgumentException("Guest does not have a loyalty account");
        }

        LoyaltyAccount account = accountOpt.get();

        // Redeem points
        BigDecimal discount = loyaltyService.redeemPoints(account, pointsToRedeem, reservation);

        reservation.setLoyaltyPointsUsed(pointsToRedeem);
        reservation.setLoyaltyDiscount(discount);
        reservation.calculateTotal();

        if (reservationRepository != null) {
            reservation = reservationRepository.save(reservation);
        }

        LOGGER.info(String.format("Applied loyalty discount of $%.2f (%d points) to reservation %s",
                discount, pointsToRedeem, reservation.getConfirmationNumber()));

        return reservation;
    }

    // ==================== Modification ====================

    /**
     * Update reservation dates.
     *
     * @param reservation The reservation
     * @param newCheckIn New check-in date
     * @param newCheckOut New check-out date
     * @return The updated reservation
     */
    public Reservation updateDates(Reservation reservation, LocalDate newCheckIn, LocalDate newCheckOut) {
        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
                reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new IllegalStateException("Cannot modify dates after check-in");
        }

        // Validate new dates
        ValidationService.ValidationResult dateResult =
                validationService.validateDateRange(newCheckIn, newCheckOut);
        if (!dateResult.isValid()) {
            throw new IllegalArgumentException(dateResult.getFirstErrorMessage());
        }

        // Check room availability for new dates
        for (ReservationRoom rr : reservation.getReservationRooms()) {
            if (!roomService.isRoomAvailable(rr.getRoom(), newCheckIn, newCheckOut)) {
                throw new IllegalStateException(
                        "Room " + rr.getRoom().getRoomNumber() + " is not available for new dates");
            }
        }

        reservation.setCheckInDate(newCheckIn);
        reservation.setCheckOutDate(newCheckOut);
        reservation.calculateTotal();

        if (reservationRepository != null) {
            reservation = reservationRepository.save(reservation);
        }

        LOGGER.info(String.format("Updated dates for reservation %s: %s to %s",
                reservation.getConfirmationNumber(), newCheckIn, newCheckOut));

        return reservation;
    }

    /**
     * Add special requests to a reservation.
     */
    public Reservation updateSpecialRequests(Reservation reservation, String specialRequests) {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation cannot be null");
        }

        reservation.setSpecialRequests(specialRequests);

        if (reservationRepository != null) {
            reservation = reservationRepository.save(reservation);
        }

        return reservation;
    }

    // ==================== Statistics and Reports ====================

    /**
     * Get reservation statistics for a date range.
     */
    public ReservationStats getStatistics(LocalDate startDate, LocalDate endDate) {
        ReservationStats stats = new ReservationStats();

        if (reservationRepository != null) {
            stats.setTotalReservations(reservationRepository.countByDateRange(startDate, endDate));
            stats.setPendingCount(reservationRepository.countByStatus(ReservationStatus.PENDING));
            stats.setConfirmedCount(reservationRepository.countByStatus(ReservationStatus.CONFIRMED));
            stats.setCheckedInCount(reservationRepository.countByStatus(ReservationStatus.CHECKED_IN));
            stats.setCheckedOutCount(reservationRepository.countByStatus(ReservationStatus.CHECKED_OUT));
            stats.setCancelledCount(reservationRepository.countByStatus(ReservationStatus.CANCELLED));
        }

        return stats;
    }

    /**
     * Get occupancy rate for a date.
     */
    public double getOccupancyRate(LocalDate date) {
        if (roomRepository == null || reservationRepository == null) {
            return 0.0;
        }

        long totalRooms = roomRepository.count();
        long occupiedRooms = reservationRepository.findByStatus(ReservationStatus.CHECKED_IN)
                .stream()
                .mapToLong(r -> r.getReservationRooms().size())
                .sum();

        if (totalRooms == 0) return 0.0;

        return (double) occupiedRooms / totalRooms * 100;
    }

    // ==================== Setters for Dependency Injection ====================

    public void setReservationRepository(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public void setRoomRepository(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void setGuestRepository(GuestRepository guestRepository) {
        this.guestRepository = guestRepository;
    }

    public void setLoyaltyService(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    public void setValidationService(ValidationService validationService) {
        this.validationService = validationService;
    }

    public void setPricingService(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    public void setRoomService(RoomService roomService) {
        this.roomService = roomService;
    }

    // ==================== Inner Classes ====================

    /**
     * Statistics for reservations.
     */
    public static class ReservationStats {
        private long totalReservations;
        private long pendingCount;
        private long confirmedCount;
        private long checkedInCount;
        private long checkedOutCount;
        private long cancelledCount;

        public long getTotalReservations() { return totalReservations; }
        public void setTotalReservations(long totalReservations) { this.totalReservations = totalReservations; }

        public long getPendingCount() { return pendingCount; }
        public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }

        public long getConfirmedCount() { return confirmedCount; }
        public void setConfirmedCount(long confirmedCount) { this.confirmedCount = confirmedCount; }

        public long getCheckedInCount() { return checkedInCount; }
        public void setCheckedInCount(long checkedInCount) { this.checkedInCount = checkedInCount; }

        public long getCheckedOutCount() { return checkedOutCount; }
        public void setCheckedOutCount(long checkedOutCount) { this.checkedOutCount = checkedOutCount; }

        public long getCancelledCount() { return cancelledCount; }
        public void setCancelledCount(long cancelledCount) { this.cancelledCount = cancelledCount; }

        public long getActiveCount() {
            return pendingCount + confirmedCount + checkedInCount;
        }
    }
}