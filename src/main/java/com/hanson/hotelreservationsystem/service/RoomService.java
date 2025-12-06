package com.hanson.hotelreservationsystem.service;

import com.hanson.hotelreservationsystem.config.PricingConfig;
import com.hanson.hotelreservationsystem.events.RoomAvailabilityEvent;
import com.hanson.hotelreservationsystem.events.RoomAvailabilityObserver;
import com.hanson.hotelreservationsystem.model.Reservation;
import com.hanson.hotelreservationsystem.model.Room;
import com.hanson.hotelreservationsystem.model.enums.RoomStatus;
import com.hanson.hotelreservationsystem.model.enums.RoomType;
import com.hanson.hotelreservationsystem.repository.ReservationRepository;
import com.hanson.hotelreservationsystem.repository.RoomRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service for managing hotel rooms and availability.
 *
 * Responsibilities:
 * - Check strictly availability for selected dates via Repository
 * - Enforce occupancy limits per room type
 * - Suggest room combinations for group bookings
 * - Calculate dynamic pricing (weekday/weekend/seasonal)
 * - Notify observers when room availability changes
 *
 * Business Rules:
 * - Single room: max 2 people
 * - Double room: max 4 people
 * - Deluxe room: max 2 people
 * - Penthouse: max 2 people
 */
public class RoomService {

    private static final Logger LOGGER = Logger.getLogger(RoomService.class.getName());

    // Singleton instance
    private static RoomService instance;

    // Dependencies
    private RoomRepository roomRepository;
    private ReservationRepository reservationRepository;
    private PricingConfig pricingConfig;

    // Observer pattern
    private final List<RoomAvailabilityObserver> observers = new ArrayList<>();

    // Configuration Maps
    private static final Map<RoomType, Integer> OCCUPANCY_LIMITS = new EnumMap<>(RoomType.class);
    private static final Map<RoomType, BigDecimal> BASE_PRICES = new EnumMap<>(RoomType.class);

    static {
        OCCUPANCY_LIMITS.put(RoomType.SINGLE, 2);
        OCCUPANCY_LIMITS.put(RoomType.DOUBLE, 4);
        OCCUPANCY_LIMITS.put(RoomType.DELUXE, 2);
        OCCUPANCY_LIMITS.put(RoomType.PENTHOUSE, 2);

        BASE_PRICES.put(RoomType.SINGLE, new BigDecimal("99.00"));
        BASE_PRICES.put(RoomType.DOUBLE, new BigDecimal("149.00"));
        BASE_PRICES.put(RoomType.DELUXE, new BigDecimal("249.00"));
        BASE_PRICES.put(RoomType.PENTHOUSE, new BigDecimal("499.00"));
    }

    private RoomService() {
        this.pricingConfig = new PricingConfig();
    }

    public static synchronized RoomService getInstance() {
        if (instance == null) {
            instance = new RoomService();
        }
        return instance;
    }

    // ==================== Room Factory Methods ====================

    public Room createRoom(RoomType roomType, String roomNumber, int floor) {
        LOGGER.info("Creating room: " + roomNumber + " (" + roomType + ") on floor " + floor);
        Room room = new Room(roomNumber, roomType, floor);
        room.setDescription(generateRoomDescription(roomType));
        return room;
    }

    public Room createRoom(RoomType roomType, String roomNumber, int floor,
                           BigDecimal customPrice, String description) {
        Room room = createRoom(roomType, roomNumber, floor);
        if (customPrice != null) {
            room.setBasePriceOverride(customPrice.doubleValue());
        }
        if (description != null && !description.trim().isEmpty()) {
            room.setDescription(description);
        }
        return room;
    }

    private String generateRoomDescription(RoomType roomType) {
        return switch (roomType) {
            case SINGLE -> "Cozy single room with queen bed.";
            case DOUBLE -> "Spacious double room with two queen beds.";
            case DELUXE -> "Luxurious deluxe room with city view.";
            case PENTHOUSE -> "Exclusive penthouse suite with VIP services.";
        };
    }

    // ==================== Occupancy Methods ====================

    public int getMaxOccupancy(RoomType roomType) {
        return OCCUPANCY_LIMITS.getOrDefault(roomType, 2);
    }

    public boolean isOccupancyValid(RoomType roomType, int totalGuests) {
        return totalGuests > 0 && totalGuests <= getMaxOccupancy(roomType);
    }

