package com.hanson.hotelreservationsystem.repository;

import com.hanson.hotelreservationsystem.model.Feedback;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.logging.Logger;

/**
 * Repository for Feedback entity operations.
 */
public class FeedbackRepository {

    private static final Logger LOGGER = Logger.getLogger(FeedbackRepository.class.getName());
    private static FeedbackRepository instance;
    private EntityManager entityManager;

    private FeedbackRepository() {}

    public FeedbackRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public static synchronized FeedbackRepository getInstance() {
        if (instance == null) {
            instance = new FeedbackRepository();
        }
        return instance;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Save or update feedback.
     */
    public Feedback save(Feedback feedback) {
        if (entityManager == null) {
            LOGGER.severe("EntityManager is null. Cannot save feedback.");
            return feedback;
        }

        boolean startedTransaction = false;
        try {
            if (!entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().begin();
                startedTransaction = true;
            }

            if (feedback.getId() == null) {
                entityManager.persist(feedback);
            } else {
                feedback = entityManager.merge(feedback);
            }

            if (startedTransaction) {
                entityManager.getTransaction().commit();
            }
            return feedback;
        } catch (Exception e) {
            if (startedTransaction && entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            LOGGER.severe("Failed to save feedback: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Find all feedback ordered by date (newest first).
     */
    public List<Feedback> findAll() {
        if (entityManager == null) return List.of();

        // Clear cache to ensure fresh data
        entityManager.clear();

        TypedQuery<Feedback> query = entityManager.createQuery(
                "SELECT f FROM Feedback f ORDER BY f.submittedAt DESC",
                Feedback.class);
        return query.getResultList();
    }
}