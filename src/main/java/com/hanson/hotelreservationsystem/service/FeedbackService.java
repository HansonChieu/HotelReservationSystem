package com.hanson.hotelreservationsystem.service;

import com.hanson.hotelreservationsystem.config.JPAUtil;
import com.hanson.hotelreservationsystem.model.Feedback;
import com.hanson.hotelreservationsystem.model.Guest;
import com.hanson.hotelreservationsystem.model.Reservation;
import com.hanson.hotelreservationsystem.repository.FeedbackRepository;
import com.hanson.hotelreservationsystem.repository.GuestRepository;
import com.hanson.hotelreservationsystem.repository.ReservationRepository;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class FeedbackService {

    private static final Logger LOGGER = Logger.getLogger(FeedbackService.class.getName());
    private static FeedbackService instance;

    private final FeedbackRepository feedbackRepository;
    private final ReservationRepository reservationRepository;
    private final GuestRepository guestRepository;

    private FeedbackService() {
        this.feedbackRepository = FeedbackRepository.getInstance();
        this.reservationRepository = ReservationRepository.getInstance();
        this.guestRepository = GuestRepository.getInstance();

        // Ensure repositories have EntityManager
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
     */
    public void submitFeedback(Long reservationId, String guestEmail, int rating, String comments) {
        // 1. Validate inputs
        if (reservationId == null) {
            throw new IllegalArgumentException("Reservation ID is required");
        }

        // 2. Find dependencies
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        // We can use the guest from the reservation directly to ensure it matches
        Guest guest = reservation.getGuest();
        if (guest == null) {
            // Fallback lookup if reservation guest is somehow null
            guest = guestRepository.findByEmail(guestEmail)
                    .orElseThrow(() -> new IllegalArgumentException("Guest not found: " + guestEmail));
        }

        // 3. Create Feedback
        Feedback feedback = new Feedback(reservation, guest, rating, comments);

        // 4. Run business logic (Sentiment Analysis)
        feedback.autoAssignBasicSentiment();

        // 5. Save
        feedbackRepository.save(feedback);

        // 6. Update reservation to link this feedback (optional, if bidirectional)
        reservation.setFeedback(feedback);
        reservationRepository.save(reservation);

        LOGGER.info("Feedback submitted for Reservation #" + reservation.getConfirmationNumber());
    }

    /**
     * Get all feedback for Admin Viewer.
     */
    public List<Feedback> findAll() {
        return feedbackRepository.findAll();
    }
}