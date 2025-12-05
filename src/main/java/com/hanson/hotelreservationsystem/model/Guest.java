package com.hanson.hotelreservationsystem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a hotel guest.
 * Contains personal information and links to reservations and loyalty account.
 */
@Entity
@Table(name = "guests", indexes = {
        @Index(name = "idx_guest_email", columnList = "email"),
        @Index(name = "idx_guest_phone", columnList = "phone")
})
public class Guest extends BaseEntity {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Please provide a valid phone number")
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Size(max = 200, message = "Address cannot exceed 200 characters")
    @Column(name = "address", length = 200)
    private String address;

    @Size(max = 50, message = "City cannot exceed 50 characters")
    @Column(name = "city", length = 50)
    private String city;

    @Size(max = 50, message = "Country cannot exceed 50 characters")
    @Column(name = "country", length = 50)
    private String country;

    @Size(max = 20, message = "Postal code cannot exceed 20 characters")
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "id_type", length = 50)
    private String idType; // Passport, Driver's License, etc.

    @Column(name = "id_number", length = 50)
    private String idNumber;

    @Column(name = "is_loyalty_member")
    private boolean loyaltyMember = false;

    @Size(max = 50, message = "State/Province cannot exceed 50 characters")
    @Column(name = "state_province", length = 50)
    private String stateProvince;

    // Relationships
    @OneToMany(mappedBy = "guest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Reservation> reservations = new ArrayList<>();

    @OneToOne(mappedBy = "guest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private LoyaltyAccount loyaltyAccount;

    @OneToMany(mappedBy = "guest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Feedback> feedbacks = new ArrayList<>();

    // Constructors
    public Guest() {}

    public Guest(String firstName, String lastName, String email, String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
    }

    // Business Methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void enrollInLoyaltyProgram() {
        if (!loyaltyMember) {
            this.loyaltyMember = true;
            if (this.loyaltyAccount == null) {
                this.loyaltyAccount = new LoyaltyAccount(this);
            }
        }
    }

    // Getters and Setters
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public boolean isLoyaltyMember() {
        return loyaltyMember;
    }

    public void setLoyaltyMember(boolean loyaltyMember) {
        this.loyaltyMember = loyaltyMember;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    public LoyaltyAccount getLoyaltyAccount() {
        return loyaltyAccount;
    }

    public void setLoyaltyAccount(LoyaltyAccount loyaltyAccount) {
        this.loyaltyAccount = loyaltyAccount;
    }

    public List<Feedback> getFeedbacks() {
        return feedbacks;
    }

    public void setFeedbacks(List<Feedback> feedbacks) {
        this.feedbacks = feedbacks;
    }

    public String getStateProvince() {
        return stateProvince;
    }

    public void setStateProvince(String stateProvince) {
        this.stateProvince = stateProvince;
    }

    @Override
    public String toString() {
        return "Guest{" +
                "id=" + getId() +
                ", name='" + getFullName() + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", loyaltyMember=" + loyaltyMember +
                '}';
    }
}
