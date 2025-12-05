package com.hanson.hotelreservationsystem.repository;

import com.hanson.hotelreservationsystem.model.Reservation;
import com.hanson.hotelreservationsystem.model.Guest;
import com.hanson.hotelreservationsystem.model.enums.ReservationStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Repository for Reservation entity operations.
 * Provides persistence and query methods for hotel reservations.
 */
public class ReservationRepository {

    private static final Logger LOGGER = Logger.getLogger(ReservationRepository.class.getName());

    // Singleton instance
    private static ReservationRepository instance;

    private EntityManager entityManager;

    /**
     * Private constructor for Singleton pattern.
     */
    private ReservationRepository() {
        // EntityManager will be set via setter
    }

    /**
     * Constructor with EntityManager injection.
     */
    public ReservationRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Get the singleton instance.
     */
    public static synchronized ReservationRepository getInstance() {
        if (instance == null) {
            instance = new ReservationRepository();
        }
        return instance;
    }

    /**
     * Set the EntityManager for the singleton instance.
     */
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // ==================== Basic CRUD Operations ====================

    /**
     * Save or update a reservation.
     */
    public Reservation save(Reservation reservation) {
        if (entityManager == null) {
            LOGGER.warning("EntityManager is null");
            return reservation;
        }
        boolean startedTransaction = false;
        try {
            if (!entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().begin();
                startedTransaction = true;
            }

            if (reservation.getId() == null) {
                entityManager.persist(reservation);
            } else {
                reservation = entityManager.merge(reservation);
            }

            // Only commit if WE started the transaction
            if (startedTransaction) {
                entityManager.getTransaction().commit();
            }

            return reservation;
        } catch (Exception e) {
            // Rollback if something goes wrong
            if (startedTransaction && entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw e;
        }
    }

    /**
     * Find a reservation by ID.
     */
    public Optional<Reservation> findById(Long id) {
        Reservation reservation = entityManager.find(Reservation.class, id);
        return Optional.ofNullable(reservation);
    }

    /**
     * Find a reservation by confirmation number.
     */
    public Optional<Reservation> findByConfirmationNumber(String confirmationNumber) {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.confirmationNumber = :confirmationNumber",
                Reservation.class);
        query.setParameter("confirmationNumber", confirmationNumber);

        List<Reservation> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Find all reservations.
     */
    public List<Reservation> findAll() {
        entityManager.clear();

        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r ORDER BY r.checkInDate DESC",
                Reservation.class);
        return query.getResultList();
    }

    /**
     * Delete a reservation.
     */
    public void delete(Reservation reservation) {
        if (entityManager.contains(reservation)) {
            entityManager.remove(reservation);
        } else {
            Reservation attached = entityManager.find(Reservation.class, reservation.getId());
            if (attached != null) {
                entityManager.remove(attached);
            }
        }
        LOGGER.info("Deleted reservation: " + reservation.getConfirmationNumber());
    }

    /**
     * Delete a reservation by ID.
     */
    public void deleteById(Long id) {
        Reservation reservation = entityManager.find(Reservation.class, id);
        if (reservation != null) {
            entityManager.remove(reservation);
            LOGGER.info("Deleted reservation with ID: " + id);
        }
    }

    // ==================== Room Availability Queries ====================

    /**
     * Find reservations that overlap with the given date range for a specific room.
     * This is critical for checking room availability.
     *
     * A reservation overlaps if:
     * - Its check-in date is before the requested check-out date, AND
     * - Its check-out date is after the requested check-in date
     *
     * @param roomId The room ID to check
     * @param checkIn The requested check-in date
     * @param checkOut The requested check-out date
     * @return List of overlapping reservations (excluding cancelled ones)
     */
    public List<Reservation> findOverlappingReservations(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT DISTINCT r FROM Reservation r " +
                        "JOIN r.reservationRooms rr " +
                        "WHERE rr.room.id = :roomId " +
                        "AND r.status NOT IN (:cancelledStatus, :checkedOutStatus) " +
                        "AND r.checkInDate < :checkOut " +
                        "AND r.checkOutDate > :checkIn",
                Reservation.class);
        query.setParameter("roomId", roomId);
        query.setParameter("checkIn", checkIn);
        query.setParameter("checkOut", checkOut);
        query.setParameter("cancelledStatus", ReservationStatus.CANCELLED);
        query.setParameter("checkedOutStatus", ReservationStatus.CHECKED_OUT);

