package com.hanson.hotelreservationsystem.repository;

import com.hanson.hotelreservationsystem.model.LoyaltyAccount;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Repository for LoyaltyAccount entity operations.
 * Provides persistence and query methods for loyalty accounts.
 */
public class LoyaltyAccountRepository {

    private static final Logger LOGGER = Logger.getLogger(LoyaltyAccountRepository.class.getName());

    private static LoyaltyAccountRepository instance;
    private EntityManager entityManager;

    // 2. Private Constructor
    private LoyaltyAccountRepository() {}

    // 3. Public Accessor
    public static synchronized LoyaltyAccountRepository getInstance() {
        if (instance == null) {
            instance = new LoyaltyAccountRepository();
        }
        return instance;
    }

    // 4. Setter for EntityManager (Used by ServiceInitializer)
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    /**
     * Save or update a loyalty account.
     */
    public LoyaltyAccount save(LoyaltyAccount account) {
        if (entityManager == null) {
            return account;
        }

        boolean startedTransaction = false;
        try {
            if (!entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().begin();
                startedTransaction = true;
            }

            if (account.getId() == null) {
                entityManager.persist(account);
                LOGGER.info("Created new loyalty account: " + account.getLoyaltyNumber());
            } else {
                account = entityManager.merge(account);
                LOGGER.info("Updated loyalty account: " + account.getLoyaltyNumber());
            }

            if (startedTransaction) {
                entityManager.getTransaction().commit();
            }

            return account;
        } catch (Exception e) {
            if (startedTransaction && entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw e;
        }
    }

    /**
     * Find a loyalty account by ID.
     */
    public Optional<LoyaltyAccount> findById(Long id) {
        LoyaltyAccount account = entityManager.find(LoyaltyAccount.class, id);
        return Optional.ofNullable(account);
    }

    /**
     * Find a loyalty account by loyalty number.
     */
    public Optional<LoyaltyAccount> findByLoyaltyNumber(String loyaltyNumber) {
        TypedQuery<LoyaltyAccount> query = entityManager.createQuery(
                "SELECT la FROM LoyaltyAccount la WHERE la.loyaltyNumber = :loyaltyNumber",
                LoyaltyAccount.class);
        query.setParameter("loyaltyNumber", loyaltyNumber);

        List<LoyaltyAccount> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Find a loyalty account by guest's email.
     */
    public Optional<LoyaltyAccount> findByGuestEmail(String email) {
        TypedQuery<LoyaltyAccount> query = entityManager.createQuery(
                "SELECT la FROM LoyaltyAccount la WHERE LOWER(la.guest.email) = LOWER(:email)",
                LoyaltyAccount.class);
        query.setParameter("email", email);

        List<LoyaltyAccount> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Find a loyalty account by guest's phone.
     */
    public Optional<LoyaltyAccount> findByGuestPhone(String phone) {
        TypedQuery<LoyaltyAccount> query = entityManager.createQuery(
                "SELECT la FROM LoyaltyAccount la WHERE la.guest.phone = :phone",
                LoyaltyAccount.class);
        query.setParameter("phone", phone);

        List<LoyaltyAccount> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Find a loyalty account by guest ID.
     */
    public Optional<LoyaltyAccount> findByGuestId(Long guestId) {
        TypedQuery<LoyaltyAccount> query = entityManager.createQuery(
                "SELECT la FROM LoyaltyAccount la WHERE la.guest.id = :guestId",
                LoyaltyAccount.class);
        query.setParameter("guestId", guestId);

        List<LoyaltyAccount> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Find all active loyalty accounts.
     */
    public List<LoyaltyAccount> findByActiveTrue() {
        TypedQuery<LoyaltyAccount> query = entityManager.createQuery(
                "SELECT la FROM LoyaltyAccount la WHERE la.active = true ORDER BY la.enrollmentDate DESC",
                LoyaltyAccount.class);
        return query.getResultList();
    }

    /**
     * Find all loyalty accounts.
     */
    public List<LoyaltyAccount> findAll() {
        TypedQuery<LoyaltyAccount> query = entityManager.createQuery(
                "SELECT la FROM LoyaltyAccount la ORDER BY la.enrollmentDate DESC",
                LoyaltyAccount.class);
        return query.getResultList();
    }

    /**
     * Find loyalty accounts by tier.
     */
    public List<LoyaltyAccount> findByTier(String tier) {
        TypedQuery<LoyaltyAccount> query = entityManager.createQuery(
                "SELECT la FROM LoyaltyAccount la WHERE la.tier = :tier AND la.active = true",
                LoyaltyAccount.class);
        query.setParameter("tier", tier);
        return query.getResultList();
    }

    /**
     * Count active loyalty accounts.
     */
    public long countByActiveTrue() {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(la) FROM LoyaltyAccount la WHERE la.active = true",
                Long.class);
        return query.getSingleResult();
    }

    /**
     * Sum of all lifetime points across active accounts.
     */
    public long sumLifetimePoints() {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COALESCE(SUM(la.lifetimePoints), 0) FROM LoyaltyAccount la WHERE la.active = true",
                Long.class);
        Long result = query.getSingleResult();
        return result != null ? result : 0L;
    }

    /**
     * Sum of all current points balances across active accounts.
     */
    public long sumPointsBalance() {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COALESCE(SUM(la.pointsBalance), 0) FROM LoyaltyAccount la WHERE la.active = true",
                Long.class);
        Long result = query.getSingleResult();
        return result != null ? result : 0L;
    }

    /**
     * Find accounts with points above a threshold.
     */
    public List<LoyaltyAccount> findByPointsBalanceGreaterThan(int minPoints) {
        TypedQuery<LoyaltyAccount> query = entityManager.createQuery(
                "SELECT la FROM LoyaltyAccount la WHERE la.pointsBalance > :minPoints AND la.active = true",
                LoyaltyAccount.class);
        query.setParameter("minPoints", minPoints);
        return query.getResultList();
    }

    /**
     * Delete a loyalty account.
     */
    public void delete(LoyaltyAccount account) {
        if (entityManager.contains(account)) {
            entityManager.remove(account);
        } else {
            LoyaltyAccount attached = entityManager.find(LoyaltyAccount.class, account.getId());
            if (attached != null) {
                entityManager.remove(attached);
            }
        }
        LOGGER.info("Deleted loyalty account: " + account.getLoyaltyNumber());
    }

    /**
     * Check if a loyalty account exists for a guest.
     */
    public boolean existsByGuestId(Long guestId) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(la) FROM LoyaltyAccount la WHERE la.guest.id = :guestId",
                Long.class);
        query.setParameter("guestId", guestId);
        return query.getSingleResult() > 0;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }
}