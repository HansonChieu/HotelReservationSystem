package com.hanson.hotelreservationsystem.repository;

import com.hanson.hotelreservationsystem.model.Room;
import com.hanson.hotelreservationsystem.model.enums.RoomType;
import com.hanson.hotelreservationsystem.model.enums.RoomStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Repository for Room entity operations.
 * Provides persistence and query methods for hotel rooms.
 */
public class RoomRepository {

    private static final Logger LOGGER = Logger.getLogger(RoomRepository.class.getName());

    // Singleton instance
    private static RoomRepository instance;

    private EntityManager entityManager;

    /**
     * Private constructor for Singleton pattern.
     */
    private RoomRepository() {
        // EntityManager will be set via setter
    }

    /**
     * Constructor with EntityManager injection.
     */
    public RoomRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Get the singleton instance.
     */
    public static synchronized RoomRepository getInstance() {
        if (instance == null) {
            instance = new RoomRepository();
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
     * Save or update a room.
     */
    public Room save(Room room) {
        if (entityManager == null) {
            LOGGER.warning("EntityManager is null - returning room without persistence");
            return room;
        }

        if (room.getId() == null) {
            entityManager.persist(room);
            LOGGER.info("Created new room: " + room.getRoomNumber());
            return room;
        } else {
            Room merged = entityManager.merge(room);
            LOGGER.info("Updated room: " + room.getRoomNumber());
            return merged;
        }
    }

    /**
     * Find a room by ID.
     */
    public Optional<Room> findById(Long id) {
        if (entityManager == null) {
            return Optional.empty();
        }
        Room room = entityManager.find(Room.class, id);
        return Optional.ofNullable(room);
    }

    /**
     * Find a room by room number.
     */
    public Optional<Room> findByRoomNumber(String roomNumber) {
        TypedQuery<Room> query = entityManager.createQuery(
                "SELECT r FROM Room r WHERE r.roomNumber = :roomNumber",
                Room.class);
        query.setParameter("roomNumber", roomNumber);

        List<Room> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Find all rooms.
     */
    public List<Room> findAll() {
        TypedQuery<Room> query = entityManager.createQuery(
                "SELECT r FROM Room r ORDER BY r.floor, r.roomNumber",
                Room.class);
        return query.getResultList();
    }

    /**
     * Delete a room.
     */
    public void delete(Room room) {
        if (entityManager.contains(room)) {
            entityManager.remove(room);
        } else {
            Room attached = entityManager.find(Room.class, room.getId());
            if (attached != null) {
                entityManager.remove(attached);
            }
        }
        LOGGER.info("Deleted room: " + room.getRoomNumber());
    }

    /**
     * Delete a room by ID.
     */
    public void deleteById(Long id) {
        Room room = entityManager.find(Room.class, id);
        if (room != null) {
            entityManager.remove(room);
            LOGGER.info("Deleted room with ID: " + id);
        }
    }

    // ==================== Query by Room Type ====================

    /**
     * Find all rooms of a specific type.
     */
    public List<Room> findByRoomType(RoomType roomType) {
        TypedQuery<Room> query = entityManager.createQuery(
                "SELECT r FROM Room r WHERE r.roomType = :roomType ORDER BY r.roomNumber",
                Room.class);
        query.setParameter("roomType", roomType);
        return query.getResultList();
    }

    /**
     * Count rooms by type.
     */
    public long countByRoomType(RoomType roomType) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(r) FROM Room r WHERE r.roomType = :roomType",
                Long.class);
        query.setParameter("roomType", roomType);
        return query.getSingleResult();
    }

    // ==================== Query by Status ====================

    /**
     * Find all rooms with a specific status.
     */
    public List<Room> findByStatus(RoomStatus status) {
        TypedQuery<Room> query = entityManager.createQuery(
                "SELECT r FROM Room r WHERE r.status = :status ORDER BY r.roomNumber",
                Room.class);
        query.setParameter("status", status);
        return query.getResultList();
    }

    public List<Room> findStrictlyAvailableRooms(RoomType roomType, java.time.LocalDate checkIn, java.time.LocalDate checkOut) {
        // This query selects rooms of the correct type...
        // ...WHERE the room ID is NOT IN the list of rooms booked during that period.
        String jpql =
                "SELECT r FROM Room r " +
                        "WHERE r.roomType = :roomType " +
                        "AND r.id NOT IN (" +
                        "SELECT rr.room.id FROM ReservationRoom rr " +
                        "JOIN rr.reservation res " +
                        "WHERE res.status NOT IN ('CANCELLED', 'CHECKED_OUT') " +
                        "AND (" +
                        "(res.checkInDate < :checkOut AND res.checkOutDate > :checkIn)" + // Standard overlap check
                        ")" +
                        ") " +
                        "ORDER BY r.roomNumber";

        TypedQuery<Room> query = entityManager.createQuery(jpql, Room.class);
        query.setParameter("roomType", roomType);
        query.setParameter("checkIn", checkIn);
        query.setParameter("checkOut", checkOut);

        return query.getResultList();
    }

    /**
     * Find all available rooms.
     */
    public List<Room> findAvailableRooms() {
        return findByStatus(RoomStatus.AVAILABLE);
    }

    /**
     * Find all occupied rooms.
     */
    public List<Room> findOccupiedRooms() {
        return findByStatus(RoomStatus.OCCUPIED);
    }

    /**
     * Count rooms by status.
     */
    public long countByStatus(RoomStatus status) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(r) FROM Room r WHERE r.status = :status",
                Long.class);
        query.setParameter("status", status);
        return query.getSingleResult();
    }

    // ==================== Query by Floor ====================

    /**
     * Find all rooms on a specific floor.
     */
    public List<Room> findByFloor(int floor) {
        TypedQuery<Room> query = entityManager.createQuery(
                "SELECT r FROM Room r WHERE r.floor = :floor ORDER BY r.roomNumber",
                Room.class);
        query.setParameter("floor", floor);
        return query.getResultList();
    }

    /**
     * Get all distinct floor numbers.
     */
    public List<Integer> findAllFloors() {
        TypedQuery<Integer> query = entityManager.createQuery(
                "SELECT DISTINCT r.floor FROM Room r ORDER BY r.floor",
                Integer.class);
        return query.getResultList();
    }

    // ==================== Combined Queries ====================

    /**
     * Find available rooms of a specific type.
     */
    public List<Room> findAvailableByRoomType(RoomType roomType) {
        TypedQuery<Room> query = entityManager.createQuery(
                "SELECT r FROM Room r WHERE r.roomType = :roomType AND r.status = :status " +
                        "ORDER BY r.roomNumber",
                Room.class);
        query.setParameter("roomType", roomType);
        query.setParameter("status", RoomStatus.AVAILABLE);
        return query.getResultList();
    }

    /**
     * Find rooms by type and floor.
     */
    public List<Room> findByRoomTypeAndFloor(RoomType roomType, int floor) {
        TypedQuery<Room> query = entityManager.createQuery(
                "SELECT r FROM Room r WHERE r.roomType = :roomType AND r.floor = :floor " +
                        "ORDER BY r.roomNumber",
                Room.class);
        query.setParameter("roomType", roomType);
        query.setParameter("floor", floor);
        return query.getResultList();
    }

    // ==================== Statistics ====================

    /**
     * Count total rooms.
     */
    public long count() {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(r) FROM Room r",
                Long.class);
        return query.getSingleResult();
    }

    /**
     * Count available rooms.
     */
    public long countAvailable() {
        return countByStatus(RoomStatus.AVAILABLE);
    }

    /**
     * Count occupied rooms.
     */
    public long countOccupied() {
        return countByStatus(RoomStatus.OCCUPIED);
    }

    /**
     * Check if a room number already exists.
     */
    public boolean existsByRoomNumber(String roomNumber) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(r) FROM Room r WHERE r.roomNumber = :roomNumber",
                Long.class);
        query.setParameter("roomNumber", roomNumber);
        return query.getSingleResult() > 0;
    }

    // ==================== Bulk Operations ====================

    /**
     * Update status for multiple rooms.
     */
    public int updateStatusByFloor(int floor, RoomStatus newStatus) {
        return entityManager.createQuery(
                        "UPDATE Room r SET r.status = :status WHERE r.floor = :floor")
                .setParameter("status", newStatus)
                .setParameter("floor", floor)
                .executeUpdate();
    }

    /**
     * Mark all rooms on a floor as available.
     */
    public int markFloorAvailable(int floor) {
        return updateStatusByFloor(floor, RoomStatus.AVAILABLE);
    }
}