        return query.getResultList();
    }

    /**
     * Find all reservations for a specific room.
     */
    public List<Reservation> findByRoomId(Long roomId) {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT DISTINCT r FROM Reservation r " +
                        "JOIN r.reservationRooms rr " +
                        "WHERE rr.room.id = :roomId " +
                        "ORDER BY r.checkInDate DESC",
                Reservation.class);
        query.setParameter("roomId", roomId);
        return query.getResultList();
    }

    /**
     * Find active reservations for a specific room (not cancelled or checked out).
     */
    public List<Reservation> findActiveByRoomId(Long roomId) {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT DISTINCT r FROM Reservation r " +
                        "JOIN r.reservationRooms rr " +
                        "WHERE rr.room.id = :roomId " +
                        "AND r.status NOT IN (:cancelledStatus, :checkedOutStatus) " +
                        "ORDER BY r.checkInDate",
                Reservation.class);
        query.setParameter("roomId", roomId);
        query.setParameter("cancelledStatus", ReservationStatus.CANCELLED);
        query.setParameter("checkedOutStatus", ReservationStatus.CHECKED_OUT);
        return query.getResultList();
    }

    // ==================== Query by Status ====================

    /**
     * Find all reservations with a specific status.
     */
    public List<Reservation> findByStatus(ReservationStatus status) {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.status = :status ORDER BY r.checkInDate",
                Reservation.class);
        query.setParameter("status", status);
        return query.getResultList();
    }

    /**
     * Find all pending reservations.
     */
    public List<Reservation> findPendingReservations() {
        return findByStatus(ReservationStatus.PENDING);
    }

    /**
     * Find all confirmed reservations.
     */
    public List<Reservation> findConfirmedReservations() {
        return findByStatus(ReservationStatus.CONFIRMED);
    }

    /**
     * Find all checked-in reservations.
     */
    public List<Reservation> findCheckedInReservations() {
        return findByStatus(ReservationStatus.CHECKED_IN);
    }

    /**
     * Count reservations by status.
     */
    public long countByStatus(ReservationStatus status) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(r) FROM Reservation r WHERE r.status = :status",
                Long.class);
        query.setParameter("status", status);
        return query.getSingleResult();
    }

    // ==================== Query by Guest ====================

    /**
     * Find all reservations for a guest.
     */
    public List<Reservation> findByGuest(Guest guest) {
        return findByGuestId(guest.getId());
    }

    /**
     * Find all reservations for a guest by guest ID.
     */
    public List<Reservation> findByGuestId(Long guestId) {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.guest.id = :guestId ORDER BY r.checkInDate DESC",
                Reservation.class);
        query.setParameter("guestId", guestId);
        return query.getResultList();
    }

    /**
     * Find reservations by guest email.
     */
    public List<Reservation> findByGuestEmail(String email) {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE LOWER(r.guest.email) = LOWER(:email) " +
                        "ORDER BY r.checkInDate DESC",
                Reservation.class);
        query.setParameter("email", email);
        return query.getResultList();
    }

    /**
     * Find reservations by guest phone.
     */
    public List<Reservation> findByGuestPhone(String phone) {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.guest.phone = :phone " +
                        "ORDER BY r.checkInDate DESC",
                Reservation.class);
        query.setParameter("phone", phone);
        return query.getResultList();
    }

    /**
     * Find active reservations for a guest (not cancelled or checked out).
     */
    public List<Reservation> findActiveByGuestId(Long guestId) {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.guest.id = :guestId " +
                        "AND r.status NOT IN (:cancelledStatus, :checkedOutStatus) " +
                        "ORDER BY r.checkInDate",
                Reservation.class);
        query.setParameter("guestId", guestId);
        query.setParameter("cancelledStatus", ReservationStatus.CANCELLED);
        query.setParameter("checkedOutStatus", ReservationStatus.CHECKED_OUT);
        return query.getResultList();
    }

    // ==================== Query by Date ====================

    /**
     * Find reservations with check-in on a specific date.
     */
    public List<Reservation> findByCheckInDate(LocalDate checkInDate) {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.checkInDate = :checkInDate " +
                        "AND r.status NOT IN (:cancelledStatus) " +
                        "ORDER BY r.guest.lastName",
                Reservation.class);
        query.setParameter("checkInDate", checkInDate);
        query.setParameter("cancelledStatus", ReservationStatus.CANCELLED);
        return query.getResultList();
    }

    /**
     * Find reservations with check-out on a specific date.
     */
    public List<Reservation> findByCheckOutDate(LocalDate checkOutDate) {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.checkOutDate = :checkOutDate " +
                        "AND r.status NOT IN (:cancelledStatus) " +
                        "ORDER BY r.guest.lastName",
                Reservation.class);
        query.setParameter("checkOutDate", checkOutDate);
        query.setParameter("cancelledStatus", ReservationStatus.CANCELLED);
        return query.getResultList();
    }

    /**
     * Find today's check-ins.
     */
    public List<Reservation> findTodaysCheckIns() {
        return findByCheckInDate(LocalDate.now());
    }

    /**
     * Find today's check-outs.
     */
    public List<Reservation> findTodaysCheckOuts() {
        return findByCheckOutDate(LocalDate.now());
    }

    /**
     * Find reservations within a date range (check-in falls within range).
     */
    public List<Reservation> findByCheckInDateBetween(LocalDate startDate, LocalDate endDate) {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.checkInDate >= :startDate " +
                        "AND r.checkInDate <= :endDate " +
                        "AND r.status NOT IN (:cancelledStatus) " +
                        "ORDER BY r.checkInDate",
                Reservation.class);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("cancelledStatus", ReservationStatus.CANCELLED);
        return query.getResultList();
    }

    /**
     * Find reservations created within a date range.
     */
    public List<Reservation> findByCreatedAtBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.createdAt >= :startDateTime " +
                        "AND r.createdAt <= :endDateTime " +
                        "ORDER BY r.createdAt DESC",
                Reservation.class);
        query.setParameter("startDateTime", startDateTime);
        query.setParameter("endDateTime", endDateTime);
        return query.getResultList();
    }

    /**
     * Find upcoming reservations (check-in date is in the future).
     */
    public List<Reservation> findUpcomingReservations() {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.checkInDate > :today " +
                        "AND r.status IN (:pendingStatus, :confirmedStatus) " +
                        "ORDER BY r.checkInDate",
                Reservation.class);
        query.setParameter("today", LocalDate.now());
        query.setParameter("pendingStatus", ReservationStatus.PENDING);
        query.setParameter("confirmedStatus", ReservationStatus.CONFIRMED);
        return query.getResultList();
    }

    /**
     * Find past reservations (already checked out).
     */
    public List<Reservation> findPastReservations() {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.status = :checkedOutStatus " +
                        "ORDER BY r.checkOutDate DESC",
                Reservation.class);
        query.setParameter("checkedOutStatus", ReservationStatus.CHECKED_OUT);
        return query.getResultList();
    }

    // ==================== Query by Booking Source ====================

    /**
     * Find reservations booked via kiosk.
     */
    public List<Reservation> findKioskBookings() {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.bookedViaKiosk = true ORDER BY r.createdAt DESC",
                Reservation.class);
        return query.getResultList();
    }

    /**
     * Find reservations booked via admin system.
     */
    public List<Reservation> findAdminBookings() {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.bookedViaKiosk = false ORDER BY r.createdAt DESC",
                Reservation.class);
        return query.getResultList();
    }

    // ==================== Payment-Related Queries ====================

    /**
     * Find reservations with outstanding balance.
     */
    public List<Reservation> findWithOutstandingBalance() {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.amountPaid < r.totalAmount " +
                        "AND r.status NOT IN (:cancelledStatus) " +
                        "ORDER BY r.checkInDate",
                Reservation.class);
        query.setParameter("cancelledStatus", ReservationStatus.CANCELLED);
        return query.getResultList();
    }

    /**
     * Find fully paid reservations.
     */
    public List<Reservation> findFullyPaid() {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.amountPaid >= r.totalAmount " +
                        "ORDER BY r.checkInDate DESC",
                Reservation.class);
        return query.getResultList();
    }

    /**
     * Find checked-in reservations with outstanding balance (cannot checkout).
     */
    public List<Reservation> findCheckedInWithBalance() {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.status = :checkedInStatus " +
                        "AND r.amountPaid < r.totalAmount " +
                        "ORDER BY r.checkOutDate",
                Reservation.class);
        query.setParameter("checkedInStatus", ReservationStatus.CHECKED_IN);
        return query.getResultList();
    }

    // ==================== Feedback Eligibility ====================

    /**
     * Find checked-out reservations without feedback (eligible for feedback).
     */
    public List<Reservation> findEligibleForFeedback() {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.status = :checkedOutStatus " +
                        "AND r.feedback IS NULL " +
                        "ORDER BY r.actualCheckOut DESC",
                Reservation.class);
        query.setParameter("checkedOutStatus", ReservationStatus.CHECKED_OUT);
        return query.getResultList();
    }

    /**
     * Find reservations that have feedback.
     */
    public List<Reservation> findWithFeedback() {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.feedback IS NOT NULL " +
                        "ORDER BY r.actualCheckOut DESC",
                Reservation.class);
        return query.getResultList();
    }

    // ==================== Statistics ====================

    /**
     * Count total reservations.
     */
    public long count() {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(r) FROM Reservation r",
                Long.class);
        return query.getSingleResult();
    }

    /**
     * Count reservations for a specific date range.
     */
    public long countByDateRange(LocalDate startDate, LocalDate endDate) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(r) FROM Reservation r WHERE r.checkInDate >= :startDate " +
                        "AND r.checkInDate <= :endDate",
                Long.class);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        return query.getSingleResult();
    }

    /**
     * Get average occupancy for a date range.
     */
    public double getAverageGuestsPerReservation() {
        TypedQuery<Double> query = entityManager.createQuery(
                "SELECT AVG(r.numAdults + r.numChildren) FROM Reservation r " +
                        "WHERE r.status NOT IN (:cancelledStatus)",
                Double.class);
        query.setParameter("cancelledStatus", ReservationStatus.CANCELLED);
        Double result = query.getSingleResult();
        return result != null ? result : 0.0;
    }

    // ==================== Search Methods ====================

    /**
     * Search reservations by guest name (partial match).
     */
    public List<Reservation> searchByGuestName(String searchTerm) {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE " +
                        "LOWER(r.guest.firstName) LIKE LOWER(:searchTerm) OR " +
                        "LOWER(r.guest.lastName) LIKE LOWER(:searchTerm) " +
                        "ORDER BY r.checkInDate DESC",
                Reservation.class);
        query.setParameter("searchTerm", "%" + searchTerm + "%");
        return query.getResultList();
    }

    /**
     * Search reservations by confirmation number (partial match).
     */
    public List<Reservation> searchByConfirmationNumber(String searchTerm) {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE " +
                        "UPPER(r.confirmationNumber) LIKE UPPER(:searchTerm) " +
                        "ORDER BY r.checkInDate DESC",
                Reservation.class);
        query.setParameter("searchTerm", "%" + searchTerm + "%");
        return query.getResultList();
    }

    /**
     * Check if a confirmation number exists.
     */
    public boolean existsByConfirmationNumber(String confirmationNumber) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(r) FROM Reservation r WHERE r.confirmationNumber = :confirmationNumber",
                Long.class);
        query.setParameter("confirmationNumber", confirmationNumber);
        return query.getSingleResult() > 0;
    }

    // ==================== Loyalty Points Related ====================

    /**
     * Find reservations where loyalty points were used.
     */
    public List<Reservation> findWithLoyaltyPointsUsed() {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.loyaltyPointsUsed > 0 " +
                        "ORDER BY r.checkInDate DESC",
                Reservation.class);
        return query.getResultList();
    }

    /**
     * Find reservations with loyalty discount applied.
     */
    public List<Reservation> findWithLoyaltyDiscount() {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.loyaltyDiscount > 0 " +
                        "ORDER BY r.checkInDate DESC",
                Reservation.class);
        return query.getResultList();
    }

    // ==================== Paginated Queries ====================

    /**
     * Find all reservations with pagination.
     */
    public List<Reservation> findAllPaginated(int page, int size) {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r ORDER BY r.checkInDate DESC",
                Reservation.class);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    /**
     * Find reservations by status with pagination.
     */
    public List<Reservation> findByStatusPaginated(ReservationStatus status, int page, int size) {
        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT r FROM Reservation r WHERE r.status = :status ORDER BY r.checkInDate DESC",
                Reservation.class);
        query.setParameter("status", status);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }
}