package com.hanson.hotelreservationsystem.model;

import com.hanson.hotelreservationsystem.model.enums.AddOnType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "reservation_addons")
public class ReservationAddOn extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Enumerated(EnumType.STRING)
    @Column(name = "addon_type", nullable = false)
    private AddOnType addOnType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "date_added")
    private LocalDate dateAdded;

    public LocalDate getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDate dateAdded) {
        this.dateAdded = dateAdded;
    }

    // Constructor, getters, setters

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public void setAddOnType(AddOnType addOnType) {
        this.addOnType = addOnType;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public AddOnType getAddOnType() {
        return addOnType;
    }
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    public Integer getQuantity(){
        return quantity;
    }
}