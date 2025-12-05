package com.hanson.hotelreservationsystem.repository;

import com.hanson.hotelreservationsystem.model.LoyaltyAccount;
import com.hanson.hotelreservationsystem.model.LoyaltyTransaction;
import com.hanson.hotelreservationsystem.model.enums.LoyaltyTransactionType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Repository for LoyaltyTransaction entity operations.
 * Provides persistence and query methods for loyalty transactions.
 */
public class LoyaltyTransactionRepository {

    private static final Logger LOGGER = Logger.getLogger(LoyaltyTransactionRepository.class.getName());

    private final EntityManager entityManager;

    /**
     * Constructor with EntityManager injection.
     */
    public LoyaltyTransactionRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Save a loyalty transaction.
     */
    public LoyaltyTransaction save(LoyaltyTransaction transaction) {
        if (entityManager == null) {
            return transaction;
        }

        boolean startedTransaction = false;
        try {
            if (!entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().begin();
                startedTransaction = true;
            }

            if (transaction.getId() == null) {
                entityManager.persist(transaction);
                LOGGER.info("Created new loyalty transaction: " + transaction.getTransactionType());
            } else {
                transaction = entityManager.merge(transaction);
                LOGGER.info("Updated loyalty transaction: " + transaction.getId());
            }

            if (startedTransaction) {
                entityManager.getTransaction().commit();
            }

            return transaction;
        } catch (Exception e) {
            if (startedTransaction && entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw e;
        }
    }

    /**
     * Find a transaction by ID.
     */
    public Optional<LoyaltyTransaction> findById(Long id) {
        LoyaltyTransaction transaction = entityManager.find(LoyaltyTransaction.class, id);
        return Optional.ofNullable(transaction);
    }

    /**
     * Find all transactions for a loyalty account, ordered by date descending.
     */
    public List<LoyaltyTransaction> findByLoyaltyAccountOrderByTransactionDateDesc(LoyaltyAccount account) {
        TypedQuery<LoyaltyTransaction> query = entityManager.createQuery(
                "SELECT lt FROM LoyaltyTransaction lt WHERE lt.loyaltyAccount = :account " +
                        "ORDER BY lt.transactionDate DESC",
                LoyaltyTransaction.class);
        query.setParameter("account", account);
        return query.getResultList();
    }

    /**
     * Find transactions by account and type.
     */
    public List<LoyaltyTransaction> findByLoyaltyAccountAndTransactionTypeOrderByTransactionDateDesc(
            LoyaltyAccount account, LoyaltyTransactionType transactionType) {
        TypedQuery<LoyaltyTransaction> query = entityManager.createQuery(
                "SELECT lt FROM LoyaltyTransaction lt WHERE lt.loyaltyAccount = :account " +
                        "AND lt.transactionType = :type ORDER BY lt.transactionDate DESC",
                LoyaltyTransaction.class);
        query.setParameter("account", account);
        query.setParameter("type", transactionType);
        return query.getResultList();
    }

    /**
     * Find transactions within a date range.
     */
    public List<LoyaltyTransaction> findByTransactionDateBetween(LocalDateTime startDate, LocalDateTime endDate) {
        TypedQuery<LoyaltyTransaction> query = entityManager.createQuery(
                "SELECT lt FROM LoyaltyTransaction lt WHERE lt.transactionDate BETWEEN :startDate AND :endDate " +
                        "ORDER BY lt.transactionDate DESC",
                LoyaltyTransaction.class);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        return query.getResultList();
    }

    /**
     * Find transactions by reservation ID.
     */
    public List<LoyaltyTransaction> findByReservationId(Long reservationId) {
        TypedQuery<LoyaltyTransaction> query = entityManager.createQuery(
                "SELECT lt FROM LoyaltyTransaction lt WHERE lt.reservation.id = :reservationId " +
                        "ORDER BY lt.transactionDate DESC",
                LoyaltyTransaction.class);
        query.setParameter("reservationId", reservationId);
        return query.getResultList();
    }

    /**
     * Sum points earned for an account.
     */
    public long sumPointsEarnedByAccount(LoyaltyAccount account) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COALESCE(SUM(lt.points), 0) FROM LoyaltyTransaction lt " +
                        "WHERE lt.loyaltyAccount = :account AND lt.transactionType IN :earnTypes",
                Long.class);
        query.setParameter("account", account);
        query.setParameter("earnTypes", List.of(LoyaltyTransactionType.EARN, LoyaltyTransactionType.BONUS));
        Long result = query.getSingleResult();
        return result != null ? result : 0L;
    }

    /**
     * Sum points redeemed for an account.
     */
    public long sumPointsRedeemedByAccount(LoyaltyAccount account) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COALESCE(SUM(ABS(lt.points)), 0) FROM LoyaltyTransaction lt " +
                        "WHERE lt.loyaltyAccount = :account AND lt.transactionType = :redeemType",
                Long.class);
        query.setParameter("account", account);
        query.setParameter("redeemType", LoyaltyTransactionType.REDEEM);
        Long result = query.getSingleResult();
        return result != null ? result : 0L;
    }

    /**
     * Count transactions by type.
     */
    public long countByTransactionType(LoyaltyTransactionType transactionType) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(lt) FROM LoyaltyTransaction lt WHERE lt.transactionType = :type",
                Long.class);
        query.setParameter("type", transactionType);
        return query.getSingleResult();
    }

    /**
     * Get recent transactions (limit by count).
     */
    public List<LoyaltyTransaction> findRecentTransactions(int limit) {
        TypedQuery<LoyaltyTransaction> query = entityManager.createQuery(
                "SELECT lt FROM LoyaltyTransaction lt ORDER BY lt.transactionDate DESC",
                LoyaltyTransaction.class);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    /**
     * Delete a transaction.
     */
    public void delete(LoyaltyTransaction transaction) {
        if (entityManager.contains(transaction)) {
            entityManager.remove(transaction);
        } else {
            LoyaltyTransaction attached = entityManager.find(LoyaltyTransaction.class, transaction.getId());
            if (attached != null) {
                entityManager.remove(attached);
            }
        }
        LOGGER.info("Deleted loyalty transaction: " + transaction.getId());
    }
}