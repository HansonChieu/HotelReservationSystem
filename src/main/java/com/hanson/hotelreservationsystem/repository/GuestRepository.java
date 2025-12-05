package com.hanson.hotelreservationsystem.repository;

import com.hanson.hotelreservationsystem.model.Guest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Repository for Guest entity operations.
 * Provides persistence and query methods for hotel guests.
 */
public class GuestRepository {

    private static final Logger LOGGER = Logger.getLogger(GuestRepository.class.getName());

    // Singleton instance
    private static GuestRepository instance;

    // EntityManager for database operations
    private EntityManager entityManager;

    /**
     * Private constructor for Singleton pattern.
     */
    private GuestRepository() {
        // EntityManager will be set via setter or dependency injection
    }

    /**
     * Constructor with EntityManager injection.
     */
    public GuestRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Get the singleton instance.
     */
    public static synchronized GuestRepository getInstance() {
        if (instance == null) {
            instance = new GuestRepository();
        }
        return instance;
    }

    // ==================== Basic CRUD Operations ====================

    /**
     * Save or update a guest.
     */
    public Guest save(Guest guest) {
        if (entityManager == null) {
            LOGGER.warning("EntityManager is null - returning guest without persistence");
            return guest;
        }

        boolean startedTransaction = false;
        try {
            // Start transaction if one isn't already active
            if (!entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().begin();
                startedTransaction = true;
            }

            if (guest.getId() == null) {
                entityManager.persist(guest);
                LOGGER.info("Created new guest: " + guest.getFullName());
            } else {
                guest = entityManager.merge(guest);
                LOGGER.info("Updated guest: " + guest.getFullName());
            }

            // Commit only if we started it
            if (startedTransaction) {
                entityManager.getTransaction().commit();
            }

            return guest;
        } catch (Exception e) {
            LOGGER.severe("Error saving guest: " + e.getMessage());
            if (startedTransaction && entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw e;
        }
    }

    /**
     * Find a guest by ID.
     */
    public Optional<Guest> findById(Long id) {
        if (entityManager == null) {
            return Optional.empty();
        }
        Guest guest = entityManager.find(Guest.class, id);
        return Optional.ofNullable(guest);
    }

    /**
     * Find a guest by email address.
     */
    public Optional<Guest> findByEmail(String email) {
        if (entityManager == null || email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }

        TypedQuery<Guest> query = entityManager.createQuery(
                "SELECT g FROM Guest g WHERE LOWER(g.email) = LOWER(:email)",
                Guest.class);
        query.setParameter("email", email.trim());

        List<Guest> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Find a guest by phone number.
     */
    public Optional<Guest> findByPhone(String phone) {
        if (entityManager == null || phone == null || phone.trim().isEmpty()) {
            return Optional.empty();
        }

        // Normalize phone number (remove non-digits for comparison)
        String normalizedPhone = phone.replaceAll("[^0-9]", "");

        TypedQuery<Guest> query = entityManager.createQuery(
                "SELECT g FROM Guest g WHERE REPLACE(REPLACE(REPLACE(g.phone, '-', ''), ' ', ''), '(', '') LIKE :phone",
                Guest.class);
        query.setParameter("phone", "%" + normalizedPhone + "%");

        List<Guest> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Find a guest by email or phone.
     */
    public Optional<Guest> findByEmailOrPhone(String email, String phone) {
        // Try email first
        Optional<Guest> byEmail = findByEmail(email);
        if (byEmail.isPresent()) {
            return byEmail;
        }
        // Try phone
        return findByPhone(phone);
    }

    /**
     * Find all guests.
     */
    public List<Guest> findAll() {
        if (entityManager == null) {
            return List.of();
        }

        TypedQuery<Guest> query = entityManager.createQuery(
                "SELECT g FROM Guest g ORDER BY g.lastName, g.firstName",
                Guest.class);
        return query.getResultList();
    }

    /**
     * Delete a guest.
     */
    public void delete(Guest guest) {
        if (entityManager == null) {
            return;
        }

        if (entityManager.contains(guest)) {
            entityManager.remove(guest);
        } else {
            Guest attached = entityManager.find(Guest.class, guest.getId());
            if (attached != null) {
                entityManager.remove(attached);
            }
        }
        LOGGER.info("Deleted guest: " + guest.getFullName());
    }

    /**
     * Delete a guest by ID.
     */
    public void deleteById(Long id) {
        if (entityManager == null) {
            return;
        }

        Guest guest = entityManager.find(Guest.class, id);
        if (guest != null) {
            entityManager.remove(guest);
            LOGGER.info("Deleted guest with ID: " + id);
        }
    }

    // ==================== Search Methods ====================

    /**
     * Search guests by name (partial match on first or last name).
     */
    public List<Guest> searchByName(String searchTerm) {
        if (entityManager == null || searchTerm == null || searchTerm.trim().isEmpty()) {
            return List.of();
        }

        TypedQuery<Guest> query = entityManager.createQuery(
                "SELECT g FROM Guest g WHERE " +
                        "LOWER(g.firstName) LIKE LOWER(:term) OR " +
                        "LOWER(g.lastName) LIKE LOWER(:term) " +
                        "ORDER BY g.lastName, g.firstName",
                Guest.class);
        query.setParameter("term", "%" + searchTerm.trim() + "%");
        return query.getResultList();
    }

    /**
     * Find guests by country.
     */
    public List<Guest> findByCountry(String country) {
        if (entityManager == null || country == null) {
            return List.of();
        }

        TypedQuery<Guest> query = entityManager.createQuery(
                "SELECT g FROM Guest g WHERE g.country = :country ORDER BY g.lastName",
                Guest.class);
        query.setParameter("country", country);
        return query.getResultList();
    }

    /**
     * Find guests by city.
     */
    public List<Guest> findByCity(String city) {
        if (entityManager == null || city == null) {
            return List.of();
        }

        TypedQuery<Guest> query = entityManager.createQuery(
                "SELECT g FROM Guest g WHERE LOWER(g.city) = LOWER(:city) ORDER BY g.lastName",
                Guest.class);
        query.setParameter("city", city);
        return query.getResultList();
    }

    // ==================== Loyalty Program Queries ====================

    /**
     * Find guests with loyalty accounts.
     */
    public List<Guest> findLoyaltyMembers() {
        if (entityManager == null) {
            return List.of();
        }

        TypedQuery<Guest> query = entityManager.createQuery(
                "SELECT g FROM Guest g WHERE g.loyaltyAccount IS NOT NULL ORDER BY g.lastName",
                Guest.class);
        return query.getResultList();
    }

    /**
     * Find guests without loyalty accounts.
     */
    public List<Guest> findNonLoyaltyMembers() {
        if (entityManager == null) {
            return List.of();
        }

        TypedQuery<Guest> query = entityManager.createQuery(
                "SELECT g FROM Guest g WHERE g.loyaltyAccount IS NULL ORDER BY g.lastName",
                Guest.class);
        return query.getResultList();
    }

    // ==================== Statistics ====================

    /**
     * Count total guests.
     */
    public long count() {
        if (entityManager == null) {
            return 0;
        }

        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(g) FROM Guest g",
                Long.class);
        return query.getSingleResult();
    }

    /**
     * Check if email already exists.
     */
    public boolean existsByEmail(String email) {
        if (entityManager == null || email == null) {
            return false;
        }

        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(g) FROM Guest g WHERE LOWER(g.email) = LOWER(:email)",
                Long.class);
        query.setParameter("email", email.trim());
        return query.getSingleResult() > 0;
    }

    /**
     * Check if phone already exists.
     */
    public boolean existsByPhone(String phone) {
        if (entityManager == null || phone == null) {
            return false;
        }

        String normalizedPhone = phone.replaceAll("[^0-9]", "");
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(g) FROM Guest g WHERE REPLACE(REPLACE(g.phone, '-', ''), ' ', '') LIKE :phone",
                Long.class);
        query.setParameter("phone", "%" + normalizedPhone);
        return query.getSingleResult() > 0;
    }

    // ==================== Paginated Queries ====================

    /**
     * Find all guests with pagination.
     */
    public List<Guest> findAllPaginated(int page, int size) {
        if (entityManager == null) {
            return List.of();
        }

        TypedQuery<Guest> query = entityManager.createQuery(
                "SELECT g FROM Guest g ORDER BY g.lastName, g.firstName",
                Guest.class);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    // ==================== Setters ====================

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }
}