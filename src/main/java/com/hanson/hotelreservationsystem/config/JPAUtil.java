package com.hanson.hotelreservationsystem.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JPA Utility class - Singleton pattern for EntityManagerFactory.
 *
 * Per assignment requirements:
 * - "EntityManagerFactory must be created once and treated as a singleton"
 * - "EntityManager must be created per transaction/unit of work"
 */
public class JPAUtil {

    private static final Logger LOGGER = Logger.getLogger(JPAUtil.class.getName());
    private static final String PERSISTENCE_UNIT_NAME = "HotelReservationPU";

    private static EntityManagerFactory entityManagerFactory;

    // Private constructor - Singleton pattern
    private JPAUtil() {}

    /**
     * Get the singleton EntityManagerFactory.
     * Created once and reused throughout application lifecycle.
     */
    public static synchronized EntityManagerFactory getEntityManagerFactory() {
        if (entityManagerFactory == null) {
            try {
                LOGGER.info("Creating EntityManagerFactory...");
                entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
                LOGGER.info("EntityManagerFactory created successfully");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to create EntityManagerFactory", e);
                throw new RuntimeException("Failed to initialize JPA", e);
            }
        }
        return entityManagerFactory;
    }

    /**
     * Create a new EntityManager (per transaction/unit of work).
     * Each EntityManager should be used for a single unit of work and then closed.
     */
    public static EntityManager createEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    /**
     * Shutdown the EntityManagerFactory.
     * Should be called when application closes.
     */
    public static synchronized void shutdown() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
            entityManagerFactory = null;
            LOGGER.info("EntityManagerFactory closed");
        }
    }

    /**
     * Check if EntityManagerFactory is initialized and open.
     */
    public static boolean isInitialized() {
        return entityManagerFactory != null && entityManagerFactory.isOpen();
    }
}