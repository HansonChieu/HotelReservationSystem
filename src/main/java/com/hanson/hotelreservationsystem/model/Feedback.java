package com.hanson.hotelreservationsystem.model;

import com.hanson.hotelreservationsystem.model.enums.SentimentTag;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing guest feedback after checkout.
 * Links to reservation and guest for tracking and analysis.
 */
@Entity
@Table(name = "feedbacks", indexes = {
        @Index(name = "idx_feedback_reservation", columnList = "reservation_id"),
        @Index(name = "idx_feedback_guest", columnList = "guest_id"),
        @Index(name = "idx_feedback_rating", columnList = "rating"),
        @Index(name = "idx_feedback_date", columnList = "submitted_at")
})
public class Feedback extends BaseEntity {

    @NotNull(message = "Reservation is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @NotNull(message = "Guest is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Size(max = 1000, message = "Comments cannot exceed 1000 characters")
    @Column(name = "comments", length = 1000)
    private String comments;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "feedback_sentiment_tags", joinColumns = @JoinColumn(name = "feedback_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment_tag")
    private Set<SentimentTag> sentimentTags = new HashSet<>();

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "submitted_via_kiosk")
    private boolean submittedViaKiosk = false;

    // Optional detailed ratings
    @Min(value = 1, message = "Cleanliness rating must be at least 1")
    @Max(value = 5, message = "Cleanliness rating cannot exceed 5")
    @Column(name = "cleanliness_rating")
    private Integer cleanlinessRating;

    @Min(value = 1, message = "Service rating must be at least 1")
    @Max(value = 5, message = "Service rating cannot exceed 5")
    @Column(name = "service_rating")
    private Integer serviceRating;

    @Min(value = 1, message = "Comfort rating must be at least 1")
    @Max(value = 5, message = "Comfort rating cannot exceed 5")
    @Column(name = "comfort_rating")
    private Integer comfortRating;

    @Min(value = 1, message = "Value rating must be at least 1")
    @Max(value = 5, message = "Value rating cannot exceed 5")
    @Column(name = "value_rating")
    private Integer valueRating;

    @Min(value = 1, message = "Location rating must be at least 1")
    @Max(value = 5, message = "Location rating cannot exceed 5")
    @Column(name = "location_rating")
    private Integer locationRating;

    @Column(name = "would_recommend")
    private Boolean wouldRecommend;

    // Constructors
    public Feedback() {
        this.submittedAt = LocalDateTime.now();
    }

    public Feedback(Reservation reservation, Guest guest, int rating) {
        this();
        this.reservation = reservation;
        this.guest = guest;
        this.rating = rating;
    }

    public Feedback(Reservation reservation, Guest guest, int rating, String comments) {
        this(reservation, guest, rating);
        this.comments = comments;
    }

    // Business Methods

    /**
     * Adds a sentiment tag to this feedback.
     */
    public void addSentimentTag(SentimentTag tag) {
        sentimentTags.add(tag);
    }

    /**
     * Removes a sentiment tag from this feedback.
     */
    public void removeSentimentTag(SentimentTag tag) {
        sentimentTags.remove(tag);
    }

    /**
     * Automatically assigns sentiment tags based on rating.
     */
    public void autoAssignBasicSentiment() {
        if (rating >= 4) {
            sentimentTags.add(SentimentTag.POSITIVE);
        } else if (rating == 3) {
            sentimentTags.add(SentimentTag.NEUTRAL);
        } else {
            sentimentTags.add(SentimentTag.NEGATIVE);
        }
    }

    /**
     * Calculates the average of all detailed ratings if available.
     */
    public Double getAverageDetailedRating() {
        int count = 0;
        int sum = 0;

        if (cleanlinessRating != null) { sum += cleanlinessRating; count++; }
        if (serviceRating != null) { sum += serviceRating; count++; }
        if (comfortRating != null) { sum += comfortRating; count++; }
        if (valueRating != null) { sum += valueRating; count++; }
        if (locationRating != null) { sum += locationRating; count++; }

        return count > 0 ? (double) sum / count : null;
    }

    /**
     * Checks if this is a positive feedback (4 or 5 stars).
     */
    public boolean isPositive() {
        return rating >= 4;
    }

    /**
     * Checks if this is a negative feedback (1 or 2 stars).
     */
    public boolean isNegative() {
        return rating <= 2;
    }

    // Getters and Setters
    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public Guest getGuest() {
        return guest;
    }

    public void setGuest(Guest guest) {
        this.guest = guest;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Set<SentimentTag> getSentimentTags() {
        return sentimentTags;
    }

    public void setSentimentTags(Set<SentimentTag> sentimentTags) {
        this.sentimentTags = sentimentTags;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public boolean isSubmittedViaKiosk() {
        return submittedViaKiosk;
    }

    public void setSubmittedViaKiosk(boolean submittedViaKiosk) {
        this.submittedViaKiosk = submittedViaKiosk;
    }

    public Integer getCleanlinessRating() {
        return cleanlinessRating;
    }

    public void setCleanlinessRating(Integer cleanlinessRating) {
        this.cleanlinessRating = cleanlinessRating;
    }

    public Integer getServiceRating() {
        return serviceRating;
    }

    public void setServiceRating(Integer serviceRating) {
        this.serviceRating = serviceRating;
    }

    public Integer getComfortRating() {
        return comfortRating;
    }

    public void setComfortRating(Integer comfortRating) {
        this.comfortRating = comfortRating;
    }

    public Integer getValueRating() {
        return valueRating;
    }

    public void setValueRating(Integer valueRating) {
        this.valueRating = valueRating;
    }

    public Integer getLocationRating() {
        return locationRating;
    }

    public void setLocationRating(Integer locationRating) {
        this.locationRating = locationRating;
    }

    public Boolean getWouldRecommend() {
        return wouldRecommend;
    }

    public void setWouldRecommend(Boolean wouldRecommend) {
        this.wouldRecommend = wouldRecommend;
    }

    @Override
    public String toString() {
        return "Feedback{" +
                "reservationId=" + (reservation != null ? reservation.getId() : "null") +
                ", guest=" + (guest != null ? guest.getFullName() : "null") +
                ", rating=" + rating +
                ", submittedAt=" + submittedAt +
                '}';
    }
}
