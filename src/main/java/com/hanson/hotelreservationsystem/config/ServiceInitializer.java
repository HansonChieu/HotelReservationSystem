package com.hanson.hotelreservationsystem.config;

import com.hanson.hotelreservationsystem.model.*;
import com.hanson.hotelreservationsystem.model.enums.*;
import com.hanson.hotelreservationsystem.repository.*;
import com.hanson.hotelreservationsystem.service.*;
import jakarta.persistence.EntityManager;
import org.mindrot.jbcrypt.BCrypt;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central configuration class for Dependency Injection.
 *
 * Per assignment requirements:
 * - "A central configuration class must wire dependencies and provide singletons where appropriate"
 * - "Controllers, services, and repositories must use constructor injection"
 * - "Services must depend on repositories, not on ORM APIs directly"
 *
 * This class initializes all repositories with EntityManager and wires
 * all services with their required dependencies at application startup.
 */
public class ServiceInitializer {

    private static final Logger LOGGER = Logger.getLogger(ServiceInitializer.class.getName());
    private static boolean initialized = false;
    private static EntityManager sharedEntityManager;

    /**
     * Initialize all services and repositories with their dependencies.
     * This should be called once at application startup.
     */
    public static synchronized void initialize() {
        if (initialized) {
            LOGGER.info("Services already initialized - skipping");
            return;
        }

        LOGGER.info("========================================");
        LOGGER.info("Initializing Services and Repositories");
        LOGGER.info("========================================");

        try {
            // Create shared EntityManager for this session
            sharedEntityManager = JPAUtil.createEntityManager();
            LOGGER.info("EntityManager created");

            // ==================== Initialize Repositories ====================
            LOGGER.info("Initializing repositories...");

            RoomRepository roomRepository = RoomRepository.getInstance();
            roomRepository.setEntityManager(sharedEntityManager);
            LOGGER.info("  - RoomRepository initialized");

            ReservationRepository reservationRepository = ReservationRepository.getInstance();
            reservationRepository.setEntityManager(sharedEntityManager);
            LOGGER.info("  - ReservationRepository initialized");

            GuestRepository guestRepository = GuestRepository.getInstance();
            guestRepository.setEntityManager(sharedEntityManager);
            LOGGER.info("  - GuestRepository initialized");


            AdminRepository adminRepository = AdminRepository.getInstance();
            adminRepository.setEntityManager(sharedEntityManager);

//            LoyaltyAccountRepository loyaltyRepository = LoyaltyRepository.getInstance();
//            loyaltyRepository.setEntityManager(sharedEntityManager);

            // ==================== Initialize Services ====================
            LOGGER.info("Initializing services...");

            // RoomService - depends on RoomRepository, ReservationRepository
            RoomService roomService = RoomService.getInstance();
            roomService.setRoomRepository(roomRepository);
            roomService.setReservationRepository(reservationRepository);
            LOGGER.info("  - RoomService initialized");

            // PricingService
            PricingService pricingService = PricingService.getInstance();
            // pricingService.setPricingConfig(new PricingConfig());
            LOGGER.info("  - PricingService initialized");

            // LoyaltyService
            LoyaltyService loyaltyService = LoyaltyService.getInstance();
            // loyaltyService.setLoyaltyRepository(loyaltyRepository);
            LOGGER.info("  - LoyaltyService initialized");

            // ValidationService
            ValidationService validationService = ValidationService.getInstance();
            LOGGER.info("  - ValidationService initialized");

            // ReservationService - depends on multiple repositories and services
            ReservationService reservationService = ReservationService.getInstance();
            reservationService.setReservationRepository(reservationRepository);
            reservationService.setRoomRepository(roomRepository);
            reservationService.setGuestRepository(guestRepository);
            reservationService.setRoomService(roomService);
            reservationService.setLoyaltyService(loyaltyService);
            reservationService.setPricingService(pricingService);
            reservationService.setValidationService(validationService);
            LOGGER.info("  - ReservationService initialized");

            // ==================== Create Sample Data ====================
            createSampleDataIfNeeded(roomRepository);

            initialized = true;
            LOGGER.info("========================================");
            LOGGER.info("All services initialized successfully!");
            LOGGER.info("========================================");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize services", e);
            throw new RuntimeException("Service initialization failed", e);
        }
    }

