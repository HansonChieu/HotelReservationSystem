package com.hanson.hotelreservationsystem.service;

import com.hanson.hotelreservationsystem.config.JPAUtil;
import com.hanson.hotelreservationsystem.model.Feedback;
import com.hanson.hotelreservationsystem.model.Guest;
import com.hanson.hotelreservationsystem.model.Reservation;
import com.hanson.hotelreservationsystem.repository.FeedbackRepository;
import com.hanson.hotelreservationsystem.repository.GuestRepository;
import com.hanson.hotelreservationsystem.repository.ReservationRepository;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FeedbackService {

    private static final Logger LOGGER = Logger.getLogger(FeedbackService.class.getName());
    private static FeedbackService instance;

    // Default repositories for read-only operations
    private final FeedbackRepository feedbackRepository;

    private FeedbackService() {
        this.feedbackRepository = FeedbackRepository.getInstance();

        if (JPAUtil.isInitialized()) {
            this.feedbackRepository.setEntityManager(JPAUtil.createEntityManager());
        }
    }

    public static synchronized FeedbackService getInstance() {
        if (instance == null) {
            instance = new FeedbackService();
        }
        return instance;
    }

    /**
     * Submit new feedback linked to a reservation.
     * Uses a fresh EntityManager to ensure transactional integrity and avoid session conflicts.
     */
    public void submitFeedback(Long reservationId, String guestEmail, int rating, String comments) {
        LOGGER.info("Starting feedback submission transaction...");

        // 1. Create a FRESH EntityManager for this transaction
        EntityManager em = JPAUtil.createEntityManager();

        try {
            em.getTransaction().begin();

            // 2. Create LOCAL repositories linked to this single EntityManager
            ReservationRepository localResRepo = new ReservationRepository(em);
            GuestRepository localGuestRepo = new GuestRepository(em);
            FeedbackRepository localFeedbackRepo = new FeedbackRepository(em);

            // 3. Find Dependencies (Managed by 'em')
            if (reservationId == null) throw new IllegalArgumentException("Reservation ID is required");

            Reservation reservation = localResRepo.findById(reservationId)
                    .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

            // Get Guest (attached to 'em' via reservation)
            Guest guest = reservation.getGuest();
            if (guest == null) {
                guest = localGuestRepo.findByEmail(guestEmail)
                        .orElseThrow(() -> new IllegalArgumentException("Guest not found: " + guestEmail));
            }

            // 4. Create and Save Feedback
            Feedback feedback = new Feedback(reservation, guest, rating, comments);
            feedback.autoAssignBasicSentiment();

            // Save feedback (uses 'em')
            localFeedbackRepo.save(feedback);

            // Link back to reservation (optional, but good for data consistency)
            reservation.setFeedback(feedback);
            localResRepo.save(reservation);

            // 5. Commit Transaction
            em.getTransaction().commit();
            LOGGER.info("Feedback submitted successfully for Reservation #" + reservation.getConfirmationNumber());

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            LOGGER.log(Level.SEVERE, "Failed to submit feedback", e);
            throw e; // Re-throw so controller knows it failed
        } finally {
            em.close(); // Clean up
        }
    }

    /**
     * Get all feedback for Admin Viewer.
     */
    public List<Feedback> findAll() {
        return feedbackRepository.findAll();
    }
}