    public List<RoomType> getSuitableRoomTypes(int totalGuests) {
        return Arrays.stream(RoomType.values())
                .filter(type -> getMaxOccupancy(type) >= totalGuests)
                .collect(Collectors.toList());
    }

    // ==================== Availability Methods (OPTIMIZED) ====================

    /**
     * Check if a specific room is available for the given dates.
     * Uses Strict DB check for overlapping reservations.
     */
    public boolean isRoomAvailable(Room room, LocalDate checkIn, LocalDate checkOut) {
        if (room == null || checkIn == null || checkOut == null) return false;

        // Rooms under maintenance are never available
        if (room.getStatus() == RoomStatus.MAINTENANCE) return false;

        // Strict DB Check
        if (reservationRepository != null) {
            List<Reservation> overlapping = reservationRepository
                    .findOverlappingReservations(room.getId(), checkIn, checkOut);
            return overlapping.isEmpty();
        }

        // Fallback if repository is missing (mostly for unit testing without mocks)
        if (checkIn.equals(LocalDate.now())) {
            return room.getStatus() == RoomStatus.AVAILABLE;
        }
        return true;
    }

    /**
     * PRIMARY METHOD: Get all strictly available rooms of a specific type.
     * Delegates entirely to the Repository's custom SQL query for efficiency.
     */
    public List<Room> getAvailableRooms(RoomType type, LocalDate checkIn, LocalDate checkOut) {
        if (roomRepository == null) return Collections.emptyList();

        // Call the strict repository method we added previously
        return roomRepository.findStrictlyAvailableRooms(type, checkIn, checkOut);
    }

    /**
     * Wrapper for getAvailableRooms to support legacy calls or clearer naming.
     */
    public List<Room> getAvailableRoomsByType(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        return getAvailableRooms(roomType, checkIn, checkOut);
    }

    /**
     * Get a specific number of available rooms (used for assigning to reservations).
     */
    public List<Room> getAvailableRooms(RoomType roomType, LocalDate checkIn, LocalDate checkOut, int quantity) {
        List<Room> available = getAvailableRooms(roomType, checkIn, checkOut);

        if (available.size() <= quantity) {
            return available;
        }
        return available.subList(0, quantity);
    }

    /**
     * Gets occupancy statistics for the current date (Today).
     * @return Map containing "Total", "Available", and "Occupied" counts.
     */
    public Map<String, Integer> getTodayOccupancyStats() {
        LocalDate today = LocalDate.now();

        // 1. Get Total Rooms
        int totalRooms = (int) roomRepository.countAllRooms();

        // 2. Get Available Rooms for Tonight
        // (Available from Today to Tomorrow)
        List<Room> availableRooms = roomRepository.findStrictlyAvailableRooms(today, today.plusDays(1));
        int availableCount = availableRooms.size();

        // 3. Calculate Occupied
        int occupiedCount = totalRooms - availableCount;

        // 4. Return Summary
        Map<String, Integer> stats = new HashMap<>();
        stats.put("Total", totalRooms);
        stats.put("Available", availableCount);
        stats.put("Occupied", occupiedCount);

        return stats;
    }

    /**
     * Get a summary count of available rooms for ALL types.
     * Iterates through Enums to build the map.
     */
    public Map<RoomType, Integer> getAvailabilityByType(LocalDate checkIn, LocalDate checkOut) {
        Map<RoomType, Integer> availability = new EnumMap<>(RoomType.class);

        for (RoomType type : RoomType.values()) {
            List<Room> rooms = getAvailableRooms(type, checkIn, checkOut);
            availability.put(type, rooms.size());
        }

        return availability;
    }