    /**
     * Create sample rooms if database is empty.
     * This ensures the system has rooms available for booking.
     */
    private static void createSampleDataIfNeeded(RoomRepository roomRepository) {
        try {
            createDefaultAdmin();
            long roomCount = roomRepository.count();

            if (roomCount == 0) {
                LOGGER.info("No rooms found in database - creating sample data...");

                sharedEntityManager.getTransaction().begin();

                // Create Single Rooms (Floor 1: 101-105) - Max 2 guests
                for (int i = 1; i <= 5; i++) {
                    Room room = new Room("10" + i, RoomType.SINGLE, 1);
                    room.setStatus(RoomStatus.AVAILABLE);
                    room.setDescription("Cozy single room with queen bed, perfect for solo travelers or couples.");
                    sharedEntityManager.persist(room);
                }
                LOGGER.info("  - Created 5 Single rooms (101-105)");

                // Create Double Rooms (Floor 2: 201-205) - Max 4 guests
                for (int i = 1; i <= 5; i++) {
                    Room room = new Room("20" + i, RoomType.DOUBLE, 2);
                    room.setStatus(RoomStatus.AVAILABLE);
                    room.setDescription("Spacious double room with two queen beds, ideal for families or groups.");
                    sharedEntityManager.persist(room);
                }
                LOGGER.info("  - Created 5 Double rooms (201-205)");

                // Create Deluxe Rooms (Floor 3: 301-303) - Max 2 guests, premium price
                for (int i = 1; i <= 3; i++) {
                    Room room = new Room("30" + i, RoomType.DELUXE, 3);
                    room.setStatus(RoomStatus.AVAILABLE);
                    room.setDescription("Luxurious deluxe room with king bed, premium amenities, and city view.");
                    sharedEntityManager.persist(room);
                }
                LOGGER.info("  - Created 3 Deluxe rooms (301-303)");

                // Create Penthouse Suites (Floor 4: 401-402) - Max 2 guests, highest price
                for (int i = 1; i <= 2; i++) {
                    Room room = new Room("40" + i, RoomType.PENTHOUSE, 4);
                    room.setStatus(RoomStatus.AVAILABLE);
                    room.setDescription("Exclusive penthouse suite with panoramic views, separate living area, and VIP services.");
                    sharedEntityManager.persist(room);
                }
                LOGGER.info("  - Created 2 Penthouse suites (401-402)");

                sharedEntityManager.getTransaction().commit();

                LOGGER.info("Sample data created: 15 total rooms");
                LOGGER.info("  - 5 Single ($100/night, max 2 guests)");
                LOGGER.info("  - 5 Double ($150/night, max 4 guests)");
                LOGGER.info("  - 3 Deluxe ($250/night, max 2 guests)");
                LOGGER.info("  - 2 Penthouse ($500/night, max 2 guests)");

            } else {
                LOGGER.info("Database already has " + roomCount + " rooms - skipping sample data creation");
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create sample data", e);
            if (sharedEntityManager.getTransaction().isActive()) {
                sharedEntityManager.getTransaction().rollback();
            }
        }
        createDefaultAdmin();
    }

    private static void createDefaultAdmin() {
        AdminRepository adminRepository = AdminRepository.getInstance();

        // ==========================================
        // 1. CREATE DEFAULT ADMIN (admin / admin123)
        // ==========================================
        boolean adminExists = false;

        // Check username
        if (adminRepository.findByUsername("admin").isPresent()) {
            LOGGER.info("Default user 'admin' already exists.");
            adminExists = true;
        }

        // Check email (to prevent unique constraint violation)
        if (!adminExists) {
            try {
                Long emailCount = sharedEntityManager.createQuery(
                                "SELECT COUNT(a) FROM Admin a WHERE a.email = :email", Long.class)
                        .setParameter("email", "admin@archotel.com")
                        .getSingleResult();
                if (emailCount > 0) {
                    LOGGER.info("Email 'admin@archotel.com' already exists. Skipping admin creation.");
                    adminExists = true;
                }
            } catch (Exception e) { /* ignore */ }
        }

        // Create Admin if not exists
        if (!adminExists) {
            LOGGER.info("Creating default 'admin' user...");
            try {
                sharedEntityManager.getTransaction().begin();
                Admin admin = new Admin();
                admin.setUsername("admin");
                admin.setFirstName("System");
                admin.setLastName("Admin");
                admin.setEmail("admin@archotel.com");
                admin.setRole(Role.ADMIN);
                admin.setActive(true);
                admin.setPasswordHash(BCrypt.hashpw("admin123", BCrypt.gensalt()));

                sharedEntityManager.persist(admin);
                sharedEntityManager.getTransaction().commit();
                LOGGER.info("Default admin created successfully.");
            } catch (Exception e) {
                if (sharedEntityManager.getTransaction().isActive()) {
                    sharedEntityManager.getTransaction().rollback();
                }
                LOGGER.warning("Failed to create admin: " + e.getMessage());
            }
        }

        // ==============================================
        // 2. CREATE DEFAULT MANAGER (manager / manager123)
        // ==============================================
        boolean managerExists = false;

        // Check username
        if (adminRepository.findByUsername("manager").isPresent()) {
            LOGGER.info("Default user 'manager' already exists.");
            managerExists = true;
        }

        // Check email
        if (!managerExists) {
            try {
                Long emailCount = sharedEntityManager.createQuery(
                                "SELECT COUNT(a) FROM Admin a WHERE a.email = :email", Long.class)
                        .setParameter("email", "manager@archotel.com")
                        .getSingleResult();
                if (emailCount > 0) {
                    LOGGER.info("Email 'manager@archotel.com' already exists. Skipping manager creation.");
                    managerExists = true;
                }
            } catch (Exception e) { /* ignore */ }
        }

        // Create Manager if not exists
        if (!managerExists) {
            LOGGER.info("Creating default 'manager' user...");
            try {
                sharedEntityManager.getTransaction().begin();
                Admin manager = new Admin();
                manager.setUsername("manager");
                manager.setFirstName("Hotel");
                manager.setLastName("Manager");
                manager.setEmail("manager@archotel.com");
                manager.setRole(Role.MANAGER);
                manager.setActive(true);
                manager.setPasswordHash(BCrypt.hashpw("manager123", BCrypt.gensalt()));

                sharedEntityManager.persist(manager);
                sharedEntityManager.getTransaction().commit();
                LOGGER.info("Default manager created successfully.");
            } catch (Exception e) {
                if (sharedEntityManager.getTransaction().isActive()) {
                    sharedEntityManager.getTransaction().rollback();
                }
                LOGGER.warning("Failed to create manager: " + e.getMessage());
            }
        }
    }

    /**
     * Get the shared EntityManager.
     * Use this for manual transaction control in services.
     */
    public static EntityManager getEntityManager() {
        if (!initialized) {
            throw new IllegalStateException("Services not initialized. Call initialize() first.");
        }
        return sharedEntityManager;
    }

    /**
     * Begin a new transaction.
     */
    public static void beginTransaction() {
        if (sharedEntityManager != null && !sharedEntityManager.getTransaction().isActive()) {
            sharedEntityManager.getTransaction().begin();
        }
    }

    /**
     * Commit the current transaction.
     */
    public static void commitTransaction() {
        if (sharedEntityManager != null && sharedEntityManager.getTransaction().isActive()) {
            sharedEntityManager.getTransaction().commit();
        }
    }

    /**
     * Rollback the current transaction.
     */
    public static void rollbackTransaction() {
        if (sharedEntityManager != null && sharedEntityManager.getTransaction().isActive()) {
            sharedEntityManager.getTransaction().rollback();
        }
    }

    /**
     * Check if services are initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Shutdown and cleanup all resources.
     * Should be called when application closes.
     */
    public static void shutdown() {
        LOGGER.info("Shutting down services...");

        if (sharedEntityManager != null && sharedEntityManager.isOpen()) {
            if (sharedEntityManager.getTransaction().isActive()) {
                sharedEntityManager.getTransaction().rollback();
            }
            sharedEntityManager.close();
        }

        JPAUtil.shutdown();
        initialized = false;

        LOGGER.info("Services shut down successfully");
    }
}