    /**
     * Check if there is ANY availability in the hotel.
     */
    public boolean hasAvailability(LocalDate checkIn, LocalDate checkOut) {
        for (RoomType type : RoomType.values()) {
            if (!getAvailableRooms(type, checkIn, checkOut).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // ==================== Group Booking Suggestions ====================

    /**
     * Suggest room combinations for a group booking.
     */
    public List<RoomSuggestion> suggestRoomsForGroup(int adults, int children,
                                                     LocalDate checkIn, LocalDate checkOut) {
        List<RoomSuggestion> suggestions = new ArrayList<>();
        int totalGuests = adults + children;

        if (totalGuests <= 0) return suggestions;

        Map<RoomType, Integer> availability = getAvailabilityByType(checkIn, checkOut);

        suggestions.add(suggestMinimumRooms(totalGuests, availability, checkIn, checkOut));

        if (totalGuests <= 4) {
            suggestions.add(suggestComfortOption(totalGuests, availability, checkIn, checkOut));
        }

        suggestions.add(suggestPremiumOption(totalGuests, availability, checkIn, checkOut));

        // Filter valid suggestions and sort by price
        suggestions.removeIf(s -> s == null || !s.isValid());
        suggestions.sort(Comparator.comparing(RoomSuggestion::getTotalPrice));

        return suggestions;
    }

    private RoomSuggestion suggestMinimumRooms(int totalGuests, Map<RoomType, Integer> availability,
                                               LocalDate checkIn, LocalDate checkOut) {
        RoomSuggestion suggestion = new RoomSuggestion();
        suggestion.setName("Economy");
        suggestion.setDescription("Minimum rooms needed for your group");

        int remainingGuests = totalGuests;
        Map<RoomType, Integer> selectedRooms = new EnumMap<>(RoomType.class);

        // Strategy: Maximize Doubles first
        int doubleCap = getMaxOccupancy(RoomType.DOUBLE);
        int doubleAvail = availability.getOrDefault(RoomType.DOUBLE, 0);
        int doublesNeeded = remainingGuests / doubleCap;
        int doublesToUse = Math.min(doublesNeeded, doubleAvail);

        if (doublesToUse > 0) {
            selectedRooms.put(RoomType.DOUBLE, doublesToUse);
            remainingGuests -= doublesToUse * doubleCap;
        }

        // Fill rest with Singles
        if (remainingGuests > 0) {
            int singleCap = getMaxOccupancy(RoomType.SINGLE);
            int singleAvail = availability.getOrDefault(RoomType.SINGLE, 0);
            int singlesNeeded = (int) Math.ceil((double) remainingGuests / singleCap);
            int singlesToUse = Math.min(singlesNeeded, singleAvail);

            if (singlesToUse > 0) {
                selectedRooms.put(RoomType.SINGLE, singlesToUse);
                remainingGuests -= singlesToUse * singleCap;
            }
        }

        if (remainingGuests > 0) return null; // Cannot accommodate

        suggestion.setRooms(selectedRooms);
        suggestion.setTotalPrice(calculateTotalPrice(selectedRooms, checkIn, checkOut));
        return suggestion;
    }

    private RoomSuggestion suggestComfortOption(int totalGuests, Map<RoomType, Integer> availability,
                                                LocalDate checkIn, LocalDate checkOut) {
        RoomSuggestion suggestion = new RoomSuggestion();
        suggestion.setName("Comfort");
        suggestion.setDescription("More space and privacy");
        Map<RoomType, Integer> selectedRooms = new EnumMap<>(RoomType.class);

        if (totalGuests <= 2) {
            if (availability.getOrDefault(RoomType.DELUXE, 0) > 0) selectedRooms.put(RoomType.DELUXE, 1);
            else if (availability.getOrDefault(RoomType.DOUBLE, 0) > 0) selectedRooms.put(RoomType.DOUBLE, 1);
            else return null;
        } else if (totalGuests <= 4) {
            if (availability.getOrDefault(RoomType.SINGLE, 0) >= 2) selectedRooms.put(RoomType.SINGLE, 2);
            else if (availability.getOrDefault(RoomType.DOUBLE, 0) > 0) selectedRooms.put(RoomType.DOUBLE, 1);
            else return null;
        }

        if (selectedRooms.isEmpty()) return null;
        suggestion.setRooms(selectedRooms);
        suggestion.setTotalPrice(calculateTotalPrice(selectedRooms, checkIn, checkOut));
        return suggestion;
    }

    private RoomSuggestion suggestPremiumOption(int totalGuests, Map<RoomType, Integer> availability,
                                                LocalDate checkIn, LocalDate checkOut) {
        RoomSuggestion suggestion = new RoomSuggestion();
        suggestion.setName("Premium");
        suggestion.setDescription("Luxury experience");
        Map<RoomType, Integer> selectedRooms = new EnumMap<>(RoomType.class);

        if (availability.getOrDefault(RoomType.PENTHOUSE, 0) > 0 && totalGuests <= 2) {
            selectedRooms.put(RoomType.PENTHOUSE, 1);
        } else if (availability.getOrDefault(RoomType.DELUXE, 0) > 0) {
            int needed = (int) Math.ceil((double) totalGuests / getMaxOccupancy(RoomType.DELUXE));
            if (availability.get(RoomType.DELUXE) >= needed) {
                selectedRooms.put(RoomType.DELUXE, needed);
            }
        }

        if (selectedRooms.isEmpty()) return null;
        suggestion.setRooms(selectedRooms);
        suggestion.setTotalPrice(calculateTotalPrice(selectedRooms, checkIn, checkOut));
        return suggestion;
    }

    // ==================== Pricing Methods ====================

    public BigDecimal getBasePrice(RoomType roomType) {
        return BASE_PRICES.getOrDefault(roomType, new BigDecimal("99.00"));
    }

    public BigDecimal calculateNightlyRate(RoomType roomType, LocalDate date) {
        BigDecimal basePrice = getBasePrice(roomType);
        BigDecimal multiplier = BigDecimal.ONE;

        if (isWeekend(date)) {
            multiplier = multiplier.multiply(pricingConfig.getWeekendMultiplier());
        } else {
            multiplier = multiplier.multiply(pricingConfig.getWeekdayMultiplier());
        }

        if (isSeasonalPeriod(date)) {
            multiplier = multiplier.multiply(pricingConfig.getSeasonalMultiplier());
        }

        return basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateStayPrice(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            total = total.add(calculateNightlyRate(roomType, current));
            current = current.plusDays(1);
        }
        return total;
    }

    private BigDecimal calculateTotalPrice(Map<RoomType, Integer> rooms, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<RoomType, Integer> entry : rooms.entrySet()) {
            BigDecimal roomPrice = calculateStayPrice(entry.getKey(), checkIn, checkOut);
            total = total.add(roomPrice.multiply(new BigDecimal(entry.getValue())));
        }
        return total;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private boolean isSeasonalPeriod(LocalDate date) {
        if (pricingConfig == null || pricingConfig.getSeasonalPeriods() == null) return false;
        for (PricingConfig.SeasonalPeriod period : pricingConfig.getSeasonalPeriods()) {
            if (!date.isBefore(period.getStartDate()) && !date.isAfter(period.getEndDate())) return true;
        }
        return false;
    }

    // ==================== Observer Pattern Methods ====================

    public void addObserver(RoomAvailabilityObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void removeObserver(RoomAvailabilityObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(RoomAvailabilityEvent event) {
        for (RoomAvailabilityObserver observer : observers) {
            try {
                observer.onAvailabilityChange(event);
            } catch (Exception e) {
                LOGGER.warning("Error notifying observer: " + e.getMessage());
            }
        }
    }

    public void markRoomAvailable(Room room, LocalDate availableDate) {
        if (room == null) return;
        room.setStatus(RoomStatus.AVAILABLE);
        if (roomRepository != null) roomRepository.save(room);

        RoomAvailabilityEvent event = new RoomAvailabilityEvent(
                room.getRoomType(), room.getRoomNumber(), availableDate, true
        );
        notifyObservers(event);
    }

    public void markRoomOccupied(Room room) {
        if (room == null) return;
        room.setStatus(RoomStatus.OCCUPIED);
        if (roomRepository != null) roomRepository.save(room);
    }

    // ==================== Utility Methods ====================

    public List<Room> getAllRooms() {
        return roomRepository != null ? roomRepository.findAll() : Collections.emptyList();
    }

    public Optional<Room> getRoomByNumber(String roomNumber) {
        return roomRepository != null ? roomRepository.findByRoomNumber(roomNumber) : Optional.empty();
    }

    public Room saveRoom(Room room) {
        return (roomRepository != null && room != null) ? roomRepository.save(room) : room;
    }

    // ==================== Setters ====================

    public void setRoomRepository(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void setReservationRepository(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public void setPricingConfig(PricingConfig pricingConfig) {
        this.pricingConfig = pricingConfig;
    }

    // ==================== Inner Class ====================

    public static class RoomSuggestion {
        private String name;
        private String description;
        private Map<RoomType, Integer> rooms = new EnumMap<>(RoomType.class);
        private BigDecimal totalPrice;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Map<RoomType, Integer> getRooms() { return rooms; }
        public void setRooms(Map<RoomType, Integer> rooms) { this.rooms = rooms; }
        public BigDecimal getTotalPrice() { return totalPrice; }
        public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
        public boolean isValid() { return rooms != null && !rooms.isEmpty() && totalPrice != null; }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<RoomType, Integer> entry : rooms.entrySet()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(entry.getValue()).append("x ").append(entry.getKey().getDisplayName());
            }
            return sb.toString();
        }
    }
}